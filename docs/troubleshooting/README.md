# 트러블슈팅 가이드

이 디렉토리는 4d4cat-services 프로젝트에서 발생한 주요 이슈와 해결 방법을 정리한 문서들을 포함합니다.

## 목차

### 1. Redis 관련

#### [어드민 대량 처리로 인한 레디스 부하 최적화](./redis-bulk-operation-optimization.md)
**상태**: ✅ 해결 완료
**증상**: 지원서 다건 승인/거절 시 레디스 연결 대기 및 응답 지연 발생
**원인**: 부적절한 커넥션 풀 설정, O(N) 개별 이벤트 발행, 트랜잭션 분리 미흡
**해결**: `pushAll` 기반 배치 처리, 커넥션 설정 튜닝(Virtual Thread 최적화), Self-Injection 활용

#### [Redis 레이턴시 이슈 해결](./redis-latency-issue-resolution.md)
**상태**: ✅ 해결 완료 (배포 대기)
**증상**: 운영 환경에서 Redis 조회 시 1~5초 딜레이 발생
**원인**: Docker `host.docker.internal` 사용으로 인한 네트워크 오버헤드
**해결**: Docker 네트워크 사용 및 docker-compose 전환
**개선율**: ~95% (1~5초 → ~50-100ms)

**핵심 내용**:
- 문제 분석 및 원인 파악
- Docker 네트워크 구성 변경
- cd-data.yml 배포 스크립트 개선
- 성능 측정 및 검증 방법
#### [Redis 레이턴시 진단 가이드](./redis-latency-diagnosis.md)
**상태**: 📖 참고 문서
**목적**: Redis 성능 문제 진단 및 측정 방법 제공

#### [Redis 커넥션 풀 최적화 가이드](./redis-connection-pool-optimization.md)
**상태**: 📖 가이드 문서
**목적**: 가상 스레드 환경에서의 Redis 연결 풀 조정 기준 제공

#### [Redis 직렬화 에러 및 폼 제출 무반응 이슈](./redis-serialization-error.md)
**상태**: ✅ 해결 완료
**증상**: 캐시 적용 API 호출 시 500 에러 및 프론트엔드 폼 무반응
**원인**: BaseResponse 및 프로젝션의 직렬화 부재, 프론트엔드 에러 피드백 부족
**해결**: Serializable DTO 도입 및 UI 에러 메시지 강화

#### [Redis 다형성 직렬화 및 통계 배치 최적화](./redis-serialization-and-batch-optimization.md)
**상태**: ✅ 해결 완료
**증상**: 복합 객체(Generic, Slice) 직렬화 실패 및 배치 집계 데이터 정합성 이슈
**원인**: Jackson 다형성 설정 불일치, 캐시 무효화 로직 부재, 배치 SQL 구조 결함
**해결**: PageResponse 도입, As.PROPERTY 타입 설정, 배치 리스너 기반 캐시 삭제 구현

---
**핵심 내용**:
- Redis 연결 테스트 명령어
- 성능 모니터링 (slowlog, INFO, CLIENT LIST)
- 네트워크 레이턴시 측정
- 리소스 사용량 확인
- 해결 방안 (Docker 네트워크, Host 모드, 타임아웃 조정 등)

---

### 2. 배포 및 수집 관련

#### [RSS 피드 수집 403 에러 및 테스트 안정성 개선](./rss-feed-collection-failure.md)
**상태**: ✅ 해결 완료
**증상**: RSS 수집 시 403 차단 및 CI 테스트 실패
**원인**: 헤더 부재 및 HttpClient 하드코딩으로 인한 테스트 불가능 구조
**해결**: HttpClient 생성자 주입 및 Mocking을 통한 안정화

#### [502 에러 진단 가이드](./502-error-diagnosis.md)
**상태**: 📖 참고 문서
**증상**: API 서버 배포 후 502 Bad Gateway 에러 발생
**원인**: 다양 (컨테이너 미실행, 포트 바인딩 실패, 헬스체크 실패 등)

**핵심 내용**:
- 502 에러 원인 분석
- 컨테이너 상태 확인
- 로그 분석 방법
- 네트워크 및 포트 확인

---

### 3. 모니터링 및 보안 관련

