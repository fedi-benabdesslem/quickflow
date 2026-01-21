package com.ai.application.Services;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating PDFs from HTML content.
 * 
 * Uses iText library to convert rich text HTML to professional PDF format.
 */
@Service
public class PdfService {

    /**
     * Generates a PDF from HTML content.
     * 
     * @param htmlContent The HTML content to convert
     * @return PDF as byte array
     */
    public byte[] generatePdf(String htmlContent) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Wrap in full HTML document with styling
            String fullHtml = wrapInDocument(htmlContent);

            HtmlConverter.convertToPdf(fullHtml, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a meeting minutes PDF with proper formatting.
     * 
     * @param title   Meeting title
     * @param date    Meeting date
     * @param content HTML content of the meeting minutes
     * @return PDF as byte array
     */
    public byte[] generateMeetingMinutesPdf(String title, LocalDate date, String content) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        String html = """
                <html>
                <head>
                    <style>
                        @page {
                            margin: 60px 50px;
                        }
                        body {
                            font-family: 'Helvetica', 'Arial', sans-serif;
                            font-size: 11pt;
                            line-height: 1.6;
                            color: #333;
                        }
                        .header {
                            border-bottom: 2px solid #4a5568;
                            padding-bottom: 15px;
                            margin-bottom: 25px;
                        }
                        .title {
                            font-size: 22pt;
                            font-weight: bold;
                            color: #1a202c;
                            margin: 0 0 10px 0;
                        }
                        .date {
                            font-size: 12pt;
                            color: #718096;
                            margin: 0;
                        }
                        .content {
                            margin-top: 20px;
                        }
                        h1, h2, h3 {
                            color: #2d3748;
                            margin-top: 20px;
                        }
                        h1 { font-size: 16pt; }
                        h2 { font-size: 14pt; }
                        h3 { font-size: 12pt; }
                        ul, ol {
                            margin-left: 20px;
                            padding-left: 10px;
                        }
                        li {
                            margin-bottom: 5px;
                        }
                        p {
                            margin: 10px 0;
                        }
                        .footer {
                            position: fixed;
                            bottom: 0;
                            left: 0;
                            right: 0;
                            text-align: center;
                            font-size: 9pt;
                            color: #a0aec0;
                            padding: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <p class="title">Meeting Minutes</p>
                        <p class="date">%s · %s</p>
                    </div>
                    <div class="content">
                        %s
                    </div>
                </body>
                </html>
                """.formatted(title, formattedDate, content);

        return generatePdfFromHtml(html);
    }

    /**
     * Generates a PDF filename for meeting minutes.
     */
    public String getMeetingMinutesFilename(LocalDate date) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return "Meeting_Minutes_" + formattedDate + ".pdf";
    }

    /**
     * Internal method to generate PDF from complete HTML document.
     */
    private byte[] generatePdfFromHtml(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            HtmlConverter.convertToPdf(html, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Wraps content in a styled HTML document.
     */
    private String wrapInDocument(String content) {
        return """
                <html>
                <head>
                    <style>
                        @page {
                            margin: 50px;
                        }
                        body {
                            font-family: 'Helvetica', 'Arial', sans-serif;
                            font-size: 11pt;
                            line-height: 1.6;
                            color: #333;
                        }
                        h1, h2, h3 {
                            color: #2d3748;
                        }
                        ul, ol {
                            margin-left: 15px;
                            padding-left: 10px;
                        }
                        p {
                            margin: 10px 0;
                        }
                    </style>
                </head>
                <body>
                    %s
                </body>
                </html>
                """.formatted(content);
    }
}
