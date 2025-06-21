# 通用Spring Boot Dockerfile模板
FROM alpine/git AS cloner
ARG REPO_URL
ARG BRANCH=main
WORKDIR /src

# 浅克隆加速[1,3](@ref)
RUN git clone --branch ${BRANCH} --depth 1 ${REPO_URL} .  && \
    # 获取提交ID和消息[1](@ref)
    git log -1 --format='%H %s' > commit-info.txt

# 阶段2：应用构建（如Maven）
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
 #从克隆阶段复制代码
COPY --from=cloner /src .

# 输出提交信息到构建日志（关键修改）
RUN cp commit-info.txt . && \
    echo "当前构建的提交信息: $(cat commit-info.txt)" && \
    mvn clean package -DskipTests  # 构建命令[6,8](@ref)

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