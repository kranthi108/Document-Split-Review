package com.ascend.ascend_doc_split_review.service;

import com.ascend.ascend_doc_split_review.entity.OriginalDocument;
import com.ascend.ascend_doc_split_review.entity.User;
import com.ascend.ascend_doc_split_review.repository.OriginalDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OriginalDocumentService {

    @Autowired
    private OriginalDocumentRepository originalDocumentRepository;

    @Retryable(
            value = {TransientDataAccessException.class, OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000)
    )
    public OriginalDocument createOriginalDocument(User user, String originalFilename) {
        OriginalDocument doc = new OriginalDocument();
        doc.setUser(user);
        doc.setOriginalFilename(originalFilename);
        doc.setStatus(OriginalDocument.Status.PENDING);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        return originalDocumentRepository.save(doc);
    }

    public Optional<OriginalDocument> getById(Long id) {
        return originalDocumentRepository.findById(id);
    }

    public List<OriginalDocument> getByUser(Long userId) {
        return originalDocumentRepository.findByUserId(userId);
    }

    @Retryable(
            value = {TransientDataAccessException.class, OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000)
    )
    public OriginalDocument finalizeDocument(Long id) {
        Optional<OriginalDocument> opt = originalDocumentRepository.findById(id);
        if (opt.isPresent()) {
            OriginalDocument doc = opt.get();
            doc.setStatus(OriginalDocument.Status.FINALIZED);
            doc.setUpdatedAt(LocalDateTime.now());
            return originalDocumentRepository.save(doc);
        }
        throw new RuntimeException("Original document not found");
    }

    @Retryable(
            value = {TransientDataAccessException.class, OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000)
    )
    public void deleteOriginalDocument(Long id) {
        Optional<OriginalDocument> opt = originalDocumentRepository.findById(id);
        if (opt.isPresent()) {
            OriginalDocument doc = opt.get();
            if (doc.getStatus() == OriginalDocument.Status.FINALIZED) {
                throw new IllegalArgumentException("Cannot delete a finalized document");
            }
            originalDocumentRepository.delete(doc);
            return;
        }
        throw new RuntimeException("Original document not found");
    }
}

