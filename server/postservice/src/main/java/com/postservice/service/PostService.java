package com.postservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postservice.dto.CommentMessage;
import com.postservice.dto.PostResponseDTO;
import com.postservice.dto.UserIds;
import com.postservice.entity.MediaEntity;
import com.postservice.entity.Post;
import com.postservice.entity.Visibility;
import com.postservice.repo.MediaRepository;
import com.postservice.repo.PostRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.management.modelmbean.InvalidTargetObjectTypeException;
import java.util.*;
import java.util.stream.Collectors;

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
    @Value("${services.userservice.url}")
    private String userserviceUrl;

    public Post savePost(Post post, List<String> mediaIds) {
        if (mediaIds != null && !mediaIds.isEmpty()) {
            mediaIds.forEach(id -> {
                mediaRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Media not found: " + id));
            });
            post.setMediaIds(mediaIds);
        }
        postRepo.save(post);
        return post;
    }

    // ─── Get Posts of a User ─────────────────────────────────────────────────

    public List<PostResponseDTO> getPostsOfUser(String userId) {
        List<Post> posts = postRepo.findPostByUserId(userId);
        Map<String, Boolean> likedMap = fetchLikedStatus(userId,
                posts.stream()
                        .map(Post::getId)
                        .toList()  // ← terminates the stream into a List
        );
        Map<String, String> userNameMap = fetchUserNames(Set.of(userId));
        return mapPostsToDTOs(posts, likedMap, userNameMap);
    }

    // ─── Get Single Post ─────────────────────────────────────────────────────

    public PostResponseDTO getPost(String postId) {
        Post post = postRepo.findPostById(postId);
        if (post == null) return null;
        return mapPostsToDTOs(Collections.singletonList(post), Map.of(), Map.of()).get(0);
    }

    // ─── Get All Posts (feed) with liked status ───────────────────────────────

    public List<PostResponseDTO> getPosts(String userId) {
        List<Post> posts = postRepo.findAll();
        if (posts.isEmpty()) return List.of();
        posts = posts.stream().filter(e->!e.getIsDeleted()).toList();
        List<String> postIds = posts.stream()
                .map(Post::getId)
                .toList();
        Set<String> userIds = posts.stream()
                .map(Post::getUserId)
                .collect(Collectors.toSet());
        Map<String, Boolean> likedMap = fetchLikedStatus(userId, postIds);
        Map<String, String> userNameMap = fetchUserNames(userIds);

        return mapPostsToDTOs(posts, likedMap, userNameMap);
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
    private Map<String, String> fetchUserNames(Set<String> userIds){
        if(userIds.isEmpty())
            return Map.of();
        try {
            String url = userserviceUrl+"/api/users/usernames";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new UserIds(userIds.stream().toList()), Map.class);
            if (response.getBody() != null) {

                Map<String, String> result = (Map<String, String>) response.getBody().get("users");
                return result;
            }
        }
        catch (Exception e){
            log.error("Could not fetch username from user service: {}", e.getMessage());
        }
        return Map.of();
    }

    private Map<String, String> fetchProfilePictureMediaIds(Set<String> userIds){
        if(userIds.isEmpty())
            return Map.of();
        try {
            String url = userserviceUrl+"/api/users/profile-pictures";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new UserIds(userIds.stream().toList()), Map.class);
            if (response.getBody() != null) {
                return (Map<String, String>) response.getBody();
            }
        }
        catch (Exception e){
            log.error("Could not fetch profile pictures from user service: {}", e.getMessage());
        }
        return Map.of();
    }

    private Map<String, Boolean> fetchLikedStatus(String userId, List<String> postIds) {
        if (userId == null || userId.isBlank() || postIds.isEmpty()) return Map.of();

        try {
            String url = interactionServiceUrl + "/api/post/interact/liked-by/" + userId + "/batch";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, postIds, Map.class);

            if (response.getBody() != null) {

                Map<String, Boolean> result = (Map<String, Boolean>) response.getBody().get("data");
                return result;
            }
        } catch (Exception e) {
            log.error("Could not fetch liked status from interaction service: {}", e.getMessage());
        }

        return Map.of();
    }

    // ─── Private: Map posts to DTOs ──────────────────────────────────────────

    private List<PostResponseDTO> mapPostsToDTOs(List<Post> posts, Map<String, Boolean> likedMap, Map<String,String> userNameMap) {
        Set<String> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<String, String> profilePicMediaIds = fetchProfilePictureMediaIds(userIds);

        List<String> mediaIds = new ArrayList<>();
        posts.stream()
                .filter(p -> p.getMediaIds() != null)
                .flatMap(p -> p.getMediaIds().stream())
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .forEach(mediaIds::add);
        
        profilePicMediaIds.values().stream()
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .forEach(mediaIds::add);

        Map<String, MediaEntity> mediaMap = new HashMap<>();
        if (!mediaIds.isEmpty()) {
            mediaRepository.findAllById(mediaIds)
                    .forEach(m -> mediaMap.put(m.getId(), m));
        }

        return posts.stream()
                .map(post -> {
                    List<Map<String, String>> mediaUrlsList = new ArrayList<>();

                    if (post.getMediaIds() != null && !post.getMediaIds().isEmpty()) {
                        for (String mId : post.getMediaIds()) {
                            MediaEntity media = mediaMap.get(mId);
                            if (media != null && media.getStatus().equalsIgnoreCase("ready")) {
                                Map<String, String> mediaUrls = new HashMap<>();
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
                                mediaUrlsList.add(mediaUrls);
                            }
                        }
                    }

                    String profilePicUrl = null;
                    String profileMediaId = profilePicMediaIds.get(post.getUserId());
                    if (profileMediaId != null) {
                        MediaEntity media = mediaMap.get(profileMediaId);
                        if (media != null && media.getStatus().equalsIgnoreCase("ready")) {
                            if (media.getProcessedKeysJson() != null && !media.getProcessedKeysJson().isEmpty()) {
                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    Map<String, String> rawKeys = mapper.readValue(
                                            media.getProcessedKeysJson(),
                                            new TypeReference<HashMap<String, String>>() {}
                                    );
                                    Map<String, String> urls = mediaService.resolveUrls(rawKeys);
                                    profilePicUrl = urls.getOrDefault("resized", urls.getOrDefault("original", urls.get("url")));
                                } catch (Exception ex) {
                                    log.error("Failed to parse processedKeysJson for profile media: {}", media.getId(), ex);
                                }
                            }
                        }
                    }

                    return new PostResponseDTO(
                            post.getId(),
                            post.getContent(),
                            post.getUserId(),
                            mediaUrlsList,
                            post.getVisibility(),
                            post.getLikeCount(),
                            post.getCommentCount(),
                            post.getShareCount(),
                            post.getCreatedAt(),
                            post.getUpdatedAt(),
                            post.getIsDeleted(),
                            likedMap.getOrDefault(post.getId(), false),
                            userNameMap.get(post.getUserId()),
                            profilePicUrl
                    );
                })
                .toList();
    }

    public List<Post> getRecommendationCandidates(int limit) {

        // Protect the service from unreasonable values
        limit = Math.min(Math.max(limit, 1), 100);

        Pageable pageable = PageRequest.of(0, limit);

        return postRepo.findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                Visibility.PUBLIC,
                pageable
        );
    }

    public List<PostResponseDTO> getRecommendationCandidatesDTO(String userId, int limit) {
        List<Post> posts = getRecommendationCandidates(limit);
        if (posts.isEmpty()) return List.of();

        Set<String> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        List<String> postIds = posts.stream().map(Post::getId).toList();
        
        Map<String, Boolean> likedMap = fetchLikedStatus(userId, postIds);
        Map<String, String> userNameMap = fetchUserNames(userIds);

        return mapPostsToDTOs(posts, likedMap, userNameMap);
    }



    //---------------------delete post(soft delete)------------------
    public String deletePost(String postId) throws Exception{
        if(postId==null)
            throw new InvalidTargetObjectTypeException("Post id is required");
        Post post = postRepo.findPostById(postId);
        if(post == null)
            throw new InvalidTargetObjectTypeException("Post id "+postId+" does not exist");
        post.setIsDeleted(true);
        postRepo.save(post);
        return "Post is deleted successfully";
    }
}