# 宠物信息管理系统 - 文件存储版本

## 修改说明

本项目已从MySQL数据库存储改为JSON文件本地存储。

## 主要变更

### 1. 存储方式
- **之前**: 使用MySQL数据库 + MyBatis-Plus
- **现在**: 使用JSON文件存储在本地 `data/` 目录

### 2. 数据文件位置
应用运行后，数据将存储在以下JSON文件中：
```
data/
  ├── users.json              # 用户数据
  ├── pets.json               # 宠物数据
  ├── medical_records.json    # 医疗记录
  ├── vaccine_records.json    # 疫苗记录
  ├── boarding_orders.json    # 寄养订单
  ├── notifications.json      # 通知消息
  └── sys_configs.json        # 系统配置
```

### 3. 启动方式

#### 方式一：使用Maven
```bash
cd backend
mvn spring-boot:run
```

#### 方式二：使用jar包
```bash
cd backend
java -jar target/pet-mgmt-0.0.1-SNAPSHOT.jar
```

### 4. 默认管理员账号
- 用户名: `admin`
- 密码: `admin123`

应用首次启动时会自动创建管理员账号。

### 5. 访问地址
- 后端API: http://localhost:8080
- API文档: 参考原有文档

## 技术变更

### 移除的依赖
- ~~MySQL Connector~~
- ~~MyBatis-Plus~~

### 新增组件
- `FileStorage`: JSON文件存储服务
- `Repository`: 数据访问层（替代Mapper）
- `PageHelper`: 分页辅助类

### 保留的功能
- ✅ JWT身份认证
- ✅ 角色权限控制
- ✅ 宠物档案管理
- ✅ 医疗记录管理
- ✅ 疫苗提醒功能
- ✅ 寄养订单管理
- ✅ 数据统计报表
- ✅ 通知消息系统

## 注意事项

1. **数据持久化**: 所有数据保存在JSON文件中，删除`data/`目录将丢失所有数据
2. **并发安全**: 已实现读写锁保证多线程安全
3. **ID生成**: 自动维护自增ID
4. **性能**: 适合中小规模数据（建议单表<10000条记录）

## 数据备份

建议定期备份`data/`目录：
```bash
# Windows
xcopy data data_backup /E /I

# Linux/Mac
cp -r data data_backup
```

## 恢复到数据库版本

如需恢复使用MySQL数据库，请查看Git历史记录中的上一个版本。
