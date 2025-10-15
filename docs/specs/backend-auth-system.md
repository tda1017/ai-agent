# 后端认证授权系统实现规范

## 1. 概述

为AI Agent项目添加用户认证授权功能，包括用户注册、登录、权限控制。采用Spring Security + JWT实现。

**核心原则（Linus式思考）**：
- 数据结构第一：用户-角色-权限的三层关系必须清晰
- 消除特殊情况：统一使用JWT，不搞Session和Token两套
- 保持简单：只做必需的功能，不过度设计
- 零破坏性：不影响现有AI对话功能，只添加保护层

---

## 实现状态标注（截至 2025-10-15）

- 数据库/模型
  - [x] M0 `users` 表与实体（MyBatis-Plus）
  - [ ] `role`/`permission` 及关联表（规划中，未实现）
- 安全链路
  - [x] `JwtTokenProvider` 生成/解析、`JwtAuthenticationFilter` 注入认证
  - [x] `SecurityFilterChain` 无状态、白名单放行 `/api/auth/**`、其余 `/api/**` 需认证
  - [ ] `CustomUserDetailsService`/`SecurityUser`（未实现，当前不依赖）
  - [~] JWT 载荷含 `roles`/`permissions`，过滤器仅将 `roles` 映射为 Authorities；`permissions` 未使用
  - [ ] 方法级鉴权（`@PreAuthorize`）未接入，`chat:use` 未强制到方法级
- 接口
  - [x] `POST /api/auth/register`（已实现：用户名/邮箱唯一校验 + BCrypt）
  - [x] `POST /api/auth/login`（已实现：发放 JWT + 基础用户信息）
  - [~] `GET /api/auth/getUserInfo`（占位仅返回 OK，未回显用户详情/角色/权限）
  - [ ] `POST /api/auth/logout`（未实现）
  - [ ] 刷新 Token（未实现；前端有占位逻辑，后端无接口）
- DTO/返回结构
  - [x] 统一返回 `Result`/`ResultCode` 与全局异常处理器
  - [~] DTO 未抽离到独立类，当前登录/注册请求体为 Controller 内部类
- 前端
  - [x] 统一请求封装与业务码判断（`httpClient` + `utils/errorCodes`）
  - [x] 登录/注册页错误提示接入
  - [ ] 其它页面错误提示统一化（如聊天页）
- 文档与实现偏差
  - [~] 文档含 JPA 示例配置；实际使用 MyBatis-Plus，JPA 未启用
  - [ ] Phase 5 测试用例（单测/集成）未实现

## 2. 数据库设计

### 2.1 核心表结构

```sql
-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    nickname VARCHAR(50) COMMENT '昵称',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    role_key VARCHAR(50) NOT NULL UNIQUE COMMENT '角色标识（如ROLE_ADMIN）',
    description VARCHAR(200) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role_key (role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户-角色关联表
CREATE TABLE user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 权限表（可选，初期可以简化为只用角色）
CREATE TABLE permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_name VARCHAR(50) NOT NULL UNIQUE COMMENT '权限名称',
    permission_key VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识（如chat:create）',
    description VARCHAR(200) COMMENT '权限描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_permission_key (permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 角色-权限关联表
CREATE TABLE role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 初始化数据
INSERT INTO role (role_name, role_key, description) VALUES
('管理员', 'ROLE_ADMIN', '系统管理员，拥有所有权限'),
('普通用户', 'ROLE_USER', '普通用户，可以使用AI对话功能');

INSERT INTO permission (permission_name, permission_key, description) VALUES
('AI对话', 'chat:use', '使用AI对话功能'),
('用户管理', 'user:manage', '管理用户'),
('系统配置', 'system:config', '配置系统参数');

-- 管理员拥有所有权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3);

-- 普通用户只有对话权限
INSERT INTO role_permission (role_id, permission_id) VALUES
(2, 1);
```

### 2.2 数据库配置文件

在 `src/main/resources/application.yml` 中添加：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: none  # 不自动生成表，手动执行SQL
    show-sql: true
    properties:
      hibernate:
        format_sql: true

# JWT配置
jwt:
  secret: your-secret-key-min-256-bits-for-HS256-algorithm-security
  expiration: 86400000  # 24小时（毫秒）
  header: Authorization
  token-prefix: Bearer 
```

---

## 3. 技术实现方案

### 3.1 依赖添加

在 `pom.xml` 中添加：

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<!-- MySQL驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- MyBatis-Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.7</version>
    </dependency>
```

### 3.2 包结构设计

