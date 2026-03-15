# SmpCouponSvc

> **대규모 트래픽 환경의 선착순 쿠폰 발급 서비스**
>
> 실무형 백엔드 프로젝트 목표 : 동시성 제어 · Kubernetes 운영 · 완전 자동화 CI/CD 구현 

---

## 프로젝트 소개

선착순 쿠폰 발급처럼 **수천 명이 동시에 요청**하는 상황에서도 데이터 정합성을 보장하는 서비스입니다.

단순한 기능 구현을 넘어 **분산 환경의 동시성 문제**, **DDD 기반 도메인 설계**, **Kubernetes 운영**, **완전 자동화된 CI/CD 파이프라인**까지 실무에서 맞닥뜨리는 문제들을 직접 해결하며 구축했습니다.

---

## 핵심 구현 포인트

### 1. 분산 락으로 동시성 제어

수천 건의 동시 요청에서 쿠폰이 초과 발급되거나 중복 발급되는 문제를 해결합니다.

```
요청 1000개 동시 도착
       ↓
분산 락(Redis) 획득 → 트랜잭션 시작
       ↓
재고 확인 → 발급 → 커밋
       ↓
락 해제 → 다음 요청
```

**핵심 설계 원칙**: 락 획득 → 트랜잭션 시작 → 커밋 → 락 해제 순서 엄수.
트랜잭션이 커밋되기 전에 락이 해제되면 다음 요청이 커밋 전 데이터를 보게 되는 **가시성 문제** 발생.
이를 AOP + `REQUIRES_NEW` 전파 속성으로 해결.

```java
@DistributedLock(key = "'coupon_lock_' + #couponId")
public Long issue(Long couponId, Long userId) {
    return callByDistributedLockTransaction.proceed(() -> {
        // REQUIRES_NEW: 락 안에서 새 트랜잭션 시작
        // 커밋 완료 후 락 해제 → 다음 요청에서 최신 데이터 보장
    });
}
```

| 방식 | 처리 방식 | 적합한 환경 |
|------|-----------|------------|
| 분산 락 (Redisson) | Redis 기반, 서버 간 공유 | 다중 서버 (실무 권장) |
| 비관적 락 (JPA) | DB `SELECT FOR UPDATE` | 단일 서버, 낮은 동시성 |

### 2. DDD 기반 도메인 설계

계층 간 책임이 명확하지 않은 초기 구조를 DDD 원칙으로 개선했습니다.

- Controller → Repository 직접 의존 제거 (계층 책임 분리)
- 할인 계산 로직을 `Coupon` 도메인 내부로 이동
- `Money` Value Object 도입 (금액 관련 비즈니스 규칙 캡슐화)
- 주문 취소 시 쿠폰 복구를 `OrderCancelledEvent` 도메인 이벤트로 처리 (도메인 간 결합도 제거)

```
주문 취소
   ↓
Orders.cancel() → OrderCancelledEvent 발행
   ↓
CouponEventHandler.restoreCoupon() 처리 (REQUIRES_NEW 별도 트랜잭션)
```

### 3. Kubernetes 기반 운영 환경

로컬 minikube 환경에서 실무 수준의 K8s 운영을 구현했습니다.

```
smp-coupon namespace
├── App Pod × 3          (Deployment, Rolling Update)
├── MySQL                (StatefulSet + PVC로 데이터 영속성 보장)
├── Redis                (Deployment)
└── HPA                  (CPU 70% 초과 시 최대 10개까지 자동 확장)
```

Helm Chart를 통해 dev/prod 환경별 설정 분리:

```bash
helm install smp-coupon-svc ./helm/smp-coupon-svc -f helm/smp-coupon-svc/values-dev.yaml
```

### 4. 완전 자동화 CI/CD 파이프라인

`git push` 한 번으로 테스트 → 이미지 빌드 → K8s 배포까지 자동화했습니다.

```
git push main
    ↓
[GitHub Actions - CI]
  MySQL + Redis 컨테이너 포함 전체 테스트 자동 실행
    ↓ 성공 시만
[GitHub Actions - CD]
  Docker 이미지 빌드 → GHCR 푸시
    ↓
[ArgoCD - GitOps]
  Git 변경 감지 → K8s 자동 배포 / 롤백
```

ArgoCD의 `selfHeal: true`로 누가 실수로 Pod를 삭제해도 자동 복구.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Backend** | Java 21, Spring Boot 3.5.11, Spring Data JPA |
| **Database** | MySQL 8.0, Redis (Redisson) |
| **Infra** | Docker, Kubernetes (minikube), Helm |
| **CI/CD** | GitHub Actions, GHCR, ArgoCD |
| **Monitoring** | Prometheus, Grafana, Micrometer |

---

## 프로젝트 구조

```
SmpCouponSvc/
├── src/main/java/org/ggne/test/
│   ├── common/
│   │   ├── aop/           # 분산 락 AOP (@DistributedLock, CallByDistributedLockTransaction)
│   │   ├── config/        # JPA, Redisson 설정
│   │   ├── domain/        # Money Value Object
│   │   └── exception/     # BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── coupon/            # 쿠폰 도메인 (발급, 동시성 제어)
│   ├── order/             # 주문 도메인 (생성, 결제, 취소, 쿠폰 복구 이벤트)
│   └── user/              # 사용자 도메인
│
├── k8s/                   # Kubernetes Manifest (Kustomize)
│   ├── app/               # Deployment, Service, ConfigMap, Secret, HPA
│   ├── mysql/             # StatefulSet, PVC, Service
│   └── redis/             # Deployment, Service
│
├── helm/smp-coupon-svc/   # Helm Chart (dev/prod 환경별 values 분리)
├── argocd/                # ArgoCD Application 정의
├── .github/workflows/     # GitHub Actions CI/CD 파이프라인
└── Dockerfile             # Multi-stage 빌드
```

---

## 로컬 실행 방법

### Docker Compose (개발용)

```bash
# 인프라 기동 (MySQL, Redis, Prometheus, Grafana)
docker-compose up -d

# 애플리케이션 실행
./gradlew bootRun
```

### Kubernetes (minikube)

```bash
minikube start
kubectl apply -k k8s/
kubectl get pods -n smp-coupon
minikube service smp-coupon-svc -n smp-coupon --url
```

---

## API

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/users` | 사용자 생성 |
| POST | `/api/coupons` | 쿠폰 생성 |
| POST | `/api/coupons/{couponId}/issue` | 쿠폰 발급 (분산 락 적용) |
| POST | `/api/orders` | 주문 생성 (쿠폰 적용 가능) |
| POST | `/api/orders/{orderId}/pay` | 주문 결제 |
| POST | `/api/orders/{orderId}/cancel` | 주문 취소 (쿠폰 자동 복구) |
| GET | `/actuator/health` | 헬스 체크 |

---

## 동시성 성능 테스트

100명이 동시에 같은 쿠폰을 발급 요청하는 시나리오에서 분산 락 vs 비관적 락 성능을 비교합니다.

```bash
./gradlew test --tests org.ggne.test.coupon.service.CouponIssueConcurrencyTest --info
```

---

## 모니터링

| 서비스 | URL |
|--------|-----|
| Prometheus | http://localhost:19090 |
| Grafana | http://localhost:3000 (admin/admin) |
| Actuator | http://localhost:8080/actuator/health |
