# Build & Deployment

## 빌드 명령어

### 전체 빌드
```bash
# 빌드 (테스트 포함)
./gradlew build

# 테스트만 실행
./gradlew test

# 테스트 스킵하고 빌드
./gradlew build -x test

# 코드 포맷팅
./gradlew spotlessApply
```

### 모듈별 빌드
```bash
# Core 모듈
./gradlew :core:build

# Data Server JAR 생성
./gradlew :data:bootJar
# 결과: data/build/libs/data.jar

# API Server JAR 생성
./gradlew :api:bootJar
# 결과: api/build/libs/api.jar
```

### 로컬 실행
```bash
# Docker Compose로 전체 실행 (Redis + Data Server + API Server)
docker-compose up -d

# 개별 모듈 실행
./gradlew :data:bootRun
./gradlew :api:bootRun
```

## Docker 배포

### 이미지 구조
```
4d4cat-services/
├── docker-compose.yml        # 로컬 개발용 (전체)
├── docker-compose.data.yml   # Data Server 배포용
├── data/
│   └── Dockerfile
└── api/
    └── Dockerfile
```

### Docker Compose 실행
```bash
# 로컬 전체 실행
docker-compose up -d

# Data Server만 실행 (새 서버)
docker-compose -f docker-compose.data.yml up -d

# 로그 확인
docker-compose logs -f
```

### Redis 관리
```bash
# Redis CLI 접속
docker exec -it 4d4cat-redis redis-cli

# 키 확인
KEYS *

# 데이터 확인
LRANGE pixabayVideos 0 10
GET message:last
```

## CI/CD (GitHub Actions)

### CI - Pull Request 시 (`ci.yml`)
1. Java 21 환경 설정
2. 프로젝트 체크아웃
3. 전체 테스트 실행 (`./gradlew test`)
4. 테스트 결과 게시

### CD - API Server (`cd-oci.yml`)
**트리거:** main 브랜치 병합 시
**대상:** Oracle 서버 2대

1. Java 21 환경 설정
2. `./gradlew :api:bootJar` 빌드
3. Docker 이미지 빌드 및 푸시
4. SSH로 Oracle 서버에 배포

### CD - Data Server (`cd-data.yml`)
**트리거:** main 병합 시 (core/, data/ 변경 시)
**대상:** Data Server 1대

1. Java 21 환경 설정
2. `./gradlew :data:bootJar` 빌드
3. Docker 이미지 빌드 및 푸시
4. SSH로 Data Server에 배포
5. Redis 컨테이너 확인/실행

## 환경 변수

### Data Server (.env)
```bash
PIXABAY_KEY=your_pixabay_api_key
PIXABAY_VIDEO_URL=https://pixabay.com/api/videos/
PIXABAY_MUSIC_URL=https://pixabay.com/api/music/
REDIS_HOST=localhost
REDIS_PORT=6379
```

### API Server (.env)
```bash
REDIS_HOST=<data-server-ip>
REDIS_PORT=6379
CORS_ALLOWED_ORIGINS=https://yourdomain.com
DISCORD_WEBHOOK_URL=your_discord_webhook_url
OMNIWATCH_DB_URL=jdbc:mysql://localhost:3306/omniwatch
OMNIWATCH_DB_USERNAME=root
OMNIWATCH_DB_PASSWORD=password
```

## GitHub Secrets

| Secret 이름 | 설명 |
|------------|------|
| `DOCKER_HUB_USERNAME` | Docker Hub 사용자명 |
| `DOCKER_HUB_TOKEN` | Docker Hub 액세스 토큰 |
| `ORACLE_MAIN_IP` | Oracle 메인 서버 IP |
| `ORACLE_SUB_IP` | Oracle 서브 서버 IP |
| `ORACLE_USER` | Oracle 서버 SSH 사용자명 |
| `ORACLE_KEY` | Oracle 서버 SSH Private Key |
| `DATA_SERVER_IP` | Data Server IP |
| `DATA_SERVER_USER` | Data Server SSH 사용자명 |
| `DATA_SERVER_KEY` | Data Server SSH Private Key |
| `PIXABAY_KEY` | Pixabay API 키 |
| `SUBMODULE_TOKEN` | 서브모듈 접근용 토큰 |

## 서버 구성

### Data Server (1대)
- Redis 컨테이너
- data-server 컨테이너
- Port: 6379 (Redis), 8081 (Data Server)

### API Server (N대, Oracle)
- api-server 컨테이너
- Port: 8080
- REDIS_HOST로 Data Server IP 설정

## 배포 전 체크리스트

- [ ] 모든 테스트가 통과하는가?
- [ ] 환경 변수가 올바르게 설정되었는가?
- [ ] 코드 리뷰가 완료되었는가?
- [ ] Redis 연결이 정상인가?
- [ ] Data Server가 정상 동작하는가?
- [ ] 롤백 계획이 수립되었는가?

## 롤백

```bash
# 이전 버전 이미지로 롤백 (Oracle 서버에서)
sudo docker stop 4d4cat-api
sudo docker rm 4d4cat-api
sudo docker run -d \
  --name 4d4cat-api \
  -p 8080:8080 \
  --env-file /home/opc/.env \
  username/4d4cat-api:<previous-sha>
```
