# k6 + Prometheus + Grafana + Alloy + Loki 통합 모니터링

두 종류의 메트릭을 하나의 Prometheus와 Grafana에서 확인합니다.

- k6: 배포 서비스에 부하를 발생시키고 외부 체감 성능을 Remote Write로 전송
- Spring MVC: HTTP 요청, JVM, CPU, HikariCP 내부 상태를 `/actuator/prometheus`로 노출
- Log4j2: 애플리케이션 로그를 콘솔과 `logs/application.log`에 동시 기록
- Grafana Alloy: 로그 파일을 tail하고 처리 위치를 보존한 뒤 Loki로 전송
- Loki: 로그를 7일간 보관하고 Grafana Explore의 LogQL 조회를 제공

## 로그 수집 구조

```text
Spring/Log4j2 -> logs/application.log -> Grafana Alloy -> Loki -> Grafana Explore
```

Alloy는 파일에 접근해야 하므로 백엔드와 같은 호스트에서 실행해야 합니다. 기본 구성은 저장소의
`./logs`를 Alloy 컨테이너의 `/var/log/durimoa`에 읽기 전용으로 마운트합니다. 백엔드를 다른
작업 디렉터리에서 실행한다면 `DURIMOA_LOG_DIR`을 `./logs`로 맞추거나 compose의 호스트
마운트 경로를 실제 로그 디렉터리로 변경하세요.

## 배포 백엔드 모니터링

기본 모니터링 대상은 배포 백엔드 `https://durimoa.kro.kr`입니다. 저장소 루트에서 실행합니다.

```powershell
docker compose -f docker-compose.monitoring.yml up -d prometheus loki alloy grafana
```

확인 주소:

- Backend metrics: <https://durimoa.kro.kr/actuator/prometheus>
- Prometheus targets: <http://localhost:9090/targets>
- Prometheus: <http://localhost:9090>
- Loki readiness: <http://localhost:3100/ready>
- Alloy UI: <http://localhost:12345>
- Grafana: <http://localhost:3000> (기본 계정 `admin` / `admin`)
- Spring dashboard: **DuriMoa / DuriMoa Spring Application**
- k6 dashboard: **DuriMoa / DuriMoa k6 Load Test**

Grafana의 Prometheus/Loki 데이터소스와 두 대시보드는 수동 Import 없이 자동 등록됩니다.

## Loki 로그 수집 확인

1. 백엔드를 저장소 루트에서 실행하여 `logs/application.log`가 생성되는지 확인합니다.
2. Grafana의 **Explore**에서 데이터소스를 `Loki`로 선택합니다.
3. 다음 LogQL을 실행합니다.

```logql
{job="durimoa-backend"}
```

오류 로그만 조회할 때는 다음 쿼리를 사용합니다.

```logql
{job="durimoa-backend", level="ERROR"}
```

API로도 수집 여부를 확인할 수 있습니다.

```powershell
Invoke-RestMethod 'http://localhost:3100/loki/api/v1/query_range?query=%7Bjob%3D%22durimoa-backend%22%7D&limit=20'
```

컨테이너와 설정 상태를 확인하는 명령은 다음과 같습니다.

```powershell
docker compose -f docker-compose.monitoring.yml ps
docker compose -f docker-compose.monitoring.yml logs loki alloy
```

Log4j2 로그 파일은 매일 또는 100 MB 도달 시 롤링되며 최대 14개 압축 파일을 유지합니다.
Loki 로컬 데이터 보존 기간은 7일입니다.

## 배포 환경 로그 디렉터리

Tomcat과 모니터링 스택을 같은 서버에서 실행할 때 두 프로세스가 동일한 호스트 디렉터리를
사용하도록 설정합니다. 예를 들어 애플리케이션에는 `DURIMOA_LOG_DIR=/var/log/durimoa`를
지정하고, compose의 Alloy 볼륨 왼쪽 경로도 `/var/log/durimoa`로 변경합니다.

```yaml
volumes:
  - /var/log/durimoa:/var/log/durimoa:ro
```

기본 Loki 구성은 단일 인스턴스와 로컬 파일시스템 저장소를 사용하는 개발·검증용입니다.
운영 고가용성이 필요하면 S3 같은 오브젝트 스토리지, 인증 프록시, TLS, 복제 구성을 별도로
적용해야 합니다. Loki와 Alloy UI 포트는 기본 compose에서 로컬 호스트에만 바인딩됩니다.

## k6 부하 테스트

기본 테스트는 인증이나 DB 데이터가 필요 없는 `/v2/api-docs`를 호출합니다.

```powershell
docker compose -f docker-compose.monitoring.yml --profile load-test run --rm k6
```

실제 배포 서버 테스트 예시:

```powershell
$env:BASE_URL='https://durimoa.kro.kr'
$env:API_PATH='/api/goal/stat'
$env:K6_VUS='10'
$env:K6_DURATION='30s'
$env:K6_TEST_RUN_ID='production-smoke-001'
docker compose -f docker-compose.monitoring.yml --profile load-test run --rm k6
```

인증 API는 `ACCESS_TOKEN`에 JWT 값만 지정합니다. 토큰은 파일이나 Git에 저장하지 마세요.

## 배포 백엔드 내부 메트릭 수집

기본 Prometheus 대상은 배포 백엔드입니다.

```yaml
- job_name: spring-monitoring-production
  scheme: https
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: ["durimoa.kro.kr"]
```

다른 배포 서버의 내부 메트릭을 수집하려면
`monitoring/prometheus/prometheus.yml`의 target과 scheme을 해당 배포 환경에 맞게 변경합니다.

```yaml
- job_name: spring-monitoring-production
  scheme: https
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: ["durimoa.kro.kr"]
```

변경 후 설정을 다시 불러옵니다.

```powershell
docker compose -f docker-compose.monitoring.yml restart prometheus
```

운영 환경에서는 `/actuator/prometheus`를 인터넷 전체에 공개하지 말고 방화벽, 보안 그룹,
Reverse Proxy allowlist 등으로 Prometheus 서버에서만 접근할 수 있도록 제한하세요.

## 주요 PromQL

```promql
http_server_requests_seconds_count
http_server_requests_seconds_count{status=~"5.."}
jvm_memory_used_bytes{area="heap"}
hikaricp_connections_active
histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket[5m])))
```

## 종료 및 데이터 삭제

```powershell
docker compose -f docker-compose.monitoring.yml down
docker compose -f docker-compose.monitoring.yml down -v
```

두 번째 명령은 Prometheus와 Grafana 볼륨의 기존 데이터를 함께 삭제합니다.
