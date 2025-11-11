package com.ascend.ascend_doc_split_review.config;

import com.ascend.ascend_doc_split_review.entity.Page;
import com.ascend.ascend_doc_split_review.entity.Split;
import com.ascend.ascend_doc_split_review.entity.User;
import com.ascend.ascend_doc_split_review.repository.UserRepository;
import com.ascend.ascend_doc_split_review.service.DocumentService;
import com.ascend.ascend_doc_split_review.service.SplitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedDemoData(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   SplitService splitService,
                                   DocumentService documentService) {
        return args -> {
            if (userRepository.findByUsername("demo").isPresent()) {
                log.info("Demo data already present, skipping seeding");
                return;
            }

            // Create demo user
            User demo = new User();
            demo.setUsername("demo");
            demo.setEmail("demo@example.com");
            demo.setPassword(passwordEncoder.encode("password"));
            demo = userRepository.save(demo);
            log.info("Created demo user username=demo password=password id={}", demo.getId());

            // Create a split
            Split split = splitService.createSplit(demo, "bundle.pdf");
            log.info("Created split id={} for user id={}", split.getId(), demo.getId());

            // Create documents with pages
            Page p1 = new Page();
            p1.setPageNumber(1);
            p1.setContent("Sample content page 1");
            Page p2 = new Page();
            p2.setPageNumber(2);
            p2.setContent("Sample content page 2");
            List<Page> doc80cPages = Arrays.asList(p1, p2);
            var doc80c = documentService.createDocument(split, "Form 80C", "80C", "client_80c.pdf", doc80cPages);
            log.info("Created document 80C id={} with {} pages", doc80c.getId(), doc80cPages.size());

            Page p3 = new Page();
            p3.setPageNumber(3);
            p3.setContent("Sample content page 3");
            List<Page> doc80dPages = Arrays.asList(p3);
            var doc80d = documentService.createDocument(split, "Form 80D", "80D", "client_80d.pdf", doc80dPages);
            log.info("Created document 80D id={} with {} pages", doc80d.getId(), doc80dPages.size());

            log.info("Demo data seeded successfully. Try logging in with username=demo password=password");
        };
    }
}

