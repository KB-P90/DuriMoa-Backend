# k6 + Prometheus + Grafana 통합 모니터링

두 종류의 메트릭을 하나의 Prometheus와 Grafana에서 확인합니다.

- k6: 배포 서비스에 부하를 발생시키고 외부 체감 성능을 Remote Write로 전송
- Spring MVC: HTTP 요청, JVM, CPU, HikariCP 내부 상태를 `/actuator/prometheus`로 노출

## 로컬 백엔드 모니터링

백엔드를 `http://localhost:8080`에서 먼저 실행한 뒤 저장소 루트에서 실행합니다.

```powershell
docker compose -f docker-compose.monitoring.yml up -d prometheus grafana
```

확인 주소:

- Backend metrics: <http://localhost:8080/actuator/prometheus>
- Prometheus targets: <http://localhost:9090/targets>
- Prometheus: <http://localhost:9090>
- Grafana: <http://localhost:3000> (기본 계정 `admin` / `admin`)
- Spring dashboard: **DuriMoa / DuriMoa Spring Application**
- k6 dashboard: **DuriMoa / DuriMoa k6 Load Test**

Grafana 데이터소스와 두 대시보드는 수동 Import 없이 자동 등록됩니다.

## k6 부하 테스트

기본 테스트는 인증이나 DB 데이터가 필요 없는 `/v2/api-docs`를 호출합니다.

```powershell
docker compose -f docker-compose.monitoring.yml --profile load-test run --rm k6
```

실제 배포 서버 테스트 예시:

```powershell
$env:BASE_URL='https://api.example.com'
$env:API_PATH='/api/goal/stat'
$env:K6_VUS='10'
$env:K6_DURATION='30s'
$env:K6_TEST_RUN_ID='production-smoke-001'
docker compose -f docker-compose.monitoring.yml --profile load-test run --rm k6
```

인증 API는 `ACCESS_TOKEN`에 JWT 값만 지정합니다. 토큰은 파일이나 Git에 저장하지 마세요.

## 배포 백엔드 내부 메트릭 수집

기본 Prometheus 대상은 Docker 호스트에서 실행 중인 로컬 백엔드입니다.

```yaml
- job_name: spring-monitoring
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: ["host.docker.internal:8080"]
```

배포 서버의 내부 메트릭을 수집하려면
`monitoring/prometheus/prometheus.yml`의 target과 scheme을 배포 환경에 맞게 변경합니다.

```yaml
- job_name: spring-monitoring-production
  scheme: https
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: ["api.example.com"]
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
