# Redis 레이턴시 진단 가이드

## 1. 컨테이너에서 Redis 연결 테스트

```bash
# 운영 서버에서 실행
sudo docker exec -it 4d4cat-data sh

# Redis 연결 테스트 (컨테이너 내부에서)
time redis-cli -h host.docker.internal PING

# 반복 테스트로 레이턴시 확인
for i in {1..10}; do
  time redis-cli -h host.docker.internal PING
done
```

## 2. Redis 성능 모니터링

```bash
# Redis slowlog 확인
sudo docker exec -it 4d4cat-redis redis-cli SLOWLOG GET 10

# Redis 정보 확인
sudo docker exec -it 4d4cat-redis redis-cli INFO stats
sudo docker exec -it 4d4cat-redis redis-cli INFO commandstats
sudo docker exec -it 4d4cat-redis redis-cli INFO persistence

# 현재 연결 수 확인
sudo docker exec -it 4d4cat-redis redis-cli CLIENT LIST
```

## 3. 네트워크 레이턴시 측정

```bash
# 호스트에서 Redis 응답 시간 (비교용)
time redis-cli -h localhost PING

# 컨테이너에서 호스트로 핑
sudo docker exec -it 4d4cat-data ping -c 10 host.docker.internal
```

## 4. 리소스 사용량 확인

```bash
# CPU, 메모리 사용량
sudo docker stats 4d4cat-data 4d4cat-redis

# 디스크 I/O 확인
iostat -x 1 5
```

## 해결 방안

### 방안 1: Docker 네트워크 사용 (권장)

가장 좋은 방법은 `host.docker.internal` 대신 Docker 네트워크를 사용하는 것입니다.

```yaml
# docker-compose.yml 또는 run 명령 수정
services:
  redis:
    container_name: 4d4cat-redis
    networks:
      - app-network

  data:
    container_name: 4d4cat-data
    environment:
      - REDIS_HOST=4d4cat-redis  # 컨테이너 이름으로 직접 연결
    networks:
      - app-network

networks:
  app-network:
    driver: bridge
```

### 방안 2: Host 네트워크 모드 (간단하지만 격리 없음)

```bash
# Redis
sudo docker run -d \
  --name 4d4cat-redis \
  --network host \
  redis:7-alpine ...

# Data 서비스
sudo docker run -d \
  --name 4d4cat-data \
  --network host \
  -e REDIS_HOST=localhost \
  ...
```

### 방안 3: Redis 타임아웃 조정

일시적 해결책으로 타임아웃을 늘릴 수 있지만, 근본 원인은 해결되지 않습니다.

```java
// RedisConfig.java
SocketOptions socketOptions =
    SocketOptions.builder()
        .connectTimeout(Duration.ofMillis(500))  // 2000 -> 500으로 줄여보기
        .build();

LettucePoolingClientConfiguration clientConfig =
    LettucePoolingClientConfiguration.builder()
        .commandTimeout(Duration.ofMillis(1000))  // 3000 -> 1000으로 줄여보기
        ...
```

### 방안 4: Redis Pipeline 최적화

현재 `RedisDataStorage.setListData`는 pipeline을 사용 중입니다.
조회 시에도 pipeline을 활용할 수 있는지 검토하세요.

## 예상 개선 효과

- **Docker 네트워크 사용**: 50-80% 레이턴시 감소
- **Host 네트워크 모드**: 70-90% 레이턴시 감소
- **타임아웃 조정**: 문제 해결 안됨 (마스킹만 됨)

## 다음 단계

1. 위의 진단 명령어들을 실행하여 정확한 레이턴시 측정
2. Redis slowlog 확인으로 느린 쿼리 파악
3. Docker 네트워크 구성 변경 (방안 1 권장)
4. 변경 후 성능 재측정
