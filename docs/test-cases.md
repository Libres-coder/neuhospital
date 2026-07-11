# 东软医院 APP - 验收用例清单（端到端）

> 所有用例假设：
> - 后端已 `mvn spring-boot:run` 启动在 `localhost:8080`
> - Android `ApiProvider.useMock = false`、`backendBaseUrl = "http://<电脑局域网IP>:8080/"`
> - 手机和电脑在同一 WiFi

## 1. 新用户注册登录

| 步骤 | 操作 | 期望 |
|---|---|---|
| 1.1 | 打开 APP → 看到登录页 | 默认在登录页 |
| 1.2 | 输入手机号 `13800000001` → 点击"发送验证码" | Toast "验证码已发送" |
| 1.3 | 输入验证码 `123456` → 点击登录 | 自动跳转首页 |
| 1.4 | 查看 MySQL：`SELECT * FROM users;` | 新增一条 `phone='13800000001'` 的记录 |

后端调用栈：
```
POST /api/auth/sms   → 200 OK
POST /api/auth/login → 200 OK, 返回 token
```

## 2. 实名认证 + 绑电子健康卡

| 步骤 | 操作 | 期望 |
|---|---|---|
| 2.1 | 我的 → 实名认证 | 进入实名页 |
| 2.2 | 输入姓名"张三" + 18 位身份证 → 提交 | "认证成功" |
| 2.3 | 我的 → 电子健康卡 | 输入卡号 → "绑定成功" |
| 2.4 | MySQL：`SELECT is_verified, ehs_bound FROM users WHERE phone='13800000001';` | 两列都为 1 |

## 3. 挂号流程（核心链路）

| 步骤 | 操作 | 期望 |
|---|---|---|
| 3.1 | 首页 → 点"内科"卡片 | 进入内科子科室列表 |
| 3.2 | 点"心血管内科" | 看到 2 位医生（李建华、王玉梅） |
| 3.3 | 点"李建华" | 进入医生详情，看到 7 天排班（每 15 分钟一个时段） |
| 3.4 | 选某个时段 → 点"下一步" | 进入预约确认页 |
| 3.5 | 点"确认预约" | 跳转"我的预约"页，看到一条新预约 |
| 3.6 | MySQL：`SELECT * FROM appointments WHERE patient_id=(...);` | 看到一条新预约，status='payed' |

后端调用栈：
```
GET  /api/departments?parentId=null
GET  /api/departments?parentId=d1
GET  /api/doctors?departmentId=d1_1
GET  /api/doctors/doc1
POST /api/appointments  → 返回预约实体
GET  /api/appointments/mine
```

## 4. AI 预问诊 + 智能推荐

| 步骤 | 操作 | 期望 |
|---|---|---|
| 4.1 | 首页 → 点"智能问诊"卡片 | 进入预问诊首页 |
| 4.2 | 点"开始症状采集" | 进入聊天页，输入"胸痛" |
| 4.3 | 发送 2 条症状后点"完成问诊" | 跳转分诊结果页 |
| 4.4 | 看到 1-3 个推荐科室（含"心血管内科"）+ 1-3 个可能疾病 | 概率合理 |

后端调用栈：
```
POST /api/preconsult/triage
```

## 5. AI 助手聊天

| 步骤 | 操作 | 期望 |
|---|---|---|
| 5.1 | 底部 Tab 切到"AI助手" | 进入 AI 聊天页 |
| 5.2 | 输入"我胃痛怎么办" → 发送 | 几秒内收到 AI 回复（本地兜底或后端调用） |
| 5.3 | 进入"对话历史"页 | 看到刚创建的会话 |
| 5.4 | MySQL：`SELECT * FROM chat_sessions WHERE patient_id=(...);` | 一条记录，title 含"胃痛" |

后端调用栈：
```
POST /api/aichat/send
GET  /api/aichat/sessions
```

## 6. 术后随访 + 慢病管理

| 步骤 | 操作 | 期望 |
|---|---|---|
| 6.1 | 底部 Tab 切到"健康" | 进入随访首页 |
| 6.2 | 点"创建随访计划" → 填疾病"骨折术后" + 手术日期 → 提交 | 列表新增一条计划 |
| 6.3 | 点进计划 → 看到 7/14/30/60/90 天的任务 | 5 个任务点 |
| 6.4 | 点"高血压"卡片 | 进入慢病看板 |
| 6.5 | 点右上"+" → 填收缩压 190、舒张压 120 → 提交 | 跳转回看板，新记录被标记红色（危险） |
| 6.6 | 顶部"告警" → 看到刚生成的告警 | alert level=3 |

后端调用栈：
```
POST /api/followup/plans
GET  /api/followup/plans
GET  /api/followup/plans/{id}/tasks
POST /api/chronic/records
GET  /api/chronic/alerts
```

## 验收通过标准

✅ 6 个用例全部跑通
✅ MySQL 8 张表都有真实数据（users / appointments / chat_sessions / follow_up_plans 等）
✅ Android UI 无 ANR / Crash
✅ Token 持久化：杀掉 APP 重启无需重新登录

## 已知限制

- 验证码固定为 `123456`，生产环境需要接真实 SMS 网关
- 慢病阈值规则较简单（高血压/糖尿病），更多病种需要扩展 `FollowUpService.computeAlertLevel`
- 没用 Redis：验证码/会话依赖 Caffeine 内存缓存，单机重启会丢失
- 没用 WebSocket：聊天历史靠客户端每次 refresh 拉取
- 没做"踢下线"：JWT 默认 7 天有效；如需提前失效需要引入 token 黑名单