package com.ascend.ascend_doc_split_review.dto;

import com.ascend.ascend_doc_split_review.entity.Document;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentResponse {
    private Long id;
    private String name;
    private String classification;
    private String filename;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PageResponse> pages;

    public static DocumentResponse fromEntity(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setName(document.getName());
        response.setClassification(document.getClassification());
        response.setFilename(document.getFilename());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setPages(document.getPages().stream().map(PageResponse::fromEntity).toList());
        return response;
    }
}