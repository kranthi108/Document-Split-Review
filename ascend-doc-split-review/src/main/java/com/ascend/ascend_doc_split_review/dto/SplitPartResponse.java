package com.ascend.ascend_doc_split_review.dto;

import com.ascend.ascend_doc_split_review.entity.SplitPart;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SplitPartResponse {
    private Long id;
    private String name;
    private String classification;
    private String filename;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer fromPage;
    private Integer toPage;
    private List<PageResponse> pages;

    public static SplitPartResponse fromEntity(SplitPart splitPart) {
        SplitPartResponse response = new SplitPartResponse();
        response.setId(splitPart.getId());
        response.setName(splitPart.getName());
        response.setClassification(splitPart.getClassification());
        response.setFilename(splitPart.getFilename());
        response.setCreatedAt(splitPart.getCreatedAt());
        response.setUpdatedAt(splitPart.getUpdatedAt());
        response.setFromPage(splitPart.getFromPage());
        response.setToPage(splitPart.getToPage());
        response.setPages(splitPart.getPages().stream().map(PageResponse::fromEntity).toList());
        return response;
    }
}

