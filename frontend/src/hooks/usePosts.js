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

export const useCreatePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ content, visibility = 'PUBLIC', mediaFile = null }) => {
      let mediaId = null;

      if (mediaFile) {
        // Step 1: Get presigned upload URL
        const isVideo = mediaFile.type.startsWith('video/');
        const mediaType = isVideo ? 'video' : 'image';
        
        const uploadUrlResponse = await axios.post('http://localhost:8079/api/media/upload-url', {
          filename: mediaFile.name,
          contentType: mediaFile.type,
          mediaType: mediaType
        });
        
        const uploadData = uploadUrlResponse.data.data || uploadUrlResponse.data;
        const uploadUrl = uploadData.uploadUrl;
        mediaId = uploadData.mediaId;

        // Step 2: Upload media directly to S3 via presigned URL
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
        await axios.post(`http://localhost:8079/api/media/${mediaId}/complete`);
      }

      // Step 4: Create the post
      const postPayload = {
        content,
        visibility,
        ...(mediaId && { mediaId }) // Only include mediaId if it exists
      };

      const response = await axios.post('http://localhost:8079/api/posts', postPayload);
      return response.data.data;
    },
    onSuccess: () => {
      // Invalidate and refetch the posts query so the new post appears immediately
      queryClient.invalidateQueries({ queryKey: ['posts'] });
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
