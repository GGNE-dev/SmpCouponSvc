# ============================================================
# Multi-stage Build Dockerfile
# ============================================================
# Multi-stage build 전략:
#   Stage 1 (builder): 소스코드 컴파일 → JAR 파일 생성
#   Stage 2 (runtime): JAR만 복사해서 실행
#
# 장점:
#   - 최종 이미지에 컴파일러(JDK), 소스코드, 빌드 도구 포함 안 됨
#   - 이미지 크기 최소화 (~300MB → ~150MB)
#   - 보안 향상 (불필요한 도구 제거)
# ============================================================

# ============================================================
# Stage 1: Build Stage
# ============================================================
# eclipse-temurin: Eclipse 재단이 관리하는 공식 OpenJDK 이미지
# 21-jdk-alpine: JDK 21 + Alpine Linux (초경량 Linux 배포판)
# AS builder: 이 스테이지에 "builder"라는 이름을 붙임 (Stage 2에서 참조)
FROM eclipse-temurin:21-jdk-alpine AS builder

# 컨테이너 내부 작업 디렉토리 지정
WORKDIR /app

# ── 의존성 캐시 최적화 ──────────────────────────────────────
# Gradle Wrapper 먼저 복사 (소스코드보다 변경이 적음)
# Docker는 레이어 캐시를 사용: 변경 없는 레이어는 재사용
# build.gradle.kts만 변경 시 gradlew 레이어는 캐시 히트 → 빠른 빌드
COPY gradlew .
COPY gradle gradle

# 빌드 스크립트 복사
COPY build.gradle.kts .
COPY settings.gradle.kts .

# gradlew 실행 권한 부여 (Linux에서 필수)
RUN chmod +x ./gradlew

# 의존성만 먼저 다운로드 (소스코드 변경과 무관하게 캐시됨)
# --no-daemon: CI 환경에서 Gradle 데몬 사용 안 함 (메모리 절약)
RUN ./gradlew dependencies --no-daemon || true

# ── 소스코드 복사 및 빌드 ────────────────────────────────────
# 소스코드는 자주 변경되므로 의존성 다운로드 레이어 이후에 복사
COPY src src

# 애플리케이션 빌드
# bootJar: 실행 가능한 fat JAR 생성 (의존성 모두 포함)
# -x test: 테스트 스킵 (CI에서 별도로 실행)
# --no-daemon: CI 환경 최적화
RUN ./gradlew bootJar -x test --no-daemon

# 빌드 결과 확인 (디버깅용)
RUN ls -la build/libs/

# ============================================================
# Stage 2: Runtime Stage
# ============================================================
# JRE (Java Runtime Environment): JDK에서 컴파일러 등 제거한 실행 전용
# alpine 기반으로 이미지 크기 최소화
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# ── 보안: 비 root 유저 생성 ──────────────────────────────────
# 컨테이너를 root로 실행하면 보안 취약점 발생 시 호스트까지 위험
# spring 그룹 + spring 유저 생성 후 전환
RUN addgroup -S spring && adduser -S spring -G spring

# ── Stage 1에서 빌드된 JAR 복사 ──────────────────────────────
# --from=builder: "builder" 스테이지에서 파일 복사
# *.jar: 정확한 파일명을 몰라도 됨 (버전이 파일명에 포함될 수 있음)
COPY --from=builder /app/build/libs/*.jar app.jar

# 파일 소유권 변경 (spring 유저가 읽을 수 있도록)
RUN chown spring:spring app.jar

# spring 유저로 전환
USER spring:spring

# ── 헬스 체크 ────────────────────────────────────────────────
# Docker가 컨테이너 상태를 주기적으로 확인
# K8s의 livenessProbe와 별개로 Docker 레벨에서도 확인
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 컨테이너가 사용하는 포트 문서화 (실제 포트 열기는 docker run -p로)
EXPOSE 8080

# ── JVM 튜닝 옵션으로 앱 실행 ────────────────────────────────
# -XX:+UseContainerSupport: 컨테이너의 CPU/메모리 제한 인식 (K8s resources 반영)
# -XX:MaxRAMPercentage=75.0: 컨테이너 메모리의 75%를 JVM 힙으로 사용
# -Djava.security.egd=...: 난수 생성기 성능 향상 (Spring Boot 시작 속도 개선)
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "app.jar"]
