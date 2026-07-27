package com.postservice.service;

import com.postservice.config.RabbitMqConfig;
import com.postservice.dto.MediaUploadedEvent;
import com.postservice.entity.MediaEntity;
import com.postservice.repo.MediaRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;

@Service
public class MediaProcessingWorker {

    @Autowired private S3Client s3Client;
    @Autowired private MediaRepository repository;

    @Value("${minio.raw-bucket}")      private String rawBucket;
    @Value("${minio.processed-bucket}") private String processedBucket;

    @RabbitListener(queues = "media.queue")
    public void handleMediaUploaded(MediaUploadedEvent event) {
        System.out.println("Processing media: " + event.mediaId());

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("media-" + event.mediaId());

            // 1. Download raw file from MinIO
            Path rawFile = workDir.resolve("original." + getExtension(event.storageKey()));
            s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(rawBucket)
                            .key(event.storageKey())
                            .build(),
                    rawFile
            );

            String processedKeysJson;

            if ("video".equals(event.mediaType())) {
                processedKeysJson = processVideo(event.mediaId(), rawFile, workDir);
            } else {
                processedKeysJson = processImage(event.mediaId(), rawFile, workDir);
            }

            // 4. Update DB — status ready
            MediaEntity entity = repository.findById(event.mediaId())
                    .orElseThrow();
            entity.setStatus("ready");
            entity.setProcessedKeysJson(processedKeysJson);
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);

            System.out.println("Done processing: " + event.mediaId());

        } catch (Exception e) {
            System.err.println("Processing failed for " + event.mediaId() + ": " + e.getMessage());
            repository.findById(event.mediaId()).ifPresent(entity -> {
                entity.setStatus("failed");
                entity.setUpdatedAt(Instant.now());
                repository.save(entity);
            });
        } finally {
            // cleanup temp files
            if (workDir != null) deleteDirectory(workDir);
        }
    }

    private String processVideo(String mediaId, Path rawFile, Path workDir)
            throws IOException, InterruptedException {

        // Transcode to 720p
        Path output720p = workDir.resolve("720p.mp4");
        runFfmpeg(List.of(
                "ffmpeg", "-i", rawFile.toString(),
                "-vf", "scale=1280:720",
                "-c:v", "libx264", "-preset", "fast", "-b:v", "2500k",
                "-c:a", "aac", "-b:a", "128k",
                "-movflags", "+faststart",
                output720p.toString()
        ));

        // Extract thumbnail at 1 second
        Path thumbnail = workDir.resolve("thumbnail.jpg");
        runFfmpeg(List.of(
                "ffmpeg", "-i", rawFile.toString(),
                "-ss", "00:00:01",
                "-frames:v", "1",
                "-update", "1",
                thumbnail.toString()
        ));

        // Upload both to processed bucket
        String key720p     = "processed/%s/720p.mp4".formatted(mediaId);
        String keyThumb    = "processed/%s/thumbnail.jpg".formatted(mediaId);

        uploadToStorage(output720p, key720p, "video/mp4");
        uploadToStorage(thumbnail, keyThumb, "image/jpeg");

        return """
            {"720p":"%s","thumbnail":"%s"}
            """.formatted(key720p, keyThumb).trim();
    }

    private String processImage(String mediaId, Path rawFile, Path workDir)
            throws IOException, InterruptedException {

        // Resize to max 1080px wide, keep aspect ratio
        Path resized = workDir.resolve("resized.jpg");
        runFfmpeg(List.of(
                "ffmpeg", "-i", rawFile.toString(),
                "-vf", "scale=1080:-1",
                resized.toString()
        ));

        String keyResized = "processed/%s/resized.jpg".formatted(mediaId);
        uploadToStorage(resized, keyResized, "image/jpeg");

        return """
            {"resized":"%s"}
            """.formatted(keyResized).trim();
    }

    private void runFfmpeg(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        // drain output so process doesn't block
        new Thread(() -> {
            try { process.getInputStream().transferTo(System.out); }
            catch (IOException ignored) {}
        }).start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
        }
    }

    private void uploadToStorage(Path file, String key, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(processedBucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromFile(file)
        );
    }

    private String getExtension(String key) {
        return key.contains(".")
                ? key.substring(key.lastIndexOf('.') + 1)
                : "tmp";
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}