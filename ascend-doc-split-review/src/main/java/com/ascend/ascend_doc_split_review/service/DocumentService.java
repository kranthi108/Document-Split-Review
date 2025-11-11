package com.ascend.ascend_doc_split_review.service;

import com.ascend.ascend_doc_split_review.entity.Document;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.Split;
import com.ascend.ascend_doc_split_review.repository.DocumentRepository;
import com.ascend.ascend_doc_split_review.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PageRepository pageRepository;

    public Document createDocument(Split split, String name, String classification, String filename, List<Page> pages) {
        Document document = new Document();
        document.setSplit(split);
        document.setName(name);
        document.setClassification(classification);
        document.setFilename(filename);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        document.setPages(pages);
        for (Page page : pages) {
            page.setDocument(document);
        }
        return documentRepository.save(document);
    }

    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    public List<Document> getDocumentsBySplit(Long splitId) {
        return documentRepository.findBySplitId(splitId);
    }

    public Document updateDocument(Long id, String name, String classification, String filename) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isPresent()) {
            Document doc = docOpt.get();
            if (name != null) doc.setName(name);
            if (classification != null) doc.setClassification(classification);
            if (filename != null) doc.setFilename(filename);
            doc.setUpdatedAt(LocalDateTime.now());
            return documentRepository.save(doc);
        }
        throw new RuntimeException("Document not found");
    }

    @Transactional
    public void deleteDocument(Long id) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isPresent()) {
            Document doc = docOpt.get();
            // Move pages to null (unassigned)
            List<Page> pages = pageRepository.findByDocumentId(id);
            for (Page page : pages) {
                page.setDocument(null);
                pageRepository.save(page);
            }
            documentRepository.delete(doc);
        } else {
            throw new RuntimeException("Document not found");
        }
    }
}