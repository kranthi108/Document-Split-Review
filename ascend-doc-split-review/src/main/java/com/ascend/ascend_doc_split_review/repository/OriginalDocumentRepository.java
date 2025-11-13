package com.ascend.ascend_doc_split_review.repository;

import com.ascend.ascend_doc_split_review.entity.OriginalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OriginalDocumentRepository extends JpaRepository<OriginalDocument, Long> {
    List<OriginalDocument> findByUserId(Long userId);
}

