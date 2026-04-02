package com.epass.food.modules.ai.controller;

import com.epass.food.modules.ai.dto.SystemKnowledgeIndexStatusResponse;
import com.epass.food.modules.ai.service.SystemKnowledgeIndexService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeControllerTest {

    @Mock
    private SystemKnowledgeIndexService systemKnowledgeIndexService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        AiKnowledgeController controller = new AiKnowledgeController(systemKnowledgeIndexService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getStatusShouldReturnCurrentKnowledgeIndexState() throws Exception {
        when(systemKnowledgeIndexService.getStatus()).thenReturn(
                new SystemKnowledgeIndexStatusResponse("system", 6, 123456L, 5, 0.75)
        );

        mockMvc.perform(get("/ai/knowledge/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.knowledgeBase").value("system"))
                .andExpect(jsonPath("$.data.documentCount").value(6))
                .andExpect(jsonPath("$.data.lastRebuildAt").value(123456))
                .andExpect(jsonPath("$.data.topK").value(5))
                .andExpect(jsonPath("$.data.similarityThreshold").value(0.75));

        verify(systemKnowledgeIndexService).getStatus();
    }

    @Test
    void rebuildShouldReturnFreshKnowledgeIndexState() throws Exception {
        when(systemKnowledgeIndexService.rebuildIndex()).thenReturn(
                new SystemKnowledgeIndexStatusResponse("system", 8, 222222L, 6, 0.8)
        );

        mockMvc.perform(post("/ai/knowledge/system/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.knowledgeBase").value("system"))
                .andExpect(jsonPath("$.data.documentCount").value(8))
                .andExpect(jsonPath("$.data.lastRebuildAt").value(222222))
                .andExpect(jsonPath("$.data.topK").value(6))
                .andExpect(jsonPath("$.data.similarityThreshold").value(0.8));

        verify(systemKnowledgeIndexService).rebuildIndex();
    }
}
