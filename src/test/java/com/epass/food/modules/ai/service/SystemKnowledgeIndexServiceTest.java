package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.SystemKnowledgeIndexStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemKnowledgeIndexServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private SystemKnowledgeDocumentFactory documentFactory;

    @Mock
    private AiMetricsService aiMetricsService;

    private SystemKnowledgeRagProperties ragProperties;
    private SystemKnowledgeIndexService systemKnowledgeIndexService;

    @BeforeEach
    void setUp() {
        ragProperties = new SystemKnowledgeRagProperties();
        ragProperties.setTopK(5);
        ragProperties.setSimilarityThreshold(0.66);

        systemKnowledgeIndexService = new SystemKnowledgeIndexService(
                vectorStore,
                documentFactory,
                ragProperties,
                aiMetricsService
        );
    }

    @Test
    void rebuildIndexShouldDeleteOldDocumentsAndAddFreshOnes() {
        List<String> documentIds = List.of("doc-1", "doc-2");
        List<Document> documents = List.of(
                Document.builder().id("doc-1").text("auth info").build(),
                Document.builder().id("doc-2").text("permission info").build()
        );

        when(documentFactory.getDocumentIds()).thenReturn(documentIds);
        when(documentFactory.createDocuments()).thenReturn(documents);

        SystemKnowledgeIndexStatusResponse status = systemKnowledgeIndexService.rebuildIndex();

        verify(vectorStore).delete(documentIds);
        verify(vectorStore).add(documents);
        verify(aiMetricsService).recordKnowledgeRebuild("system", 2);

        assertThat(status.getKnowledgeBase()).isEqualTo("system");
        assertThat(status.getDocumentCount()).isEqualTo(2);
        assertThat(status.getTopK()).isEqualTo(5);
        assertThat(status.getSimilarityThreshold()).isEqualTo(0.66);
        assertThat(status.getLastRebuildAt()).isNotNull();
    }
}
