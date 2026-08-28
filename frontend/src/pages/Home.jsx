import React, { useState } from 'react';
import { useGetPosts, useGetRecommendations } from '../hooks/usePosts';
import CreatePost from '../components/posts/CreatePost';
import PostCard from '../components/posts/PostCard';
import { Sparkles, Clock } from 'lucide-react';

const Home = () => {
  const [feedType, setFeedType] = useState('recommendations'); // 'recommendations' | 'latest'

  const recommendationsQuery = useGetRecommendations();
  const latestQuery = useGetPosts();

  const activeQuery = feedType === 'recommendations' ? recommendationsQuery : latestQuery;
  const { data: posts = [], isLoading, isError, error, isFetching } = activeQuery;

  if (isLoading && posts.length === 0) {
    return (
      <div className="max-w-2xl mx-auto p-4">
        <div className="bg-white rounded-lg shadow-md p-6 mb-6 animate-pulse">
          <div className="flex items-center space-x-3 mb-4">
            <div className="w-10 h-10 bg-gray-300 rounded-full"></div>
            <div className="flex-1 space-y-2">
              <div className="h-4 bg-gray-300 rounded w-3/4"></div>
              <div className="h-3 bg-gray-300 rounded w-1/2"></div>
            </div>
          </div>
          <div className="space-y-3">
            <div className="h-4 bg-gray-300 rounded"></div>
            <div className="h-4 bg-gray-300 rounded w-5/6"></div>
            <div className="h-4 bg-gray-300 rounded w-4/6"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-4">
      <CreatePost />
      
      {/* Feed Switcher Tabs */}
      <div className="flex border-b border-gray-200 bg-white rounded-t-lg mb-6 shadow-sm overflow-hidden">
        <button
          onClick={() => setFeedType('recommendations')}
          className={`flex-1 py-3 px-4 text-center font-medium text-sm flex items-center justify-center space-x-2 transition-colors duration-150 ${
            feedType === 'recommendations'
              ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50/50'
              : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
          }`}
        >
          <Sparkles className="w-4 h-4" />
          <span>For You</span>
        </button>
        <button
          onClick={() => setFeedType('latest')}
          className={`flex-1 py-3 px-4 text-center font-medium text-sm flex items-center justify-center space-x-2 transition-colors duration-150 ${
            feedType === 'latest'
              ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50/50'
              : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
          }`}
        >
          <Clock className="w-4 h-4" />
          <span>Latest Posts</span>
        </button>
      </div>

      {isError && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6">
          {error?.message || 'Failed to load posts'}
        </div>
      )}

      <div className="space-y-6">
        {posts.length === 0 ? (
          <div className="bg-white rounded-lg shadow-md p-8 text-center">
            <div className="text-gray-500 mb-4">
              <svg className="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9.5a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">No posts yet</h3>
            <p className="text-gray-500">
              {feedType === 'recommendations'
                ? 'No recommended posts yet. Explore recent posts in the Latest tab!'
                : 'Share your first thought with the world!'}
            </p>
          </div>
        ) : (
          posts.map((post) => (
            <PostCard key={post.id} post={post} />
          ))
        )}
      </div>

      {isFetching && posts.length > 0 && (
        <div className="flex justify-center py-4">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      )}
    </div>
  );
};

export default Home;