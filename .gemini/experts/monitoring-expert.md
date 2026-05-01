# Monitoring Expert Skill Guide

가시성 확보와 시스템 안정성을 위한 모니터링 구성 지침입니다.

## 🧠 핵심 처리 지침 (Core Instructions)

### 1. 일관된 메트릭 명명 규칙
- 새로운 커스텀 메트릭 추가 시 `domain.action.target` (예: `redis.random.access`) 형식을 준수하십시오.
- 공통 지표는 `core` 모듈의 유틸리티 클래스나 AOP를 통해 일괄 수집되도록 설계하십시오.

### 2. 태그(Tag) 활용 최적화
- 메트릭 분석을 용이하게 하기 위해 `application`, `type`, `status` 등의 태그를 적극 활용하십시오.
- 단, 카디널리티(Cardinality)가 너무 높은 값(예: 사용자 ID, UUID)을 태그로 사용하는 것은 피하십시오.

### 3. Actuator 보안 및 노출 관리
- 운영 환경에서는 필요한 엔드포인트(`health`, `prometheus`)만 선택적으로 노출하십시오.
- 민감한 정보가 포함될 수 있는 `env`, `configprops` 등은 노출 범위를 엄격히 제한하십시오.

### 4. 대시보드 정렬 및 알람 설정
- Grafana 대시보드 구성 시 서비스별 핵심 지표(Golden Signals: Latency, Traffic, Errors, Saturation)를 우선 배치하십시오.
- 임계치 초과 시 알람이 Discord 등 채널로 전송되도록 Prometheus Alertmanager 설정을 검토하십시오.

### 5. 신규 서비스 필수 메트릭 표준 (Required Metrics)
신규 도메인/서비스 구현 시, 가시성 확보를 위해 다음 4가지 핵심 지표를 반드시 포함하십시오.

1. **API Traffic (`domain.api.request`)**
   - 목적: 서비스 호출 빈도 및 필터 사용 패턴 분석
   - 태그: `has_filter`, `type`, `status` 등 활용
2. **Cache Efficiency (`domain.cache.access`)**
   - 목적: Redis 등 캐시 계층의 효율성 및 DB 부하 전이 측정
   - 태그: `status` (hit/miss)
3. **DB Performance (`domain.query.duration`)**
   - 목적: 캐시 미스 시 발생하는 실 DB 조회 쿼리의 Latency 모니터링 (Timer 사용)
4. **Business Volume (`domain.action.target`)**
   - 목적: 비즈니스 핵심 액션(예: 클릭, 전송, 수집)의 전체 볼륨 측정
   - 주의: `user_id`, `post_id` 등 카디널리티가 높은 값은 태그에서 제외

## 🔍 작업 전 체크리스트
- [ ] 신규 메트릭이 `/actuator/prometheus`에 정상적으로 노출되는가?
- [ ] Prometheus가 해당 타겟을 성공적으로 스크랩하고 있는가 (UP 상태 확인)?
- [ ] 메트릭의 태그 구성이 다차원 분석에 적합한가?
- [ ] 가상 스레드 관련 지표(Virtual Threads count 등)가 수집되고 있는가?

## 🧪 테스트 전략 (Testing Strategy)
- **Mocking 대상**: `MeterRegistry`
- **검증 포인트**:
  - `@SpringBootTest`를 활용하여 `/actuator/prometheus` 엔드포인트가 정상적으로 200 OK를 반환하고 커스텀 메트릭 포맷이 포함되는지 검증합니다.
  - 비즈니스 로직(예: Redis Random Access) 실행 시 `MeterRegistry.counter().increment()`가 정확히 호출되어 카운트가 증가하는지 확인합니다.
