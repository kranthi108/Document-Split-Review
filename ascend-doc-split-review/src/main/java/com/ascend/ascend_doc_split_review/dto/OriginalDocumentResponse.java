package com.ascend.ascend_doc_split_review.dto;

import com.ascend.ascend_doc_split_review.entity.OriginalDocument;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OriginalDocumentResponse {
    private Long id;
    private String originalFilename;
    private OriginalDocument.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SplitPartResponse> splitParts;

    public static OriginalDocumentResponse fromEntity(OriginalDocument doc) {
        OriginalDocumentResponse response = new OriginalDocumentResponse();
        response.setId(doc.getId());
        response.setOriginalFilename(doc.getOriginalFilename());
        response.setStatus(doc.getStatus());
        response.setCreatedAt(doc.getCreatedAt());
        response.setUpdatedAt(doc.getUpdatedAt());
        response.setSplitParts(doc.getSplitParts().stream().map(SplitPartResponse::fromEntity).toList());
        return response;
    }
}

