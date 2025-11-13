package com.ascend.ascend_doc_split_review.service;

import com.ascend.ascend_doc_split_review.entity.SplitPart;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.OriginalDocument;
import com.ascend.ascend_doc_split_review.repository.SplitPartRepository;
import com.ascend.ascend_doc_split_review.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
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

    private void recomputeAndSaveRange(SplitPart splitPart) {
        List<Page> currentPages = pageRepository.findBySplitPartId(splitPart.getId());
        if (currentPages.isEmpty()) {
            splitPart.setFromPage(null);
            splitPart.setToPage(null);
        } else {
            int minPage = currentPages.stream().map(Page::getPageNumber).min(Integer::compareTo).orElse(0);
            int maxPage = currentPages.stream().map(Page::getPageNumber).max(Integer::compareTo).orElse(0);
            splitPart.setFromPage(minPage);
            splitPart.setToPage(maxPage);
        }
        splitPartRepository.save(splitPart);
    }

    private boolean isContiguous(List<Integer> pageNumbers) {
        if (pageNumbers.isEmpty()) return true;
        List<Integer> sortedDistinct = pageNumbers.stream().distinct().sorted().toList();
        for (int i = 1; i < sortedDistinct.size(); i++) {
            if (sortedDistinct.get(i) - sortedDistinct.get(i - 1) != 1) {
                return false;
            }
        }
        return true;
    }

    // Better method
    @Transactional
    @Retryable(
            value = {TransientDataAccessException.class, OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000)
    )
    public void movePagesToSplitPart(List<Long> pageIds, SplitPart targetSplitPart) {
        if (targetSplitPart.getOriginalDocument().getStatus() == OriginalDocument.Status.FINALIZED) {
            throw new IllegalArgumentException("Cannot modify a finalized document");
        }
        if (targetSplitPart.getStatus() == SplitPart.Status.FINALIZED) {
            throw new IllegalArgumentException("Cannot move pages into a finalized split part");
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
            if (source.getStatus() == SplitPart.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot move pages from a finalized split part");
            }
        }
        // Contiguity validation for target
        List<Integer> targetExisting = pageRepository.findBySplitPartId(targetSplitPart.getId())
                .stream().map(Page::getPageNumber).toList();
        List<Integer> movingNumbers = pages.stream().map(Page::getPageNumber).toList();
        List<Integer> targetCombined = Stream.concat(targetExisting.stream(), movingNumbers.stream()).toList();
        if (!isContiguous(targetCombined)) {
            throw new IllegalArgumentException("Move rejected: target split would become non-contiguous. Include all pages in the range.");
        }
        // Contiguity validation for each source after removal
        for (Map.Entry<SplitPart, List<Page>> entry : bySource.entrySet()) {
            SplitPart source = entry.getKey();
            if (source.getId().equals(targetSplitPart.getId())) {
                // Same split; contiguity already checked via combined set
                continue;
            }
            List<Integer> sourceExisting = pageRepository.findBySplitPartId(source.getId())
                    .stream().map(Page::getPageNumber).toList();
            List<Integer> removed = entry.getValue().stream().map(Page::getPageNumber).toList();
            List<Integer> remaining = sourceExisting.stream()
                    .filter(n -> !removed.contains(n))
                    .toList();
            if (!remaining.isEmpty() && !isContiguous(remaining)) {
                throw new IllegalArgumentException("Move rejected: source split would become non-contiguous. Move full contiguous ranges.");
            }
        }
        // Perform move
        for (Page page : pages) {
            page.setSplitPart(targetSplitPart);
            pageRepository.save(page);
        }
        // Recompute ranges for affected split parts and remove any now-empty sources
        for (SplitPart source : bySource.keySet()) {
            if (!source.getId().equals(targetSplitPart.getId())) {
                List<Page> remainingPages = pageRepository.findBySplitPartId(source.getId());
                long remaining = remainingPages.size();
                if (remaining == 0) {
                    splitPartRepository.delete(source);
                } else {
                    recomputeAndSaveRange(source);
                }
            }
        }
        // Finally, recompute target range after pages have been moved
        recomputeAndSaveRange(targetSplitPart);
    }
}