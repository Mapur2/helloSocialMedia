import React, { useState, useRef } from 'react';
import { useCreatePost } from '../../hooks/usePosts';
import { PhotoIcon, GlobeAltIcon, LockClosedIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { useAuth } from '../../contexts/AuthContext';

const CreatePost = () => {
  const [content, setContent] = useState('');
  const [visibility, setVisibility] = useState('PUBLIC');
  const [mediaFiles, setMediaFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [isExpanded, setIsExpanded] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [uploadStatus, setUploadStatus] = useState('');
  const { mutateAsync: createPost, isPending: loading } = useCreatePost();
  const { user } = useAuth();
  
  const fileInputRef = useRef(null);

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const files = Array.from(e.dataTransfer.files);
    handleFiles(files);
  };

  const handleFileChange = (e) => {
    const files = Array.from(e.target.files);
    handleFiles(files);
  };

  const handleFiles = (files) => {
    const validFiles = files.filter(f => f.type.startsWith('image/') || f.type.startsWith('video/'));
    if (validFiles.length > 0) {
      setMediaFiles((prev) => [...prev, ...validFiles]);
      validFiles.forEach((file) => {
        const reader = new FileReader();
        reader.onload = (e) => {
          setPreviews((prev) => [...prev, { url: e.target.result, type: file.type }]);
        };
        reader.readAsDataURL(file);
      });
      setIsExpanded(true);
    }
  };

  const removeMedia = (index) => {
    setMediaFiles((prev) => prev.filter((_, i) => i !== index));
    setPreviews((prev) => prev.filter((_, i) => i !== index));
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!content.trim() && mediaFiles.length === 0) return;

    try {
      setUploadStatus('Starting...');
      await createPost({ 
        content, 
        visibility, 
        mediaFiles, 
        onProgress: (status) => setUploadStatus(status) 
      });
      setContent('');
      setMediaFiles([]);
      setPreviews([]);
      setIsExpanded(false);
      setUploadStatus('');
    } catch (error) {
      console.error("Failed to create post:", error);
      setUploadStatus('');
    }
  };

  const userNameFirstLetter = user?.name ? user.name.charAt(0).toUpperCase() : 'U';

  return (
    <div 
      className={`relative bg-white/90 backdrop-blur-xl rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.06)] border ${isDragging ? 'border-indigo-400 ring-4 ring-indigo-50' : 'border-gray-100 hover:shadow-[0_8px_40px_rgb(0,0,0,0.1)]'} ${uploadStatus ? 'pt-14 p-5' : 'p-5'} mb-8 transition-all duration-300 ease-in-out`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {uploadStatus && (
        <div className="absolute top-0 left-0 right-0 bg-indigo-50/80 backdrop-blur-sm border-b border-indigo-100/60 px-5 py-2.5 rounded-t-2xl flex items-center justify-between text-xs font-semibold text-indigo-700 animate-fade-in z-40">
          <div className="flex items-center space-x-2">
            <div className="w-3.5 h-3.5 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
            <span>{uploadStatus}</span>
          </div>
          <div className="w-24 bg-indigo-100 rounded-full h-1.5 overflow-hidden">
            <div className="bg-gradient-to-r from-blue-500 to-indigo-600 h-full animate-pulse" style={{ width: '100%' }}></div>
          </div>
        </div>
      )}
      {isDragging && (
        <div className="absolute inset-0 z-50 bg-indigo-50/90 backdrop-blur-sm rounded-2xl flex flex-col items-center justify-center border-2 border-dashed border-indigo-400">
          <PhotoIcon className="w-16 h-16 text-indigo-500 mb-4 animate-bounce" />
          <h3 className="text-xl font-bold text-indigo-900">Drop media here</h3>
          <p className="text-indigo-600/80 mt-1">Upload images or videos directly to your post</p>
        </div>
      )}
      <form onSubmit={handleSubmit}>
        <div className="flex items-start space-x-4">
          <div className="flex-shrink-0">
            <div className="w-12 h-12 bg-gradient-to-tr from-blue-500 via-indigo-500 to-purple-500 rounded-full flex items-center justify-center text-white text-lg font-bold shadow-inner">
              {userNameFirstLetter}
            </div>
          </div>
          
          <div className="flex-1 min-w-0 w-full relative">
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              onFocus={() => setIsExpanded(true)}
              placeholder="What's happening right now?"
              className={`w-full bg-gray-50/50 hover:bg-gray-50 focus:bg-white rounded-xl border-none resize-none transition-all duration-300 text-gray-800 placeholder-gray-400 focus:ring-2 focus:ring-blue-100 focus:shadow-sm ${isExpanded ? 'p-4 min-h-[100px]' : 'p-3 min-h-[50px] leading-6'}`}
            />
            
            {previews.length > 0 && (
              <div className="mt-4 grid grid-cols-2 sm:grid-cols-3 gap-3">
                {previews.map((previewObj, index) => (
                  <div key={index} className="relative group rounded-2xl overflow-hidden shadow-sm border border-gray-100 aspect-square">
                    {previewObj.type.startsWith('video/') ? (
                      <video 
                        src={previewObj.url} 
                        className="w-full h-full object-cover bg-black"
                      />
                    ) : (
                      <img 
                        src={previewObj.url} 
                        alt={`Preview ${index}`} 
                        className="w-full h-full object-cover"
                      />
                    )}
                    <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-200"></div>
                    <button
                      type="button"
                      onClick={() => removeMedia(index)}
                      className="absolute top-2 right-2 bg-white/20 backdrop-blur-md text-white rounded-full p-1.5 opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-red-500 hover:scale-110"
                    >
                      <XMarkIcon className="w-5 h-5" />
                    </button>
                  </div>
                ))}
                
                {/* Add More Tile */}
                <label className="rounded-2xl border-2 border-dashed border-gray-200 hover:border-indigo-400 bg-gray-50/50 hover:bg-indigo-50/50 aspect-square flex flex-col items-center justify-center cursor-pointer transition-all duration-200 group">
                  <div className="w-10 h-10 bg-white rounded-full shadow-sm flex items-center justify-center mb-1 group-hover:scale-110 transition-transform">
                    <PhotoIcon className="w-5 h-5 text-indigo-500" />
                  </div>
                  <span className="text-xs font-medium text-gray-500 group-hover:text-indigo-600">Add More</span>
                  <input 
                    type="file" 
                    accept="image/*,video/*" 
                    multiple 
                    onChange={handleFileChange} 
                    className="hidden" 
                  />
                </label>
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
                  multiple
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
                  setMediaFiles([]);
                  setPreviews([]);
                  if (fileInputRef.current) fileInputRef.current.value = '';
                }}
                className="px-5 py-2 text-sm font-medium text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-full transition-all duration-200"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={(!content.trim() && mediaFiles.length === 0) || loading}
                className="px-6 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-sm font-medium rounded-full hover:from-blue-700 hover:to-indigo-700 shadow-md hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:shadow-md transform transition-all duration-200 hover:-translate-y-0.5 active:translate-y-0"
              >
                {loading ? (
                  <div className="flex items-center justify-center space-x-2">
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>{uploadStatus || 'Posting...'}</span>
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