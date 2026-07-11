# 东软医院 APP - 前后端 API 契约（v0.1）

> Base URL（开发）：`http://192.168.x.x:8080`
> 所有响应统一格式：`{ "code": 0, "message": "ok", "data": T }`，`code != 0` 表示错误。
> 鉴权：除登录注册相关接口外，所有请求 header 必须带 `Authorization: Bearer <jwt>`。

---

## 一、认证模块（Auth）

### 1.1 发送短信验证码

```
POST /api/auth/sms
Content-Type: application/json

Request:
{
  "phone": "13800000000"        // 11 位手机号
}

Response 200:
{
  "code": 0,
  "message": "ok",
  "data": {
    "traceId": "uuid",          // 仅用于查询；开发模式下返回固定码 123456
    "ttlSeconds": 300
  }
}
```

**开发模式**：短信验证码固定为 `123456`，`traceId` 仅做日志查询用。

### 1.2 验证码登录（首次自动注册）

```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "phone": "13800000000",
  "code": "123456"
}

Response 200:
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOi...",   // JWT，TTL = 7 天
    "userId": "u_xxxxxx",
    "phone": "13800000000",
    "name": "用户_0042",         // 首次登录默认名
    "isVerified": false,
    "hasEhsCard": false
  }
}
```

**错误码**：
- `4001` 验证码错误或已过期
- `4002` 手机号格式错误

### 1.3 获取当前用户信息

```
GET /api/auth/me
Header: Authorization: Bearer <token>

Response 200:
{
  "code": 0,
  "message": "ok",
  "data": {
    "userId": "u_xxxxxx",
    "phone": "13800000000",
    "name": "用户_0042",
    "isVerified": false,
    "hasEhsCard": false
  }
}
```

### 1.4 实名认证（身份证 + 姓名）

```
POST /api/auth/verify-idcard
Header: Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "name": "张三",
  "idCard": "110101199001011234"
}

Response 200:
{
  "code": 0,
  "message": "ok",
  "data": {
    "verified": true
  }
}
```

### 1.5 绑定电子健康卡

```
POST /api/auth/bind-ehs
Header: Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "ehsCardNo": "EHS-2026-0001"
}

Response 200:
{
  "code": 0,
  "message": "ok",
  "data": {
    "ehsCardNo": "EHS-2026-0001"
  }
}
```

### 1.6 退出登录

```
POST /api/auth/logout
Header: Authorization: Bearer <token>

Response 200:
{
  "code": 0,
  "message": "ok",
  "data": null
}
```

> 当前为无状态 JWT，logout 仅客户端清 token，服务端无需维护黑名单。后续如需"踢人下线"可补 jti 列表。

---

## 二、待补：家庭成员 / 挂号 / AI / 随访模块

- `docs/api-contract.md` 将按模块持续追加；每个模块完成后会标记为 ✅