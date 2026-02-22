package com.ai.application.Services;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class PdfGenerationService {

    /**
     * Generates a professional PDF from HTML content.
     * Wraps the content in a complete HTML document with styling.
     */
    public byte[] generatePdf(String htmlContent, Map<String, String> metadata, Map<String, Object> preferences) {
        return generatePdf(htmlContent, metadata, preferences, false);
    }

    /**
     * Generates a professional PDF from content.
     * When isMarkdown is true, the content is raw Markdown and will be converted
     * via commonmark
     * (preserving tables). When false, the content is treated as HTML.
     */
    public byte[] generatePdf(String content, Map<String, String> metadata, Map<String, Object> preferences,
            boolean isMarkdown) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4);

            // Add Footer Event Handler
            String footerText = preferences != null ? (String) preferences.getOrDefault("pdfFooter", "") : "";
            if (footerText != null && !footerText.isEmpty() && !"None".equals(footerText)) {
                pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler(footerText));
            }

            // Build complete HTML document with styling
            String completeHtml = buildHtmlDocument(content, metadata, isMarkdown);

            // Configure converter with font support
            ConverterProperties converterProperties = new ConverterProperties();
            FontProvider fontProvider = new DefaultFontProvider(true, true, true);
            converterProperties.setFontProvider(fontProvider);

            // Convert HTML to PDF
            HtmlConverter.convertToPdf(completeHtml, pdf, converterProperties);

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a complete HTML document with professional styling.
     */
    private String buildHtmlDocument(String bodyContent, Map<String, String> metadata) {
        return buildHtmlDocument(bodyContent, metadata, false);
    }

    /**
     * Builds a complete HTML document with professional styling.
     * When forceMarkdown is true, always runs commonmark conversion.
     */
    private String buildHtmlDocument(String bodyContent, Map<String, String> metadata, boolean forceMarkdown) {
        String title = metadata != null ? metadata.getOrDefault("title", "Meeting Minutes") : "Meeting Minutes";
        String date = metadata != null ? metadata.getOrDefault("date", "") : "";
        String startTime = metadata != null ? metadata.getOrDefault("startTime", "") : "";
        String endTime = metadata != null ? metadata.getOrDefault("endTime", "") : "";
        String location = metadata != null ? metadata.getOrDefault("location", "") : "";
        String organizer = metadata != null ? metadata.getOrDefault("organizer", "") : "";

        // Convert content: if forceMarkdown, always run commonmark; otherwise use
        // heuristic
        String processedContent = forceMarkdown
                ? forceConvertMarkdownToHtml(bodyContent)
                : convertMarkdownToHtml(bodyContent);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <style>\n");
        html.append(
                "    body { font-family: Arial, Helvetica, sans-serif; font-size: 11pt; line-height: 1.6; color: #333; margin: 40px; }\n");
        html.append(
                "    h1 { font-size: 18pt; color: #1a365d; border-bottom: 2px solid #1a365d; padding-bottom: 8px; margin-bottom: 20px; }\n");
        html.append("    h2 { font-size: 14pt; color: #2c5282; margin-top: 20px; margin-bottom: 10px; }\n");
        html.append("    h3 { font-size: 12pt; color: #2d3748; margin-top: 15px; margin-bottom: 8px; }\n");
        html.append("    p { margin: 8px 0; }\n");
        html.append("    ul, ol { margin: 10px 0; padding-left: 25px; }\n");
        html.append("    li { margin: 5px 0; }\n");
        html.append("    strong, b { font-weight: bold; }\n");
        html.append("    em, i { font-style: italic; }\n");
        html.append(
                "    .header { background-color: #f7fafc; padding: 15px; border-radius: 5px; margin-bottom: 25px; }\n");
        html.append("    .header-title { font-size: 20pt; font-weight: bold; color: #1a365d; margin-bottom: 10px; }\n");
        html.append("    .header-meta { font-size: 10pt; color: #4a5568; }\n");
        html.append("    .header-meta span { margin-right: 20px; }\n");
        html.append("    .content { margin-top: 20px; }\n");
        html.append("    .section { margin-bottom: 20px; }\n");
        html.append("    table { width: 100%; border-collapse: collapse; margin: 15px 0; }\n");
        html.append("    th, td { border: 1px solid #e2e8f0; padding: 8px 12px; text-align: left; }\n");
        html.append("    th { background-color: #edf2f7; font-weight: bold; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header with metadata
        html.append("  <div class=\"header\">\n");
        html.append("    <div class=\"header-title\">").append(escapeHtml(title)).append("</div>\n");
        html.append("    <div class=\"header-meta\">\n");
        if (!date.isEmpty()) {
            html.append("      <span><strong>Date:</strong> ").append(escapeHtml(date)).append("</span>\n");
        }
        if (!startTime.isEmpty() || !endTime.isEmpty()) {
            String timeStr = startTime;
            if (!endTime.isEmpty())
                timeStr += " - " + endTime;
            html.append("      <span><strong>Time:</strong> ").append(escapeHtml(timeStr)).append("</span>\n");
        }
        if (!location.isEmpty()) {
            html.append("      <span><strong>Location:</strong> ").append(escapeHtml(location)).append("</span>\n");
        }
        if (!organizer.isEmpty()) {
            html.append("      <span><strong>Chair:</strong> ").append(escapeHtml(organizer)).append("</span>\n");
        }
        html.append("    </div>\n");
        html.append("  </div>\n");

        // Main content
        html.append("  <div class=\"content\">\n");
        html.append(processedContent);
        html.append("  </div>\n");

        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Converts Markdown to HTML using commonmark, but skips if content looks like
     * HTML already.
     */
    private String convertMarkdownToHtml(String content) {
        if (content == null || content.isEmpty()) {
            return "<p>No content provided.</p>";
        }

        // If content already has proper HTML tags, return as-is
        if (content.contains("<p>") || content.contains("<div>") || content.contains("<h1>")) {
            return content;
        }

        return forceConvertMarkdownToHtml(content);
    }

    /**
     * Unconditionally converts Markdown to HTML using the commonmark parser.
     * Always runs the conversion — used when we know the input is raw Markdown.
     * Supports GFM tables.
     */
    private String forceConvertMarkdownToHtml(String content) {
        if (content == null || content.isEmpty()) {
            return "<p>No content provided.</p>";
        }

        // Parse Markdown with GFM tables extension
        org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder()
                .extensions(java.util.List.of(
                        org.commonmark.ext.gfm.tables.TablesExtension.create()))
                .build();

        org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder()
                .extensions(java.util.List.of(
                        org.commonmark.ext.gfm.tables.TablesExtension.create()))
                .build();

        org.commonmark.node.Node document = parser.parse(content);
        return renderer.render(document);
    }

    /**
     * Escapes special HTML characters.
     */
    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // Helper class for adding footer
    private static class FooterHandler implements IEventHandler {
        private final String footerText;

        public FooterHandler(String footerText) {
            this.footerText = footerText;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdfDoc.getPageNumber(page);
            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);

            try {
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

                // Add footer text (confidentiality etc)
                try (Canvas canvas = new Canvas(pdfCanvas, pageSize)) {
                    canvas.showTextAligned(new Paragraph(footerText).setFont(font).setFontSize(9),
                            pageSize.getWidth() / 2,
                            pageSize.getBottom() + 20,
                            TextAlignment.CENTER);
                }

                // Add page number
                try (Canvas canvas = new Canvas(pdfCanvas, pageSize)) {
                    canvas.showTextAligned(new Paragraph("Page " + pageNumber).setFont(font).setFontSize(9),
                            pageSize.getWidth() - 40,
                            pageSize.getBottom() + 20,
                            TextAlignment.RIGHT);
                }

            } catch (IOException e) {
                // Ignore font loading errors
            }
        }
    }
}
