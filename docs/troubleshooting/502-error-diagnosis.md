# 502 에러 진단 및 해결

## 문제 상황
- 외부에서 api.4d4cat.site 접근 시 502 Bad Gateway 에러 발생
- DNS에 Main IP와 Sub IP 두 개 설정됨

## 원인
DNS 라운드 로빈으로 Sub IP로 접근 시, Sub 서버에 nginx가 없어서 502 에러 발생

## 해결 방법

### 1단계: DNS 설정 확인
```bash
# 현재 DNS 설정 확인
dig api.4d4cat.site +short
```

### 2단계: DNS 수정 (권장)
**DNS에서 Main IP만 A 레코드로 설정**

Before:
```
api.4d4cat.site.  A  <MAIN_IP>
api.4d4cat.site.  A  <SUB_IP>
```

After:
```
api.4d4cat.site.  A  <MAIN_IP>
```

### 3단계: 각 서버 상태 확인

**Main 서버 (nginx 있음):**
```bash
ssh opc@<MAIN_IP>

# nginx 상태
sudo systemctl status nginx
sudo nginx -t

# 컨테이너 상태
sudo docker ps | grep 4d4cat-api

# 포트 확인
sudo netstat -tlnp | grep -E ':(80|443|8080)'

# 로컬 테스트
curl http://localhost:8080
```

**Sub 서버 (nginx 없음, 컨테이너만):**
```bash
ssh opc@<SUB_IP>

# 컨테이너 상태
sudo docker ps | grep 4d4cat-api

# 포트 확인
sudo netstat -tlnp | grep 8080

# 로컬 테스트
curl http://localhost:8080
```

### 4단계: Nginx Upstream 확인

Main 서버의 nginx.conf:
```nginx
upstream api_servers {
    server localhost:8080;           # Main 서버 컨테이너
    server <SUB_IP>:8080;           # Sub 서버 컨테이너
}
```

**Sub IP가 정확한지 확인**:
```bash
# Main 서버에서 Sub 서버 연결 테스트
curl -v http://<SUB_IP>:8080

# Telnet으로 포트 확인
telnet <SUB_IP> 8080
```

### 5단계: 방화벽 확인

Sub 서버의 8080 포트가 Main 서버에서 접근 가능한지 확인:

```bash
# Sub 서버에서
sudo firewall-cmd --list-all
sudo iptables -L -n | grep 8080

# 8080 포트가 차단되어 있다면
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

## 테스트

### DNS 수정 후 테스트
```bash
# DNS 변경 전파 확인 (최대 TTL 시간)
dig api.4d4cat.site +short

# 외부에서 API 테스트
curl -v https://api.4d4cat.site
curl -v https://api.4d4cat.site/video
curl -v https://api.4d4cat.site/music
```

### Nginx 로그 모니터링
```bash
# Main 서버
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# upstream 연결 확인
sudo grep "upstream" /var/log/nginx/error.log | tail -20
```

## 예상 결과

DNS를 Main IP만 설정하면:
```
Client → api.4d4cat.site (Main IP)
       → nginx (Main 서버)
       → upstream 로드밸런싱
          ├─ localhost:8080 (Main 컨테이너)
          └─ <SUB_IP>:8080 (Sub 컨테이너)
```

## 롤백 계획

문제 발생 시:
1. DNS를 원래대로 복구
2. Main 서버 nginx 로그 확인
3. Sub 서버 컨테이너 상태 확인

## 추가 개선 사항

향후 고려사항:
- Sub 서버에도 nginx 설치 (Active-Active 구성)
- Health check endpoint 추가
- Prometheus + Grafana 모니터링 설정
