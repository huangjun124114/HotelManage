# HotelManage Docker 部署指南

## 方案一：本地构建 → 推送到腾讯云 CCR → 轻量服务器拉取运行

### 1. 登录腾讯云容器镜像仓库
```bash
# 登录（替换为你的地域，如 ap-guangzhou）
docker login ccr.ccs.tencentyun.com --username=<腾讯云账号ID>
# 密码在腾讯云控制台「访问管理 → API密钥 → 密钥管理」获取
```

### 2. 构建并推送镜像
```bash
cd /path/to/HotelManage

# 构建镜像（标签使用你的 CCR 地址）
docker build -t ccr.ccs.tencentyun.com/<命名空间>/hotelmanage:latest .

# 推送到腾讯云 CCR
docker push ccr.ccs.tencentyun.com/<命名空间>/hotelmanage:latest
```

### 3. 轻量应用服务器部署
在腾讯云轻量服务器控制台：
- 选择「使用容器镜像」
- Docker镜像：`ccr.ccs.tencentyun.com/<命名空间>/hotelmanage:latest`
- 端口映射：80（前端）+ 8081（后端）
- 数据卷：本地 `/data` 映射到容器 `/app/data`（持久化数据库）

### 4. 访问
- 前端：http://<轻量服务器公网IP>
- 后端 API：http://<轻量服务器公网IP>:8081/api

---

## 方案二：SSH 登录轻量服务器后 docker-compose 部署

### 1. 把项目文件传到服务器
```bash
scp -r HotelManage/ root@<服务器IP>:/opt/
```

### 2. SSH 登录后启动
```bash
ssh root@<服务器IP>
cd /opt/HotelManage
docker-compose up -d
```

---

## 数据持久化说明
- 数据库文件 `data/linxi.db` 映射到容器外 `/app/data`
- 重新部署时数据不会丢失
- 备份：`docker cp hotelmanage:/app/data/linxi.db ./backup/`

## 日志查看
```bash
# 后端日志
docker exec hotelmanage tail -f /app/backend.log

# Nginx 日志
docker exec hotelmanage tail -f /var/log/nginx/access.log
```
