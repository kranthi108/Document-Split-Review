package com.ascend.ascend_doc_split_review.dto;

import com.ascend.ascend_doc_split_review.entity.Split;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SplitResponse {
    private Long id;
    private String originalFilename;
    private Split.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DocumentResponse> documents;

    public static SplitResponse fromEntity(Split split) {
        SplitResponse response = new SplitResponse();
        response.setId(split.getId());
        response.setOriginalFilename(split.getOriginalFilename());
        response.setStatus(split.getStatus());
        response.setCreatedAt(split.getCreatedAt());
        response.setUpdatedAt(split.getUpdatedAt());
        response.setDocuments(split.getDocuments().stream().map(DocumentResponse::fromEntity).toList());
        return response;
    }
}