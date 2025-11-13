package com.ascend.ascend_doc_split_review.dto;

import lombok.Data;

@Data
public class UpdateSplitPartRequest {
    private String name;
    private String classification;
    private String filename;
}

