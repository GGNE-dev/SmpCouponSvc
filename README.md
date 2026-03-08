# SmpCouponSvc (선착순 쿠폰 발급 및 주문 서비스)

`SmpCouponSvc`는 대규모 트래픽 환경에서 발생하는 **동시성 문제(Concurrency Issue)**를 해결하고, 시스템의 상태를 실시간으로 추적하는 **관측성(Observability)**을 확보하는 데 중점을 둔 쿠폰 관리 예제 서비스입니다.

---

## 🚀 주요 기능 (Key Features)

### 1. 선착순 쿠폰 발급 및 동시성 제어
- **분산 락 (Redisson)**: Redis 기반의 분산 락을 사용하여 다중 서버 환경에서도 정합성을 보장합니다. AOP를 통해 비즈니스 로직과 락 로직을 분리했습니다.
- **비관적 락 (Pessimistic Lock)**: JPA의 `PESSIMISTIC_WRITE`를 사용하여 데이터베이스 수준에서 동시성을 제어합니다.
- **성능 비교**: 두 방식 간의 지연 시간(Latency) 및 처리량(Throughput) 비교 테스트 환경을 제공합니다.

### 2. 주문 및 결제 프로세스
- **주문 생성**: 쿠폰 할인 적용이 가능한 주문 생성 로직을 포함합니다.
- **상태 관리**: 결제(`PAID`), 취소(`CANCELLED`) 등 주문 생애주기를 관리합니다.
- **쿠폰 복구**: 주문 취소 시 사용된 쿠폰을 자동으로 다시 사용 가능(`AVAILABLE`) 상태로 복구합니다.

### 3. 실시간 모니터링 
- **Prometheus**: Spring Boot Actuator를 통해 수집된 JVM 및 비즈니스 메트릭을 수집합니다.
- **Grafana**: 수집된 데이터를 시각화하여 대시보드 형태로 제공합니다. (CPU, Memory, TPS, Error Rate 등)

---

## 🛠 기술 스택 (Tech Stack)

- **Backend**: Java 21, Spring Boot 3.5.11, Spring Data JPA
- **Database**: MySQL 8.0
- **In-Memory**: Redis (Redisson)
- **Monitoring**: Prometheus, Grafana, Micrometer
- **Build & Infra**: Gradle, Docker, Docker Compose

---

## 🏃 시작하기 (Quick Start)

### 1. 인프라 기동 (Docker)
애플리케이션 실행 전 필요한 외부 인프라(DB, Redis, 모니터링 도구)를 실행합니다.
```powershell
docker-compose up -d
```

### 2. 애플리케이션 실행
```powershell
./gradlew bootRun
```

### 3. 테스트 실행 (동시성 성능 측정)
```powershell
./gradlew test --tests org.ggne.test.coupon.service.CouponIssueConcurrencyTest --info
```

---

## 📊 모니터링 주소 (Monitoring Links)

- **Prometheus**: `http://localhost:19090`
- **Grafana**: `http://localhost:3000` (기본 ID/PW: `admin` / `admin`)
- **Actuator Prometheus Metric**: `http://localhost:8080/actuator/prometheus`

---

## 📂 프로젝트 구조 (Architecture)

```
src/main/java/org/ggne/test/
├── common/         # 공통 설정, 예외 처리, AOP 분산 락
├── user/           # 사용자 도메인
├── coupon/         # 쿠폰 발급 및 동시성 제어 도메인
└── order/          # 주문 및 결제 처리 도메인
```

---

## 💡 배우고 싶었던 학습 포인트
- 분산 환경에서 Redis를 활용한 분산 락의 필요성과 오버헤드 이해.
- `REQUIRES_NEW` 트랜잭션 전파 속성을 이용한 락 정합성 확보.
- Micrometer와 Prometheus를 활용한 서버 메트릭 수집 및 Grafana 시각화 기초.
