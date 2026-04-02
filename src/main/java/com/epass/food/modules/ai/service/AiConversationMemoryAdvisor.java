package com.epass.food.modules.ai.service;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class AiConversationMemoryAdvisor implements BaseAdvisor {

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Map<String, Object> context = request.context();
        String summary = (String) context.get(AiAdvisorContextKeys.MEMORY_SUMMARY);
        @SuppressWarnings("unchecked")
        List<AiConversationMemoryService.ConversationTurn> recentTurns =
                (List<AiConversationMemoryService.ConversationTurn>) context.getOrDefault(
                        AiAdvisorContextKeys.MEMORY_RECENT_TURNS,
                        List.of()
                );

        boolean hasSummary = StringUtils.hasText(summary);
        boolean hasRecentTurns = recentTurns != null && !recentTurns.isEmpty();
        if (!hasSummary && !hasRecentTurns) {
            return request;
        }

        StringBuilder historyBuilder = new StringBuilder();
        historyBuilder.append("\n\n下面是与当前会话相关的上下文，仅用于辅助理解当前问题：\n");
        if (hasSummary) {
            historyBuilder.append(summary).append("\n");
        }

        if (hasRecentTurns) {
            historyBuilder.append("最近几轮对话：\n");
        }

        int index = 1;
        for (AiConversationMemoryService.ConversationTurn turn : recentTurns) {
            historyBuilder.append(index)
                    .append(". 用户：")
                    .append(turn.userMessage())
                    .append("\n");
            historyBuilder.append(index)
                    .append(". 助手：")
                    .append(turn.assistantMessage())
                    .append("\n");
            index++;
        }

        historyBuilder.append("回答当前问题时优先依据本轮问题；如果历史上下文与本轮冲突，以本轮为准。");

        Prompt prompt = request.prompt().augmentSystemMessage(historyBuilder.toString());
        return request.mutate().prompt(prompt).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
