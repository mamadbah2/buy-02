package sn.dev.media_service.services.impl;

import java.io.IOException;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import sn.dev.media_service.exceptions.CloudStorageException;
import sn.dev.media_service.services.CloudStorageService;

@RequiredArgsConstructor
@Service
public class CloudStorageServiceImpl implements CloudStorageService {

    @Value(value = "${supabase.project-url}")
    public String projectUrl;

    @Value(value = "${supabase.api-key}")
    public String apiKey;

    @Value(value = "${supabase.bucket-name}")
    public String bucketName;

    private final RestTemplate restTemplate;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CloudStorageException("File must not be null or empty");
        }

        // Generate a unique file name using UUID and the sanitized original file name
        String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "_" + sanitizedFileName;

        // Create headers with the API key and content type
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new CloudStorageException("Missing content type on uploaded file");
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.valueOf(contentType);
        } catch (IllegalArgumentException ex) {
            throw new CloudStorageException("Unsupported content type: " + contentType, ex);
        }

        headers.setContentType(mediaType);

        // Construct the upload URL
        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", projectUrl, bucketName, fileName);

        // Create the request entity with the file bytes and headers
        HttpEntity<byte[]> requestEntity;
        try {
            requestEntity = new HttpEntity<>(file.getBytes(), headers);
        } catch (IOException ioe) {
            throw new CloudStorageException("Failed to read uploaded file bytes", ioe);
        }

        // Send the PUT request to upload the file
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class);
        } catch (RestClientResponseException rcre) {
            String responseBody = rcre.getResponseBodyAsString();
            int statusCode = rcre.getStatusCode().value();
            throw new CloudStorageException(
                    String.format("Supabase upload failed: HTTP %d - %s", statusCode, responseBody), rcre);
        }

        if (response.getStatusCode().is2xxSuccessful()) {
            return String.format("%s/storage/v1/object/public/%s/%s", projectUrl, bucketName, fileName);
        } else {
            throw new CloudStorageException("Failed to upload file: " + response.getStatusCode());
        }
    }

    /**
     * Sanitizes a filename by removing special characters, emojis, and spaces
     * that are not allowed in Supabase Storage keys
     */
    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "file";
        }

        // Remove file extension first
        String nameWithoutExtension = originalFileName;
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            nameWithoutExtension = originalFileName.substring(0, lastDotIndex);
            extension = originalFileName.substring(lastDotIndex);
        }

        // Remove emojis and special characters, keep only alphanumeric, dots, hyphens, and underscores
        String sanitized = nameWithoutExtension.replaceAll("[^a-zA-Z0-9._-]", "");

        // Remove multiple consecutive dots, hyphens, or underscores
        sanitized = sanitized.replaceAll("[._-]+", "_");

        // Remove leading/trailing dots, hyphens, or underscores (group alternatives explicitly)
        sanitized = sanitized.replaceAll("(^[._-]+)|([._-]+$)", "");

        // If sanitized name is empty, use "file"
        if (sanitized.isEmpty()) {
            sanitized = "file";
        }

        // Limit length to 50 characters (excluding extension)
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        return sanitized + extension;
    }
}
