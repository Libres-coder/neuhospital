# Hospital Backend — Local Dev

## 前置条件

- JDK 17+（已验证 JDK 23 可用）
- Maven 3.9+（已配置 `D:\maven\apache-maven-3.9.9-bin\apache-maven-3.9.9`）
- MySQL 8.0（已检测可用，路径 `D:\MySQL\MySQL Server 8.0`）

## 第一次启动

### 1. 填 MySQL 密码

编辑 `server\src\main\resources\application.yml`，把

```yaml
password: CHANGE_ME
```

改成你的实际密码（空密码就保留空字符串 `""`）。

### 2. 启动后端

```bash
cd d:\neusoft_hospital\server
mvn spring-boot:run
```

启动成功后会自动：
- 通过 JDBC URL 的 `createDatabaseIfNotExist=true` 创建 `neusoft_hospital` 库
- 通过 Flyway 执行 `V1` ~ `V4` migration，建表 + 导入 13 科室 + 13 医生数据

### 3. 打开 Swagger UI

浏览器访问 http://localhost:8080/swagger-ui.html 即可看到所有接口。

## 联调认证模块（curl 模拟 Android）

```bash
# 1. 发送验证码
curl -X POST http://localhost:8080/api/auth/sms \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"13800000000\"}"

# 2. 用 123456 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"13800000000\",\"code\":\"123456\"}"
# 把返回的 token 复制下来
$TOKEN=eyJhbGciOi...

# 3. 调 /me 验证 token 有效
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"

# 4. 验证身份证
curl -X POST http://localhost:8080/api/auth/verify-idcard \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"张三\",\"idCard\":\"110101199001011234\"}"

# 5. 绑定电子健康卡
curl -X POST http://localhost:8080/api/auth/bind-ehs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"ehsCardNo\":\"EHS-2026-0001\"}"
```

期望第 3 步返回的 `isVerified: true`、`hasEhsCard: true`。

## Android 端切换到真实后端

编辑 `app\src\main\java\com\example\neusoft_hospital\core\network\ApiProvider.kt`：

```kotlin
var useMock: Boolean = false   // 改为 false
var backendBaseUrl: String = "http://192.168.x.x:8080/"  // 改成电脑局域网 IP
```

然后重新 `gradle :app:assembleDebug`，安装 APK 到手机。

> 模拟器调试时，电脑本机 = `10.0.2.2`；真机调试时，电脑本机 = `ipconfig` 看到的 IPv4。

## 一键起 Docker（推荐）

项目根目录提供 `docker-compose.yml`，把 MySQL + 后端打成一个 stack：

```bash
# 1. 复制 .env 模板（可选）
cp .env.example .env

# 2. 启动
docker compose up -d --build

# 3. 看日志
docker compose logs -f hospital
docker compose logs -f mysql

# 4. 关掉
docker compose down            # 保留 MySQL 数据卷
docker compose down -v         # 同时清掉数据卷（生产慎用）
```

### 默认端口映射
| 服务 | 容器端口 | 主机端口 | 备注 |
|---|---|---|---|
| hospital | 8090 | **8090** | Spring Boot |
| mysql | 3306 | **3307** | 避免和本机已跑的 MySQL 冲突 |

### 默认账号
- 应用账号 `hospital / hospital_pass`（在 `docker-compose.yml` 里覆盖）
- 验证码 `123456`（开发模式；生产应换成真实短信网关）

### 健康检查
| 端点 | 公开？ | 用途 |
|---|---|---|
| `/actuator/health` | 是 | 综合状态 |
| `/actuator/health/liveness` | 是 | K8s liveness 探针 |
| `/actuator/health/readiness` | 是 | K8s readiness 探针 |
| `/actuator/info` | 是 | build / java / os 元数据 |
| `/actuator/**` (其它) | **否**（401） | 安全 |

### 镜像层
后端 Dockerfile 用 layered jar，每个 layer（dependencies / snapshot-dependencies / application）单独 COPY，只改源代码时 Docker 能复用前两层缓存，构建时间从分钟级降到秒级。