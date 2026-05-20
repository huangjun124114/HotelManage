# HotelManage 全栈部署镜像
# 基于 Nginx 托管前端，同时运行后端 Spring Boot 服务

FROM nginx:alpine

# 安装 Java 运行时和 SQLite 依赖
RUN apk add --no-cache openjdk17-jre sqlite-libs

# 工作目录
WORKDIR /app

# 复制后端 JAR
COPY target/linxi-backend-1.0.0.jar /app/backend.jar
COPY data/linxi.db /app/data/linxi.db

# 复制前端构建产物到 Nginx 目录
COPY frontend/dist /usr/share/nginx/html

# 复制 Nginx 配置（后端 API 反向代理）
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf

# 复制启动脚本
COPY deploy/start.sh /app/start.sh
RUN chmod +x /app/start.sh

# 暴露端口（80=前端, 8081=后端）
EXPOSE 80 8081

# 启动后端 + Nginx
CMD ["/app/start.sh"]
