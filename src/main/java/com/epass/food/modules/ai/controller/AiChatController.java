package com.epass.food.modules.ai.controller;

import com.epass.food.common.result.Result;
import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.ai.dto.AiChatRequest;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiConversationSessionDetail;
import com.epass.food.modules.ai.dto.AiConversationSessionRenameRequest;
import com.epass.food.modules.ai.dto.AiConversationSessionSummary;
import com.epass.food.modules.ai.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
                        request.getSessionId(),
                        loginUser.getUserId(),
                        canViewAnyOrder
                )
        );
    }

    @GetMapping("/sessions")
    public Result<List<AiConversationSessionSummary>> listSessions(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication
    ) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(aiChatService.listSessions(loginUser.getUserId(), limit));
    }

    @GetMapping("/session/{sessionId}")
    public Result<AiConversationSessionDetail> getSessionDetail(@PathVariable String sessionId,
                                                                Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(aiChatService.getSessionDetail(sessionId, loginUser.getUserId()));
    }

    @PutMapping("/session/{sessionId}/title")
    public Result<Void> renameSession(@PathVariable String sessionId,
                                      @Valid @RequestBody AiConversationSessionRenameRequest request,
                                      Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        aiChatService.renameSession(sessionId, request.getTitle(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/session/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId,
                                     Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        aiChatService.clearSession(sessionId, loginUser.getUserId());
        return Result.success();
    }
}
