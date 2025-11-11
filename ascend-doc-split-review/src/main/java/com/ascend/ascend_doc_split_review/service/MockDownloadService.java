package com.ascend.ascend_doc_split_review.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class MockDownloadService {

    private static final int NUM_MOCK_FILES = 100;
    private final Map<Long, byte[]> mockFiles = new HashMap<>();
    private final Random random = new Random();

    public MockDownloadService() {
        generateMockFiles();
    }

    private void generateMockFiles() {
        for (long i = 1; i <= NUM_MOCK_FILES; i++) {
            try {
                mockFiles.put(i, generateRandomPDF(i));
            } catch (IOException e) {
                // Fallback to empty byte array
                mockFiles.put(i, new byte[0]);
            }
        }
    }

    private byte[] generateRandomPDF(long id) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Mock Document ID: " + id);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Random Content: " + random.nextInt(1000));
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    public byte[] getMockFile(Long id) {
        return mockFiles.getOrDefault(id, new byte[0]);
    }
}