# Build & Deployment

## 빌드 명령어

```bash
# 빌드 (테스트 포함)
./gradlew build

# 테스트만 실행
./gradlew test

# 테스트 스킵하고 빌드
./gradlew build -x test

# 실행 가능한 JAR 생성
./gradlew bootJar

# 애플리케이션 실행
./gradlew bootRun
```

## CI/CD (GitHub Actions)

### CI - Pull Request 시
1. Java 21 환경 설정
2. 프로젝트 체크아웃
3. 테스트 실행 (`./gradlew test`)
4. 테스트 결과 게시

### CD - main 브랜치 병합 시
1. Java 21 환경 설정
2. `bootJar` 빌드
3. SCP로 JAR 파일 서버 전송
4. SSH 접속하여 배포:
   - 기존 프로세스 종료
   - 새 JAR 파일 실행
   - 프로세스 실행 확인

## 환경 변수

```bash
export PIXABAY_KEY=your_pixabay_api_key
export DISCORD_WEBHOOK_URL=your_discord_webhook_url
```

## systemd 서비스 설정

`/etc/systemd/system/4d4cat-services.service`:

```ini
[Unit]
Description=4d4cat Services

[Service]
User=deploy
WorkingDirectory=/home/deploy/app
ExecStart=/usr/bin/java -jar /home/deploy/app/4d4cat-services.jar
Environment="PIXABAY_KEY=your_key"
Environment="DISCORD_WEBHOOK_URL=your_url"
Restart=always

[Install]
WantedBy=multi-user.target
```

## 서비스 관리

```bash
sudo systemctl start 4d4cat-services
sudo systemctl stop 4d4cat-services
sudo systemctl restart 4d4cat-services
sudo systemctl status 4d4cat-services
sudo journalctl -u 4d4cat-services -f  # 로그 확인
```

## GitHub Secrets

| Secret 이름 | 설명 |
|------------|------|
| `SERVER_HOST` | 배포 서버 호스트 |
| `SERVER_USER` | SSH 사용자명 |
| `SERVER_SSH_KEY` | SSH Private Key |
| `PIXABAY_KEY` | Pixabay API 키 |
| `DISCORD_WEBHOOK_URL` | Discord 웹훅 URL |

## 배포 전 체크리스트

- [ ] 모든 테스트가 통과하는가?
- [ ] 환경 변수가 올바르게 설정되었는가?
- [ ] 코드 리뷰가 완료되었는가?
- [ ] 서버 디스크 용량이 충분한가?
- [ ] 롤백 계획이 수립되었는가?
