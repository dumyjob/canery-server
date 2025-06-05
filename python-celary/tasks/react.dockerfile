# 阶段1：构建环境
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
# 依赖锁定,避免版本漂移
RUN npm ci --frozen-lockfile
COPY . .
RUN npm run build  # 生成静态文件到 /app/dist

# 阶段2：运行环境
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
#自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]