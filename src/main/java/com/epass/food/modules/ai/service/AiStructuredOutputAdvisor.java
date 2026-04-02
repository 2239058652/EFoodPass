package com.epass.food.modules.ai.service;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AiStructuredOutputAdvisor implements BaseAdvisor {

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Map<String, Object> context = request.context();
        @SuppressWarnings("unchecked")
        List<String> structuredFields = (List<String>) context.getOrDefault(
                AiAdvisorContextKeys.STRUCTURED_FIELDS,
                List.of("content")
        );
        @SuppressWarnings("unchecked")
        List<String> toolStatusOptions = (List<String>) context.getOrDefault(
                AiAdvisorContextKeys.TOOL_STATUS_OPTIONS,
                List.of()
        );

        StringBuilder ruleBuilder = new StringBuilder();
        ruleBuilder.append("\n\n输出约束：\n");
        ruleBuilder.append("1. 只返回结构化字段，不要输出 Markdown、代码块或额外解释。\n");
        ruleBuilder.append("2. 需要填写的字段有：")
                .append(String.join("、", structuredFields))
                .append("。\n");

        if (!toolStatusOptions.isEmpty()) {
            ruleBuilder.append("3. toolStatus 只能取：")
                    .append(toolStatusOptions.stream().collect(Collectors.joining("、")))
                    .append("。\n");
        }

        Prompt prompt = request.prompt().augmentSystemMessage(ruleBuilder.toString());
        return request.mutate().prompt(prompt).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
