package com.postservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postservice.config.RabbitMqConfig;
import com.postservice.dto.MediaStatusDTO;
import com.postservice.dto.MediaUploadedEvent;
import com.postservice.dto.UploadUrlRequest;
import com.postservice.dto.UploadUrlResponse;
import com.postservice.entity.MediaEntity;
import com.postservice.repo.MediaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class MediaService {

    @Autowired
    private S3Presigner presigner;
    @Autowired
    private  MediaRepository repository;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitMqConfig rabbitMqConfig;

    @Value("${minio.processed-bucket}")
    private String processedBucket;

    public UploadUrlResponse createPendingUpload(String userId, UploadUrlRequest req) {
        String mediaId = UUID.randomUUID().toString();
        String key = "raw/%s/%s.%s".formatted(userId, mediaId, req.extension());

        var putRequest = PutObjectRequest.builder()
                .bucket("media-raw")
                .key(key)
                .contentType(req.contentType())
                .build();

        var presigned = presigner.presignPutObject(b -> b
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putRequest));

        MediaEntity entity = new MediaEntity();
        entity.setId(mediaId);
        entity.setUserId(userId);
        entity.setMediaType(req.mediaType());
        entity.setStatus("pending");
        entity.setStorageKey(key);
        entity.setCreatedAt(Instant.now());
        repository.save(entity);

        return new UploadUrlResponse(mediaId, presigned.url().toString());
    }


    // MediaService.java — add this method
    public void handleUploadComplete(String mediaId, String userId) {
        MediaEntity entity = repository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

        entity.setStatus("processing");
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);

        rabbitTemplate.convertAndSend(
                rabbitMqConfig.mediaExchange,
                rabbitMqConfig.mediaRoutingKey,
                new MediaUploadedEvent(mediaId, entity.getStorageKey(), entity.getMediaType())
        );
    }

    public MediaEntity getMediaById(String media){
        return repository.findById(media).orElseThrow();
    }

    //Status of the upload media,
    public MediaStatusDTO getMediaProcessingStatus(String mediaId){
        Optional<MediaEntity> mediaEntity = repository.findById(mediaId);
        if(mediaEntity.isPresent()){
            return new MediaStatusDTO(mediaId, mediaEntity.get().getStatus() );
        }
        throw new NoSuchElementException("Media not present");
    }

    public String generateDownloadUrl(String storageKey) {
        var getRequest = GetObjectRequest.builder()
                .bucket(processedBucket)
                .key(storageKey)
                .build();

        var presigned = presigner.presignGetObject(b -> b
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest));

        return presigned.url().toString();
    }

    // call this when building PostResponse
    public Map<String, String> resolveUrls(Map<String, String> rawKeys) {
        Map<String, String> resolved = new HashMap<>();
        rawKeys.forEach((quality, key) ->
                resolved.put(quality, generateDownloadUrl(key)));
        return resolved;
    }

    public Map<String, String> getUrlsByMediaId(String mediaId) {
        MediaEntity entity = repository.findById(mediaId)
                .orElseThrow(() -> new NoSuchElementException("Media not found: " + mediaId));
        
        if (entity.getProcessedKeysJson() == null || entity.getProcessedKeysJson().isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> rawKeys = mapper.readValue(
                entity.getProcessedKeysJson(), 
                new TypeReference<Map<String, String>>() {}
            );
            log.info("Json: "+entity.getProcessedKeysJson());
            return resolveUrls(rawKeys);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse processed keys", e);
        }
    }
}