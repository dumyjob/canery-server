# 阶段1：克隆代码与安装依赖
FROM alpine/git AS git-clone
WORKDIR /src
# 克隆代码（支持私有仓库）
ARG REPO_URL
ARG BRANCH=main
RUN git clone --branch ${BRANCH} --depth 1 ${REPO_URL} .

# 阶段2：构建应用
FROM node:18-alpine AS builder
WORKDIR /app
# 从 git-clone 阶段复制代码
COPY --from=git-clone /src .
# 安装依赖并构建
RUN npm ci --frozen-lockfile && npm run build

# 阶段3：运行环境
FROM nginx:alpine
# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html
# 自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]