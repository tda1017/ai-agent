package com.xin.aiagent.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: TDA
 * @date: 13/9/2025 03:03
 * @description: 集中注册工具供智能体使用
 * 工厂模式： allTools() 方法作为一个工厂方法，负责创建和配置多个工具实例
 * 将它们包装成统一的数组返回。
 * 集中创建对象并隐藏创建细节
 */


@Configuration
public class ToolRegistration {

    @Bean
    public ToolCallback[] allTools() {
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        return ToolCallbacks.from(
                webScrapingTool
        );
    }
}
