-- Minimal schema for chat persistence (M0)
-- Conversations and Messages with soft delete and simple pagination by id

-- Conversations table
CREATE TABLE IF NOT EXISTS conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT 'owner user id',
  title VARCHAR(200) NULL COMMENT 'optional title (first message summary)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL COMMENT 'soft delete timestamp',
  INDEX idx_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Chat conversations';

-- Messages table
CREATE TABLE IF NOT EXISTS messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL COMMENT 'FK to conversations.id',
  role ENUM('user', 'assistant') NOT NULL COMMENT 'message role',
  content TEXT NOT NULL COMMENT 'message content',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL COMMENT 'soft delete timestamp',
  INDEX idx_conversation_id (conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Chat messages';
