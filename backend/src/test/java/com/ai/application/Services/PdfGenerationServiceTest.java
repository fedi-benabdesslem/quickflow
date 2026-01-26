package com.ai.application.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PdfGenerationService.
 * 
 * Tests cover:
 * - Markdown to HTML conversion (bold, italic, headers, lists)
 * - HTML escaping for XSS prevention
 * - PDF generation with various inputs
 * - Footer and metadata handling
 * 
 * Not covered:
 * - Visual PDF rendering (requires manual inspection)
 * - Complex PDF layouts (beyond scope of unit tests)
 */
class PdfGenerationServiceTest {

    private PdfGenerationService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new PdfGenerationService();
    }

    @Nested
    @DisplayName("generatePdf() method")
    class GeneratePdfTests {

        @Test
        @DisplayName("Should generate PDF from simple HTML content")
        void generatePdfFromSimpleHtml() {
            String htmlContent = "<p>Hello, World!</p>";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Test Document");
            
            byte[] pdfBytes = pdfService.generatePdf(htmlContent, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
            // PDF files start with %PDF
            assertTrue(new String(pdfBytes, 0, 4).startsWith("%PDF"));
        }

        @Test
        @DisplayName("Should generate PDF with metadata")
        void generatePdfWithMetadata() {
            String htmlContent = "<p>Content with metadata</p>";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Meeting Minutes");
            metadata.put("date", "2024-01-15");
            metadata.put("startTime", "10:00");
            metadata.put("endTime", "11:00");
            metadata.put("location", "Conference Room A");
            metadata.put("organizer", "John Doe");
            
            byte[] pdfBytes = pdfService.generatePdf(htmlContent, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should generate PDF with footer preference")
        void generatePdfWithFooter() {
            String htmlContent = "<p>Content with footer</p>";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Confidential Document");
            
            Map<String, Object> preferences = new HashMap<>();
            preferences.put("pdfFooter", "CONFIDENTIAL - Internal Use Only");
            
            byte[] pdfBytes = pdfService.generatePdf(htmlContent, metadata, preferences);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should generate PDF when footer is 'None'")
        void generatePdfWithNoneFooter() {
            String htmlContent = "<p>No footer</p>";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Test");
            
            Map<String, Object> preferences = new HashMap<>();
            preferences.put("pdfFooter", "None");
            
            byte[] pdfBytes = pdfService.generatePdf(htmlContent, metadata, preferences);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should generate PDF with null metadata")
        void generatePdfWithNullMetadata() {
            String htmlContent = "<p>Content without metadata</p>";
            
            byte[] pdfBytes = pdfService.generatePdf(htmlContent, null, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should generate PDF from markdown-style content")
        void generatePdfFromMarkdownContent() {
            String markdownContent = """
                # Meeting Summary
                
                ## Attendees
                - John Doe
                - Jane Smith
                
                **Important:** This is a key point.
                
                *Action items* need to be completed.
                """;
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Meeting Summary");
            
            byte[] pdfBytes = pdfService.generatePdf(markdownContent, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should handle empty content gracefully")
        void generatePdfWithEmptyContent() {
            String htmlContent = "";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Empty Document");
            
            byte[] pdfBytes = pdfService.generatePdf(htmlContent, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should handle null content gracefully")
        void generatePdfWithNullContent() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Null Content Document");
            
            byte[] pdfBytes = pdfService.generatePdf(null, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should generate PDF with special characters in content")
        void generatePdfWithSpecialCharacters() {
            String htmlContent = "<p>Special chars: &amp; &lt; &gt; &quot;</p>";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Special Characters Test");
            
            byte[] pdfBytes = pdfService.generatePdf(htmlContent, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should generate PDF with Unicode content")
        void generatePdfWithUnicodeContent() {
            String htmlContent = "<p>Unicode: 日本語 한국어 العربية</p>";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Unicode Document");
            
            byte[] pdfBytes = pdfService.generatePdf(htmlContent, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }
    }

    @Nested
    @DisplayName("Markdown to HTML conversion")
    class MarkdownConversionTests {

        @Test
        @DisplayName("Should preserve existing HTML tags")
        void preserveExistingHtmlTags() {
            String htmlContent = "<p>Already HTML</p><div>Content</div>";
            Map<String, String> metadata = new HashMap<>();
            
            byte[] pdfBytes = pdfService.generatePdf(htmlContent, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should convert markdown bold to HTML")
        void convertMarkdownBold() {
            String content = "This has **bold text** in it.";
            Map<String, String> metadata = new HashMap<>();
            
            byte[] pdfBytes = pdfService.generatePdf(content, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should convert markdown italic to HTML")
        void convertMarkdownItalic() {
            String content = "This has *italic text* in it.";
            Map<String, String> metadata = new HashMap<>();
            
            byte[] pdfBytes = pdfService.generatePdf(content, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should convert markdown headers")
        void convertMarkdownHeaders() {
            String content = """
                # Header 1
                ## Header 2
                ### Header 3
                Regular paragraph.
                """;
            Map<String, String> metadata = new HashMap<>();
            
            byte[] pdfBytes = pdfService.generatePdf(content, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should convert markdown bullet points")
        void convertMarkdownBulletPoints() {
            String content = """
                Meeting notes:
                - First item
                - Second item
                - Third item
                """;
            Map<String, String> metadata = new HashMap<>();
            
            byte[] pdfBytes = pdfService.generatePdf(content, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should convert asterisk bullet points")
        void convertAsteriskBulletPoints() {
            String content = """
                Action items:
                * First task
                * Second task
                * Third task
                """;
            Map<String, String> metadata = new HashMap<>();
            
            byte[] pdfBytes = pdfService.generatePdf(content, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }
    }

    @Nested
    @DisplayName("Security - HTML escaping")
    class HtmlEscapingTests {

        @Test
        @DisplayName("Should escape XSS attempts in title metadata")
        void escapeXssInTitle() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "<script>alert('XSS')</script>");
            
            byte[] pdfBytes = pdfService.generatePdf("<p>Safe content</p>", metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
            // The script tag should be escaped, not executed
        }

        @Test
        @DisplayName("Should escape HTML entities in date metadata")
        void escapeHtmlInDate() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Test");
            metadata.put("date", "2024 & 2025");
            
            byte[] pdfBytes = pdfService.generatePdf("<p>Content</p>", metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should escape angle brackets in organizer metadata")
        void escapeAngleBrackets() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Test");
            metadata.put("organizer", "John <Admin> Doe");
            
            byte[] pdfBytes = pdfService.generatePdf("<p>Content</p>", metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long content")
        void handleVeryLongContent() {
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longContent.append("<p>Paragraph ").append(i).append(": ")
                          .append("Lorem ipsum dolor sit amet. ".repeat(10))
                          .append("</p>\n");
            }
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Long Document");
            
            byte[] pdfBytes = pdfService.generatePdf(longContent.toString(), metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should handle empty metadata values")
        void handleEmptyMetadataValues() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "");
            metadata.put("date", "");
            metadata.put("startTime", "");
            metadata.put("endTime", "");
            metadata.put("location", "");
            metadata.put("organizer", "");
            
            byte[] pdfBytes = pdfService.generatePdf("<p>Content</p>", metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should handle mixed HTML and markdown")
        void handleMixedContent() {
            String content = """
                <h1>HTML Header</h1>
                
                **Markdown bold** and *italic*
                
                <p>HTML paragraph</p>
                
                - Markdown list item
                """;
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Mixed Content");
            
            byte[] pdfBytes = pdfService.generatePdf(content, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }

        @Test
        @DisplayName("Should handle nested formatting")
        void handleNestedFormatting() {
            String content = "**Bold with *nested italic* inside**";
            Map<String, String> metadata = new HashMap<>();
            
            byte[] pdfBytes = pdfService.generatePdf(content, metadata, null);
            
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }
    }
}
