package com.ascend.ascend_doc_split_review.dto;

import lombok.Data;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateSplitPartRequest {
    @NotNull
    private Long originalDocumentId;
    @NotNull
    private String name;
    @NotNull
    private String classification;
    @NotNull
    private String filename;
    @NotEmpty
    private List<Long> pageIds;
}

