# Skill: Monitoring (Observability & Metrics)

이 스킬은 프로젝트의 가시성과 시스템 건전성(Health) 모니터링 메트릭을 추가하거나 변경할 때 탑재하는 가이드라인입니다.

---

## 1. 도메인 개요 및 비즈니스 핵심
- **목표:** Micrometer, Actuator, Prometheus, Grafana 스택을 활용하여 각 모듈 및 도메인의 실시간 API 트래픽, DB Latency, Redis 캐시 효율성 및 비즈니스 처리량을 모니터링하고 가시성을 확보하는 것을 목표로 합니다.

---

## 2. 주요 관련 파일

### 2.1 monitoring 모듈
- Spring Boot Actuator: 각 서버의 상태 지표를 수집하고 노출 (`/actuator/prometheus`).
- Prometheus: 각 서비스로부터 노출된 메트릭을 Scraping하여 시계열 데이터베이스에 저장 (`prometheus.yml` 설정).
- Grafana: Prometheus를 데이터 소스로 삼아 대시보드 시각화.

### 2.2 core 및 api/data 모듈
- `MeterRegistry` (Micrometer): 메트릭 수집 및 가상 스레드 관련 지표 캡처.

---

## 3. 핵심 처리 지침 & 전문가 규칙

### 3.1 일관된 메트릭 명명 규칙 및 태그(Tag) 최적화
- **명명 규칙:** 새로운 커스텀 메트릭 추가 시 `domain.action.target` (예: `redis.random.access`) 형식을 준수하십시오. 공통 지표는 `core` 모듈의 유틸리티나 AOP를 통해 수집되도록 합니다.
- **태그 활용:** `application`, `type`, `status` 등의 태그를 사용하되, **카디널리티(Cardinality)가 너무 높은 값(사용자 ID, UUID 등)을 태그로 사용하는 것은 메모리 누수를 유발하므로 절대 피하십시오.**

### 3.2 Actuator 보안 관리
- 운영 환경에서는 필요한 엔드포인트(`health`, `prometheus`)만 선택적으로 노출하고, `env`, `configprops` 등은 노출 범위를 엄격히 차단하십시오.

### 3.3 신규 서비스 필수 메트릭 표준 4종 (Required Metrics)
신규 도메인/서비스를 구현할 때는 가시성 확보를 위해 다음 4가지 핵심 지표를 반드시 포함하여 설계해야 합니다.
1. **API Traffic (`domain.api.request`):** 서비스 호출 빈도 분석. (태그: `has_filter`, `type`, `status`)
2. **Cache Efficiency (`domain.cache.access`):** Redis 캐시 계층 효율성. (태그: `status` - hit/miss)
3. **DB Performance (`domain.query.duration`):** 캐시 미스 시 실 DB 조회 쿼리 Latency. (Timer 사용)
4. **Business Volume (`domain.action.target`):** 클릭, 전송, 수집 등 핵심 액션의 볼륨 측정.

---

## 4. 자가 검증 체크리스트
- [ ] 신규 메트릭이 `/actuator/prometheus`에 정상적으로 노출되는가?
- [ ] Prometheus가 해당 타겟을 성공적으로 스크랩하고 있는가 (UP 상태 확인)?
- [ ] 메트릭의 태그 구성이 다차원 분석에 적합하며 카디널리티가 지나치게 높지 않은가?
- [ ] 가상 스레드 관련 지표(Virtual Threads count 등)가 수집되고 있는가?

---

## 5. 테스트 검증 전략 (Testing Strategy)
- **Mocking 대상:** `MeterRegistry`
- **검증 포인트:**
  - `@SpringBootTest`를 활용하여 `/actuator/prometheus` 엔드포인트가 정상적으로 200 OK를 반환하고 커스텀 메트릭 포맷이 포함되는지 검증합니다.
  - 비즈니스 로직(예: Redis Random Access) 실행 시 `MeterRegistry.counter().increment()`가 정확히 호출되어 카운트가 증가하는지 확인합니다.