#### [Spring Security 6 계층형 권한 이슈 해결](./spring-security-6-role-hierarchy-fix.md)
**상태**: ✅ 해결 완료
**증상**: ADMIN 권한자가 USER 권한 API 접근 시 403 Forbidden 발생
**원인**: Spring Security 6에서 RoleHierarchy 자동 적용 방식 변화
**해결**: WebSecurityExpressionHandler 설정 및 JWT 필터 내 권한 확장 적용

#### [ApplyDays 보안 구현 보고서](./applydays-security-implementation.md)
**상태**: 📖 참고 문서
**목적**: JWT 및 Role Hierarchy 도입 과정 기록

#### [Prometheus 및 Grafana 진단 가이드]
**상태**: 📖 참고 문서
**목적**: 애플리케이션 및 시스템 모니터링 문제 진단 및 확인 방법 제공

**핵심 내용**:
- **Prometheus UI 확인**: `http://localhost:9090` 접속하여 `Status -> Targets` 메뉴에서 `api-service` 및 `monitoring` 서비스의 상태 확인. `UP` 상태여야 메트릭이 정상적으로 수집되고 있음을 의미합니다.
- **Grafana 대시보드 확인**: `http://localhost:3000` 접속 후 Prometheus 데이터 소스가 올바르게 연결되어 있는지 확인하고, 관련 대시보드에서 메트릭 시각화 확인.

### 4. 데이터베이스 및 JPA 관련

#### [HikariCP 커넥션 누수 오탐지 경고 (Apparent Connection Leak Detected) 트러블슈팅](./hikari-apparent-connection-leak-warning.md)
**상태**: ✅ 해결 완료
**증상**: 지원서 등록 시 `com.zaxxer.hikari.pool.ProxyLeakTask` 커넥션 누수 의심 로그 출력
**원인**: `leak-detection-threshold` 설정이 지나치게 짧음(3초), 일시적 DB/네트워크 지연 시 오탐지
**해결**: `leak-detection-threshold` 값을 30초(`30000`ms)로 완화

#### [JPA 벌크 업데이트 영속성 컨텍스트 분리 버그 (clearAutomatically 이슈)](./jpa-bulk-update-side-effect.md)
**상태**: ✅ 해결 완료
**증상**: 엔티티 상태 변경이 DB에 커밋되지 않는 현상 (더티 체킹 실패)
**원인**: `@Modifying(clearAutomatically = true)` 사용으로 인한 영속성 컨텍스트 강제 초기화 및 엔티티 준영속화
**해결**: `clearAutomatically = false, flushAutomatically = true` 조합으로 변경 및 수동 상태 동기화

---

### 5. 프론트엔드 및 React 관련

#### [React Query Cache 충돌 및 인증 로딩 UX 개선](./react-query-cache-and-auth-ux-fix.md)
**상태**: ✅ 해결 완료
**증상**: 페이지 전환 시 렌더링 에러(하얀 화면) 및 통계 데이터가 순간적으로 '??'로 표기되는 현상.
**원인**: 동일한 QueryKey 사용으로 인한 데이터 캐시 오염 및 인증 상태 로딩 처리 누락.

---

## 이슈 분류

### 🔴 Critical (서비스 중단)
- 502 에러 (API 서버 미응답)

### 🟡 Performance (성능 저하)
- Redis 레이턴시 (1~5초 딜레이)

### 🟢 Resolved (해결 완료)
- Redis 레이턴시 이슈 → Docker 네트워크 전환으로 해결

---

## 새로운 이슈 추가 가이드

트러블슈팅 문서를 작성할 때 다음 구조를 따르세요:

```markdown
# [이슈 제목]

## 문제 상황
- 증상
- 발생 시점
- 영향 범위

## 원인 분석
- 로그 분석
- 코드 분석
- 환경 차이

## 해결 방법
- 최종 선택한 방법
- 구현 상세
- 변경 사항

## 예상 개선 효과
- 성능 비교 테이블
- 추가 이점

## 배포 및 검증
- 로컬 테스트
- 운영 배포
- 검증 방법

## 트러블슈팅
- 추가 문제 및 해결

## 추가 최적화 고려사항
- 향후 개선 아이디어

## 참고 자료
- 관련 파일 링크
- 외부 문서
```

---

## 관련 디렉토리

- [프로젝트 지침](../../AGENTS.md) - 저장소 공통 개발 정책
- [워크플로우](../../.github/workflows/) - CI/CD 파이프라인
- [Docker 구성](../../docker-compose.yml) - 컨테이너 설정

---

**마지막 업데이트**: 2026-04-11
