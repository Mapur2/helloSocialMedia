import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useReactToPost, useGetLikes, useDeletePost } from '../../hooks/usePosts';
import { useAddComment, useGetComments } from '../../hooks/useSocial';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { useAuth } from '../../contexts/AuthContext';
import { 
  HeartIcon, 
  HandThumbDownIcon,
  ChatBubbleOvalLeftIcon,
  ShareIcon,
  EllipsisHorizontalIcon,
  XMarkIcon,
  ChevronLeftIcon,
  ChevronRightIcon
} from '@heroicons/react/24/outline';
import { HeartIcon as HeartSolidIcon, PaperAirplaneIcon } from '@heroicons/react/24/solid';

// Extract mediaId from a processed S3 URL like .../processed/{mediaId}/resized.jpg?...
const extractMediaIdFromUrl = (url) => {
  if (!url) return null;
  const match = url.match(/\/processed\/([\w-]+)\//);
  return match ? match[1] : null;
};

// Sub-component: fetches and renders caption for a single image
const ImageWithCaption = ({ source }) => {
  const mediaId = extractMediaIdFromUrl(source);

  const { data: captionData } = useQuery({
    queryKey: ['caption', mediaId],
    queryFn: async () => {
      const res = await axios.get(`http://localhost:8079/api/media/${mediaId}/caption`);
      return res.data?.data || res.data;
    },
    enabled: !!mediaId,
    staleTime: Infinity,
  });

  const caption = captionData?.description || captionData?.caption || captionData?.text || (typeof captionData === 'string' ? captionData : null);

  return (
    <div>
      <img
        src={source}
        alt={caption || 'Post media'}
        className="w-full max-h-[500px] object-cover hover:scale-[1.02] transition-transform duration-500"
      />
      {caption && (
        <div className="px-4 py-2 bg-gray-50/80 border-t border-gray-100">
          <p className="text-xs text-gray-500 italic leading-relaxed">
            <span className="font-semibold text-indigo-500 not-italic">AI: </span>
            {caption}
          </p>
        </div>
      )}
    </div>
  );
};

const PostCard = ({ post }) => {
  const [showComments, setShowComments] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [isLiked, setIsLiked] = useState(post?.isLikedByUser || false);
  const [isDisliked, setIsDisliked] = useState(false);
  const [showLikesModal, setShowLikesModal] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const menuRef = useRef(null);
  
  const { user } = useAuth();
  const { mutateAsync: reactToPost } = useReactToPost();
  const { mutateAsync: addComment } = useAddComment();
  const { mutateAsync: deletePost } = useDeletePost();

  const isAuthor = user?.id === post.userId;

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setShowMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleDelete = async () => {
    setShowMenu(false);
    if (window.confirm("Are you sure you want to delete this post?")) {
      try {
        await deletePost(post.id);
      } catch (error) {
        console.error("Failed to delete post:", error);
      }
    }
  };
  const { data: postComments = [], isLoading: isCommentsLoading } = useGetComments(post.id, showComments);
  const { data: likes = [], isLoading: isLikesLoading } = useGetLikes(post.id, showLikesModal);

  useEffect(() => {
    setIsLiked(post?.isLikedByUser || false);
  }, [post?.isLikedByUser]);

  const handleLike = async () => {
    const action = isLiked ? 'unlike' : 'like';
    try {
      await reactToPost({ postId: post.id, action });
      setIsLiked(!isLiked);
      if (isDisliked) setIsDisliked(false);
    } catch (error) {
      console.error("Failed to like post", error);
    }
  };

  const handleDislike = async () => {
    const action = isDisliked ? 'undislike' : 'dislike';
    try {
      await reactToPost({ postId: post.id, action });
      setIsDisliked(!isDisliked);
      if (isLiked) setIsLiked(false);
    } catch (error) {
      console.error("Failed to dislike post", error);
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    try {
      await addComment({ postId: post.id, text: newComment, commentorId: user?.id || "" });
      setNewComment('');
    } catch (error) {
      console.error("Failed to add comment", error);
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInHours = Math.floor((now - date) / (1000 * 60 * 60));
    
    if (diffInHours < 1) return 'Just now';
    if (diffInHours < 24) return `${diffInHours}h ago`;
    return date.toLocaleDateString();
  };

  const mediaList = Array.isArray(post.media) ? post.media : (post.media && Object.keys(post.media).length > 0 ? [post.media] : []);
  const hasMedia = mediaList.length > 0;

  const videoRefs = useRef([]);
  const mediaContainerRef = useRef(null);
  const [currentMediaIndex, setCurrentMediaIndex] = useState(0);

  const handleScroll = (e) => {
    const container = e.target;
    const index = Math.round(container.scrollLeft / container.clientWidth);
    setCurrentMediaIndex(index);
  };

  const scrollTo = (index) => {
    if (mediaContainerRef.current) {
      mediaContainerRef.current.scrollTo({
        left: index * mediaContainerRef.current.clientWidth,
        behavior: 'smooth'
      });
      setCurrentMediaIndex(index);
    }
  };

  useEffect(() => {
    if (!hasMedia) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const playPromise = entry.target.play();
            if (playPromise !== undefined) {
              playPromise.catch((error) => {
                console.log("Autoplay prevented:", error);
              });
            }
          } else {
            entry.target.pause();
          }
        });
      },
      { threshold: 0.6 }
    );

    videoRefs.current.forEach(video => {
      if (video) observer.observe(video);
    });

    return () => {
      videoRefs.current.forEach(video => {
        if (video) observer.unobserve(video);
      });
    };
  }, [hasMedia, mediaList]);

  return (
    <div className="bg-white/95 backdrop-blur-xl rounded-2xl shadow-[0_4px_20px_rgb(0,0,0,0.04)] border border-gray-100 mb-6 overflow-hidden transition-all duration-300 hover:shadow-[0_8px_30px_rgb(0,0,0,0.08)]">
      {/* Post Header */}
      <div className="p-5 flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link to={`/${post.userName}`} className="w-11 h-11 bg-gradient-to-tr from-blue-500 via-indigo-500 to-purple-500 rounded-full flex items-center justify-center text-white font-bold shadow-inner text-lg hover:scale-105 transition-transform cursor-pointer overflow-hidden">
            {post.userProfilePicture ? (
              <img src={post.userProfilePicture} alt={post.userName} className="w-full h-full object-cover" />
            ) : (
              post.userName?.charAt(0).toUpperCase() || 'U'
            )}
          </Link>
          <div>
            <Link to={`/${post.userName}`}>
              <h3 className="font-semibold text-gray-900 text-[15px] hover:underline cursor-pointer">
                {post.userName || 'Unknown User'}
              </h3>
            </Link>
            <p className="text-xs text-gray-500 font-medium">{formatDate(post.createdAt)}</p>
          </div>
        </div>
        <div className="relative" ref={menuRef}>
          <button 
            onClick={() => setShowMenu(!showMenu)}
            className="text-gray-400 hover:text-gray-700 hover:bg-gray-100 p-2 rounded-full transition-colors duration-200"
          >
            <EllipsisHorizontalIcon className="h-5 w-5" />
          </button>
          
          {showMenu && (
            <div className="absolute right-0 mt-1 w-48 bg-white rounded-xl shadow-lg border border-gray-100 py-1 z-35 animate-fade-in">
              {isAuthor ? (
                <button
                  onClick={handleDelete}
                  className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors font-medium"
                >
                  Delete Post
                </button>
              ) : (
                <button
                  onClick={() => {
                    alert("Post reported");
                    setShowMenu(false);
                  }}
                  className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  Report Post
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Post Content */}
      <div className="px-5 pb-4">
        <p className="text-gray-800 leading-relaxed text-[15px] whitespace-pre-wrap">{post.content}</p>
      </div>

      {/* Post Media */}
      {hasMedia && (
        <div className="px-5 pb-4">
          <div className="relative group/media">
            <div 
              ref={mediaContainerRef}
              onScroll={handleScroll}
              className={`rounded-xl overflow-hidden shadow-sm border border-gray-100 bg-gray-50/50 flex ${mediaList.length > 1 ? 'overflow-x-auto snap-x snap-mandatory hide-scrollbar' : ''}`}
            >
              {mediaList.map((mediaItem, idx) => {
                const isVideoItem = mediaItem.type === 'video';
                const source = isVideoItem ? (mediaItem['720p'] || mediaItem.url) : (mediaItem.resized || mediaItem.original || mediaItem.url);
                const poster = mediaItem.thumbnail;
                
                if (!source) return null;

                return (
                  <div key={idx} className="w-full flex-shrink-0 snap-center relative">
                    {isVideoItem ? (
                      <video 
                        ref={el => videoRefs.current[idx] = el}
                        controls
                        poster={poster}
                        muted
                        loop
                        playsInline
                        className="w-full max-h-[500px] object-contain bg-black/5"
                      >
                        <source src={source} type="video/mp4" />
                        Your browser does not support the video tag.
                      </video>
                    ) : (
                      <ImageWithCaption source={source} />
                    )}
                  </div>
                );
              })}
            </div>

            {/* Top-Right Page Indicator Badge */}
            {mediaList.length > 1 && (
              <div className="absolute top-3 right-3 bg-black/60 backdrop-blur-md text-white text-[11px] font-semibold px-2.5 py-1 rounded-full z-10 select-none">
                {currentMediaIndex + 1} / {mediaList.length}
              </div>
            )}

            {/* Left and Right Overlay Arrows */}
            {mediaList.length > 1 && currentMediaIndex > 0 && (
              <button
                onClick={() => scrollTo(currentMediaIndex - 1)}
                className="absolute left-3 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white text-gray-800 rounded-full p-2 shadow-md hover:scale-110 transition-all z-10 opacity-0 group-hover/media:opacity-100"
              >
                <ChevronLeftIcon className="w-5 h-5" />
              </button>
            )}
            {mediaList.length > 1 && currentMediaIndex < mediaList.length - 1 && (
              <button
                onClick={() => scrollTo(currentMediaIndex + 1)}
                className="absolute right-3 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white text-gray-800 rounded-full p-2 shadow-md hover:scale-110 transition-all z-10 opacity-0 group-hover/media:opacity-100"
              >
                <ChevronRightIcon className="w-5 h-5" />
              </button>
            )}
          </div>

          {/* Dot Indicators */}
          {mediaList.length > 1 && (
            <div className="flex justify-center space-x-1.5 mt-3">
              {mediaList.map((_, idx) => (
                <button
                  key={idx}
                  onClick={() => scrollTo(idx)}
                  className={`w-1.5 h-1.5 rounded-full transition-all duration-200 ${
                    idx === currentMediaIndex 
                      ? 'bg-indigo-600 w-3' 
                      : 'bg-gray-300 hover:bg-gray-400'
                  }`}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {/* Post Stats */}
      <div className="px-5 py-3 flex items-center justify-between text-xs font-medium text-gray-500 border-t border-gray-50">
        <div className="flex items-center space-x-1 cursor-pointer hover:text-indigo-600 transition-colors">
          <div className="w-5 h-5 bg-indigo-50 rounded-full flex items-center justify-center">
            <HeartSolidIcon className="w-3 h-3 text-indigo-500" />
          </div>
          <span>{post.likesCount || post.likeCount || 0}</span>
        </div>
        <div className="flex space-x-4">
          <span className="cursor-pointer hover:text-gray-700 hover:underline">{post.commentCount || postComments.length} comments</span>
          <span className="cursor-pointer hover:text-gray-700 hover:underline">{post.shareCount || 0} shares</span>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="px-2 py-1 border-t border-gray-100">
        <div className="flex items-center justify-between p-1">
          <button
            onClick={handleLike}
            onContextMenu={(e) => {
              e.preventDefault();
              setShowLikesModal(true);
            }}
            className={`flex-1 flex items-center justify-center space-x-2 py-2.5 mx-1 rounded-xl transition-all duration-200 ${
              isLiked 
                ? 'text-pink-600 bg-pink-50 font-semibold' 
                : 'text-gray-600 hover:bg-gray-100 font-medium'
            }`}
          >
            {isLiked ? (
              <HeartSolidIcon className="h-5 w-5 drop-shadow-sm scale-110" />
            ) : (
              <HeartIcon className="h-5 w-5" />
            )}
            <span className="text-sm">Like</span>
          </button>

          {/* <button
            onClick={handleDislike}
            className={`flex-1 flex items-center justify-center space-x-2 py-2.5 mx-1 rounded-xl transition-all duration-200 ${
              isDisliked 
                ? 'text-indigo-600 bg-indigo-50 font-semibold' 
                : 'text-gray-600 hover:bg-gray-100 font-medium'
            }`}
          >
            <HandThumbDownIcon className="h-5 w-5" />
            <span className="text-sm">Dislike</span>
          </button> */}

          <button
            onClick={() => setShowComments(!showComments)}
            className="flex-1 flex items-center justify-center space-x-2 py-2.5 mx-1 rounded-xl text-gray-600 hover:bg-gray-100 transition-all duration-200 font-medium"
          >
            <ChatBubbleOvalLeftIcon className="h-5 w-5" />
            <span className="text-sm">Comment</span>
          </button>

          <button className="flex-1 flex items-center justify-center space-x-2 py-2.5 mx-1 rounded-xl text-gray-600 hover:bg-gray-100 transition-all duration-200 font-medium">
            <ShareIcon className="h-5 w-5" />
            <span className="text-sm">Share</span>
          </button>
        </div>
      </div>

      {/* Comments Section */}
      {showComments && (
        <div className="border-t border-gray-100 bg-gray-50/30 animate-fade-in-down">
          {/* Add Comment */}
          <form onSubmit={handleAddComment} className="p-4 border-b border-gray-100">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-gradient-to-tr from-blue-500 to-indigo-500 rounded-full flex items-center justify-center text-white font-bold text-xs shadow-inner flex-shrink-0">
                U
              </div>
              <div className="flex-1 relative">
                <input
                  type="text"
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  placeholder="Write a public comment..."
                  className="w-full pl-4 pr-12 py-2.5 bg-white border border-gray-200 rounded-full text-sm focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition-all shadow-sm"
                />
                <button
                  type="submit"
                  disabled={!newComment.trim()}
                  className="absolute right-1.5 top-1.5 bottom-1.5 p-1.5 bg-indigo-600 text-white rounded-full hover:bg-indigo-700 disabled:opacity-50 disabled:bg-gray-400 transition-colors flex items-center justify-center"
                >
                  <PaperAirplaneIcon className="h-4 w-4" />
                </button>
              </div>
            </div>
          </form>

          {/* Comments List */}
          <div className="max-h-72 overflow-y-auto px-4 py-2 space-y-4 custom-scrollbar">
            {isCommentsLoading ? (
              <div className="flex justify-center py-4">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-indigo-600"></div>
              </div>
            ) : postComments.length === 0 ? (
               <div className="py-8 text-center text-gray-400 flex flex-col items-center justify-center space-y-2">
                 <ChatBubbleOvalLeftIcon className="h-8 w-8 opacity-50" />
                 <p className="text-sm font-medium">No comments yet. Be the first!</p>
               </div>
            ) : (
              postComments.map((comment, index) => {
                const isYou = user?.id === comment.commentorId;
                const displayName = isYou ? 'You' : (comment.commentorUserName || 'Unknown User');
                const initial = displayName.charAt(0).toUpperCase();
                const profileLink = `/${isYou ? user?.userName : comment.commentorUserName}`;

                return (
                  <div key={index} className="flex space-x-3 group">
                    <Link to={profileLink} className="w-8 h-8 bg-gradient-to-br from-emerald-400 to-teal-500 rounded-full flex items-center justify-center text-white font-bold text-xs shadow-inner flex-shrink-0 mt-1 hover:scale-105 transition-transform cursor-pointer">
                      {initial}
                    </Link>
                    <div className="flex-1">
                      <div className="bg-white border border-gray-100 shadow-sm rounded-2xl rounded-tl-none px-4 py-2.5 inline-block max-w-[90%]">
                        <Link to={profileLink}>
                          <p className="font-semibold text-[13px] text-gray-900 mb-0.5 hover:underline cursor-pointer">
                            {displayName}
                          </p>
                        </Link>
                        <p className="text-gray-700 text-sm">{comment.text}</p>
                      </div>
                      <div className="flex items-center space-x-4 mt-1.5 ml-2 text-[11px] font-medium text-gray-400">
                        <span>{comment.createdAt ? formatDate(comment.createdAt) : 'Just now'}</span>
                        <button className="hover:text-gray-600 transition-colors">Reply</button>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* Likes Modal */}
      {showLikesModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden transform transition-all scale-100 opacity-100">
            <div className="flex items-center justify-between p-4 border-b border-gray-100 bg-gray-50/50">
              <h3 className="font-bold text-gray-900 text-lg">Likes</h3>
              <button 
                onClick={() => setShowLikesModal(false)}
                className="text-gray-400 hover:text-gray-700 hover:bg-gray-200 p-1.5 rounded-full transition-colors"
              >
                <XMarkIcon className="w-5 h-5" />
              </button>
            </div>
            <div className="p-2 max-h-[60vh] overflow-y-auto custom-scrollbar bg-white">
              {isLikesLoading ? (
                <div className="flex justify-center py-10">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-pink-500"></div>
                </div>
              ) : likes.length === 0 ? (
                <div className="text-center py-10 text-gray-500">
                  <HeartIcon className="w-12 h-12 mx-auto text-gray-300 mb-2" />
                  <p className="text-sm font-medium">No likes yet.</p>
                </div>
              ) : (
                <div className="space-y-1">
                  {likes.map((like, i) => (
                    <div key={i} className="flex items-center space-x-3 p-3 hover:bg-gray-50 rounded-xl transition-colors cursor-pointer group">
                      <div className="w-10 h-10 bg-gradient-to-tr from-pink-500 to-rose-400 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-inner group-hover:scale-105 transition-transform">
                        {(like.username || 'U').charAt(0).toUpperCase()}
                      </div>
                      <div className="flex-1">
                        <p className="font-semibold text-gray-900 text-[15px]">
                          {like.userId === user?.id ? 'You' : (like.username || 'Unknown User')}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PostCard;