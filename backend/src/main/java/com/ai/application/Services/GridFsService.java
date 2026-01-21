package com.ai.application.Services;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service for storing and retrieving files using MongoDB GridFS.
 * 
 * Used for storing PDF files that may exceed the 16MB BSON limit.
 */
@Service
public class GridFsService {

    @Autowired
    private GridFsOperations gridFsOperations;

    /**
     * Stores a file in GridFS.
     * 
     * @param content     File content as byte array
     * @param filename    Name of the file
     * @param contentType MIME type of the file
     * @return The ObjectId of the stored file
     */
    public String storeFile(byte[] content, String filename, String contentType) {
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            ObjectId fileId = gridFsOperations.store(inputStream, filename, contentType);
            return fileId.toHexString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    /**
     * Stores a PDF file in GridFS.
     * 
     * @param pdfBytes PDF content
     * @param filename Name of the PDF file
     * @return The ObjectId of the stored file
     */
    public String storePdf(byte[] pdfBytes, String filename) {
        return storeFile(pdfBytes, filename, "application/pdf");
    }

    /**
     * Retrieves a file from GridFS by ID.
     * 
     * @param fileId The ObjectId string of the file
     * @return The file content as byte array, or null if not found
     */
    public byte[] getFile(String fileId) {
        try {
            GridFSFile file = gridFsOperations.findOne(
                    new Query(Criteria.where("_id").is(new ObjectId(fileId))));

            if (file == null) {
                return null;
            }

            GridFsResource resource = gridFsOperations.getResource(file);
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve file: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a file from GridFS by filename.
     * 
     * @param filename The name of the file
     * @return The file content as byte array, or null if not found
     */
    public byte[] getFileByName(String filename) {
        try {
            GridFSFile file = gridFsOperations.findOne(
                    new Query(Criteria.where("filename").is(filename)));

            if (file == null) {
                return null;
            }

            GridFsResource resource = gridFsOperations.getResource(file);
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve file: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from GridFS.
     * 
     * @param fileId The ObjectId string of the file to delete
     */
    public void deleteFile(String fileId) {
        gridFsOperations.delete(
                new Query(Criteria.where("_id").is(new ObjectId(fileId))));
    }

    /**
     * Checks if a file exists in GridFS.
     * 
     * @param fileId The ObjectId string of the file
     * @return true if file exists
     */
    public boolean fileExists(String fileId) {
        GridFSFile file = gridFsOperations.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        return file != null;
    }
}
