package com.xin.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 说明：原使用阿里 DashScope 云检索（知识库索引），现切换为本地向量库检索以适配 DeepSeek（OpenAI 兼容）。
 * 若后续接入 DeepSeek 官方云检索能力，可在此处替换为对应的 DocumentRetriever 实现。
 */
@Configuration
@Slf4j
public class AppRagCloudAdvisorConfig {

    /**
     * 用本地 VectorStore 封装一个问答增强 Advisor，实现与云检索近似体验。
     */
    @Bean
    public Advisor appRagCloudAdvisor(VectorStore appVectorStore) {
        return new QuestionAnswerAdvisor(appVectorStore);
    }
}
