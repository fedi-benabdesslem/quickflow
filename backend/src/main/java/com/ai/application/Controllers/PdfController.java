package com.ai.application.Controllers;

import com.ai.application.Services.GridFsService;
import com.ai.application.Services.PdfGenerationService;
import com.ai.application.model.PdfGenerationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private static final Logger logger = LoggerFactory.getLogger(PdfController.class);

    private final PdfGenerationService pdfGenerationService;
    private final GridFsService gridFsService;

    @Autowired
    public PdfController(PdfGenerationService pdfGenerationService, GridFsService gridFsService) {
        this.pdfGenerationService = pdfGenerationService;
        this.gridFsService = gridFsService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generatePdf(@RequestBody PdfGenerationRequest request) {
        try {
            // Determine which content to use: prefer raw markdown (preserves tables)
            String contentForPdf = request.getMarkdownContent();
            boolean isMarkdown = contentForPdf != null && !contentForPdf.isEmpty();

            if (!isMarkdown) {
                contentForPdf = request.getHtmlContent();
            }

            if (contentForPdf == null || contentForPdf.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
            }

            // Generate PDF - pass isMarkdown flag so service knows to run commonmark
            byte[] pdfBytes = pdfGenerationService.generatePdf(
                    contentForPdf,
                    request.getMeetingMetadata(),
                    request.getOutputPreferences(),
                    isMarkdown);

            // Generate filename
            String title = request.getMeetingMetadata().getOrDefault("title", "Meeting_Minutes");
            String date = request.getMeetingMetadata().getOrDefault("date", "Unknown_Date");
            String filename = "Meeting_Minutes_" + date + "_" + title.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";

            // Store in GridFS
            String fileId = gridFsService.storePdf(pdfBytes, filename);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "fileId", fileId,
                    "filename", filename,
                    "message", "PDF generated successfully"));

        } catch (Exception e) {
            logger.error("Error generating PDF", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "PDF generation failed"));
        }
    }

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<byte[]> previewPdf(@PathVariable String fileId) {
        return getPdfResponse(fileId, "inline");
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String fileId) {
        return getPdfResponse(fileId, "attachment");
    }

    private ResponseEntity<byte[]> getPdfResponse(String fileId, String disposition) {
        byte[] content = gridFsService.getFile(fileId);

        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"meeting_minutes.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
