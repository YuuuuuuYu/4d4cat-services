# 프로젝트 규칙 (Project Rules)

이 문서는 `4d4cat-services` 프로젝트의 원활한 협업을 위한 세부 규칙을 정의합니다.

## 1. Git 사용 규칙

### 1.1. 브랜치 이름 전략

- **feature**: `feature/기능-요약` (예: `feature/clock-domain-search`)
- **fix**: `fix/수정-내용-요약` (예: `fix/pixabay-api-error`)
- **refactor**: `refactor/리팩토링-범위` (예: `refactor/datainitialization-service`)

### 1.2. 커밋 메시지 형식

Angular Commit Convention을 따릅니다.

```
<type>: <subject>
<BLANK LINE>
<body>
```

- **type**: `feat`, `fix`, `refactor`, `style`, `docs`, `test`, `chore` 등