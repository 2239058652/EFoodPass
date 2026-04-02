package com.epass.food.modules.ai.service;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemKnowledgeRagConfiguration {

    @Bean
    public VectorStore systemKnowledgeVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public QuestionAnswerAdvisor systemKnowledgeAdvisor(VectorStore systemKnowledgeVectorStore,
                                                        SystemKnowledgeRagProperties ragProperties) {
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(ragProperties.getTopK())
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .build();

        return QuestionAnswerAdvisor.builder(systemKnowledgeVectorStore)
                .searchRequest(searchRequest)
                .build();
    }
}
