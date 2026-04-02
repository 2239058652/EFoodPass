package com.epass.food.modules.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SystemKnowledgeDocumentFactory {

    private final SystemModuleCatalog systemModuleCatalog;

    public SystemKnowledgeDocumentFactory(SystemModuleCatalog systemModuleCatalog) {
        this.systemModuleCatalog = systemModuleCatalog;
    }

    public List<Document> createDocuments() {
        List<Document> documents = new ArrayList<>();
        documents.add(document(
                "auth-overview",
                "认证与当前用户",
                """
                        auth 模块负责登录和获取当前用户信息。
                        系统采用 JWT 做登录态传递，登录成功后由后端签发 token。
                        与当前登录用户相关的问题，优先参考 auth 模块。
                        """,
                Map.of("moduleCode", "auth", "topic", "authentication")
        ));
        documents.add(document(
                "permission-overview",
                "权限控制",
                """
                        系统采用 Spring Security 做认证鉴权。
                        权限控制主要通过 @PreAuthorize 实现。
                        system 模块负责用户、角色、权限管理。
                        """,
                Map.of("moduleCode", "system", "topic", "authorization")
        ));
        documents.add(document(
                "api-style",
                "统一接口风格",
                """
                        当前项目接口统一返回 Result<T> 结构。
                        AI 场景回答应优先基于项目真实模块和权限设计，不要编造不存在的模块能力。
                        """,
                Map.of("moduleCode", "system", "topic", "api-style")
        ));

        for (SystemModuleCatalog.ModuleInfo module : systemModuleCatalog.getModules()) {
            documents.add(document(
                    "module-" + module.code().replace("/", "-"),
                    module.name(),
                    """
                            模块编码：%s
                            模块名称：%s
                            模块说明：%s
                            """.formatted(module.code(), module.name(), module.description()),
                    Map.of("moduleCode", module.code(), "topic", "module")
            ));
        }

        return documents;
    }

    public List<String> getDocumentIds() {
        return createDocuments().stream().map(Document::getId).toList();
    }

    private Document document(String id,
                              String title,
                              String content,
                              Map<String, Object> extraMetadata) {
        return Document.builder()
                .id(id)
                .text(content)
                .metadata("knowledgeBase", "system")
                .metadata("documentId", id)
                .metadata("title", title)
                .metadata(extraMetadata)
                .build();
    }
}
