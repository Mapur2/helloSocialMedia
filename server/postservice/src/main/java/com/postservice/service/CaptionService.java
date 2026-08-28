package com.postservice.service;

import com.postservice.entity.MediaCaption;
import com.postservice.repo.MediaCaptionRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class CaptionService {

    @Autowired
    private MediaCaptionRepository mediaCaptionRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.caption-service.url:http://192.168.0.9:5000/caption-url}")
    private String captionServiceUrl;

    @Data
    public static class CaptionRequest {
        private String url;

        public CaptionRequest(String url) {
            this.url = url;
        }
    }

    @Data
    public static class CaptionResponse {
        private String description;
        private String url;
    }

    @Async
    public void generateCaptionForImage(String mediaId, String imageUrl) {
        log.info("[AutoCaption] Starting auto-caption generation for mediaId: {}", mediaId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            CaptionRequest requestPayload = new CaptionRequest(imageUrl);
            HttpEntity<CaptionRequest> entity = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<CaptionResponse> response = restTemplate.postForEntity(
                    captionServiceUrl,
                    entity,
                    CaptionResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                CaptionResponse body = response.getBody();
                String description = body.getDescription();

                log.info("[AutoCaption] Received caption for mediaId {}: {}", mediaId, description);

                MediaCaption captionEntity = MediaCaption.builder()
                        .mediaId(mediaId)
                        .description(description)
                        .imageUrl(imageUrl)
                        .build();

                mediaCaptionRepository.save(captionEntity);
                log.info("[AutoCaption] Saved caption to media_captions table for mediaId: {}", mediaId);
            } else {
                log.warn("[AutoCaption] Caption service returned non-200 or empty body for mediaId {}: {}",
                        mediaId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("[AutoCaption] Error generating auto-caption for mediaId {}: {}", mediaId, e.getMessage(), e);
        }
    }

    public Optional<MediaCaption> getCaptionByMediaId(String mediaId) {
        return mediaCaptionRepository.findByMediaId(mediaId);
    }
}
