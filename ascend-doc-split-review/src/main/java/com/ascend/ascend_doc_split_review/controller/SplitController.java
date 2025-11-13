package com.ascend.ascend_doc_split_review.controller;

import com.ascend.ascend_doc_split_review.dto.*;
import com.ascend.ascend_doc_split_review.entity.SplitPart;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.OriginalDocument;
import com.ascend.ascend_doc_split_review.repository.OriginalDocumentRepository;
import com.ascend.ascend_doc_split_review.repository.SplitPartRepository;
import com.ascend.ascend_doc_split_review.repository.PageRepository;
import com.ascend.ascend_doc_split_review.security.UserPrincipal;
import com.ascend.ascend_doc_split_review.service.SplitPartService;
import com.ascend.ascend_doc_split_review.service.PageService;
import com.ascend.ascend_doc_split_review.service.OriginalDocumentService;
import com.ascend.ascend_doc_split_review.service.MockDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SplitController {

    private static final Logger logger = LoggerFactory.getLogger(SplitController.class);

    @Autowired
    private OriginalDocumentService originalDocumentService;

    @Autowired
    private SplitPartService splitPartService;

    @Autowired
    private PageService pageService;

    @Autowired
    private OriginalDocumentRepository originalDocumentRepository;

    @Autowired
    private SplitPartRepository splitPartRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private MockDownloadService mockDownloadService;

    @Autowired
    private MeterRegistry meterRegistry;

    // Alias: Get split (original document) by id
    @GetMapping("/splits/{id}")
    public ResponseEntity<OriginalDocumentResponse> getSplit(@PathVariable Long id, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        logger.info("User {} requesting split {}", userPrincipal.getUsername(), id);
        meterRegistry.counter("api.split.get").increment();
        Optional<OriginalDocument> docOpt = originalDocumentService.getById(id);
        if (docOpt.isPresent() && docOpt.get().getUser().getId().equals(userPrincipal.getUser().getId())) {
            return ResponseEntity.ok(OriginalDocumentResponse.fromEntity(docOpt.get()));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<OriginalDocumentResponse> getDocument(@PathVariable Long documentId, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        logger.info("User {} requesting document {}", userPrincipal.getUsername(), documentId);
        meterRegistry.counter("api.document.get").increment();
        Optional<OriginalDocument> docOpt = originalDocumentService.getById(documentId);
        if (docOpt.isPresent() && docOpt.get().getUser().getId().equals(userPrincipal.getUser().getId())) {
            logger.info("Document {} retrieved successfully", documentId);
            return ResponseEntity.ok(OriginalDocumentResponse.fromEntity(docOpt.get()));
        }
        logger.warn("Document {} not found or access denied for user {}", documentId, userPrincipal.getUsername());
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/split-parts/{id}")
    public ResponseEntity<SplitPartResponse> updateSplitPart(@PathVariable Long id, @Valid @RequestBody UpdateSplitPartRequest request, Authentication auth) {
        meterRegistry.counter("api.splitpart.update").increment();
        SplitPart updated = splitPartService.updateSplitPart(id, request.getName(), request.getClassification(), request.getFilename());
        return ResponseEntity.ok(SplitPartResponse.fromEntity(updated));
    }

    // Alias: Update document (split part) metadata
    @PatchMapping("/document/{id}")
    public ResponseEntity<SplitPartResponse> updateDocument(@PathVariable Long id, @Valid @RequestBody UpdateSplitPartRequest request, Authentication auth) {
        meterRegistry.counter("api.document.update").increment();
        SplitPart updated = splitPartService.updateSplitPart(id, request.getName(), request.getClassification(), request.getFilename());
        return ResponseEntity.ok(SplitPartResponse.fromEntity(updated));
    }

    @PostMapping("/pages/move")
    public ResponseEntity<?> movePages(@Valid @RequestBody MovePagesRequest request, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        logger.info("User {} moving pages {} to splitPart {}", userPrincipal.getUsername(), request.getPageIds(), request.getTargetSplitPartId());
        meterRegistry.counter("api.pages.move").increment();
        Optional<SplitPart> targetOpt = splitPartRepository.findById(request.getTargetSplitPartId());
        if (targetOpt.isPresent()) {
            SplitPart target = targetOpt.get();
            // Ownership check
            if (!target.getOriginalDocument().getUser().getId().equals(userPrincipal.getUser().getId())) {
                logger.warn("Access denied for user {} to target splitPart {}", userPrincipal.getUsername(), target.getId());
                return ResponseEntity.status(403).build();
            }
            if (target.getOriginalDocument().getStatus() == OriginalDocument.Status.FINALIZED) {
                logger.warn("Attempt to modify finalized document {}", target.getOriginalDocument().getId());
                return ResponseEntity.badRequest().body("Cannot modify a finalized document");
            }
            pageService.movePagesToSplitPart(request.getPageIds(), target);
            logger.info("Pages moved successfully");
            return ResponseEntity.ok().build();
        }
        logger.warn("Target splitPart {} not found", request.getTargetSplitPartId());
        return ResponseEntity.notFound().build();
    }

    // Alias: Create document (split part) with page IDs and metadata
    @PostMapping("/document")
    public ResponseEntity<SplitPartResponse> createDocument(@Valid @RequestBody CreateSplitPartRequest request, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        meterRegistry.counter("api.document.create").increment();
        Optional<OriginalDocument> docOpt = originalDocumentRepository.findById(request.getOriginalDocumentId());
        if (docOpt.isPresent() && docOpt.get().getUser().getId().equals(userPrincipal.getUser().getId())) {
            if (docOpt.get().getStatus() == OriginalDocument.Status.FINALIZED) {
                return ResponseEntity.badRequest().body(null);
            }
            List<Page> pages = pageRepository.findByIdIn(request.getPageIds());
            SplitPart sp = splitPartService.createSplitPart(docOpt.get(), request.getName(), request.getClassification(), request.getFilename(), pages);
            return ResponseEntity.ok(SplitPartResponse.fromEntity(sp));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/split-parts")
    public ResponseEntity<SplitPartResponse> createSplitPart(@Valid @RequestBody CreateSplitPartRequest request, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        meterRegistry.counter("api.splitpart.create").increment();
        Optional<OriginalDocument> docOpt = originalDocumentRepository.findById(request.getOriginalDocumentId());
        if (docOpt.isPresent() && docOpt.get().getUser().getId().equals(userPrincipal.getUser().getId())) {
            if (docOpt.get().getStatus() == OriginalDocument.Status.FINALIZED) {
                return ResponseEntity.badRequest().body(null);
            }
            List<Page> pages = pageRepository.findByIdIn(request.getPageIds());
            SplitPart sp = splitPartService.createSplitPart(docOpt.get(), request.getName(), request.getClassification(), request.getFilename(), pages);
            return ResponseEntity.ok(SplitPartResponse.fromEntity(sp));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/split-parts/{id}")
    public ResponseEntity<?> deleteSplitPart(@PathVariable Long id, @RequestParam(value = "reassignTo", required = false) Long reassignTo, Authentication auth) {
        meterRegistry.counter("api.splitpart.delete").increment();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Optional<SplitPart> spOpt = splitPartRepository.findById(id);
        if (spOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        SplitPart sp = spOpt.get();
        if (!sp.getOriginalDocument().getUser().getId().equals(userPrincipal.getUser().getId())) {
            return ResponseEntity.status(403).build();
        }
        splitPartService.deleteSplitPart(id, reassignTo);
        return ResponseEntity.ok().build();
    }

    // Alias: Delete document (split part)
    @DeleteMapping("/document/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id, @RequestParam(value = "reassignTo", required = false) Long reassignTo, Authentication auth) {
        meterRegistry.counter("api.document.delete").increment();
        return deleteSplitPart(id, reassignTo, auth);
    }

    @PostMapping("/split-parts/{id}/finalize")
    public ResponseEntity<SplitPartResponse> finalizeSplitPart(@PathVariable Long id, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        meterRegistry.counter("api.splitpart.finalize").increment();
        Optional<SplitPart> spOpt = splitPartRepository.findById(id);
        if (spOpt.isPresent() && spOpt.get().getOriginalDocument().getUser().getId().equals(userPrincipal.getUser().getId())) {
            SplitPart finalized = splitPartService.finalizeSplitPart(id);
            return ResponseEntity.ok(SplitPartResponse.fromEntity(finalized));
        }
        return ResponseEntity.notFound().build();
    }
    @PostMapping("/documents/{documentId}/finalize")
    public ResponseEntity<OriginalDocumentResponse> finalizeDocument(@PathVariable Long documentId, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        meterRegistry.counter("api.document.finalize").increment();
        Optional<OriginalDocument> docOpt = originalDocumentRepository.findById(documentId);
        if (docOpt.isPresent() && docOpt.get().getUser().getId().equals(userPrincipal.getUser().getId())) {
            OriginalDocument finalized = originalDocumentService.finalizeDocument(documentId);
            return ResponseEntity.ok(OriginalDocumentResponse.fromEntity(finalized));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        logger.info("User {} downloading document {}", userPrincipal.getUsername(), id);
        meterRegistry.counter("api.document.download").increment();
        byte[] pdfContent = mockDownloadService.getMockFile(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"document_" + id + ".pdf\"")
                .body(pdfContent);
    }
}