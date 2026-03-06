# Metrics Monitoring Guide

Micrometer 표준 API 및 `Timer.Sample`을 활용한 정밀한 측정이 적용되었습니다.

## 1. 공통 인프라 메트릭 (Core Module)

`core` 모듈에 설정된 메트릭은 이를 사용하는 모든 서비스(`api`, `data`)에서 공통으로 수집됩니다.

### Redis 메트릭
| 메트릭 이름 | 타입 | 태그 | 설명 |
| :--- | :--- | :--- | :--- |
| `redis.pipeline.duration` | Timer | `key` | Redis 파이프라인(벌크 저장) 소요 시간 |
| `redis.random.access` | Counter | `key`, `status` (hit/miss) | 랜덤 데이터 조회 성공/실패 횟수 |

### Discord 알림 메트릭
| 메트릭 이름 | 타입 | 태그 | 설명 |
| :--- | :--- | :--- | :--- |
| `discord.webhook.sent` | Counter | `status` (success/failure) | Discord 웹훅 전송 성공/실패 횟수 |

### AOP 작업 메트릭
| 메트릭 이름 | 타입 | 태그 | 설명 |
| :--- | :--- | :--- | :--- |
| `task.execution.duration` | Timer | `service`, `task` | `@NotifyDiscord` 작업의 소요 시간 (Timer.Sample 적용) |
| `task.execution.total` | Counter | `task`, `status` (success/failure) | 작업 실행 횟수 및 성공/실패 여부 |

---

## 2. 데이터 수집 메트릭 (Data Module)

`data` 서비스의 수집 프로세스 상태를 모니터링합니다.

| 메트릭 이름 | 타입 | 태그 | 설명 |
| :--- | :--- | :--- | :--- |
| `pixabay.collection.items` | Counter | `type` (Video/Music) | 수집 및 저장된 총 아이템 수 |
| `pixabay.collection.filters` | Counter | `type`, `status` (success/failure) | 카테고리/장르별 수집 성공 및 실패 횟수 |

---

## 3. API 서비스 메트릭 (Api Module)

`api` 서비스의 요청 처리 및 예외 상황을 모니터링합니다.

| 메트릭 이름 | 타입 | 태그 | 설명 |
| :--- | :--- | :--- | :--- |
| `api.errors.total` | Counter | `code`, `status` | `GlobalExceptionHandler`에서 처리된 에러 코드별 횟수 |

---

## 4. Grafana 활용 팁

### Prometheus Query 예시
- **Redis 조회 실패율:** `sum(rate(redis_random_access_total{status="miss"}[5m])) / sum(rate(redis_random_access_total[5m]))`
- **전체 에러 발생 추이:** `sum by (code) (rate(api_errors_total[1m]))`
- **수집 데이터 처리량:** `sum by (type) (increase(pixabay_collection_items_total[24h]))`
- **작업 평균 소요 시간:** `rate(task_execution_duration_seconds_sum[5m]) / rate(task_execution_duration_seconds_count[5m])`

### 대시보드 구성 권장
1. **[Core] Infrastructure Dashboard:** Redis Connection Pool, Redis Latency, Discord Webhook Status
2. **[Data] Collection Dashboard:** Collection Success Rate, Items Count, Scheduled Task Duration
3. **[Api] Service Dashboard:** Request Latency (Actuator 기본), Error Code Count (api.errors.total)
