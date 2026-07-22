import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

export const useGetFollowers = () => {
  return useQuery({
    queryKey: ['followers'],
    queryFn: async () => {
      const response = await axios.get('http://localhost:8079/api/followers');
      return response.data.data?.followers || [];
    }
  });
};

export const useFollowUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (followerId) => {
      const response = await axios.post('http://localhost:8079/api/followers/', { followerId });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['followers'] });
    }
  });
};

export const useGetComments = (postId, enabled = true) => {
  return useQuery({
    queryKey: ['comments', postId],
    queryFn: async () => {
      const response = await axios.get(`http://localhost:8079/api/post/interact/comment/all/${postId}`);
      return response.data.data || [];
    },
    enabled: !!postId && enabled // Only run query if a valid postId is provided and enabled is true
  });
};

export const useAddComment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ postId, text }) => {
      const response = await axios.post('http://localhost:8079/api/post/interact/comment', { postId, text });
      return response.data;
    },
    onSuccess: (data, variables) => {
      // Invalidate the specific post's comments cache
      queryClient.invalidateQueries({ queryKey: ['comments', variables.postId] });
      // Also invalidate posts to update the comment count
      queryClient.invalidateQueries({ queryKey: ['posts'] });
    }
  });
};
