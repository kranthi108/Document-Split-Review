package com.ascend.ascend_doc_split_review.dto;

import com.ascend.ascend_doc_split_review.entity.Page;
import lombok.Data;

@Data
public class PageResponse {
    private Long id;
    private Integer pageNumber;
    private String content;

    public static PageResponse fromEntity(Page page) {
        PageResponse response = new PageResponse();
        response.setId(page.getId());
        response.setPageNumber(page.getPageNumber());
        response.setContent(page.getContent());
        return response;
    }
}