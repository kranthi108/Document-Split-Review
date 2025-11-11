package com.ascend.ascend_doc_split_review.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateDocumentRequest {
    private Long splitId;
    private String name;
    private String classification;
    private String filename;
    private List<Long> pageIds;
}