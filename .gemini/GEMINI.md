# 프로젝트 통합 가이드 (Project Unified Instructions)

이 디렉토리는 `4d4cat-services` 프로젝트의 AI 어시스턴트를 위한 통합 가이드라인입니다. 
아래 정의된 페르소나, 프로젝트 문맥, 개발 규칙을 숙지하고 코드를 생성하거나 리뷰할 때 엄격히 준수하십시오.
또한, 모든 라이브러리/API 문서, 코드 생성, 설정 또는 설정 단계가 필요할 때 항상 Context7 MCP를 사용하세요.

## 파일 구조

- `project.md` - 프로젝트 개요, 기술 스택, 외부 서비스
- `architecture.md` - 아키텍처, 디자인 패턴, 설계 원칙
- `workflows.md` - 도메인별 상세 워크플로우
- `standards.md` - 코딩 스타일 가이드
- `testing.md` - 테스트 작성 규칙 및 예제
- `deployment.md` - 빌드 및 배포 가이드

## 서비스 도메인 및 전문 지침 (Domains & Experts)

특정 서비스나 도메인 관련 작업을 수행할 때는 아래 디렉토리의 컨텍스트와 스킬 가이드를 우선적으로 참조하십시오.

### 도메인 컨텍스트 (`.gemini/domains/`)
각 서비스의 기능적 개요, 주요 파일, 워크플로우를 정의합니다.
- `pixabay.md` (키워드: Pixabay API, Video, Music, Scheduler, Random, Cache)
- `message.md` (키워드: Message, Validation, Discord Webhook, 단일 메시지)
- `omniwatch.md` (키워드: Watch, Brand, Attribute, JPA, Entity, Auditing)
- `techblog.md` (키워드: RSS/Atom, Data Collection, Feed Retrieval, Filtering, Tag, Company)

### 전문가 스킬 가이드 (`.gemini/experts/`)
해당 도메인을 다룰 때 AI가 준수해야 할 전문적인 지침, 체크리스트, 테스트 전략을 정의합니다.
- `pixabay-expert.md` (키워드: Virtual Threads, 병렬 처리, Rate Limit, 에러 복원력)
- `message-expert.md` (키워드: 알림 연동, AOP 설정, 저장소 캡슐화, 유효성 검증)
- `omniwatch-expert.md` (키워드: N+1 문제, Fetch Join, DDD, Auditing)
- `techblog-expert.md` (키워드: RSS Parsing, Deduplication, Virtual Threads, Dynamic Query)
- `monitoring-expert.md` (키워드: Actuator 노출, Metric Naming, Tag 최적화)
- `security-expert.md` (키워드: Secret, Environment, Error Masking, XSS, CORS)

### 모듈별 아키텍처 및 인프라 (`.gemini/modules/`)
각 모듈의 기술적 구현, 공통 인프라, 설정 및 의존성을 정의합니다.
- `core.md` (키워드: RedisDataStorage, DiscordNotifierAspect, BaseResponse, 의존성 없음)
- `data.md` (키워드: PixabayDataCollector, 가상 스레드, RestClient, 의존성: core)
- `api.md` (키워드: GlobalExceptionHandler, JPA, OpenAPI, CORS, 의존성: core)
- `monitoring.md` (키워드: Prometheus, Grafana, Actuator, Micrometer)

## 🔄 컨텍스트 동기화 규칙 (Doc Sync Rule)

작업의 무결성을 유지하기 위해 AI(Gemini CLI)는 다음 규칙을 반드시 준수해야 합니다.

1. **상태 변화 감지**: 코드 수정, 아키텍처 변경, 새로운 도메인 추가 등의 작업이 완료되면 관련 `.gemini/` 내 문서들이 최신 상태인지 검토하십시오.
2. **업데이트 제안**: 변경 사항이 문서에 반영되어야 한다고 판단되면, 사용자에게 **"관련 문서를 업데이트할까요?"**라고 명시적으로 묻고 승인 시 즉시 반영하십시오.
3. **영향도 체크**: `core` 모듈 등 공통 영역을 수정할 때는 `Dependency Map`에 정의된 영향을 받는 모든 도메인/모듈 가이드를 함께 검토하십시오.

각 파일은 독립적으로 참조 가능하며, 필요한 주제의 문서만 선택적으로 읽을 수 있습니다.

## 🛡️ AI 어시스턴트 필수 가드레일 (Guardrails)

작업의 누락을 방지하고 코드의 품질을 보장하기 위해 다음 규칙을 엄격히 준수하십시오.

1. **테스트 우선주의 (Test-First & 묶음 작업)**:
   기능 구현을 완료한 뒤 테스트를 미루지 마십시오. 핵심 비즈니스 로직(특히 Expert 가이드에 명시된 Testing Strategy)을 작성할 때는 반드시 테스트 코드를 함께 작성하고 통과 여부를 증명해야 합니다.
2. **최종 체크리스트 강제 확인 (Self-Reflection)**:
   작업을 완료하고 사용자에게 보고하기 전에, 관련된 `*-expert.md` 파일에 있는 **<작업 전 체크리스트>**와 **<테스트 전략>** 항목을 모두 완수했는지 스스로 명시적으로 점검하고, 그 결과를 답변에 포함하십시오.