```
com.xin.aiagent/
├── entity/              # 实体类
│   ├── User.java
│   ├── Role.java
│   ├── Permission.java
│   ├── UserRole.java
│   └── RolePermission.java
├── mapper/              # 数据访问层（MyBatis-Plus）
│   └── UserMapper.java
├── service/             # 业务逻辑层
│   ├── UserService.java
│   ├── AuthService.java
│   └── impl/
│       ├── UserServiceImpl.java
│       └── AuthServiceImpl.java
├── controller/          # 控制器层（已存在）
│   ├── AuthController.java      # 新增：认证相关API
│   └── ChatController.java      # 已存在：AI对话API
├── security/            # 安全配置
│   ├── SecurityConfig.java      # Spring Security配置
│   ├── JwtTokenProvider.java    # JWT工具类
│   ├── JwtAuthenticationFilter.java  # JWT过滤器
│   ├── CustomUserDetailsService.java # 用户详情服务
│   └── SecurityUser.java        # Security用户包装类
├── dto/                 # 数据传输对象
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── LoginResponse.java
│   └── UserDTO.java
└── common/              # 通用类
    ├── Result.java      # 统一响应结果
    └── ResultCode.java  # 响应码枚举
```

### 3.3 核心功能实现

#### 3.3.1 认证流程

1. **注册**：
   - 接收用户名、密码、邮箱
   - 密码使用BCrypt加密
   - 默认分配`ROLE_USER`角色
   - 返回用户基本信息

2. **登录**：
   - 验证用户名密码
   - 生成JWT token
   - 返回token和用户信息

3. **访问保护接口**：
   - 前端在请求头中携带`Authorization: Bearer <token>`
   - 后端通过JwtAuthenticationFilter验证token
   - 解析token获取用户身份
   - 注入到SecurityContext供业务代码使用

#### 3.3.2 权限控制方案

使用注解方式：
```java
// 方法级别权限控制
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) { }

@PreAuthorize("hasAuthority('chat:use')")
public String chat(String message) { }

// 或使用更简单的
@Secured("ROLE_ADMIN")
public void adminOnly() { }
```

#### 3.3.3 白名单配置

不需要认证的接口：
- `/api/auth/login` - 登录
- `/api/auth/register` - 注册
- `/doc.html`, `/swagger-ui/**` - API文档
- `/error` - 错误页面

---

## 4. 实现步骤

### Phase 0: 极简实现（MySQL 版，建议优先）
1. 依赖与配置：
   - 新增依赖：`spring-boot-starter-security`、`spring-boot-starter-data-jpa`、`mysql-connector-j`、`jjwt`（或等价 JWT 库）。
   - `application.yml` 新增数据源与 JPA：
     ```yaml
     spring:
       datasource:
         url: jdbc:mysql://localhost:3306/ai_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
         username: root
         password: <your_password>
         driver-class-name: com.mysql.cj.jdbc.Driver
       jpa:
         hibernate:
           ddl-auto: update   # 开发态自动建表/更新，降低上手成本
         show-sql: true
         properties:
           hibernate:
             format_sql: true
     ```
   - JWT 配置同前文（secret、有效期、header、prefix）。

2. 最小表结构（仅用户表，先不引入角色/权限表）：
   - 为避免保留字冲突，优先使用表名 `users`。
   - DDL：
     ```sql
     CREATE TABLE IF NOT EXISTS users (
       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
       username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
       password VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
       email VARCHAR(100) UNIQUE COMMENT '邮箱',
       status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       INDEX idx_username (username)
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（M0 极简）';
     ```
   - 初始化一名演示用户（示例 hash 为 `demo`，请在本地用 `BCryptPasswordEncoder` 生成后替换）：
     ```sql
     INSERT INTO users (username, password, email) VALUES
     ('demo', '$2a$10$REPLACE_WITH_YOUR_BCRYPT_HASH', 'demo@example.com');
     ```

3. 业务与安全链路：
   - `User` 实体与 `UserRepository`（`findByUsername`/`findByUsernameAndStatus`）。
   - `AuthService.login(username,password)`：查库→BCrypt 比对→签发 JWT（载荷含 `sub/uid/roles/permissions/exp`，其中 `roles=['ROLE_USER']`、`permissions=['chat:use']` 可先写死）。
   - `SecurityFilterChain`：`csrf().disable()`、`cors()`、`sessionManagement(STATELESS)`；放行 `/api/auth/**` 与 `OPTIONS`；其余 `/api/**` 需认证。
   - `JwtAuthenticationFilter`：解析 `Authorization: Bearer <jwt>`，通过后写入 `SecurityContext`。

4. 最小接口：
   - `POST /api/auth/login`：请求读库校验后签发 JWT；响应 `{ token, tokenType: 'Bearer', expiresIn, user }`。
   - `GET /api/auth/me`：从 JWT 获取 `uid` 回显（可选二次查库刷新用户状态/昵称）。
   - 注册/刷新/退出暂不实现（后续里程碑再补）。

