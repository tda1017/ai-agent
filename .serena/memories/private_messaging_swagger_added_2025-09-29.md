时间：2025-09-29
操作：为消息模块新增 Swagger 文档
变更：
- backend/src/routes/messageRoutes.js 添加 5 个端点的 @swagger 注释（标签“消息”）。
- backend/src/models/Conversation.js 添加 Conversation schema（含 otherUser/unreadCount 说明）。
- backend/src/models/Message.js 添加 Message schema（含 status 映射说明）。
效果：启动后端后访问 /api-docs 可视化与调试消息接口；/api-docs.json 可导入 Postman/SDK 生成。