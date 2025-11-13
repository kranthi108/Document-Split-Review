package com.ascend.ascend_doc_split_review.service;

import com.ascend.ascend_doc_split_review.entity.SplitPart;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.OriginalDocument;
import com.ascend.ascend_doc_split_review.repository.SplitPartRepository;
import com.ascend.ascend_doc_split_review.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SplitPartService {

    @Autowired
    private SplitPartRepository splitPartRepository;

    @Autowired
    private PageRepository pageRepository;

    public SplitPart createSplitPart(OriginalDocument originalDocument, String name, String classification, String filename, List<Page> pages) {
        if (originalDocument.getStatus() == OriginalDocument.Status.FINALIZED) {
            throw new IllegalArgumentException("Cannot modify a finalized document");
        }
        // If a page is already assigned, ensure it belongs to the same original document
        for (Page page : pages) {
            if (page.getSplitPart() != null && page.getSplitPart().getOriginalDocument() != null) {
                if (!page.getSplitPart().getOriginalDocument().getId().equals(originalDocument.getId())) {
                    throw new IllegalArgumentException("All pages must belong to the same original document");
                }
            }
        }
        SplitPart splitPart = new SplitPart();
        splitPart.setOriginalDocument(originalDocument);
        splitPart.setName(name);
        splitPart.setClassification(classification);
        splitPart.setFilename(filename);
        splitPart.setCreatedAt(LocalDateTime.now());
        splitPart.setUpdatedAt(LocalDateTime.now());
        splitPart.setPages(pages);
        int minPage = pages.stream().map(Page::getPageNumber).min(Integer::compareTo).orElse(0);
        int maxPage = pages.stream().map(Page::getPageNumber).max(Integer::compareTo).orElse(0);
        splitPart.setFromPage(minPage);
        splitPart.setToPage(maxPage);
        for (Page page : pages) {
            page.setSplitPart(splitPart);
        }
        return splitPartRepository.save(splitPart);
    }

    public Optional<SplitPart> getById(Long id) {
        return splitPartRepository.findById(id);
    }

    public List<SplitPart> getByOriginalDocument(Long originalDocumentId) {
        return splitPartRepository.findByOriginalDocumentId(originalDocumentId);
    }

    public SplitPart updateSplitPart(Long id, String name, String classification, String filename) {
        Optional<SplitPart> opt = splitPartRepository.findById(id);
        if (opt.isPresent()) {
            SplitPart sp = opt.get();
            if (sp.getOriginalDocument().getStatus() == OriginalDocument.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot modify a finalized document");
            }
            if (sp.getStatus() == SplitPart.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot modify a finalized split part");
            }
            if (name != null) sp.setName(name);
            if (classification != null) sp.setClassification(classification);
            if (filename != null) sp.setFilename(filename);
            sp.setUpdatedAt(LocalDateTime.now());
            return splitPartRepository.save(sp);
        }
        throw new RuntimeException("Split part not found");
    }

    @Transactional
    public void deleteSplitPart(Long id, Long reassignToSplitPartId) {
        Optional<SplitPart> opt = splitPartRepository.findById(id);
        if (opt.isPresent()) {
            SplitPart sp = opt.get();
            if (sp.getOriginalDocument().getStatus() == OriginalDocument.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot modify a finalized document");
            }
            if (sp.getStatus() == SplitPart.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot delete a finalized split part");
            }
            List<Page> pages = pageRepository.findBySplitPartId(id);
            SplitPart target = null;
            if (reassignToSplitPartId != null) {
                if (reassignToSplitPartId.equals(id)) {
                    throw new IllegalArgumentException("Cannot reassign pages to the same split part being deleted");
                }
                target = splitPartRepository.findById(reassignToSplitPartId)
                        .orElseThrow(() -> new IllegalArgumentException("Target split part not found: " + reassignToSplitPartId));
                if (!target.getOriginalDocument().getId().equals(sp.getOriginalDocument().getId())) {
                    throw new IllegalArgumentException("Target split part must belong to the same original document");
                }
                if (target.getOriginalDocument().getStatus() == OriginalDocument.Status.FINALIZED) {
                    throw new IllegalArgumentException("Cannot modify a finalized document");
                }
                if (target.getStatus() == SplitPart.Status.FINALIZED) {
                    throw new IllegalArgumentException("Cannot reassign pages to a finalized split part");
                }
            }
            for (Page page : pages) {
                page.setSplitPart(target);
                pageRepository.save(page);
            }
            splitPartRepository.delete(sp);
        } else {
            throw new RuntimeException("Split part not found");
        }
    }

    public SplitPart finalizeSplitPart(Long id) {
        Optional<SplitPart> opt = splitPartRepository.findById(id);
        if (opt.isPresent()) {
            SplitPart sp = opt.get();
            if (sp.getOriginalDocument().getStatus() == OriginalDocument.Status.FINALIZED) {
                return sp; // already effectively locked by parent
            }
            sp.setStatus(SplitPart.Status.FINALIZED);
            sp.setUpdatedAt(LocalDateTime.now());
            return splitPartRepository.save(sp);
        }
        throw new RuntimeException("Split part not found");
    }
}

