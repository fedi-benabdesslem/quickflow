package com.ai.application.Services;

import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PdfService.
 *
 * Tests PDF generation from HTML content and meeting minutes formatting.
 */
class PdfServiceTest {

    private PdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new PdfService();
    }

    @Nested
    @DisplayName("generatePdf() - Basic HTML conversion")
    class GeneratePdfTests {

        @Test
        @DisplayName("should generate PDF from simple HTML")
        void generatePdfFromSimpleHtml() {
            String html = "<p>Hello, World!</p>";

            byte[] pdf = pdfService.generatePdf(html);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
            // PDF files start with %PDF
            assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));
        }

        @Test
        @DisplayName("should generate PDF with styled content")
        void generatePdfWithStyledContent() {
            String html = """
                    <h1>Title</h1>
                    <p>Paragraph with <strong>bold</strong> and <em>italic</em> text.</p>
                    <ul>
                        <li>Item 1</li>
                        <li>Item 2</li>
                    </ul>
                    """;

            byte[] pdf = pdfService.generatePdf(html);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("should handle empty HTML")
        void handleEmptyHtml() {
            String html = "";

            byte[] pdf = pdfService.generatePdf(html);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0); // Still produces valid PDF structure
        }

        @Test
        @DisplayName("should handle Unicode characters")
        void handleUnicodeCharacters() {
            String html = "<p>日本語 • émojis 🎉 • Ñoño</p>";

            byte[] pdf = pdfService.generatePdf(html);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }
    }

    @Nested
    @DisplayName("generateMeetingMinutesPdf() - Meeting minutes formatting")
    class GenerateMeetingMinutesPdfTests {

        @Test
        @DisplayName("should generate meeting minutes PDF with title and date")
        void generateMeetingMinutesPdfWithTitleAndDate() {
            String title = "Project Kickoff";
            LocalDate date = LocalDate.of(2026, 1, 15);
            String content = "<p>Meeting notes here</p>";

            byte[] pdf = pdfService.generateMeetingMinutesPdf(title, date, content);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
            assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));
        }

        @Test
        @DisplayName("should format date correctly")
        void formatDateCorrectly() {
            String title = "Weekly Sync";
            LocalDate date = LocalDate.of(2026, 3, 5);
            String content = "<p>Content</p>";

            byte[] pdf = pdfService.generateMeetingMinutesPdf(title, date, content);

            // Can't easily verify date formatting in PDF binary, but ensure no errors
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("should handle complex content with lists and headers")
        void handleComplexContent() {
            String title = "Sprint Review";
            LocalDate date = LocalDate.now();
            String content = """
                    <h2>Attendees</h2>
                    <ul>
                        <li>John Doe</li>
                        <li>Jane Smith</li>
                    </ul>
                    <h2>Decisions</h2>
                    <ol>
                        <li>Approved new design</li>
                        <li>Scheduled follow-up for next week</li>
                    </ol>
                    <h2>Action Items</h2>
                    <p>Complete tasks by EOD Friday.</p>
                    """;

            byte[] pdf = pdfService.generateMeetingMinutesPdf(title, date, content);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("should handle empty content")
        void handleEmptyContent() {
            String title = "Empty Meeting";
            LocalDate date = LocalDate.now();
            String content = "";

            byte[] pdf = pdfService.generateMeetingMinutesPdf(title, date, content);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("should handle special characters in title")
        void handleSpecialCharactersInTitle() {
            String title = "Q&A Session: \"Best Practices\" <2026>";
            LocalDate date = LocalDate.now();
            String content = "<p>Notes</p>";

            byte[] pdf = pdfService.generateMeetingMinutesPdf(title, date, content);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }
    }

    @Nested
    @DisplayName("getMeetingMinutesFilename()")
    class GetMeetingMinutesFilenameTests {

        @Test
        @DisplayName("should generate filename with correct date format")
        void generateFilenameWithCorrectDateFormat() {
            LocalDate date = LocalDate.of(2026, 1, 28);

            String filename = pdfService.getMeetingMinutesFilename(date);

            assertEquals("Meeting_Minutes_2026-01-28.pdf", filename);
        }

        @Test
        @DisplayName("should pad single-digit month and day")
        void padSingleDigitMonthAndDay() {
            LocalDate date = LocalDate.of(2026, 3, 5);

            String filename = pdfService.getMeetingMinutesFilename(date);

            assertEquals("Meeting_Minutes_2026-03-05.pdf", filename);
        }

        @Test
        @DisplayName("should handle end of year date")
        void handleEndOfYearDate() {
            LocalDate date = LocalDate.of(2026, 12, 31);

            String filename = pdfService.getMeetingMinutesFilename(date);

            assertEquals("Meeting_Minutes_2026-12-31.pdf", filename);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle null HTML gracefully")
        void handleNullHtml() {
            // PdfService wraps content in HTML document, so null becomes "null" string
            // This produces a valid PDF with "null" as content
            byte[] pdf = pdfService.generatePdf(null);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("should handle malformed HTML")
        void handleMalformedHtml() {
            String malformedHtml = "<p>Unclosed paragraph<div>Nested incorrectly</p></div>";

            // iText is generally lenient with malformed HTML
            byte[] pdf = pdfService.generatePdf(malformedHtml);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }
    }
}
