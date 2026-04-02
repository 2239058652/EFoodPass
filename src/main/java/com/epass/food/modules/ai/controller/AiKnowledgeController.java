package com.epass.food.modules.ai.controller;

import com.epass.food.common.result.Result;
import com.epass.food.modules.ai.dto.SystemKnowledgeIndexStatusResponse;
import com.epass.food.modules.ai.service.SystemKnowledgeIndexService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/knowledge/system")
public class AiKnowledgeController {

    private final SystemKnowledgeIndexService systemKnowledgeIndexService;

    public AiKnowledgeController(SystemKnowledgeIndexService systemKnowledgeIndexService) {
        this.systemKnowledgeIndexService = systemKnowledgeIndexService;
    }

    @GetMapping("/status")
    public Result<SystemKnowledgeIndexStatusResponse> getStatus() {
        return Result.success(systemKnowledgeIndexService.getStatus());
    }

    @PostMapping("/rebuild")
    public Result<SystemKnowledgeIndexStatusResponse> rebuild() {
        return Result.success(systemKnowledgeIndexService.rebuildIndex());
    }
}
