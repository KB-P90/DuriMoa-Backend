# Backend

KB 두리모아 서비스 백엔드 서버입니다.


## 🛠 기술스택

| 구분 | 기술 |
|------|------|
| **Backend** | Java 17, Spring Framework, Spring MVC, MyBatis, Spring Security, Maven |
| **Database** | MySQL, Redis |
| **Server** | Apache Tomcat |
| **API Documentation** | Swagger |
| **Test** | JUnit, Mockito |


## 📂 프로젝트 구조

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── project/
│   │           ├── common/
│   │           │   ├── config/
│   │           │   ├── exception/
│   │           │   ├── response/
│   │           │   └── util/
│   │           └── domain/
│   │               ├── member/
│   │               │   ├── controller/
│   │               │   ├── service/
│   │               │   ├── mapper/
│   │               │   └── dto/
│   │               ├── account/
│   │               ├── budget/
│   │               └── mission/
│   ├── resources/
│   │   ├── mapper/
│   │   │   ├── member/
│   │   │   │   └── MemberMapper.xml
│   │   │   ├── account/
│   │   │   └── budget/
│   │   ├── log4j2.xml
│   │   └── database.properties
│   └── webapp/
│       └── WEB-INF/
│           ├── spring/
│           │   ├── root-context.xml
│           │   └── appServlet/
│           │       └── servlet-context.xml
│           ├── views/
│           └── web.xml
└── test/
    └── java/
    
```
    
## ⚙️ 주요 설정 파일

| 파일 | 설명 |
|---|---|
| `pom.xml` | 의존성 및 빌드 설정 |
| `web.xml` | DispatcherServlet, Filter, Listener 설정 |
| `root-context.xml` | DB, MyBatis, Service, 공통 Bean 설정 |
| `servlet-context.xml` | Controller, ViewResolver, 정적 리소스 설정 |
| `database.properties` | 데이터베이스 접속 정보 |
| `mybatis-config.xml` | MyBatis 공통 설정 |
| `Mapper.xml` | SQL 작성 및 Mapper 메서드 연결 |
| `log4j2.xml` | 애플리케이션 로그 설정 |


## 🗃 MyBatis 작성 규칙

- Mapper 인터페이스와 Mapper XML의 이름을 동일하게 작성한다.
- Mapper XML의 `namespace`는 Mapper 인터페이스의 전체 경로로 작성한다.
- Mapper 메서드명과 SQL 태그의 `id`를 동일하게 작성한다.
- SQL의 `SELECT *` 사용을 지양한다.
- 컬럼명과 DTO 필드명이 다른 경우 `resultMap`을 사용한다.
- 여러 개의 파라미터를 전달할 때는 `@Param`을 사용한다.
- 동적 쿼리는 필요한 경우에만 사용한다.
- 복잡한 SQL은 주석으로 목적을 설명한다.
- 개인정보와 계좌번호는 로그에 출력하지 않는다.

## 📝 네이밍 규칙

### Java

- Controller: `MemberController`
- Service 인터페이스: `MemberService`
- Service 구현체: `MemberServiceImpl`
- Mapper 인터페이스: `MemberMapper`
- Request DTO: `MemberCreateRequestDTO`
- Response DTO: `MemberResponseDTO`
- Exception: `MemberNotFoundException`

### MyBatis

- Mapper XML: `MemberMapper.xml`
- 조회: `selectMember`, `findMemberById`
- 등록: `insertMember`
- 수정: `updateMember`
- 삭제: `deleteMember`
- 존재 여부: `existsMemberByEmail`
- 개수 조회: `countMembers`

## 👥 팀원 및 역할분담

<div align="center">
<table>
  <tr align="center">
    <td width="180">
      <a href="https://github.com/HwangSeo">
        <img src="https://avatars.githubusercontent.com/HwangSeo" width="100" height="100"/><br/>
        <b>황서진</b>
      </a><br/>
      월별 지출 관리, 온보딩
    </td>
    <td width="180">
      <a href="https://github.com/happysongtan">
        <img src="https://avatars.githubusercontent.com/happysongtan" width="100" height="100"/><br/>
        <b>강태규</b>
      </a><br/>
      알림, 공동 예산 관리
    </td>
    <td width="180">
      <a href="https://github.com/1juyoung">
        <img src="https://avatars.githubusercontent.com/1juyoung" width="100" height="100"/><br/>
        <b>김주영</b>
      </a><br/>
      마이페이지 
    </td>
  </tr>
  <tr align="center">
    <td width="180">
      <a href="https://github.com/kht9938">
        <img src="https://avatars.githubusercontent.com/kht9938" width="100" height="100"/><br/>
        <b>김현태</b>
      </a><br/>
      로그인/회원가입, 결혼 목표 설정 
    </td>
    <td width="180">
      <a href="https://github.com/parkgijumg-hue">
        <img src="https://avatars.githubusercontent.com/parkgijumg-hue" width="100" height="100"/><br/>
        <b>박기정</b>
      </a><br/>
      홈, 카드 전략 추천 
    </td>
    <td width="180">
      <a href="https://github.com/KoHo512">
        <img src="https://avatars.githubusercontent.com/KoHo512" width="100" height="100"/><br/>
        <b>정여원</b>
      </a><br/>
      캘린더
    </td>
  </tr>
</table>
</div>
