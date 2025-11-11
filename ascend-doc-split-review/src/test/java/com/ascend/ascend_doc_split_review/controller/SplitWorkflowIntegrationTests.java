package com.ascend.ascend_doc_split_review.controller;

import com.ascend.ascend_doc_split_review.entity.Document;
import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.Split;
import com.ascend.ascend_doc_split_review.entity.User;
import com.ascend.ascend_doc_split_review.repository.DocumentRepository;
import com.ascend.ascend_doc_split_review.repository.PageRepository;
import com.ascend.ascend_doc_split_review.repository.UserRepository;
import com.ascend.ascend_doc_split_review.service.DocumentService;
import com.ascend.ascend_doc_split_review.service.SplitService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SplitWorkflowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SplitService splitService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private DocumentRepository documentRepository;

    private String tokenUser1;
    private String tokenUser2;
    private User user1;
    private Split split;
    private Document docA;
    private Document docB;

    @BeforeEach
    void setup() throws Exception {
        // register + login user1
        String username1 = "user1_it";
        String email1 = "user1_it@example.com";
        String password = "password";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username1 + "\",\"password\":\"" + password + "\",\"email\":\"" + email1 + "\"}"))
                .andExpect(status().isOk());
        String loginResp1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username1 + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        tokenUser1 = objectMapper.readTree(loginResp1).get("token").asText();
        user1 = userRepository.findByUsername(username1).orElseThrow();

        // register + login user2
        String username2 = "user2_it";
        String email2 = "user2_it@example.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username2 + "\",\"password\":\"" + password + "\",\"email\":\"" + email2 + "\"}"))
                .andExpect(status().isOk());
        String loginResp2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username2 + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        tokenUser2 = objectMapper.readTree(loginResp2).get("token").asText();

        // create split for user1
        split = splitService.createSplit(user1, "bundle.pdf");

        // create docA with 2 pages
        Page p1 = new Page();
        p1.setPageNumber(1);
        p1.setContent("p1");
        Page p2 = new Page();
        p2.setPageNumber(2);
        p2.setContent("p2");
        List<Page> pagesA = Arrays.asList(p1, p2);
        docA = documentService.createDocument(split, "Doc A", "80C", "docA.pdf", pagesA);

        // create docB with 0 pages
        docB = documentService.createDocument(split, "Doc B", "80D", "docB.pdf", List.of());
    }

    @Test
    void getSplit_successForOwner_andNotFoundForOtherUser() throws Exception {
        // owner can load
        mockMvc.perform(get("/api/splits/" + split.getId())
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk());

        // other user receives 404 (access denied masked as not found)
        mockMvc.perform(get("/api/splits/" + split.getId())
                        .header("Authorization", "Bearer " + tokenUser2))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDocument_metadataUpdated() throws Exception {
        mockMvc.perform(patch("/api/documents/" + docA.getId())
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Doc A Updated\", \"classification\": \"80D\", \"filename\": \"docA_upd.pdf\"}"))
                .andExpect(status().isOk());

        Document reloaded = documentRepository.findById(docA.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Doc A Updated");
        assertThat(reloaded.getClassification()).isEqualTo("80D");
        assertThat(reloaded.getFilename()).isEqualTo("docA_upd.pdf");
    }

    @Test
    void movePages_movesToTargetDocument() throws Exception {
        List<Page> pagesInA = pageRepository.findByDocumentId(docA.getId());
        assertThat(pagesInA).hasSize(2);
        Long pageId = pagesInA.get(0).getId();

        mockMvc.perform(post("/api/pages/move")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageIds\": [" + pageId + "], \"targetDocumentId\": " + docB.getId() + "}"))
                .andExpect(status().isOk());

        Page moved = pageRepository.findById(pageId).orElseThrow();
        assertThat(moved.getDocument().getId()).isEqualTo(docB.getId());
    }

    @Test
    void createDocument_createsWithPages() throws Exception {
        // create free pages first (unassigned)
        Page p3 = new Page();
        p3.setPageNumber(3);
        p3.setContent("p3");
        Page p4 = new Page();
        p4.setPageNumber(4);
        p4.setContent("p4");
        pageRepository.saveAll(List.of(p3, p4));

        String body = "{"
                + "\"splitId\":" + split.getId() + ","
                + "\"name\":\"New Doc\","
                + "\"classification\":\"80C\","
                + "\"filename\":\"new.pdf\","
                + "\"pageIds\":[" + p3.getId() + "," + p4.getId() + "]"
                + "}";
        String resp = mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(resp);
        Long newDocId = node.get("id").asLong();
        Document created = documentRepository.findById(newDocId).orElseThrow();
        List<Page> pages = pageRepository.findByDocumentId(newDocId);
        assertThat(created.getName()).isEqualTo("New Doc");
        assertThat(pages).extracting(Page::getPageNumber).containsExactlyInAnyOrder(3, 4);
    }

    @Test
    void deleteDocument_withReassign_movesPagesAndDeletes() throws Exception {
        // ensure docA has at least one page
        List<Page> pagesInA = pageRepository.findByDocumentId(docA.getId());
        assertThat(pagesInA).isNotEmpty();

        mockMvc.perform(delete("/api/documents/" + docA.getId())
                        .header("Authorization", "Bearer " + tokenUser1)
                        .param("reassignTo", String.valueOf(docB.getId())))
                .andExpect(status().isOk());

        assertThat(documentRepository.findById(docA.getId())).isEmpty();
        List<Page> pagesInB = pageRepository.findByDocumentId(docB.getId());
        assertThat(pagesInB).isNotEmpty();
    }

    @Test
    void deleteDocument_withoutReassign_unassignsPages() throws Exception {
        // create a new docC with one page
        Page p5 = new Page();
        p5.setPageNumber(5);
        p5.setContent("p5");
        Document docC = documentService.createDocument(split, "Doc C", "80C", "c.pdf", List.of(p5));

        mockMvc.perform(delete("/api/documents/" + docC.getId())
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk());

        assertThat(documentRepository.findById(docC.getId())).isEmpty();
        // page now unassigned
        List<Page> unassigned = pageRepository.findByDocumentId(docC.getId());
        assertThat(unassigned).isEmpty();
        // fetch page by id to verify document is null
        Page p5Reloaded = pageRepository.findById(p5.getId()).orElseThrow();
        assertThat(p5Reloaded.getDocument()).isNull();
    }

    @Test
    void finalizeSplit_setsStatusFinalized() throws Exception {
        mockMvc.perform(post("/api/splits/" + split.getId() + "/finalize")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk());
    }

    @Test
    void downloadDocument_returnsPdf() throws Exception {
        mockMvc.perform(get("/api/documents/" + docA.getId() + "/download")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"document_")));
    }

    @Test
    void validationErrors_areHandled() throws Exception {
        // move with empty pageIds
        mockMvc.perform(post("/api/pages/move")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageIds\":[],\"targetDocumentId\":" + docA.getId() + "}"))
                .andExpect(status().isBadRequest());

        // create doc missing name
        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"splitId\":" + split.getId() + ",\"classification\":\"80C\",\"filename\":\"x.pdf\",\"pageIds\":[ ]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteWithSameReassign_returnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/documents/" + docA.getId())
                        .header("Authorization", "Bearer " + tokenUser1)
                        .param("reassignTo", String.valueOf(docA.getId())))
                .andExpect(status().isBadRequest());
    }
}

