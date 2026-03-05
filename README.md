# 宠物信息管理系统 - 启动说明

## 一、环境要求

请先安装并确保可用：

| 软件 | 版本要求 |
|------|----------|
| JDK | 25 |
| Maven | 3.8 或更高 |
| Node.js | 18 或更高 |
| pnpm | 8 或更高（可用 `npm i -g pnpm` 安装） |
| MySQL | 8.x，且服务已启动 |

---

## Docker 一键部署（推荐）

在项目根目录执行：

```bash
docker compose up -d --build
```

访问地址：

- 前端：http://localhost:3000
- 后端 API：http://localhost:8080/api

常用命令：

```bash
docker compose ps
docker compose logs -f
docker compose down
docker compose down -v
```

---

## 二、启动步骤

### 第一步：初始化数据库（首次运行必须执行）

在项目根目录下执行（将 `你的MySQL密码` 换成实际密码）：

**Windows（PowerShell 或 CMD）：**
```powershell
mysql -u root -p < backend\src\main\resources\init.sql
```
输入 MySQL 密码后回车，脚本会创建数据库 `pet_mgmt`、建表并插入默认管理员账号。

**Linux / Mac：**
```bash
mysql -u root -p < backend/src/main/resources/init.sql
```

### 第二步：配置数据库密码（可选）

后端默认使用 MySQL 密码 `123456`。若你的密码不是 123456，有两种方式：

**方式 A：改配置文件**

编辑 `backend/src/main/resources/application.yml`，修改：
```yaml
spring:
  datasource:
    password: 你的MySQL密码
```

**方式 B：用环境变量（推荐）**

- **Windows PowerShell：**
  ```powershell
  $env:DB_PASSWORD="你的MySQL密码"
  ```
- **Linux / Mac：**
  ```bash
  export DB_PASSWORD=你的MySQL密码
  ```
设置后再启动后端即可。

### 第三步：启动后端

**Windows（PowerShell）：**
```powershell
cd backend
mvn spring-boot:run
```

**Linux / Mac：**
```bash
cd backend
mvn spring-boot:run
```

看到类似 `Started PetMgmtApplication` 且无报错即表示后端已启动，默认地址：**http://localhost:8080**。

### 第四步：启动前端

**新开一个终端**，在项目根目录下执行：

**Windows（PowerShell）：**
```powershell
cd frontend
pnpm install
pnpm dev
```

**Linux / Mac：**
```bash
cd frontend
pnpm install
pnpm dev
```

看到 `Local: http://localhost:5173/` 即表示前端已启动。

---

## 三、访问系统

1. 浏览器打开：**http://localhost:5173**
2. 使用默认管理员登录：
   - **用户名**：`admin`
   - **密码**：`admin123`

---

## 四、一键启动（可选）

若已配置好数据库且密码为 123456，可在项目根目录用两个终端分别执行：

**终端 1 - 后端：**
```powershell
cd backend; mvn spring-boot:run
```

**终端 2 - 前端：**
```powershell
cd frontend; pnpm dev
```

（Linux/Mac 把分号 `;` 换成 `&&` 即可。）

---

## 五、常见问题

| 现象 | 处理办法 |
|------|----------|
| 后端报错 “Access denied for user” | 检查 MySQL 是否启动；在 `application.yml` 或环境变量中确认密码正确。 |
| 后端报错 “Unknown database 'pet_mgmt'” | 未执行初始化脚本，请先执行 **第一步** 的 `init.sql`。 |
| 前端能打开但登录/接口报错 | 确认后端已启动（http://localhost:8080），且前端是用 `pnpm dev` 启动（会走代理到后端）。 |
| 端口 8080 或 5173 被占用 | 关闭占用该端口的程序，或修改后端端口（application.yml 中 `server.port`）、前端端口（vite.config.ts 中 `server.port`）。 |

---

## 六、生产环境打包与运行

**后端打包：**
```bash
cd backend
mvn clean package -DskipTests
java -jar target/pet-mgmt-0.0.1-SNAPSHOT.jar
```

**前端打包：**
```bash
cd frontend
pnpm build
```
将 `dist` 目录部署到 Nginx 等 Web 服务器，并将 `/api` 反向代理到后端 8080 端口。详见 `DEPLOYMENT.md`。

---

**文档更新日期**：2026-02-25
