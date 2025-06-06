# 通用Spring Boot Dockerfile模板
FROM alpine/git AS cloner
ARG REPO_URL
ARG BRANCH=main
WORKDIR /src
RUN git clone --branch ${BRANCH} --depth 1 ${REPO_URL} .  # 浅克隆加速[1,3](@ref)

# 阶段2：应用构建（如Maven）
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
 #从克隆阶段复制代码
COPY --from=cloner /src .
RUN mvn clean package -DskipTests  # 构建命令[6,8](@ref)

# 阶段3：运行时（仅JRE）
# 推荐使用JRE基础镜像减小体积[7](@ref)
FROM bellsoft/liberica-runtime-container:jre-21-musl

# 动态参数声明（构建时传入）
# 默认端口可覆盖
ARG APP_PORT=8080

# 标准化目录结构
WORKDIR /app
COPY --from=builder /build/target/*.jar ./app.jar

# 统一运行时配置
EXPOSE ${APP_PORT}
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]