import React, { useState } from 'react';
import { useGetFollowers, useFollowUser } from '../hooks/useSocial';
import { UserPlusIcon, UsersIcon } from '@heroicons/react/24/outline';

const Followers = () => {
  const { data: followers = [], isLoading: loading, error, isError } = useGetFollowers();
  const { mutateAsync: followUser, isPending: followLoading } = useFollowUser();
  const [activeTab, setActiveTab] = useState('followers');
  const [searchTerm, setSearchTerm] = useState('');

  // Mock suggested users - in real app, get from API
  const [suggestedUsers] = useState([
    { id: 6, firstName: 'Alex', lastName: 'Brown', userName: 'alexb', mutualConnections: 3 },
    { id: 7, firstName: 'Emily', lastName: 'Davis', userName: 'emilyd', mutualConnections: 1 },
    { id: 8, firstName: 'Chris', lastName: 'Miller', userName: 'chrism', mutualConnections: 5 },
    { id: 9, firstName: 'Lisa', lastName: 'Taylor', userName: 'lisat', mutualConnections: 2 }
  ]);

  const handleFollowUser = async (userId) => {
    try {
      await followUser(userId);
    } catch (err) {
      console.error("Failed to follow user:", err);
    }
  };

  const filteredFollowers = followers.filter(follower =>
    follower.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    follower.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    follower.userName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredSuggested = suggestedUsers.filter(user =>
    user.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.userName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="max-w-4xl mx-auto p-4">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">Social Connections</h1>
        
        {/* Search */}
        <div className="mb-6">
          <input
            type="text"
            placeholder="Search people..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8">
            <button
              onClick={() => setActiveTab('followers')}
              className={`py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                activeTab === 'followers'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <UsersIcon className="h-5 w-5 inline mr-2" />
              Followers ({followers.length})
            </button>
            <button
              onClick={() => setActiveTab('suggested')}
              className={`py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                activeTab === 'suggested'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <UserPlusIcon className="h-5 w-5 inline mr-2" />
              Discover People
            </button>
          </nav>
        </div>
      </div>

      {isError && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6">
          {error?.message || 'An error occurred'}
        </div>
      )}

      {/* Content */}
      <div className="bg-white rounded-lg shadow-md">
        {activeTab === 'followers' && (
          <div className="p-6">
            {loading ? (
              <div className="flex justify-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              </div>
            ) : filteredFollowers.length === 0 ? (
              <div className="text-center py-8">
                <UsersIcon className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">
                  {searchTerm ? 'No followers found' : 'No followers yet'}
                </h3>
                <p className="text-gray-500">
                  {searchTerm ? 'Try adjusting your search terms' : 'Start connecting with others to build your network!'}
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredFollowers.map((follower) => (
                  <div key={follower.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                    <div className="flex items-center space-x-3 mb-3">
                      <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-blue-600 rounded-full flex items-center justify-center text-white font-medium">
                        {follower.firstName?.charAt(0)}{follower.lastName?.charAt(0)}
                      </div>
                      <div className="flex-1">
                        <p className="font-medium text-gray-900">
                          {follower.firstName} {follower.lastName}
                        </p>
                        <p className="text-sm text-gray-500">@{follower.userName}</p>
                      </div>
                    </div>
                    <div className="flex space-x-2">
                      <button className="flex-1 bg-gray-100 text-gray-700 py-2 px-3 rounded-lg hover:bg-gray-200 transition-colors text-sm font-medium">
                        Message
                      </button>
                      <button className="flex-1 bg-red-50 text-red-600 py-2 px-3 rounded-lg hover:bg-red-100 transition-colors text-sm font-medium">
                        Unfollow
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'suggested' && (
          <div className="p-6">
            {filteredSuggested.length === 0 ? (
              <div className="text-center py-8">
                <UserPlusIcon className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">
                  {searchTerm ? 'No users found' : 'No suggestions available'}
                </h3>
                <p className="text-gray-500">
                  {searchTerm ? 'Try adjusting your search terms' : 'Check back later for new people to connect with!'}
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredSuggested.map((user) => (
                  <div key={user.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                    <div className="flex items-center space-x-3 mb-3">
                      <div className="w-12 h-12 bg-gradient-to-r from-purple-500 to-pink-600 rounded-full flex items-center justify-center text-white font-medium">
                        {user.firstName?.charAt(0)}{user.lastName?.charAt(0)}
                      </div>
                      <div className="flex-1">
                        <p className="font-medium text-gray-900">
                          {user.firstName} {user.lastName}
                        </p>
                        <p className="text-sm text-gray-500">@{user.userName}</p>
                        <p className="text-xs text-gray-400">
                          {user.mutualConnections} mutual connections
                        </p>
                      </div>
                    </div>
                    <div className="flex space-x-2">
                      <button 
                        onClick={() => handleFollowUser(user.id)}
                        disabled={followLoading}
                        className="flex-1 bg-blue-600 text-white py-2 px-3 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm font-medium"
                      >
                        {followLoading ? (
                          <div className="flex items-center justify-center">
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-1"></div>
                            Following...
                          </div>
                        ) : (
                          'Follow'
                        )}
                      </button>
                      <button className="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium text-gray-700">
                        View Profile
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Followers;