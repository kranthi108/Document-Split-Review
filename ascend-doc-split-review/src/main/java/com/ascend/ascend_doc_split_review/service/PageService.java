package com.ascend.ascend_doc_split_review.service;

import com.ascend.ascend_doc_split_review.entity.Document;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.Split;
import com.ascend.ascend_doc_split_review.repository.DocumentRepository;
import com.ascend.ascend_doc_split_review.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PageService {

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private DocumentRepository documentRepository;

    public Page createPage(Document document, Integer pageNumber, String content) {
        Page page = new Page();
        page.setDocument(document);
        page.setPageNumber(pageNumber);
        page.setContent(content);
        return pageRepository.save(page);
    }

    public List<Page> getPagesByDocument(Long documentId) {
        return pageRepository.findByDocumentId(documentId);
    }

    // Better method
    @Transactional
    public void movePagesToDocument(List<Long> pageIds, Document targetDocument) {
        if (targetDocument.getSplit().getStatus() == Split.Status.FINALIZED) {
            throw new IllegalArgumentException("Cannot modify a finalized split");
        }
        List<Page> pages = pageRepository.findByIdIn(pageIds);
        if (pages.isEmpty()) {
            return;
        }
        // Group pages by their current source document
        Map<Document, List<Page>> bySourceDoc = pages.stream()
                .collect(Collectors.groupingBy(Page::getDocument));
        for (Map.Entry<Document, List<Page>> entry : bySourceDoc.entrySet()) {
            Document sourceDoc = entry.getKey();
            if (sourceDoc == null) {
                throw new IllegalArgumentException("All pages must belong to a document");
            }
            // Same split constraint
            if (!sourceDoc.getSplit().getId().equals(targetDocument.getSplit().getId())) {
                throw new IllegalArgumentException("Cannot move pages across different splits (must belong to the same original document)");
            }
            if (sourceDoc.getSplit().getStatus() == Split.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot modify a finalized split");
            }
        }
        // Perform move
        for (Page page : pages) {
            page.setDocument(targetDocument);
            pageRepository.save(page);
        }
        // Remove any now-empty source documents
        for (Document sourceDoc : bySourceDoc.keySet()) {
            if (!sourceDoc.getId().equals(targetDocument.getId())) {
                long remaining = pageRepository.findByDocumentId(sourceDoc.getId()).size();
                if (remaining == 0) {
                    documentRepository.delete(sourceDoc);
                }
            }
        }
    }
}