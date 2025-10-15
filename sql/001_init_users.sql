-- M0: 极简用户表（仅 users），后续可扩展 roles/permissions
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

-- 示例：插入演示用户。注意：请将密码替换为你本地生成的 BCrypt Hash。
-- 示例 hash 不可用，仅作位置信息说明。
-- INSERT INTO users (username, password, email) VALUES ('demo', '$2a$10$REPLACE_WITH_BCRYPT', 'demo@example.com');
