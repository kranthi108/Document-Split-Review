package com.ascend.ascend_doc_split_review.service;

import com.ascend.ascend_doc_split_review.entity.Document;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PageService {

    @Autowired
    private PageRepository pageRepository;

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

    @Transactional
    public void movePages(List<Long> pageIds, Long targetDocumentId) {
        List<Page> pages = pageRepository.findByIdIn(pageIds);
        Optional<Document> targetDocOpt = Optional.empty(); // Will be injected if needed
        // For now, assume targetDocumentId is provided, but since Document is not injected, we'll need to adjust
        // Actually, better to pass Document object
        throw new UnsupportedOperationException("Need to refactor to pass Document object");
    }

    // Better method
    @Transactional
    public void movePagesToDocument(List<Long> pageIds, Document targetDocument) {
        List<Page> pages = pageRepository.findByIdIn(pageIds);
        for (Page page : pages) {
            page.setDocument(targetDocument);
            pageRepository.save(page);
        }
    }
}