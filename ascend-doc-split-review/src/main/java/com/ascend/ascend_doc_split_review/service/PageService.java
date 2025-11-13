package com.ascend.ascend_doc_split_review.service;

import com.ascend.ascend_doc_split_review.entity.SplitPart;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.OriginalDocument;
import com.ascend.ascend_doc_split_review.repository.SplitPartRepository;
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
    private SplitPartRepository splitPartRepository;

    public Page createPage(SplitPart splitPart, Integer pageNumber, String content) {
        Page page = new Page();
        page.setSplitPart(splitPart);
        page.setPageNumber(pageNumber);
        page.setContent(content);
        return pageRepository.save(page);
    }

    public List<Page> getPagesBySplitPart(Long splitPartId) {
        return pageRepository.findBySplitPartId(splitPartId);
    }

    // Better method
    @Transactional
    public void movePagesToSplitPart(List<Long> pageIds, SplitPart targetSplitPart) {
        if (targetSplitPart.getOriginalDocument().getStatus() == OriginalDocument.Status.FINALIZED) {
            throw new IllegalArgumentException("Cannot modify a finalized document");
        }
        List<Page> pages = pageRepository.findByIdIn(pageIds);
        if (pages.isEmpty()) {
            return;
        }
        // Group pages by their current source document
        Map<SplitPart, List<Page>> bySource = pages.stream()
                .collect(Collectors.groupingBy(Page::getSplitPart));
        for (Map.Entry<SplitPart, List<Page>> entry : bySource.entrySet()) {
            SplitPart source = entry.getKey();
            if (source == null) {
                throw new IllegalArgumentException("All pages must belong to a document");
            }
            // Same split constraint
            if (!source.getOriginalDocument().getId().equals(targetSplitPart.getOriginalDocument().getId())) {
                throw new IllegalArgumentException("Cannot move pages across different original documents");
            }
            if (source.getOriginalDocument().getStatus() == OriginalDocument.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot modify a finalized document");
            }
        }
        // Perform move
        for (Page page : pages) {
            page.setSplitPart(targetSplitPart);
            pageRepository.save(page);
        }
        // Remove any now-empty source documents
        for (SplitPart source : bySource.keySet()) {
            if (!source.getId().equals(targetSplitPart.getId())) {
                long remaining = pageRepository.findBySplitPartId(source.getId()).size();
                if (remaining == 0) {
                    splitPartRepository.delete(source);
                }
            }
        }
    }
}