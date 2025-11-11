package com.ascend.ascend_doc_split_review.repository;

import com.ascend.ascend_doc_split_review.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findBySplitId(Long splitId);
}