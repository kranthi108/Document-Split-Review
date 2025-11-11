package com.ascend.ascend_doc_split_review.service;

import com.ascend.ascend_doc_split_review.entity.Split;
import com.ascend.ascend_doc_split_review.entity.User;
import com.ascend.ascend_doc_split_review.repository.SplitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SplitService {

    @Autowired
    private SplitRepository splitRepository;

    public Split createSplit(User user, String originalFilename) {
        Split split = new Split();
        split.setUser(user);
        split.setOriginalFilename(originalFilename);
        split.setStatus(Split.Status.PENDING);
        split.setCreatedAt(LocalDateTime.now());
        split.setUpdatedAt(LocalDateTime.now());
        return splitRepository.save(split);
    }

    public Optional<Split> getSplitById(Long id) {
        return splitRepository.findById(id);
    }

    public List<Split> getSplitsByUser(Long userId) {
        return splitRepository.findByUserId(userId);
    }

    public Split finalizeSplit(Long splitId) {
        Optional<Split> splitOpt = splitRepository.findById(splitId);
        if (splitOpt.isPresent()) {
            Split split = splitOpt.get();
            split.setStatus(Split.Status.FINALIZED);
            split.setUpdatedAt(LocalDateTime.now());
            return splitRepository.save(split);
        }
        throw new RuntimeException("Split not found");
    }
}