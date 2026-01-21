package com.ai.application.Controllers;

import com.ai.application.Services.PdfService;
import com.ai.application.Services.GridFsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller for PDF generation and preview.
 */
@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfService pdfService;
    private final GridFsService gridFsService;

    @Autowired
    public PdfController(PdfService pdfService, GridFsService gridFsService) {
        this.pdfService = pdfService;
        this.gridFsService = gridFsService;
    }

    /**
     * Generates a PDF preview from HTML content.
     * 
     * Request body:
     * {
     * "content": "
     * <p>
     * HTML content...
     * </p>
     * ",
     * "title": "Optional title",
     * "date": "2024-01-15" // Optional, defaults to today
     * }
     * 
     * @return PDF as binary data
     */
    @PostMapping("/preview")
    public ResponseEntity<?> previewPdf(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        String title = body.getOrDefault("title", "Document");
        String dateStr = body.get("date");

        if (content == null || content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Content is required"));
        }

        try {
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

            byte[] pdfBytes = pdfService.generateMeetingMinutesPdf(title, date, content);
            String filename = pdfService.getMeetingMinutesFilename(date);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to generate PDF: " + e.getMessage()));
        }
    }

    /**
     * Generates and stores a PDF, returning the file ID.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateAndStorePdf(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        String title = body.getOrDefault("title", "Document");
        String dateStr = body.get("date");

        if (content == null || content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Content is required"));
        }

        try {
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

            byte[] pdfBytes = pdfService.generateMeetingMinutesPdf(title, date, content);
            String filename = pdfService.getMeetingMinutesFilename(date);

            String fileId = gridFsService.storePdf(pdfBytes, filename);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "fileId", fileId,
                    "filename", filename));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to generate PDF: " + e.getMessage()));
        }
    }

    /**
     * Retrieves a stored PDF by ID.
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<?> getPdf(@PathVariable String fileId) {
        try {
            byte[] pdfBytes = gridFsService.getFile(fileId);

            if (pdfBytes == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to retrieve PDF: " + e.getMessage()));
        }
    }
}
