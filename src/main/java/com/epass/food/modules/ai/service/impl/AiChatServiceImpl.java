package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.service.AiChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;


@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public AiChatResponse chat(String message) {
        try {
            String content = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            return new AiChatResponse(content);
        } catch (NonTransientAiException e) {
            throw new BusinessException(500, "AI 调用失败: " + e.getMessage());
        }
    }
}