时间：2025-09-29
主题：私信后端最小实现代码交付
降级说明：为批量新增模型/控制器/路由文件与挂载，采用 Codex apply_patch（Serena 对多文件新建与插入不便）。
变更清单：
- 新增：backend/src/models/Conversation.js（唯一复合索引 user1Id+user2Id、lastMessage 快照、lastActiveAt、单列索引）
- 新增：backend/src/models/Message.js（索引 conversationId+createdAt、receiverId+isRead）
- 新增：backend/src/controllers/messageController.js（getConversations/getOrCreateConversation/getMessages/sendMessage/markConversationRead）
- 新增：backend/src/routes/messageRoutes.js（/api/messages 前缀）
- 修改：backend/src/app.js（挂载 /api/messages）
端点对齐：与前端服务完全一致（/conversations、/send、/conversations/:id/messages、/conversations/:id/read）。
注意：返回数据结构含 otherUser（字段 _id/username/email/avatar/bio/createdAt）以适配前端 User.fromJson。