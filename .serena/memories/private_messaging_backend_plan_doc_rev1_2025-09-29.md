时间：2025-09-29
操作：更新后端实现计划文档（rev1）
文件：docs/specs/2.1/spec-private-messaging-backend-plan-2.1.md
变更：增加会话去重策略（参与者规范化+唯一复合索引+upsert），调整索引为“唯一复合+单列or”，更新 POST /conversations 说明与实施步骤、风险说明。