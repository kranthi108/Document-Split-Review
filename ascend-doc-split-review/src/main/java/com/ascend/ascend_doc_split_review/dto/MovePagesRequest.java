package com.ascend.ascend_doc_split_review.dto;

import lombok.Data;

import java.util.List;

@Data
public class MovePagesRequest {
    private List<Long> pageIds;
    private Long targetDocumentId;
}