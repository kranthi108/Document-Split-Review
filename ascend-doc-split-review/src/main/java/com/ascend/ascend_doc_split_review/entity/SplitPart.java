package com.ascend.ascend_doc_split_review.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "split_parts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_document_id", nullable = false)
    private OriginalDocument originalDocument;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String classification;

    @Column(nullable = false)
    private String filename;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @OneToMany(mappedBy = "splitPart", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Page> pages;

    @Column(name = "from_page")
    private Integer fromPage;

    @Column(name = "to_page")
    private Integer toPage;

    public enum Status {
        PENDING, FINALIZED
    }
}

