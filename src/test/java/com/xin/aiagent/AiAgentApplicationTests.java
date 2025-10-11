package com.xin.aiagent;

import cn.hutool.core.lang.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.xin.aiagent.app.App;

@SpringBootTest
class AiAgentApplicationTests {

    @Autowired
    private App app;

    @Test
    void contextLoads() {
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String answer = app.doChatWithRagCloud(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试网页抓取：恋爱案例分析
        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");
    }

    private void testMessage(String message) {
        // ?
        String chatId = UUID.randomUUID().toString();
        String answer = app.doChatWithTools(message, chatId);
        System.out.println("answer: " + answer);
        Assertions.assertNotNull(answer);
    }

}
