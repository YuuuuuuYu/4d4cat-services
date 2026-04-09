# Monitoring Module Guide (Observability)

`monitoring` 모듈은 프로젝트의 가시성과 건전성(Health)을 확보하기 위한 서버입니다.

## 🏗️ 모니터링 인프라 (Monitoring Infrastructure)

### 1. Spring Boot Actuator
각 서버(`api`, `data`, `monitoring`)의 상태 지표를 수집하고 노출하는 핵심 기능입니다.
- **주요 엔드포인트**: `/actuator/prometheus`, `/actuator/health`, `/actuator/metrics`
- **노출 설정**: `management.endpoints.web.exposure.include`를 통해 필요한 지표만 공개합니다.

### 2. Prometheus (Metrics Scraper)
각 서비스로부터 노출된 메트릭을 주기적으로 긁어모아(Scraping) 시계열 데이터베이스로 저장합니다.
- **Config**: `prometheus.yml` 파일에서 각 서버의 IP와 포트를 타겟으로 설정합니다.
- **태그**: `application` 태그를 사용하여 여러 서버 인스턴스의 지표를 구분합니다.

### 3. Grafana (Dashboard)
Prometheus를 데이터 소스로 삼아 수집된 메트릭을 대시보드 형태로 시각화합니다.
- **Golden Signals**: Latency, Errors, Traffic, Saturation 지표를 우선적으로 모니터링합니다.
- **커스텀 대시보드**: Redis 랜덤 액세스 성공률, Pixabay 수집 통계 등을 차트로 구성합니다.

## ⚙️ 주요 라이브러리 및 도구
- **Spring Boot Actuator**: 메트릭 수집 및 노출.
- **Micrometer Registry Prometheus**: 메트릭 데이터를 Prometheus 포맷으로 변환.
- **Prometheus**: 지표 저장소.
- **Grafana**: 지표 시각화.

## 📈 모니터링 흐름 (Metrics Path)
1. **Instrument**: `core` 또는 개별 모듈에서 Micrometer를 사용하여 지표를 측정.
2. **Expose**: Actuator를 통해 `/actuator/prometheus`로 노출.
3. **Scrape**: Prometheus가 각 서버의 엔드포인트에서 지표를 수집.
4. **Visualize**: Grafana에서 쿼리를 복구하여 대시보드 렌더링.

## 🔗 의존성 및 영향도 (Dependency Map)
- **의존하는 모듈**: `api`, `data` (각 서버의 Actuator 엔드포인트를 스크랩)
- **영향을 받는 모듈**: 없음 (독립적인 관측성 스택)
