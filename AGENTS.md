# AGENTS.md

이 문서는 KB 두리모아(DuriMoa) 백엔드 프로젝트 개발에 참여하는 AI 에이전트 및 개발자를 위한 지침서입니다. 프로젝트의 구조, 컨벤션, 개발 규칙 및 프로세스를 정의합니다.

---

## 프로젝트 개요

### 프로젝트 이름
- **KB 두리모아 (DuriMoa) - 백엔드 (Backend)**
- 예비부부를 위한 자산 관리 서비스 백엔드 API 서버

### 기술 스택
- **Language**: Java 17
- **Framework**: Spring Framework 5.3.37 (Spring MVC), Spring Security 5.8.13
- **ORM / Persistence**: MyBatis 3.5.15, MyBatis-Spring 2.1.2
- **Database & Cache**: MySQL 8.3.0 (HikariCP 5.1.0 Connection Pool), Redis 2.7.18 (Lettuce 6.2.7)
- **Build Tool**: Gradle
- **WAS / Server**: Apache Tomcat (Java Web Application `.war`)
- **API Documentation**: Swagger (Springfox 2.9.2)
- **Logging**: Log4j2 (2.20.0), log4jdbc-log4j2 (1.16)
- **Utilities**: Lombok 1.18.30, Jackson 2.15.2 (JSR-310 지원), JJWT 0.11.5
- **Testing**: JUnit 5 (5.10.2), Mockito (5.11.0), Spring Test

### 코드 작성 규칙
1. **Google/Oracle Java Code Style 준수**:
   - 들여쓰기는 **4 Space**를 사용하며 탭(Tab) 사용을 금지합니다.
   - 모든 파일은 **UTF-8** 인코딩을 적용합니다.
2. **의존성 주입 (Dependency Injection)**:
   - 필드 주입(`@Autowired`) 대신 `@RequiredArgsConstructor`를 활용한 **생성자 주입**을 사용합니다.
3. **계층형 아키텍처 (Layered Architecture)**:
   - `Controller` ➔ `Service Interface` ➔ `Service Implementation` ➔ `Mapper Interface` ➔ `Mapper XML` 흐름을 엄격히 준수합니다.
4. **예외 처리 (Exception Handling)**:
   - 비즈니스 예외는 커스텀 Exception(예: `MemberNotFoundException`, `InvalidInputException`)을 정의하고 `@ControllerAdvice` / `@RestControllerAdvice`에서 일괄 처리합니다.
5. **MyBatis SQL 작성 규칙**:
   - `SELECT *` 사용을 금지하고 명시적으로 컬럼을 지정합니다.
   - 컬럼명과 DTO 필드명이 일치하지 않을 경우 `resultMap`을 사용하거나 SQL 별칭(alias)을 지정합니다.
   - 다중 파라미터를 Mapper 메서드에 전달 시 `@Param` 어노테이션을 명시합니다.
   - 로그 출력 시 개인정보(비밀번호, 주민번호) 및 계좌번호가 직접 노출되지 않도록 가공 처리합니다.

### 네이밍 규칙

#### 1. Java 클래스 및 인터페이스 (PascalCase)
- **Controller**: `[Domain]Controller` (예: `MemberController`, `AccountController`)
- **Service Interface**: `[Domain]Service` (예: `MemberService`)
- **Service Implementation**: `[Domain]ServiceImpl` (예: `MemberServiceImpl`)
- **Mapper Interface**: `[Domain]Mapper` (예: `MemberMapper`)
- **Request DTO**: `[Domain][Action]RequestDTO` (예: `MemberCreateRequestDTO`, `BudgetUpdateRequestDTO`)
- **Response DTO**: `[Domain][Action]ResponseDTO` 또는 `[Domain]ResponseDTO` (예: `MemberResponseDTO`, `AccountDetailResponseDTO`)
- **Exception**: `[Domain][Reason]Exception` (예: `MemberNotFoundException`, `BudgetExceededException`)

#### 2. 메서드 및 변수 (camelCase)
- **Java 메서드/변수**: camelCase (예: `findMemberById`, `totalAmount`)
- **MyBatis SQL 태그 ID & Mapper 메서드**:
   - 조회: `selectMember`, `findMemberById`
   - 등록: `insertMember`
   - 수정: `updateMember`
   - 삭제: `deleteMember`
   - 존재 여부: `existsMemberByEmail`
   - 개수 조회: `countMembers`

#### 3. 기타 규칙
- **상수 (Constant)**: UPPER_SNAKE_CASE (예: `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE`)
- **DB 테이블/컬럼**: lower_snake_case (예: `member_id`, `created_at`)
- **URL Endpoint**: lower-kebab-case 및 복수형 명사 사용 (예: `/api/members`, `/api/budgets/monthly`)
- **Mapper XML 파일명**: `[Domain]Mapper.xml` (예: `MemberMapper.xml`)

