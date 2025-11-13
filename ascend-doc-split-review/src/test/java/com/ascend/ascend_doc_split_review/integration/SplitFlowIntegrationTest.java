package com.ascend.ascend_doc_split_review.integration;

import com.ascend.ascend_doc_split_review.entity.OriginalDocument;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.SplitPart;
import com.ascend.ascend_doc_split_review.repository.OriginalDocumentRepository;
import com.ascend.ascend_doc_split_review.repository.PageRepository;
import com.ascend.ascend_doc_split_review.repository.SplitPartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SplitFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OriginalDocumentRepository originalDocumentRepository;

    @Autowired
    private SplitPartRepository splitPartRepository;

    @Autowired
    private PageRepository pageRepository;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String body = "{\"username\":\"demo\",\"password\":\"password\"}";
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // very simple extraction: {"token":"..."}
        token = json.replace("{", "").replace("}", "")
                .replace("\"", "").split(":")[1];
    }

    @Test
    void getSplit_returnsOnlyRequestedSplitPart() throws Exception {
        OriginalDocument doc = originalDocumentRepository.findAll().get(0);
        List<SplitPart> parts = splitPartRepository.findByOriginalDocumentId(doc.getId());
        SplitPart target = parts.get(0);

        mockMvc.perform(get("/api/splits/" + target.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(target.getId().intValue())))
                .andExpect(jsonPath("$.pages", notNullValue()))
                .andExpect(jsonPath("$.splitParts").doesNotExist());
    }

    @Test
    void movePages_nonContiguousTarget_rejected() throws Exception {
        OriginalDocument doc = originalDocumentRepository.findAll().get(0);
        List<SplitPart> parts = splitPartRepository.findByOriginalDocumentId(doc.getId());
        // Assume seeded: part A has pages 1-2, part B has page 3
        SplitPart partA = parts.get(0).getFromPage() != null && parts.get(0).getFromPage() == 1 ? parts.get(0) : parts.get(1);
        SplitPart partB = parts.get(0).getFromPage() != null && parts.get(0).getFromPage() == 3 ? parts.get(0) : parts.get(1);
        // Add page 4 to partB to create gap when moving only 4 into A
        Page p4 = new Page();
        p4.setPageNumber(4);
        p4.setSplitPart(partB);
        pageRepository.save(p4);
        // find id of page 4 we just created
        Long page4Id = pageRepository.findBySplitPartId(partB.getId()).stream()
                .filter(p -> p.getPageNumber() == 4).findFirst().get().getId();
        String req = String.format("{\"pageIds\":[%d],\"targetSplitPartId\":%d}", page4Id, partA.getId());

        mockMvc.perform(post("/api/pages/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void finalizeSplitPart_blocksUpdateAndMove() throws Exception {
        OriginalDocument doc = originalDocumentRepository.findAll().get(0);
        SplitPart part = splitPartRepository.findByOriginalDocumentId(doc.getId()).get(0);

        mockMvc.perform(post("/api/split-parts/" + part.getId() + "/finalize")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/split-parts/" + part.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        // Attempt to move existing page from this finalized split to itself (should be blocked)
        Long anyPageId = pageRepository.findBySplitPartId(part.getId()).get(0).getId();
        String req = String.format("{\"pageIds\":[%d],\"targetSplitPartId\":%d}", anyPageId, part.getId());
        mockMvc.perform(post("/api/pages/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void movePages_success_deletesEmptySource() throws Exception {
        OriginalDocument doc = originalDocumentRepository.findAll().get(0);
        List<SplitPart> parts = splitPartRepository.findByOriginalDocumentId(doc.getId());
        SplitPart partA = parts.get(0).getFromPage() != null && parts.get(0).getFromPage() == 1 ? parts.get(0) : parts.get(1);
        SplitPart partB = parts.get(0).getFromPage() != null && parts.get(0).getFromPage() == 3 ? parts.get(0) : parts.get(1);
        // move page 3 from B -> A (1-2 + 3 is contiguous)
        Long page3Id = pageRepository.findBySplitPartId(partB.getId()).get(0).getId();
        String req = String.format("{\"pageIds\":[%d],\"targetSplitPartId\":%d}", page3Id, partA.getId());

        mockMvc.perform(post("/api/pages/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // source should be deleted
        org.assertj.core.api.Assertions.assertThat(splitPartRepository.findById(partB.getId())).isEmpty();
        // target range should be updated to include 1..3
        SplitPart updatedA = splitPartRepository.findById(partA.getId()).get();
        org.assertj.core.api.Assertions.assertThat(updatedA.getFromPage()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(updatedA.getToPage()).isEqualTo(3);
    }

    @Test
    void patchDocument_updatesMetadata_viaAlias() throws Exception {
        OriginalDocument doc = originalDocumentRepository.findAll().get(0);
        SplitPart part = splitPartRepository.findByOriginalDocumentId(doc.getId()).get(0);

        mockMvc.perform(patch("/api/document/" + part.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Name\",\"classification\":\"X\",\"filename\":\"f.pdf\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.classification", is("X")))
                .andExpect(jsonPath("$.filename", is("f.pdf")));
    }
}

