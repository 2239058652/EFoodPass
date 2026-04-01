package com.epass.food.modules.ai.controller;

import com.epass.food.common.result.Result;
import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.ai.dto.AiChatRequest;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/chat")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request,
                                       Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        boolean canViewAnyOrder = authentication.getAuthorities().stream()
                .anyMatch(authority -> "food:order:detail".equals(authority.getAuthority()));

        return Result.success(
                aiChatService.chat(
                        request.getMessage(),
                        loginUser.getUserId(),
                        canViewAnyOrder
                )
        );
    }
}