import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useGetPosts } from '../hooks/usePosts';
import { useGetFollowers } from '../hooks/useSocial';
import PostCard from '../components/posts/PostCard';
import { 
  UserIcon, 
  CalendarIcon, 
  EnvelopeIcon,
  UsersIcon,
  DocumentTextIcon
} from '@heroicons/react/24/outline';

const Profile = () => {
  const { user } = useAuth();
  const { data: userPosts = [], isLoading: loading } = useGetPosts();
  const { data: userFollowers = [] } = useGetFollowers();
  const [activeTab, setActiveTab] = useState('posts');

  const stats = [
    { 
      name: 'Posts', 
      value: userPosts.length, 
      icon: DocumentTextIcon,
      key: 'posts'
    },
    { 
      name: 'Followers', 
      value: userFollowers.length, 
      icon: UsersIcon,
      key: 'followers'
    }
  ];

  return (
    <div className="max-w-4xl mx-auto p-4">
      {/* Profile Header */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden mb-6">
        {/* Cover Photo */}
        <div className="h-32 bg-gradient-to-r from-blue-500 via-purple-600 to-pink-500"></div>
        
        {/* Profile Info */}
        <div className="relative px-6 pb-6">
          {/* Profile Picture */}
          <div className="absolute -top-16 left-6">
            <div className="w-32 h-32 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full border-4 border-white flex items-center justify-center text-white text-4xl font-bold shadow-lg">
              {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
            </div>
          </div>
          
          {/* User Details */}
          <div className="pt-20">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between">
              <div>
                <h1 className="text-3xl font-bold text-gray-900 mb-2">
                  {user?.firstName} {user?.lastName}
                </h1>
                <p className="text-lg text-gray-600 mb-1">@{user?.userName}</p>
                
                <div className="flex items-center text-gray-500 mb-4">
                  <EnvelopeIcon className="h-4 w-4 mr-2" />
                  <span className="text-sm">{user?.email}</span>
                </div>
                
                <div className="flex items-center text-gray-500">
                  <CalendarIcon className="h-4 w-4 mr-2" />
                  <span className="text-sm">
                    Joined {new Date(user?.createdAt || Date.now()).toLocaleDateString('en-US', { 
                      month: 'long', 
                      year: 'numeric' 
                    })}
                  </span>
                </div>
              </div>
              
              <div className="mt-4 md:mt-0">
                <button className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors">
                  Edit Profile
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4 mb-6">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <button
              key={stat.key}
              onClick={() => setActiveTab(stat.key)}
              className={`bg-white rounded-lg shadow-md p-6 text-center transition-colors ${
                activeTab === stat.key 
                  ? 'ring-2 ring-blue-500 bg-blue-50' 
                  : 'hover:bg-gray-50'
              }`}
            >
              <Icon className="h-8 w-8 mx-auto mb-2 text-gray-600" />
              <div className="text-2xl font-bold text-gray-900">{stat.value}</div>
              <div className="text-sm text-gray-600">{stat.name}</div>
            </button>
          );
        })}
      </div>

      {/* Content Tabs */}
      <div className="bg-white rounded-lg shadow-md">
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8 px-6">
            {stats.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === tab.key
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {tab.name}
              </button>
            ))}
          </nav>
        </div>

        <div className="p-6">
          {activeTab === 'posts' && (
            <div>
              {loading ? (
                <div className="flex justify-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                </div>
              ) : userPosts.length === 0 ? (
                <div className="text-center py-8">
                  <DocumentTextIcon className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">No posts yet</h3>
                  <p className="text-gray-500">Start sharing your thoughts with the world!</p>
                </div>
              ) : (
                <div className="space-y-6">
                  {userPosts.map((post) => (
                    <PostCard key={post.id} post={post} />
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'followers' && (
            <div>
              {userFollowers.length === 0 ? (
                <div className="text-center py-8">
                  <UsersIcon className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">No followers yet</h3>
                  <p className="text-gray-500">Connect with others to build your network!</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {userFollowers.map((follower) => (
                    <div key={follower.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                      <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-gradient-to-r from-green-500 to-blue-600 rounded-full flex items-center justify-center text-white font-medium">
                          {follower.firstName?.charAt(0)}{follower.lastName?.charAt(0)}
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">
                            {follower.firstName} {follower.lastName}
                          </p>
                          <p className="text-sm text-gray-500">@{follower.userName}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Profile;