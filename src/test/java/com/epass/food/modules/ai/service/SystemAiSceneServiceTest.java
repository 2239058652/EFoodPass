package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAiSceneServiceTest {

    @Mock
    private BusinessContextProvider businessContextProvider;

    @Mock
    private QuestionAnswerAdvisor questionAnswerAdvisor;

    private SystemAiSceneService systemAiSceneService;

    @BeforeEach
    void setUp() {
        SystemKnowledgeRagProperties ragProperties = new SystemKnowledgeRagProperties();
        ragProperties.setTopK(6);
        ragProperties.setSimilarityThreshold(0.72);

        SystemModuleCatalog systemModuleCatalog = new SystemModuleCatalog();
        when(businessContextProvider.buildCommonFacts()).thenReturn("common facts");

        systemAiSceneService = new SystemAiSceneService(
                businessContextProvider,
                systemModuleCatalog,
                questionAnswerAdvisor,
                ragProperties
        );
    }

    @Test
    void buildPlanShouldInjectSystemRagParametersForPermissionQuestion() {
        AiSceneRequestContext context = new AiSceneRequestContext(
                "系统是怎么做权限控制的？",
                "session-1",
                1L,
                false
        );

        AiPromptPlan plan = systemAiSceneService.buildPlan(context);

        assertThat(plan.advisorParams())
                .containsEntry(AiAdvisorContextKeys.RAG_KNOWLEDGE_BASE, "system")
                .containsEntry(AiAdvisorContextKeys.RAG_TOP_K, 6)
                .containsEntry(AiAdvisorContextKeys.RAG_SIMILARITY_THRESHOLD, 0.72)
                .containsEntry(AiAdvisorContextKeys.RAG_FILTER_EXPRESSION, "moduleCode == 'system'")
                .containsEntry(QuestionAnswerAdvisor.FILTER_EXPRESSION, "moduleCode == 'system'");
        assertThat(plan.hasAdvisors()).isTrue();
        assertThat(plan.advisors()).containsExactly(questionAnswerAdvisor);
    }

    @Test
    void buildPlanShouldExposeRetrievalStrategyInCard() {
        AiPromptPlan plan = systemAiSceneService.buildPlan(new AiSceneRequestContext(
                "auth 模块负责什么？",
                "session-2",
                1L,
                false
        ));

        AiDisplayCard card = plan.card();

        assertThat(card.getType()).isEqualTo("system-modules");
        assertThat(card.getFields()).extracting("label")
                .containsExactly("知识库", "检索范围", "TopK", "相似度阈值");
        assertThat(card.getFields()).extracting("value")
                .contains("system", "过滤检索: moduleCode == 'auth'", "6", "0.72");
    }
}
