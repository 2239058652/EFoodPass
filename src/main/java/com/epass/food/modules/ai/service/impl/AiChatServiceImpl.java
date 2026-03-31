package com.epass.food.modules.ai.service.impl;

import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.service.AiChatService;
import com.epass.food.modules.ai.service.BusinessContextProvider;
import com.epass.food.modules.ai.service.OrderFactProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;


@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final BusinessContextProvider businessContextProvider;
    private final OrderFactProvider orderFactProvider;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             BusinessContextProvider businessContextProvider,
                             OrderFactProvider orderFactProvider) {
        this.chatClient = chatClientBuilder.build();
        this.businessContextProvider = businessContextProvider;
        this.orderFactProvider = orderFactProvider;
    }

    @Override
    public AiChatResponse chat(String message) {
        String systemPrompt = buildPromptByMessage(message);

        String content = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();

        return new AiChatResponse(content);
    }

    private String buildPromptByMessage(String message) {
        if (isOrderQuestion(message)) {
            return """
                    %s
                    
                    下面是订单领域的真实业务事实：
                    %s
                    
                    你现在是 EFoodPass 的订单助手。
                    请严格基于这些真实事实回答订单问题。
                    如果事实里没有，不要编造。
                    """.formatted(
                    businessContextProvider.buildCommonFacts(),
                    orderFactProvider.buildOrderFacts()
            );
        }

        return businessContextProvider.buildGeneralAssistantPrompt();
    }

    private boolean isOrderQuestion(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        return message.contains("订单")
                || message.contains("下单")
                || message.contains("取消")
                || message.contains("状态");
    }
}