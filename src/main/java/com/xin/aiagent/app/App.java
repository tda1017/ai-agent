package com.xin.aiagent.app;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class App {

        private final ChatClient chatClient;

        @Resource
        private VectorStore appVectorStore;

        @Resource
        private Advisor appRagCloudAdvisor;

        @Resource
        private ToolCallback[] allTools;

        public String doChatWithRagLocal(String message, String chatId) {
                ChatResponse chatResponse = chatClient
                                .prompt()
                                .user(message)
                                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                                                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                                // 开启日志，便于观察效果
                                .advisors(new MyLoggerAdvisor())
                                // 应用知识库问答
                                .advisors(new QuestionAnswerAdvisor(appVectorStore))
                                .call()
                                .chatResponse();
                String content = chatResponse.getResult().getOutput().getText();
                log.info("content: {}", content);
                return content;
        }

        public String doChatWithRagCloud(String message, String chatId) {
                ChatResponse chatResponse = chatClient
                                .prompt()
                                .user(message)
                                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                                                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                                // 开启日志，便于观察效果
                                .advisors(new MyLoggerAdvisor())
                                // 应用 RAG 知识库问答
                                .advisors(appRagCloudAdvisor)
                                .call()
                                .chatResponse();
                String content = chatResponse.getResult().getOutput().getText();
                log.info("content: {}", content);
                return content;
        }

        // 使用工具
        public String doChatWithTools(String message, String chatId){
                ChatResponse response = chatClient
                        .prompt()
                        .user(message)
                        .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                        // 开启日志，便于观察效果
                        .advisors(new MyLoggerAdvisor())
                        .tools(allTools)
                        .call()
                        .chatResponse();
                String content = response.getResult().getOutput().getText();
                log.info("content: {}", content);
                return content;
        }

        private static final String SYSTEM_PROMPT = "你是一个专业的 AI 助手，基于 DeepSeek 模型。" +
                        "请提供准确、有帮助、详细的回答。" +
                        "你可以回答各类问题，包括但不限于技术、学习、生活、工作等方面。" +
                        "回答时保持友好、专业的态度，必要时可以提供多角度的分析和建议。";

        /**
         * 说明：原项目依赖阿里 DashScope 的 ChatModel，这里切换为基于 OpenAI 兼容协议的 ChatModel。
         * DeepSeek 提供 OpenAI 兼容 API（通过 base-url+api-key 配置），因此此处仅依赖 {@link ChatModel} 类型注入即可。
         */
        public App(ChatModel chatModel) {
                // 初始化基于内存的对话记忆
                ChatMemory chatMemory = new InMemoryChatMemory();
                chatClient = ChatClient.builder(chatModel)
                                .defaultSystem(SYSTEM_PROMPT)
                                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                                .build();
        }
}
