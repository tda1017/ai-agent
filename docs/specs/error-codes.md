# 错误码规范（统一返回结构）

本项目后端所有接口统一采用结构：

- code: 数字型业务码（不等同于 HTTP 状态码）
- message: 简短的人类可读消息（可本地化）
- data: 业务数据（出错时通常为 null）

示例：

```
成功：
{ "code": 200, "message": "success", "data": { ... } }

失败：
{ "code": 1001, "message": "用户名或密码错误", "data": null }
```

实现文件：
- 统一响应：`src/main/java/com/xin/aiagent/common/Result.java`
- 错误码枚举：`src/main/java/com/xin/aiagent/common/ResultCode.java`
- 业务异常：`src/main/java/com/xin/aiagent/common/BusinessException.java`
- 全局异常拦截：`src/main/java/com/xin/aiagent/common/GlobalExceptionHandler.java`

## 编码规则

- 200：成功（与 HTTP 200 语义一致）
- 4xx/5xx：仅为 HTTP 状态，不作为业务码直接返回；业务码统一由 `ResultCode` 定义
- 业务码区间（十进制）：
  - 1000–1999：认证/校验通用错误
  - 2000–2999：用户/账户域
  - 3000–3999：聊天/对话域
  - 9000–9999：系统/基础设施错误

注意：数字一旦发布不得变更。新增时只追加，不复用已使用过的数字。

## 当前已定义业务码

- 基础与映射：
  - 200 OK
  - 400 BAD_REQUEST（用于 HTTP → 业务映射时参考，不直接作为 `Result.code` 使用）
  - 401 UNAUTHORIZED
  - 403 FORBIDDEN
  - 404 NOT_FOUND
  - 409 CONFLICT
  - 500 INTERNAL_ERROR

- 认证与校验（1000+）：
  - 1000 VALIDATION_ERROR：参数校验失败
  - 1001 INVALID_CREDENTIALS：用户名或密码错误
  - 1002 USER_NOT_FOUND_OR_DISABLED：用户不存在或被禁用
  - 1003 REGISTER_FAILED：注册失败
  - 1004 USERNAME_EXISTS：用户名已存在
  - 1005 EMAIL_EXISTS：邮箱已存在

> 如需扩展，请在相应域的区间内追加，保持语义清晰、唯一。

## HTTP 状态与业务码的关系

由 `GlobalExceptionHandler` 负责将异常映射为 HTTP 状态与业务码：

- `BusinessException`：按 `ResultCode` 映射到合适的 HTTP 状态
  - 1000/1004/1005 → HTTP 400
  - 1001/1002 → HTTP 401
  - 其他未分类 → HTTP 500
- 参数校验异常（`@Valid`/`@Validated`）：HTTP 400，`code=1000`
- 鉴权失败（BadCredentials）：HTTP 401，`code=1001`
- 无权限（AccessDenied）：HTTP 403，对应 `FORBIDDEN`
- 未捕获异常：HTTP 500，`code=500`

前端仅需统一判断：`code == 200` 表示成功；否则读取 `message` 提示用户。

## 使用规范

- 业务失败：抛出 `BusinessException(ResultCode, message)`；message 可按场景覆盖默认文案。
- 参数错误：使用 `@Valid`/`@Validated`，由全局处理器统一返回 `code=1000`。
- 禁止将堆栈/内部细节透出给 `message`；日志记录由服务端完成。

## 新增错误码流程

1) 在 `ResultCode` 中新增枚举常量：唯一数字 + 简短英文默认文案
2) 在本文件“当前已定义业务码”中登记
3) 如果需要特别的 HTTP 映射，在 `GlobalExceptionHandler` 中补充规则
4) 关联的 Controller/Service 抛出 `BusinessException`

命名建议：领域 + 语义（如 `USER_LOCKED`、`CHAT_RATE_LIMITED`），数字按区间顺序递增。

## 返回示例

- 成功：
```
{
  "code": 200,
  "message": "注册成功",
  "data": { "id": 1, "username": "test" }
}
```

- 用户名重复：
```
{
  "code": 1004,
  "message": "用户名已存在",
  "data": null
}
```

- 密码错误：
```
{
  "code": 1001,
  "message": "用户名或密码错误",
  "data": null
}
```

## 向后兼容

- “Never break userspace”：发布后的数字不可更改
- 仅允许追加新码；旧码如需废弃，标注为 Deprecated 并保留一段时间
