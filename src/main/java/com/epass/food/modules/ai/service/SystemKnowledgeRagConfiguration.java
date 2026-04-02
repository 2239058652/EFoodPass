package com.epass.food.modules.ai.service;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemKnowledgeRagConfiguration {

    @Bean
    public VectorStore systemKnowledgeVectorStore(EmbeddingModel embeddingModel,
                                                  SystemKnowledgeDocumentFactory documentFactory) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        vectorStore.add(documentFactory.createDocuments());
        return vectorStore;
    }

    @Bean
    public QuestionAnswerAdvisor systemKnowledgeAdvisor(VectorStore systemKnowledgeVectorStore) {
        return QuestionAnswerAdvisor.builder(systemKnowledgeVectorStore).build();
    }
}
