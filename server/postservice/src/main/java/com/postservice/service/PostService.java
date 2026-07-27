package com.postservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postservice.dto.CommentMessage;
import com.postservice.dto.PostResponseDTO;
import com.postservice.entity.MediaEntity;
import com.postservice.entity.Post;
import com.postservice.repo.MediaRepository;
import com.postservice.repo.PostRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PostService {

    @Autowired
    private PostRepo postRepo;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.postsinteractionservice.url}")
    private String interactionServiceUrl;

    // ─── Save Post ───────────────────────────────────────────────────────────

    public Post savePost(Post post, String mediaId) {
        if (mediaId != null && !mediaId.isEmpty()) {
            mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));
            post.setMediaId(mediaId);
        }
        postRepo.save(post);
        return post;
    }

    // ─── Get Posts of a User ─────────────────────────────────────────────────

    public List<PostResponseDTO> getPostsOfUser(String userId) {
        List<Post> posts = postRepo.findPostByUserId(userId);
        return mapPostsToDTOs(posts, Map.of());  // no liked status needed for own posts
    }

    // ─── Get Single Post ─────────────────────────────────────────────────────

    public PostResponseDTO getPost(String postId) {
        Post post = postRepo.findPostById(postId);
        if (post == null) return null;
        return mapPostsToDTOs(Collections.singletonList(post), Map.of()).get(0);
    }

    // ─── Get All Posts (feed) with liked status ───────────────────────────────

    public List<PostResponseDTO> getPosts(String userId) {
        List<Post> posts = postRepo.findAll();
        if (posts.isEmpty()) return List.of();

        List<String> postIds = posts.stream()
                .map(Post::getId)
                .toList();

        Map<String, Boolean> likedMap = fetchLikedStatus(userId, postIds);

        return mapPostsToDTOs(posts, likedMap);  // ← pass full list, not individual posts
    }

    // ─── Update Like Count ───────────────────────────────────────────────────

    public int updateLikeCount(String postId, String action) throws Exception {
        Post post = postRepo.findPostById(postId);
        if (post == null) throw new Exception("Post not found: " + postId);

        if (action.equalsIgnoreCase("like")) {
            post.setLikeCount(post.getLikeCount() + 1);
        } else if (action.equalsIgnoreCase("dislike")) {
            post.setLikeCount(post.getLikeCount() - 1);
        } else {
            throw new Exception("Invalid action: " + action);
        }

        postRepo.save(post);
        return post.getLikeCount();
    }

    // ─── Update Comment Count ────────────────────────────────────────────────

    public String updateCommentCount(CommentMessage message) {
        log.info("Received activity: {}", message);

        Post post = postRepo.findPostById(message.getPostId());
        if (post == null) {
            log.error("No such post: {}", message);
            return "No such post";
        }

        if (message.getType().equalsIgnoreCase("INCREASE")) {
            post.setCommentCount(post.getCommentCount() + 1);
        } else if (message.getType().equalsIgnoreCase("DECREASE")) {
            post.setCommentCount(post.getCommentCount() - 1);
        }

        postRepo.save(post);
        return "Comment count: " + post.getCommentCount();
    }

    // ─── Private: Batch fetch liked status from interactionService ────────────

    private Map<String, Boolean> fetchLikedStatus(String userId, List<String> postIds) {
        if (userId == null || userId.isBlank() || postIds.isEmpty()) return Map.of();

        try {
            String url = interactionServiceUrl + "/api/interactions/liked-by/" + userId + "/batch";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, postIds, Map.class);
            if (response.getBody() != null) {
                Map<String, Boolean> result = new HashMap<>();
                response.getBody().forEach((k, v) ->
                        result.put((String) k, (Boolean) v));
                return result;
            }
        } catch (Exception e) {
            log.error("Could not fetch liked status from interaction service: {}", e.getMessage());
        }

        return Map.of();
    }

    // ─── Private: Map posts to DTOs ──────────────────────────────────────────

    private List<PostResponseDTO> mapPostsToDTOs(List<Post> posts, Map<String, Boolean> likedMap) {
        // batch fetch all media in one query — no N+1
        List<String> mediaIds = posts.stream()
                .map(Post::getMediaId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .toList();

        Map<String, MediaEntity> mediaMap = new HashMap<>();
        if (!mediaIds.isEmpty()) {
            mediaRepository.findAllById(mediaIds)
                    .forEach(m -> mediaMap.put(m.getId(), m));
        }

        return posts.stream()
                .map(post -> {
                    Map<String, String> mediaUrls = new HashMap<>();

                    if (post.getMediaId() != null && !post.getMediaId().isEmpty()) {
                        MediaEntity media = mediaMap.get(post.getMediaId());
                        if (media != null && media.getStatus().equalsIgnoreCase("ready")) {
                            if (media.getProcessedKeysJson() != null
                                    && !media.getProcessedKeysJson().isEmpty()) {
                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    Map<String, String> rawKeys = mapper.readValue(
                                            media.getProcessedKeysJson(),
                                            new TypeReference<HashMap<String, String>>() {}
                                    );
                                    mediaUrls = mediaService.resolveUrls(rawKeys);
                                } catch (Exception ex) {
                                    log.error("Failed to parse processedKeysJson for media: {}",
                                            media.getId(), ex);
                                }
                            }
                            mediaUrls.put("type", media.getMediaType());
                        }
                    }

                    return new PostResponseDTO(
                            post.getId(),
                            post.getContent(),
                            mediaUrls,
                            post.getVisibility(),
                            post.getLikeCount(),
                            post.getCommentCount(),
                            post.getShareCount(),
                            post.getCreatedAt(),
                            post.getUpdatedAt(),
                            post.getIsDeleted(),
                            likedMap.getOrDefault(post.getId(), false)
                    );
                })
                .toList();
    }
}