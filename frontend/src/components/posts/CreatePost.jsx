import React, { useState, useRef } from 'react';
import { useCreatePost } from '../../hooks/usePosts';
import { PhotoIcon, GlobeAltIcon, LockClosedIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { useAuth } from '../../contexts/AuthContext';

const CreatePost = () => {
  const [content, setContent] = useState('');
  const [visibility, setVisibility] = useState('PUBLIC');
  const [mediaFile, setMediaFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [isExpanded, setIsExpanded] = useState(false);
  const { mutateAsync: createPost, isPending: loading } = useCreatePost();
  const { user } = useAuth();
  
  const fileInputRef = useRef(null);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setMediaFile(file);
      const reader = new FileReader();
      reader.onload = (e) => setPreview(e.target.result);
      reader.readAsDataURL(file);
      setIsExpanded(true);
    }
  };

  const removeMedia = () => {
    setMediaFile(null);
    setPreview(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!content.trim() && !mediaFile) return;

    try {
      await createPost({ content, visibility, mediaFile });
      setContent('');
      setMediaFile(null);
      setPreview(null);
      setIsExpanded(false);
    } catch (error) {
      console.error("Failed to create post:", error);
    }
  };

  const isVideo = mediaFile?.type?.startsWith('video/');
  const userNameFirstLetter = user?.name ? user.name.charAt(0).toUpperCase() : 'U';

  return (
    <div className="bg-white/90 backdrop-blur-xl rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.06)] border border-gray-100 p-5 mb-8 transition-all duration-300 ease-in-out hover:shadow-[0_8px_40px_rgb(0,0,0,0.1)]">
      <form onSubmit={handleSubmit}>
        <div className="flex items-start space-x-4">
          <div className="flex-shrink-0">
            <div className="w-12 h-12 bg-gradient-to-tr from-blue-500 via-indigo-500 to-purple-500 rounded-full flex items-center justify-center text-white text-lg font-bold shadow-inner">
              {userNameFirstLetter}
            </div>
          </div>
          
          <div className="flex-1 w-full relative">
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              onFocus={() => setIsExpanded(true)}
              placeholder="What's happening right now?"
              className={`w-full bg-gray-50/50 hover:bg-gray-50 focus:bg-white rounded-xl border-none resize-none transition-all duration-300 text-gray-800 placeholder-gray-400 focus:ring-2 focus:ring-blue-100 focus:shadow-sm ${isExpanded ? 'p-4 min-h-[100px]' : 'p-3 min-h-[50px] leading-6'}`}
            />
            
            {preview && (
              <div className="mt-4 relative group rounded-xl overflow-hidden shadow-sm border border-gray-100">
                {isVideo ? (
                  <video 
                    src={preview} 
                    controls 
                    className="w-full max-h-96 object-contain bg-black"
                  />
                ) : (
                  <img 
                    src={preview} 
                    alt="Preview" 
                    className="w-full max-h-96 object-cover"
                  />
                )}
                <button
                  type="button"
                  onClick={removeMedia}
                  className="absolute top-3 right-3 bg-black/60 backdrop-blur-md text-white rounded-full p-1.5 opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-black/80 hover:scale-110"
                >
                  <XMarkIcon className="w-5 h-5" />
                </button>
              </div>
            )}
          </div>
        </div>

        {isExpanded && (
          <div className="mt-4 flex items-center justify-between pt-4 border-t border-gray-100/60 animate-fade-in-down">
            <div className="flex items-center space-x-2">
              <label className="flex items-center space-x-2 text-indigo-600 bg-indigo-50 hover:bg-indigo-100 px-4 py-2 rounded-full cursor-pointer transition-colors duration-200 font-medium text-sm">
                <PhotoIcon className="h-5 w-5" />
                <span>Media</span>
                <input
                  type="file"
                  ref={fileInputRef}
                  accept="image/*,video/*"
                  onChange={handleFileChange}
                  className="hidden"
                />
              </label>
              
              <div className="relative inline-block text-gray-600">
                <select
                  value={visibility}
                  onChange={(e) => setVisibility(e.target.value)}
                  className="appearance-none bg-gray-50 hover:bg-gray-100 border-none rounded-full px-10 py-2 text-sm font-medium focus:ring-2 focus:ring-indigo-100 transition-colors duration-200 cursor-pointer"
                >
                  <option value="PUBLIC">Public</option>
                  <option value="PRIVATE">Private</option>
                </select>
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                  {visibility === 'PUBLIC' ? (
                    <GlobeAltIcon className="h-4 w-4 text-gray-500" />
                  ) : (
                    <LockClosedIcon className="h-4 w-4 text-gray-500" />
                  )}
                </div>
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <button
                type="button"
                onClick={() => {
                  setIsExpanded(false);
                  setContent('');
                  removeMedia();
                }}
                className="px-5 py-2 text-sm font-medium text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-full transition-all duration-200"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={(!content.trim() && !mediaFile) || loading}
                className="px-6 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-sm font-medium rounded-full hover:from-blue-700 hover:to-indigo-700 shadow-md hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:shadow-md transform transition-all duration-200 hover:-translate-y-0.5 active:translate-y-0"
              >
                {loading ? (
                  <div className="flex items-center justify-center space-x-2">
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Posting...</span>
                  </div>
                ) : (
                  'Share'
                )}
              </button>
            </div>
          </div>
        )}
      </form>
    </div>
  );
};

export default CreatePost;