package com.ascend.ascend_doc_split_review.repository;

import com.ascend.ascend_doc_split_review.entity.SplitPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SplitPartRepository extends JpaRepository<SplitPart, Long> {
    List<SplitPart> findByOriginalDocumentId(Long originalDocumentId);
}