5. 保护现有接口：
   - 对 `ChatController` 的 `/api/**` 路由启用 `authenticated()` 保护；SSE 路由维持不变。
   - 方法级鉴权先不启用，待引入角色/权限表后再加 `@PreAuthorize`。

6. 联调验证：
   - 前端登录→保存 token→携带访问受保护接口；校验 401→登录重定向闭环；验证 SSE 路由可用。
   - 数据库层面验证密码比对、禁用用户（`status=0`）不可访问。

### Phase 1: 数据库和基础实体
1. 创建MySQL数据库`ai_agent`
2. 执行SQL脚本创建表和初始化数据
3. 创建用户实体（支持 MyBatis-Plus 注解）
4. 创建数据访问（MyBatis-Plus Mapper）

### Phase 2: JWT工具和Security配置
1. 添加Maven依赖
2. 配置`application.yml`
3. 实现JwtTokenProvider（生成、解析token）
4. 实现CustomUserDetailsService（加载用户信息）
5. 配置SecurityConfig（白名单、过滤器链）
6. 实现JwtAuthenticationFilter

### Phase 3: 认证服务和接口
1. 实现AuthService（登录、注册逻辑，基于 MyBatis-Plus）
2. 实现UserService（用户管理）
3. 创建AuthController（登录、注册API）
4. 创建DTO和统一响应类

### Phase 4: 现有接口保护
1. 在ChatController添加`@PreAuthorize("hasAuthority('chat:use')")`
2. 测试未登录访问被拦截
3. 测试登录后可正常访问

### Phase 5: 测试和验证
1. 单元测试（Service层）
2. 集成测试（API层）
3. 手动测试完整流程

---

## 5. API设计

### 5.1 认证相关接口

```
POST /api/auth/register
Body: {
  "username": "test",
  "password": "123456",
  "email": "test@example.com",
  "nickname": "测试用户"
}
Response: {
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "test",
    "nickname": "测试用户"
  }
}

POST /api/auth/login
Body: {
  "username": "test",
  "password": "123456"
}
Response: {
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "test",
      "nickname": "测试用户",
      "roles": ["ROLE_USER"]
    }
  }
}

GET /api/auth/getUserInfo
Header: Authorization: Bearer <token>
Response: {
  "code": 200,
  "data": {
    "id": 1,
    "username": "test",
    "nickname": "测试用户",
    "roles": ["ROLE_USER"],
    "permissions": ["chat:use"]
  }
}

POST /api/auth/logout
Header: Authorization: Bearer <token>
Response: {
  "code": 200,
  "message": "退出成功"
}
```

### 5.2 现有接口改造

```
POST /api/chat/stream
Header: Authorization: Bearer <token>
权限要求: chat:use
```

---

## 6. 安全考虑

1. **密码安全**：
   - 使用BCrypt加密，永不明文存储
   - 注册时验证密码强度（可选）

2. **Token安全**：
   - JWT secret至少256位
   - Token有效期24小时
   - 前端存储在localStorage，使用时放在Header

3. **防止常见攻击**：
   - SQL注入：使用JPA参数化查询
   - XSS：前端对输出进行转义
   - CSRF：使用JWT无状态认证，不需要CSRF保护

4. **用户状态管理**：
   - 支持禁用用户（status=0）
   - 禁用后即使token有效也无法访问

---

## 7. 潜在问题和简化方案

### 问题1：权限表是否必需？
**Linus式判断**：初期不需要，权限直接硬编码在代码里。

**理由**：
- 权限不会频繁变化，硬编码更直接
- 减少数据库查询和join复杂度
- 等真正需要动态权限管理再加

**简化后的方案**：
```java
// 直接在代码中定义权限
public enum UserRole {
    ADMIN(Arrays.asList("chat:use", "user:manage", "system:config")),
    USER(Arrays.asList("chat:use"));
    
    private final List<String> permissions;
}
```

### 问题2：是否需要刷新token机制？
**Linus式判断**：不需要，24小时够长了。

**理由**：
- 刷新token增加实现复杂度（需要Redis存储）
- AI对话不是银行系统，安全要求没那么高
- 24小时过期让用户重新登录可以接受

---

## 8. 总结

这个方案的核心思路：
1. **数据结构清晰**：用户-角色-权限三层关系，标准RBAC模型
2. **实现简单**：Spring Security + JWT，业界标准方案
3. **零破坏性**：只在现有接口加注解，不改动业务逻辑
4. **可扩展**：预留权限表结构，但初期可以简化不用

**下一步**：按照Phase 1-5的步骤逐步实现。

## 9. 里程碑路线图（Milestones）

