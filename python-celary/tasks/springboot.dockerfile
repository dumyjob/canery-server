# 通用Spring Boot Dockerfile模板
# 推荐使用JRE基础镜像减小体积[7](@ref)
FROM bellsoft/liberica-runtime-container:jre-21-musl

# 动态参数声明（构建时传入）
# 默认端口可覆盖
ARG JAR_FILE
ARG APP_PORT=8080

# 标准化目录结构
WORKDIR /app
# 关键：通过变量注入JAR路径
COPY ${JAR_FILE} ./app.jar

# 统一运行时配置
EXPOSE ${APP_PORT}
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]