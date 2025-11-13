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
        if (split.getStatus() == Split.Status.FINALIZED) {
            throw new IllegalArgumentException("Cannot modify a finalized split");
        }
        // Validate: if a page is already assigned to a document, it must be within the same split
        // Allow brand-new pages (no document yet) during initial creation.
        for (Page page : pages) {
            if (page.getDocument() != null && page.getDocument().getSplit() != null) {
                if (!page.getDocument().getSplit().getId().equals(split.getId())) {
                    throw new IllegalArgumentException("All pages must belong to the same split");
                }
            }
        }
        Document document = new Document();
        document.setSplit(split);
        document.setName(name);
        document.setClassification(classification);
        document.setFilename(filename);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        document.setPages(pages);
        // Set initial source page range based on provided page numbers
        int minPage = pages.stream().map(Page::getPageNumber).min(Integer::compareTo).orElse(0);
        int maxPage = pages.stream().map(Page::getPageNumber).max(Integer::compareTo).orElse(0);
        document.setSourceFromPage(minPage);
        document.setSourceToPage(maxPage);
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
            if (doc.getSplit().getStatus() == Split.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot modify a finalized split");
            }
            if (name != null) doc.setName(name);
            if (classification != null) doc.setClassification(classification);
            if (filename != null) doc.setFilename(filename);
            doc.setUpdatedAt(LocalDateTime.now());
            return documentRepository.save(doc);
        }
        throw new RuntimeException("Document not found");
    }

    @Transactional
    public void deleteDocument(Long id, Long reassignToDocumentId) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isPresent()) {
            Document doc = docOpt.get();
            if (doc.getSplit().getStatus() == Split.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot modify a finalized split");
            }
            // Move pages either to target document or mark as unassigned (null)
            List<Page> pages = pageRepository.findByDocumentId(id);
            Document targetDoc = null;
            if (reassignToDocumentId != null) {
                if (reassignToDocumentId.equals(id)) {
                    throw new IllegalArgumentException("Cannot reassign pages to the same document being deleted");
                }
                targetDoc = documentRepository.findById(reassignToDocumentId)
                        .orElseThrow(() -> new IllegalArgumentException("Target document not found: " + reassignToDocumentId));
                // Optional: ensure targetDoc belongs to same split
                if (!targetDoc.getSplit().getId().equals(doc.getSplit().getId())) {
                    throw new IllegalArgumentException("Target document must belong to the same split");
                }
                if (targetDoc.getSplit().getStatus() == Split.Status.FINALIZED) {
                    throw new IllegalArgumentException("Cannot modify a finalized split");
                }
            }
            for (Page page : pages) {
                page.setDocument(targetDoc);
                pageRepository.save(page);
            }
            documentRepository.delete(doc);
        } else {
            throw new RuntimeException("Document not found");
        }
    }
}