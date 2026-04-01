package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.OrderQuestionType;
import org.springframework.stereotype.Component;

@Component
public class OrderQuestionClassifier {

    private final OrderIdExtractor orderIdExtractor;

    public OrderQuestionClassifier(OrderIdExtractor orderIdExtractor) {
        this.orderIdExtractor = orderIdExtractor;
    }

    public OrderQuestionType classify(String message) {
        if (message == null || message.isBlank()) {
            return OrderQuestionType.GENERAL_ORDER;
        }

        if (orderIdExtractor.extractOrderId(message) != null) {
            return OrderQuestionType.DETAIL_QUERY;
        }

        if (isRealtimeStatsQuestion(message)) {
            return OrderQuestionType.REALTIME_STATS;
        }

        if (isStatusRuleQuestion(message)) {
            return OrderQuestionType.STATUS_RULE;
        }

        return OrderQuestionType.GENERAL_ORDER;
    }

    private boolean isRealtimeStatsQuestion(String message) {
        return message.contains("当前")
                || message.contains("现在")
                || message.contains("多少")
                || message.contains("统计")
                || message.contains("整体情况")
                || message.contains("订单数")
                || message.contains("金额");
    }

    private boolean isStatusRuleQuestion(String message) {
        return message.contains("订单状态")
                || message.contains("状态有哪些")
                || message.contains("状态说明")
                || message.contains("状态含义")
                || message.contains("取消订单")
                || message.contains("下单")
                || message.contains("订单流程");
    }
}