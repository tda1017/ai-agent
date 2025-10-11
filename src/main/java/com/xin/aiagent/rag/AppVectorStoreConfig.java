package com.xin.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 向量库配置
 * 说明：默认在启动期不强制进行文档嵌入（可通过开关启用）。
 * 目的：避免开发环境缺少 DeepSeek 凭据或无网络时导致应用启动失败。
 */
@Configuration
@lombok.extern.slf4j.Slf4j
public class AppVectorStoreConfig {

    @Resource
    private AppDocumentLoader appDocumentLoader;

    // 开关：是否在启动期进行向量库文档嵌入
    @org.springframework.beans.factory.annotation.Value("${app.vector.init-on-startup:false}")
    private boolean initOnStartup;

    /**
     * 使用通用 EmbeddingModel（由 Spring AI OpenAI Starter 提供的 DeepSeek 兼容实现注入）。
     */
    @Bean
    VectorStore appVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();

        if (initOnStartup) {
            try {
                // 加载文档并在启动期完成嵌入，便于开箱即用
                List<Document> documents = appDocumentLoader.loadMarkdowns();
                simpleVectorStore.add(documents);
                log.info("向量库已在启动期完成文档嵌入，共 {} 条", documents.size());
            } catch (Exception e) {
                // 容错：失败仅记录告警，不阻断启动（例如 API Key 无效/网络不可达）
                log.warn("启动期向量库文档嵌入失败：{}，已跳过（不影响服务启动）", e.getMessage());
            }
        } else {
            log.info("已禁用启动期向量库文档嵌入（app.vector.init-on-startup=false）");
        }

        return simpleVectorStore;
    }
}