### 우선 순위
1. **기능 동작의 정확성 및 비즈니스 데이터 검증**: 금융/자산 데이터 다루므로 데이터 정확성, 보안 및 트랜잭션 처리 최우선.
2. **코드 가독성 및 아키텍처 규칙 준수**: 도메인별 계층 분리 및 DTO 객체 분리 엄수.
3. **DB 쿼리 성능 및 리소스 최적화**: MyBatis 인덱스 타는 쿼리 작성 및 Redis 캐싱 적재적소 활용.
4. **문서화 및 예외 처리 고도화**: Swagger 명세 최신화 및 사용자 친화적 공통 응답/에러 포맷 유지.

### 폴더 구조
```text
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── project/
│   │   │           ├── common/                  # 공통 인프라 및 유틸리티
│   │   │           │   ├── config/              # Spring, Security, Redis, Swagger 등 설정
│   │   │           │   ├── exception/           # 공통 예외 및 에러 핸들러
│   │   │           │   ├── response/            # 공통 API 응답 규격 (ApiResponse, ErrorResponse)
│   │   │           │   └── util/                # 날짜, 암호화, JWT 등 유틸 클래스
│   │   │           └── domain/                  # 비즈니스 도메인 모듈
│   │   │               ├── member/              # 회원 도메인
│   │   │               │   ├── controller/
│   │   │               │   ├── service/
│   │   │               │   ├── mapper/
│   │   │               │   └── dto/
│   │   │               ├── account/             # 계좌/자산 도메인
│   │   │               ├── budget/              # 예산/지출 도메인
│   │   │               └── mission/             # 미션/목표 도메인
│   │   ├── resources/
│   │   │   ├── mapper/                          # MyBatis XML 매퍼 파일 위치
│   │   │   │   ├── member/
│   │   │   │   ├── account/
│   │   │   │   └── budget/
│   │   │   ├── log4j2.xml                       # 로그 설정
│   │   │   ├── database.properties              # DB 연결 설정
│   │   │   └── mybatis-config.xml               # MyBatis 설정
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           ├── spring/                      # root-context.xml, servlet-context.xml
│   │           └── web.xml                      # DispatcherServlet 설정
│   └── test/
│       └── java/                                # JUnit5 및 Mockito 테스트 코드
├── DuriMoaDB.sql                                # 데이터베이스 스키마 및 DDL
├── build.gradle                                 # 의존성 및 빌드 설정
└── AGENTS.md                                    # 에이전트 및 개발 지침서
```

---

## 규칙

### 타입 규칙
1. **DTO 필드 타입**:
   - Java Primitive Type(`int`, `long`, `boolean`) 사용을 지양하고, **Wrapper Class**(`Integer`, `Long`, `Boolean`)를 사용하여 Null 처리 및 데이터 부재 상태 표현을 유연하게 합니다.
2. **금액/수량 처리**:
   - 금액 관련 데이터는 오차 방지를 위해 `Long` 또는 `BigDecimal` 타입을 사용하며, 소숫점 연산이 수반될 시 `BigDecimal`을 필수 적용합니다.
3. **날짜 및 시간 타입**:
   - `java.util.Date`나 `java.sql.Timestamp` 사용을 금지하고, Java 8+ `java.time.LocalDate`, `java.time.LocalDateTime`을 사용합니다.
   - JSON 직렬화/역직렬화 시 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 어노테이션을 지정합니다.
4. **공통 응답 Wrapper 타입**:
   - Controller의 반환 타입은 `ResponseEntity<ApiResponse<T>>` 형태를 유지하여 성공/실패 여부, 상태 코드, 메세지, 데이터를 통일된 규격으로 제공합니다.

### 컴포넌트 규칙
1. **Controller**:
   - HTTP 요청 수신, `@Valid`를 통한 유입 파라미터 검증, Service 레이어 호출, 응답 래핑 역할만 수행합니다.
   - Controller에 비즈니스 로직이나 DB 접근 코드가 포함되어서는 안 됩니다.
2. **Service**:
   - 비즈니스 핵심 로직 처리, 트랜잭션 관리(`@Transactional`), 도메인 간 조율을 담당합니다.
   - Service 인터페이스와 구현체(`ServiceImpl`)를 분리하여 유연성을 확보합니다.
3. **Mapper (DAO)**:
   - DB 접근 전용 레이어로 MyBatis Mapper 인터페이스 형태를 가집니다.
   - Mapper XML의 `namespace`는 Mapper 인터페이스의 FQCN(Full Qualified Class Name)과 100% 일치해야 합니다.
