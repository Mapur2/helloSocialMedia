import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

export const useGetPosts = () => {
  return useQuery({
    queryKey: ['posts'],
    queryFn: async () => {
      const response = await axios.get('http://localhost:8079/api/posts');
      return response.data.data || [];
    }
  });
};

export const useGetRecommendations = () => {
  return useQuery({
    queryKey: ['recommendations'],
    queryFn: async () => {
      try {
        const response = await axios.get('http://localhost:8079/api/recommendations');
        const recommendedPosts = response.data.data || [];
        if (recommendedPosts.length > 0) {
          return recommendedPosts;
        }
      } catch (err) {
        console.warn('Failed to fetch recommendations, falling back to regular feed:', err);
      }
      // Graceful fallback to regular posts if recommendations are empty
      const fallbackResponse = await axios.get('http://localhost:8079/api/posts');
      return fallbackResponse.data.data || [];
    }
  });
};

export const useCreatePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ content, visibility = 'PUBLIC', mediaFiles = [], onProgress }) => {
      let mediaIds = [];
      let completedCount = 0;
      const totalFiles = mediaFiles?.length || 0;

      if (mediaFiles && mediaFiles.length > 0) {
        if (onProgress) onProgress(`Preparing to upload ${totalFiles} file(s)...`);
        
        const uploadPromises = Array.from(mediaFiles).map(async (mediaFile) => {
          // Step 1: Get presigned upload URL
          const isVideo = mediaFile.type.startsWith('video/');
          const mediaType = isVideo ? 'video' : 'image';
          
          if (onProgress) onProgress(`Requesting upload URL for ${mediaFile.name}...`);
          
          const uploadUrlResponse = await axios.post('http://localhost:8079/api/media/upload-url', {
            filename: mediaFile.name,
            contentType: mediaFile.type,
            mediaType: mediaType
          });
          
          const uploadData = uploadUrlResponse.data.data || uploadUrlResponse.data;
          const uploadUrl = uploadData.uploadUrl;
          const mediaId = uploadData.mediaId;

          // Step 2: Upload media directly to S3 via presigned URL
          if (onProgress) onProgress(`Uploading ${mediaFile.name}...`);
          const uploadResult = await fetch(uploadUrl, {
            method: 'PUT',
            headers: {
              'Content-Type': mediaFile.type
            },
            body: mediaFile
          });
          
          if (!uploadResult.ok) {
              throw new Error(`Failed to upload to S3: ${uploadResult.statusText}`);
          }

          // Step 3: Mark upload as complete to trigger processing
          if (onProgress) onProgress(`Processing ${mediaFile.name}...`);
          await axios.post(`http://localhost:8079/api/media/${mediaId}/complete`);
          
          // Step 3.5: Poll status until ready
          let isReady = false;
          while (!isReady) {
            const statusRes = await axios.get(`http://localhost:8079/api/media/${mediaId}/status`);
            const currentStatus = statusRes.data?.data?.status?.toLowerCase();
            
            if (currentStatus === 'ready') {
              isReady = true;
              completedCount++;
              if (onProgress) onProgress(`Processed ${completedCount}/${totalFiles} file(s)...`);
            } else if (currentStatus === 'failed' || currentStatus === 'error') {
              throw new Error(`Media processing failed for ${mediaFile.name}`);
            } else {
              await new Promise(resolve => setTimeout(resolve, 2000));
            }
          }
          
          return mediaId;
        });

        mediaIds = await Promise.all(uploadPromises);
      }

      if (onProgress) onProgress('Publishing post...');
      // Step 4: Create the post
      const postPayload = {
        content,
        visibility,
        ...(mediaIds.length > 0 && { mediaIds }) // Include array of mediaIds
      };

      const response = await axios.post('http://localhost:8079/api/posts', postPayload);
      return response.data.data;
    },
    onSuccess: () => {
      // Invalidate and refetch both feeds
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    }
  });
};

export const useReactToPost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ postId, action }) => {
      const response = await axios.post(`http://localhost:8079/api/post/interact/like/${postId}`);
      return response.data;
    },
    onSuccess: () => {
      // Refetch posts to get updated reaction counts
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    }
  });
};

export const useGetLikes = (postId, enabled = false) => {
  return useQuery({
    queryKey: ['likes', postId],
    queryFn: async () => {
      const response = await axios.get(`http://localhost:8079/api/post/interact/like/${postId}`);
      return response.data.data || [];
    },
    enabled: !!postId && enabled
  });
};

export const useGetUserPosts = (userId) => {
  return useQuery({
    queryKey: ['posts', 'user', userId],
    queryFn: async () => {
      const response = await axios.get(`http://localhost:8079/api/posts/user?user=${userId}`);
      return response.data.data || [];
    },
    enabled: !!userId
  });
};

export const useDeletePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (postId) => {
      const response = await axios.delete(`http://localhost:8079/api/posts/${postId}`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    }
  });
};
