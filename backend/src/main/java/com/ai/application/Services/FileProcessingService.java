package com.ai.application.Services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service for extracting text content from various file formats.
 * Supports: .txt, .md, .pdf, .docx
 */
@Service
public class FileProcessingService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Extract text content from an uploaded file.
     * 
     * @param file The uploaded file
     * @return Extracted text content
     * @throws IOException              If file reading fails
     * @throws IllegalArgumentException If file type is unsupported or too large
     */
    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum size of 10 MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name is null");
        }

        String extension = getFileExtension(filename).toLowerCase();

        return switch (extension) {
            case "txt", "md" -> extractFromText(file);
            case "pdf" -> extractFromPdf(file);
            case "docx" -> extractFromDocx(file);
            default -> throw new IllegalArgumentException(
                    "Unsupported file type: " + extension + ". Supported: .txt, .md, .pdf, .docx");
        };
    }

    /**
     * Extract text from plain text files (.txt, .md)
     */
    private String extractFromText(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Extract text from PDF files using Apache PDFBox
     */
    private String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return cleanText(text);
        }
    }

    /**
     * Extract text from DOCX files using Apache POI
     */
    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
                XWPFDocument document = new XWPFDocument(is);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            return cleanText(text);
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * Clean extracted text (remove excessive whitespace, normalize line breaks)
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        // Normalize line breaks and remove excessive whitespace
        return text
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .replaceAll("[ \t]+", " ")
                .trim();
    }
}
