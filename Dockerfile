# ================================
# Stage 1: Build
# ================================
FROM gradle:8.12-jdk17-alpine AS builder

WORKDIR /app

# 의존성 캐싱 레이어 (build.gradle 변경 없으면 재사용)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew

# 의존성 미리 다운로드 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

# 소스 복사 및 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ================================
# Stage 2: Run
# ================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 보안: non-root 유저로 실행
RUN addgroup -S sparring && adduser -S sparring -G sparring

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 소유권 설정
RUN chown sparring:sparring app.jar

USER sparring

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
