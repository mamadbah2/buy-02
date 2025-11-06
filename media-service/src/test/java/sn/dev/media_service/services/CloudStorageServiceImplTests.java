package sn.dev.media_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;
import sn.dev.media_service.services.impl.CloudStorageServiceImpl;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CloudStorageServiceImplTests {

    private CloudStorageServiceImpl cloudStorageService;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        cloudStorageService = new CloudStorageServiceImpl(restTemplate);

        // inject valeurs @Value manuellement
        cloudStorageService.projectUrl = "http://localhost:8000";
        cloudStorageService.apiKey = "dummy-key";
        cloudStorageService.bucketName = "test-bucket";

        // mock RestTemplate
//        restTemplate = Mockito.mock(RestTemplate.class);
//        cloudStorageService.restTemplate = restTemplate;
    }

    @Test
    void upload_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        ResponseEntity<String> response = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        String url = cloudStorageService.upload(file);

        assertTrue(url.contains("http://localhost:8000/storage/v1/object/public/test-bucket/"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void upload_failure_statusCodeNot2xx() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fail.png", "image/png", new byte[]{1, 2, 3}
        );

        ResponseEntity<String> response = new ResponseEntity<>("ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> cloudStorageService.upload(file));
        assertTrue(ex.getMessage().contains("Failed to upload file"));
    }

    @Test
    void sanitizeFileName_normalName() {
        String result = invokeSanitize("photo.png");
        assertEquals("photo.png", result);
    }

    @Test
    void sanitizeFileName_withSpecialChars() {
        String result = invokeSanitize("p@h#o!t$o%.png");
        assertEquals("photo.png", result);
    }

    @Test
    void sanitizeFileName_withEmojis() {
        String result = invokeSanitize("😎holiday.jpg");
        assertEquals("holiday.jpg", result);
    }

    @Test
    void sanitizeFileName_emptyName() {
        String result = invokeSanitize("");
        assertEquals("file", result);
    }

    @Test
    void sanitizeFileName_nullName() {
        String result = invokeSanitize(null);
        assertEquals("file", result);
    }

    @Test
    void sanitizeFileName_tooLong() {
        String longName = "a".repeat(100) + ".jpg";
        String result = invokeSanitize(longName);
        assertTrue(result.startsWith("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")); // 50 chars
        assertTrue(result.endsWith(".jpg"));
    }

    // Helper pour tester la méthode privée via réflexion
    private String invokeSanitize(String name) {
        try {
            var method = CloudStorageServiceImpl.class.getDeclaredMethod("sanitizeFileName", String.class);
            method.setAccessible(true);
            return (String) method.invoke(cloudStorageService, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
