package com.ascend.ascend_doc_split_review.repository;

import com.ascend.ascend_doc_split_review.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    List<Page> findBySplitPartId(Long splitPartId);
    List<Page> findByIdIn(List<Long> ids);
}