4. **DTO (Data Transfer Object)**:
   - View/API 요청 및 응답에 사용하는 객체로 RequestDTO와 ResponseDTO를 명확히 분리합니다.
   - DB Entity 또는 Domain VO 객체를 Controller 외부로 직접 노출하지 않습니다.

### 금지 규칙
1. **Controller 내 비즈니스 로직 작성 금지**: 서비스 레이어를 거치지 않는 Mapper 직접 호출이나 로직 연산 금지.
2. **SQL `SELECT *` 금지**: 필요한 컬럼만 명시하여 네트워크 바이트 절감 및 인덱스 활용 유도.
3. **민감 정보 로그 출력 금지**: 비밀번호, JWT 토큰, 계좌 번호, 주민등록번호 등을 `log.info()`나 에러 메시지에 노출 금지.
4. **`System.out.println()` 사용 금지**: 반드시 Log4j2 (`log.info()`, `log.error()`, `log.debug()`)를 사용.
5. **예외 무시 (Exception Swallowing) 금지**: `catch (Exception e) {}` 와 같이 빈 catch 블록으로 에러를 묵인하는 코드 금지.
6. **main / develop 브랜치 직접 push 금지**: 반드시 작업 브랜치에서 Pull Request(PR)를 통해 코드 리뷰 후 머지.

### 커밋 규칙
- **Conventional Commits** 형식을 준수합니다.

```text
<type>: <description> (#issue_number)
```

#### Prefix (Type)
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정 (README.md, AGENTS.md 등)
- `style`: 코드 포맷팅, 세미콜론 누락 등 (코드 동작 변경 없음)
- `refactor`: 코드 리팩토링 (기능 추가 및 버그 수정 없음)
- `test`: 테스트 코드 추가 및 수정
- `chore`: 빌드 업무, 패키지 매니저, 기타 환경 설정 변경

#### 작성 예시
- `feat: 회원 가입 및 비밀번호 암호화 기능 구현 (#12)`
- `fix: 공동 예산 조회 시 NullPointerException 예외 수정 (#45)`
- `docs: AGENTS.md 지침서 문서 업데이트`

### git 규칙
1. **브랜치 전략 (Git Flow)**:
   - `main`: 운영 배포용 브랜치
   - `develop`: 개발 통합 브랜치
   - `feat/[기능명]`: 신규 기능 개발 브랜치 (예: `feat/member-login`, `feat/budget-calc`)
   - `fix/[버그명]`: 버그 수정 브랜치 (예: `fix/jwt-expiration`)
   - `docs/[문서명]`: 문서 작업 브랜치
2. **Pull Request (PR) 프로세스**:
   - `.github/ISSUE_TEMPLATE/PULL_REQUEST_TEMPLATE.md` 양식을 준수하여 PR 내용 작성.
   - 연관된 Issue 번호를 명시 (`Resolves #이슈번호`).
   - 최소 1명 이상의 팀원 승인(Approve) 후 `develop` 브랜치로 Squash and Merge 진행.

### 작업 분류
AI 에이전트 및 개발자는 작업을 진행하기 전 다음 분류에 따라 작업을 명확히 식별합니다.

1. **기능 개발 (Feature Task)**: DTO 생성 -> Mapper 인터페이스 및 XML 작성 -> Service 구현 -> Controller 구현 -> Swagger 작성 -> 테스트 코드 추가
2. **버그 수정 (Bugfix Task)**: 오류 로그 및 재현 조건 확인 -> 원인 분석 및 범위 파악 -> 수정 및 테스트 작성 -> 회귀 테스트 검증
3. **리팩토링 (Refactoring Task)**: 기존 테스트Pass 확인 -> 코드 구조 개선/성능 최적화 -> 기존 기능 영향도 테스트 재검증
4. **문서화 및 설정 (Docs/Chore Task)**: 설정 파일 수정 또는 API 명세/가이드 문서 업데이트 및 검수

### 작업 완료 보고
작업이 완료되었을 때 AI 에이전트는 작성/수정한 내용과 검증 결과를 다음 항목에 맞춰 사용자에게 체계적으로 보고해야 합니다.

1. **작업 요약 (Summary)**:
   - 수행한 주요 작업 내용을 명확하게 정리
2. **변경 파일 목록 (Changed Files)**:
   - 생성/수정/삭제된 모든 파일의 **절대 경로 기반 markdown link (`file:///...`)** 제공
3. **검증 결과 (Verification Results)**:
   - `gradlew test` 또는 실행을 통해 검증한 결과 및 테스트 통과 여부 명시
4. **주의사항 및 향후 작업 (Notes & Next Steps)**:
   - 리뷰어가 주의 깊게 봐야 할 사항이나 후속 완료해야 할 작업(TODO) 정리
