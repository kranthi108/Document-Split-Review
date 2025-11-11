package com.ascend.ascend_doc_split_review.controller;

import com.ascend.ascend_doc_split_review.dto.*;
import com.ascend.ascend_doc_split_review.entity.Document;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.Split;
import com.ascend.ascend_doc_split_review.repository.SplitRepository;
import com.ascend.ascend_doc_split_review.repository.DocumentRepository;
import com.ascend.ascend_doc_split_review.repository.PageRepository;
import com.ascend.ascend_doc_split_review.security.UserPrincipal;
import com.ascend.ascend_doc_split_review.service.DocumentService;
import com.ascend.ascend_doc_split_review.service.PageService;
import com.ascend.ascend_doc_split_review.service.SplitService;
import com.ascend.ascend_doc_split_review.service.MockDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SplitController {

    private static final Logger logger = LoggerFactory.getLogger(SplitController.class);

    @Autowired
    private SplitService splitService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private PageService pageService;

    @Autowired
    private SplitRepository splitRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private MockDownloadService mockDownloadService;

    @GetMapping("/splits/{splitId}")
    public ResponseEntity<SplitResponse> getSplit(@PathVariable Long splitId, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        logger.info("User {} requesting split {}", userPrincipal.getUsername(), splitId);
        Optional<Split> splitOpt = splitService.getSplitById(splitId);
        if (splitOpt.isPresent() && splitOpt.get().getUser().getId().equals(userPrincipal.getUser().getId())) {
            logger.info("Split {} retrieved successfully", splitId);
            return ResponseEntity.ok(SplitResponse.fromEntity(splitOpt.get()));
        }
        logger.warn("Split {} not found or access denied for user {}", splitId, userPrincipal.getUsername());
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/documents/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(@PathVariable Long id, @RequestBody UpdateDocumentRequest request, Authentication auth) {
        // Assuming authorization check - user owns the split
        Document updated = documentService.updateDocument(id, request.getName(), request.getClassification(), request.getFilename());
        return ResponseEntity.ok(DocumentResponse.fromEntity(updated));
    }

    @PostMapping("/pages/move")
    public ResponseEntity<?> movePages(@RequestBody MovePagesRequest request, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        logger.info("User {} moving pages {} to document {}", userPrincipal.getUsername(), request.getPageIds(), request.getTargetDocumentId());
        Optional<Document> targetDocOpt = documentRepository.findById(request.getTargetDocumentId());
        if (targetDocOpt.isPresent()) {
            pageService.movePagesToDocument(request.getPageIds(), targetDocOpt.get());
            logger.info("Pages moved successfully");
            return ResponseEntity.ok().build();
        }
        logger.warn("Target document {} not found", request.getTargetDocumentId());
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/documents")
    public ResponseEntity<DocumentResponse> createDocument(@RequestBody CreateDocumentRequest request, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Optional<Split> splitOpt = splitRepository.findById(request.getSplitId());
        if (splitOpt.isPresent() && splitOpt.get().getUser().getId().equals(userPrincipal.getUser().getId())) {
            List<Page> pages = pageRepository.findByIdIn(request.getPageIds());
            Document doc = documentService.createDocument(splitOpt.get(), request.getName(), request.getClassification(), request.getFilename(), pages);
            return ResponseEntity.ok(DocumentResponse.fromEntity(doc));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id, Authentication auth) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/splits/{splitId}/finalize")
    public ResponseEntity<SplitResponse> finalizeSplit(@PathVariable Long splitId, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Optional<Split> splitOpt = splitRepository.findById(splitId);
        if (splitOpt.isPresent() && splitOpt.get().getUser().getId().equals(userPrincipal.getUser().getId())) {
            Split finalized = splitService.finalizeSplit(splitId);
            return ResponseEntity.ok(SplitResponse.fromEntity(finalized));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, Authentication auth) {
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        logger.info("User {} downloading document {}", userPrincipal.getUsername(), id);
        byte[] pdfContent = mockDownloadService.getMockFile(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"document_" + id + ".pdf\"")
                .body(pdfContent);
    }
}