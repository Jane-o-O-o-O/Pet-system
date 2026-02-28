
# 宠物信息管理系统 - 部署指南

## 系统信息

- **前端**: Vue3 + Element Plus + Vite
- **后端**: Spring Boot 3.2.2 + MyBatis-Plus + Spring Security + JWT
- **数据库**: MySQL 8.x

## 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.x
- Maven 3.8+

## 快速启动

### 1. 初始化数据库

```bash
mysql -u root -p < backend/src/main/resources/init.sql
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认端口: 8080，数据库密码默认 123456。

通过环境变量自定义配置:

```bash
# Linux/Mac
export DB_PASSWORD=your_password
export JWT_SECRET=your_secret_key

# Windows PowerShell
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your_secret_key"
```

### 3. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端开发服务器默认端口: 5173，已配置 `/api` 代理到后端 8080 端口。

### 4. 生产部署

```bash
# 前端构建
cd frontend
pnpm build

# 后端打包
cd backend
mvn clean package -DskipTests
java -jar target/pet-mgmt-0.0.1-SNAPSHOT.jar
```

生产环境使用 Nginx 反代:
- 前端静态文件: `/usr/share/nginx/html`
- API 反代: `/api` -> `http://127.0.0.1:8080`

## 访问地址

- **前端应用**: http://localhost:5173 (开发) / http://localhost:3000 (生产)
- **后端API**: http://localhost:8080/api

## 默认账号

- 管理员: admin / admin123

## 数据库表

| 表名 | 说明 |
|------|------|
| sys_user | 系统用户表 |
| pet | 宠物表 |
| medical_record | 医疗记录表 |
| vaccine_record | 疫苗记录表 |
| boarding_order | 寄养订单表 |
| sys_config | 系统配置表 |
| notification | 通知表 |

## 角色权限

| 角色 | 说明 |
|------|------|
| ADMIN | 管理员 - 全量管理权限 |
| STAFF | 服务人员 - 医疗/疫苗录入，订单状态管理 |
| OWNER | 宠物主人 - 管理自己的宠物和订单 |