- M0｜极简认证（MySQL/无刷新）：完成本节 Phase 0（1–3 小时），实现“查库登录 + 签发 JWT + 过滤器校验 + 受保护接口 + `/auth/me` 闭环”。仅使用 `users` 表，角色/权限先写死在 Token。
- M1｜角色与注册（JPA+MySQL）：引入 `roles`/`user_role`，开放注册接口写库；`ddl-auto: update/create`（开发态）。将 `roles → authorities` 注入 Spring Security（1–2 天）。
- M2｜方法级鉴权与权限收敛：启用 `@EnableMethodSecurity`，为聊天接口加 `hasAuthority('chat:use')`；按需引入 `permissions` 与映射（0.5–1 天）。
- M3｜会话增强与 Redis（可选）：刷新 Token、黑名单/踢出、登录限流、用户/权限缓存、审计日志与安全加固；必要时加入 Streams 做异步流水线（1–3 天）。

---

## 10. 实施记录（Serena）

2025-10-15｜M0 骨架落地（不破坏现有业务）

- 依赖新增：Security、JPA、MySQL、JJWT（0.12.3）。
- 配置扩展：`application.yml` 添加 `spring.datasource/*`、`spring.jpa.*`、`jwt.*`。
- 数据库：新增 `sql/001_init_users.sql`（`users` 表）。
- 代码结构：
  - `entity.User`、`repository.UserRepository`
  - `security.JwtTokenProvider`、`security.JwtAuthenticationFilter`
  - `config.SecurityConfig`（无状态、白名单、`/api/**` 受保护）
  - `controller.AuthController`（`POST /api/auth/login`、`GET /api/auth/me` 占位）
- 取舍说明：M0 阶段权限硬编码进 JWT 载荷（`ROLE_USER`、`chat:use`），后续在 M1/M2 引入角色/权限与方法级鉴权。
- 验收路径：插入演示用户 → `/api/auth/login` 获取 token → 携带 `Authorization: Bearer <token>` 访问 `/api/**` 验证 200/401 行为。


## 11. 登出与刷新 Token 实施计划（待做）

【核心判断】
- 首选极简实现：无状态 Access Token + 可选 Refresh Token；登出默认前端清本地。
- 真正需要“刷新/踢出/失效”再引入 Refresh 与黑名单/版本号机制，避免过度设计。

【后端改动】
- 配置与白名单
  - [ ] 在 `SecurityConfig` 放行：`/api/auth/refresh`、（可选）`/api/auth/logout`
- 登录返回
  - [ ] `POST /api/auth/login` 响应增加：`refreshToken`、`expiresIn`
- 刷新接口（推荐 Refresh JWT 方案，避免落库）
  - [ ] `POST /api/auth/refresh`
    - 入参：`{ refreshToken }`
    - 校验：签名/过期；解析 `sub/uid/token_version`
    - 业务：校验用户启用状态；（可选）比较 `token_version` 防重放
    - 返回：新的 `token`（access token）与（可选）轮换后的 `refreshToken`
- 登出接口（可选）
  - [ ] `POST /api/auth/logout`
    - 纯无状态：直接返回 200（由前端清理本地）
    - 带失效：采用其一（复杂度递增）：
      1) 用户表增加 `token_version` 字段，登出时自增，旧 refresh 无效；
      2) Redis 维护 refresh 黑名单/白名单（过期自动清理）
- 错误码（在 `ResultCode` 中新增）
  - [ ] `REFRESH_TOKEN_INVALID / REFRESH_TOKEN_EXPIRED`
  - [ ] `LOGOUT_OK`（可不单独定义，统一用 200）

【前端改动】
- 登录/注册
  - [ ] 存储 `refreshToken`（与 `token` 一起）
- 请求拦截器（`httpClient`）
  - [ ] 捕获 401 → 若存在 `refreshToken` 调 `POST /api/auth/refresh` → 覆盖 `token` → 重放原请求
  - [ ] 刷新失败：清理本地并跳转 `/auth/login`
- 登出流程
  - [ ] 调用 `/api/auth/logout`（若实现）→ 清理本地 → 重定向登录
  - [ ] 若未实现后端登出：直接清理本地与重定向

【安全取舍】
- Access Token TTL：24h；Refresh Token TTL：7–30 天（配置化）
- Refresh JWT 与 Access JWT 建议不同 `aud`/`typ` 或 `claim` 标识，避免误用
- 如需“全端踢出”，优先 `token_version` 字段方案，简单稳健

【验收用例】
- 刷新成功：过期 access + 有效 refresh → 返回新 access；重放上一请求成功
- 刷新失败：过期 access + 失效 refresh → 401 → 前端清理并跳登录
- 登出：调用后端或仅前端清本地，后续访问受保护接口返回 401
