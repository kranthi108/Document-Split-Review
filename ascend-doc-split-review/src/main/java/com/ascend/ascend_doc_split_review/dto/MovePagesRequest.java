package com.ascend.ascend_doc_split_review.dto;

import lombok.Data;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class MovePagesRequest {
    @NotEmpty
    private List<Long> pageIds;
    @NotNull
    private Long targetSplitPartId;
}