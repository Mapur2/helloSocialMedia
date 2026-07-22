import React, { useState, useEffect, useRef } from 'react';
import { useChat } from '../contexts/ChatContext';
import { useAuth } from '../contexts/AuthContext';
import { 
  PaperAirplaneIcon,
  FaceSmileIcon,
  PaperClipIcon,
  UserIcon
} from '@heroicons/react/24/outline';

const Messages = () => {
  const [selectedChat, setSelectedChat] = useState(null);
  const [messageInput, setMessageInput] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const { 
    messages, 
    sendMessage, 
    getChatHistory, 
    connected, 
    getChatId 
  } = useChat();
  const { user } = useAuth();
  const messagesEndRef = useRef(null);

  // Mock users for demonstration - in real app, get from API
  const [availableUsers] = useState([
    { id: 2, firstName: 'John', lastName: 'Doe', userName: 'johndoe', isOnline: true },
    { id: 3, firstName: 'Jane', lastName: 'Smith', userName: 'janesmith', isOnline: false },
    { id: 4, firstName: 'Mike', lastName: 'Johnson', userName: 'mikej', isOnline: true },
    { id: 5, firstName: 'Sarah', lastName: 'Wilson', userName: 'sarahw', isOnline: false }
  ]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, selectedChat]);

  const handleSelectChat = async (userId) => {
    setSelectedChat(userId);
    await getChatHistory(userId);
  };

  const handleSendMessage = (e) => {
    e.preventDefault();
    if (!messageInput.trim() || !selectedChat) return;

    sendMessage(selectedChat, messageInput);
    setMessageInput('');
  };

  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const selectedUser = availableUsers.find(u => u.id === selectedChat);
  const currentChatMessages = selectedChat ? messages[getChatId(user.userId, selectedChat)] || [] : [];
  
  const filteredUsers = availableUsers.filter(u =>
    u.firstName.toLowerCase().includes(searchInput.toLowerCase()) ||
    u.lastName.toLowerCase().includes(searchInput.toLowerCase()) ||
    u.userName.toLowerCase().includes(searchInput.toLowerCase())
  );

  return (
    <div className="max-w-7xl mx-auto p-4">
      <div className="bg-white rounded-lg shadow-md overflow-hidden h-[calc(100vh-8rem)]">
        <div className="flex h-full">
          {/* Sidebar - Chat List */}
          <div className="w-1/3 border-r border-gray-200 flex flex-col">
            {/* Header */}
            <div className="p-4 border-b border-gray-200">
              <h2 className="text-xl font-semibold text-gray-900 mb-3">Messages</h2>
              <div className="relative">
                <input
                  type="text"
                  placeholder="Search conversations..."
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  className="w-full pl-4 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* Connection Status */}
            <div className={`px-4 py-2 text-sm ${connected ? 'text-green-600 bg-green-50' : 'text-red-600 bg-red-50'}`}>
              {connected ? '🟢 Connected' : '🔴 Disconnected'}
            </div>

            {/* Chat List */}
            <div className="flex-1 overflow-y-auto">
              {filteredUsers.map((chatUser) => (
                <button
                  key={chatUser.id}
                  onClick={() => handleSelectChat(chatUser.id)}
                  className={`w-full p-4 flex items-center space-x-3 hover:bg-gray-50 transition-colors border-b border-gray-100 ${
                    selectedChat === chatUser.id ? 'bg-blue-50 border-r-2 border-r-blue-500' : ''
                  }`}
                >
                  <div className="relative">
                    <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-blue-600 rounded-full flex items-center justify-center text-white font-medium">
                      {chatUser.firstName.charAt(0)}{chatUser.lastName.charAt(0)}
                    </div>
                    {chatUser.isOnline && (
                      <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-white rounded-full"></div>
                    )}
                  </div>
                  <div className="flex-1 text-left">
                    <p className="font-medium text-gray-900">
                      {chatUser.firstName} {chatUser.lastName}
                    </p>
                    <p className="text-sm text-gray-500">@{chatUser.userName}</p>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Main Chat Area */}
          <div className="flex-1 flex flex-col">
            {selectedChat ? (
              <>
                {/* Chat Header */}
                <div className="p-4 border-b border-gray-200 bg-gray-50">
                  <div className="flex items-center space-x-3">
                    <div className="relative">
                      <div className="w-10 h-10 bg-gradient-to-r from-green-500 to-blue-600 rounded-full flex items-center justify-center text-white font-medium">
                        {selectedUser?.firstName.charAt(0)}{selectedUser?.lastName.charAt(0)}
                      </div>
                      {selectedUser?.isOnline && (
                        <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-white rounded-full"></div>
                      )}
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        {selectedUser?.firstName} {selectedUser?.lastName}
                      </h3>
                      <p className="text-sm text-gray-500">
                        {selectedUser?.isOnline ? 'Online' : 'Offline'}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Messages */}
                <div className="flex-1 overflow-y-auto p-4 space-y-4">
                  {currentChatMessages.length === 0 ? (
                    <div className="text-center text-gray-500 mt-8">
                      <UserIcon className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                      <p>No messages yet. Start a conversation!</p>
                    </div>
                  ) : (
                    currentChatMessages.map((message, index) => {
                      const isOwn = message.senderId === user.userId;
                      return (
                        <div
                          key={index}
                          className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}
                        >
                          <div
                            className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                              isOwn
                                ? 'bg-blue-600 text-white'
                                : 'bg-gray-200 text-gray-900'
                            }`}
                          >
                            <p>{message.content}</p>
                            <p
                              className={`text-xs mt-1 ${
                                isOwn ? 'text-blue-100' : 'text-gray-500'
                              }`}
                            >
                              {formatTime(message.timestamp)}
                            </p>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Message Input */}
                <form onSubmit={handleSendMessage} className="p-4 border-t border-gray-200">
                  <div className="flex items-center space-x-3">
                    <button
                      type="button"
                      className="text-gray-400 hover:text-gray-600 transition-colors"
                    >
                      <PaperClipIcon className="h-5 w-5" />
                    </button>
                    <div className="flex-1 relative">
                      <input
                        type="text"
                        value={messageInput}
                        onChange={(e) => setMessageInput(e.target.value)}
                        placeholder={`Message ${selectedUser?.firstName}...`}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent pr-12"
                      />
                      <button
                        type="button"
                        className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                      >
                        <FaceSmileIcon className="h-5 w-5" />
                      </button>
                    </div>
                    <button
                      type="submit"
                      disabled={!messageInput.trim() || !connected}
                      className="bg-blue-600 text-white p-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      <PaperAirplaneIcon className="h-5 w-5" />
                    </button>
                  </div>
                </form>
              </>
            ) : (
              /* No Chat Selected */
              <div className="flex-1 flex items-center justify-center bg-gray-50">
                <div className="text-center">
                  <div className="w-16 h-16 bg-gray-300 rounded-full flex items-center justify-center mx-auto mb-4">
                    <UserIcon className="h-8 w-8 text-gray-500" />
                  </div>
                  <h3 className="text-lg font-medium text-gray-900 mb-2">Select a conversation</h3>
                  <p className="text-gray-500">Choose a contact to start messaging</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Messages;