import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

export const useGetFollowers = (userId) => {
  return useQuery({
    queryKey: ['followers', userId],
    queryFn: async () => {
      const url = userId 
        ? `http://localhost:8079/api/followers?user=${userId}`
        : 'http://localhost:8079/api/followers';
      const response = await axios.get(url);
      return response.data.data?.followers || [];
    }
  });
};

export const useGetFollowing = (userId) => {
  return useQuery({
    queryKey: ['following', userId],
    queryFn: async () => {
      const url = userId 
        ? `http://localhost:8079/api/followers/following?user=${userId}`
        : 'http://localhost:8079/api/followers/following';
      const response = await axios.get(url);
      return response.data.data?.followers || []; // Backend uses 'followers' key even for the following list
    }
  });
};

export const useCheckFollowStatus = (targetUserId) => {
  return useQuery({
    queryKey: ['isFollowing', targetUserId],
    queryFn: async () => {
      const response = await axios.get(`http://localhost:8079/api/followers/is-following?targetUserId=${targetUserId}`);
      return response.data.data;
    },
    enabled: !!targetUserId
  });
};

export const useFollowUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (followUserId) => {
      const response = await axios.post('http://localhost:8079/api/followers', { followUserId });
      return response.data;
    },
    onMutate: async (followUserId) => {
      // Cancel any outgoing refetches so they don't overwrite our optimistic update
      await queryClient.cancelQueries({ queryKey: ['isFollowing', followUserId] });
      const previousStatus = queryClient.getQueryData(['isFollowing', followUserId]);
      // Optimistically update to true
      queryClient.setQueryData(['isFollowing', followUserId], true);
      return { previousStatus, followUserId };
    },
    onError: (err, newFollow, context) => {
      // Rollback
      queryClient.setQueryData(['isFollowing', context.followUserId], context.previousStatus);
    },
    onSettled: (data, error, variables) => {
      queryClient.invalidateQueries({ queryKey: ['isFollowing', variables] });
      queryClient.invalidateQueries({ queryKey: ['followers'] });
      queryClient.invalidateQueries({ queryKey: ['following'] });
    }
  });
};

export const useUnfollowUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (followUserId) => {
      const response = await axios.delete('http://localhost:8079/api/followers', { 
        data: { followUserId } 
      });
      return response.data;
    },
    onMutate: async (followUserId) => {
      await queryClient.cancelQueries({ queryKey: ['isFollowing', followUserId] });
      const previousStatus = queryClient.getQueryData(['isFollowing', followUserId]);
      // Optimistically update to false
      queryClient.setQueryData(['isFollowing', followUserId], false);
      return { previousStatus, followUserId };
    },
    onError: (err, newUnfollow, context) => {
      // Rollback
      queryClient.setQueryData(['isFollowing', context.followUserId], context.previousStatus);
    },
    onSettled: (data, error, variables) => {
      queryClient.invalidateQueries({ queryKey: ['isFollowing', variables] });
      queryClient.invalidateQueries({ queryKey: ['followers'] });
      queryClient.invalidateQueries({ queryKey: ['following'] });
    }
  });
};

export const useGetComments = (postId, enabled = true) => {
  return useQuery({
    queryKey: ['comments', postId],
    queryFn: async () => {
      const response = await axios.get(`http://localhost:8079/api/post/interact/comments/${postId}`);
      return response.data.data || [];
    },
    enabled: !!postId && enabled // Only run query if a valid postId is provided and enabled is true
  });
};

export const useAddComment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ postId, text, commentorId }) => {
      const response = await axios.post('http://localhost:8079/api/post/interact/comments', { postId, text, commentorId });
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

export const useGetUserProfile = (username) => {
  return useQuery({
    queryKey: ['userProfile', username],
    queryFn: async () => {
      const response = await axios.get(`http://localhost:8079/api/users/username/${username}`);
      return response.data.data;
    },
    enabled: !!username
  });
};
