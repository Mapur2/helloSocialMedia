package com.postservice.controller;

import com.postservice.dto.MediaStatusDTO;
import com.postservice.dto.Response;
import com.postservice.dto.UploadUrlRequest;
import com.postservice.dto.UploadUrlResponse;
import com.postservice.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.validation.Valid;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    @PostMapping("/upload-url")
    public UploadUrlResponse getUploadUrl(@Valid @RequestBody UploadUrlRequest req,
                                          @RequestHeader("X-User-Id") String userId) {
        return mediaService.createPendingUpload(userId, req);
    }

    // MediaController.java — add this method
    @PostMapping("/{mediaId}/complete")
    public ResponseEntity<String> markComplete(@PathVariable String mediaId,
                                               @RequestHeader("X-User-Id") String userId) {
        mediaService.handleUploadComplete(mediaId, userId);
        return ResponseEntity.ok("Processing started");
    }

    @GetMapping("/{mediaId}/status")
    public ResponseEntity<?> getMediaProcessingStatus(@PathVariable String mediaId){
        try{
            MediaStatusDTO status = mediaService.getMediaProcessingStatus(mediaId);
            return new ResponseEntity<>(new Response(true,"Successfully retrived", status), HttpStatus.OK);
        }
        catch (NoSuchElementException e){
            return new ResponseEntity<>(new Response(false, "Could not find media", null), HttpStatus.BAD_REQUEST);
        }
        catch (Exception e){
            return new ResponseEntity<>(new Response(false, "Something went wrong", null),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{mediaId}/urls")
    public ResponseEntity<?> getMediaUrls(@PathVariable String mediaId) {
        try {
            java.util.Map<String, String> urls = mediaService.getUrlsByMediaId(mediaId);
            return new ResponseEntity<>(new Response(true, "Successfully retrieved urls", urls), HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(new Response(false, "Could not find media", null), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response(false, "Something went wrong", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{mediaId}/status/events")
    public ResponseEntity<?> getMediaProcessingStatusEvents(@PathVariable String mediaId){
        try{
            // Verify media exists first
            mediaService.getMediaProcessingStatus(mediaId);

            SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout

            new Thread(() -> {
                try {
                    boolean isComplete = false;
                    while (!isComplete) {
                        MediaStatusDTO status = mediaService.getMediaProcessingStatus(mediaId);
                        emitter.send(SseEmitter.event().name("status").data(status));

                        if ("ready".equals(status.status()) || "failed".equals(status.status())) {
                            isComplete = true;
                            emitter.complete();
                        } else {
                            Thread.sleep(2000);
                        }
                    }
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }).start();

            return ResponseEntity.ok(emitter);
        }
        catch (NoSuchElementException e){
            return new ResponseEntity<>(new Response(false, "Could not find media", null), HttpStatus.BAD_REQUEST);
        }
        catch (Exception e){
            return new ResponseEntity<>(new Response(false, "Something went wrong", null),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}