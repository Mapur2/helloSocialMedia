import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { useChat } from '../contexts/ChatContext';
import { useAuth } from '../contexts/AuthContext';
import { useGetFollowers, useGetFollowing } from '../hooks/useSocial';
import {
  PaperAirplaneIcon,
  PaperClipIcon,
  ChatBubbleLeftRightIcon
} from '@heroicons/react/24/outline';

const GATEWAY_BASE_URL = 'http://localhost:8079';

/**
 * Resolve a mediaId to a usable image URL.
 * The /api/media/{id}/urls endpoint returns an envelope like
 * { data: { original: "...", resized: "..." } } or { original, resized }.
 */
const resolveMediaUrl = async (mediaId) => {
  if (!mediaId) return null;
  try {
    const res = await axios.get(`${GATEWAY_BASE_URL}/api/media/${mediaId}/urls`);
    const data = res.data?.data || res.data || {};
    return data.resized || data.original || data.url || null;
  } catch (err) {
    console.warn('[Chat] Failed to resolve media URL for', mediaId, err);
    return null;
  }
};

/**
 * Lazy image renderer that resolves the mediaId to a real S3/MinIO URL.
 * Shows a small skeleton, then the image, or a placeholder on failure.
 */
const ChatImage = ({ mediaId }) => {
  const [src, setSrc] = useState(null);
  const [errored, setErrored] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setSrc(null);
    setErrored(false);
    if (!mediaId) return;
    resolveMediaUrl(mediaId).then((url) => {
      if (!cancelled) setSrc(url);
    });
    return () => {
      cancelled = true;
    };
  }, [mediaId]);

  if (!mediaId) {
    return <span className="italic text-xs">📷 Image Attachment</span>;
  }

  if (errored || !src) {
    return (
      <div className="my-1 w-60 h-40 bg-black/10 rounded-lg flex items-center justify-center text-xs opacity-70">
        {errored ? 'Image unavailable' : 'Loading image…'}
      </div>
    );
  }

  return (
    <div className="my-1">
      <img
        src={src}
        alt="Attached"
        loading="lazy"
        className="rounded-lg max-h-60 w-auto object-cover"
        onError={() => setErrored(true)}
      />
    </div>
  );
};

const Messages = () => {
  const { user, token } = useAuth();
  const { 
    rooms,
    activeRoom,
    messages,
    sendMessage, 
    openDirectChat,
    openExistingRoom,
    connected,
    loading: chatLoading
  } = useChat();

  const { data: followers = [], isLoading: followersLoading } = useGetFollowers();
  const { data: following = [], isLoading: followingLoading } = useGetFollowing();

  const [activeTab, setActiveTab] = useState('following'); // 'following' | 'followers' | 'rooms'
  const [selectedContact, setSelectedContact] = useState(null);
  const [messageInput, setMessageInput] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);

  const messagesEndRef = useRef(null);
  const messagesScrollRef = useRef(null);
  const fileInputRef = useRef(null);
  const stickyBottomRef = useRef(true);

  // Track whether the user is "stuck" to the bottom (within 80px).
  // If they scroll up to read history we don't yank them back down.
  const handleScroll = () => {
    const el = messagesScrollRef.current;
    if (!el) return;
    const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    stickyBottomRef.current = distanceFromBottom < 80;
  };

  const scrollToBottom = (force = false) => {
    if (!force && !stickyBottomRef.current) return;
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom(false);
  }, [messages, activeRoom]);

  // When switching rooms, always jump to the bottom.
  useEffect(() => {
    stickyBottomRef.current = true;
    scrollToBottom(true);
  }, [activeRoom?.roomId]);

  // Handle contact selection (open or create 1-on-1 room)
  const handleSelectContact = async (contact) => {
    const contactId = contact.userId || contact.id;
    setSelectedContact(contact);
    await openDirectChat(contactId);
  };

  const handleSendMessage = (e) => {
    e.preventDefault();
    if (!messageInput.trim() || !activeRoom) return;

    sendMessage(activeRoom.roomId, messageInput.trim(), 'TEXT');
    setMessageInput('');
  };

  const handleImageUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file || !activeRoom) return;

    setUploadingImage(true);
    try {
      // Step 1: Request presigned upload URL
      const ext = file.name.split('.').pop() || 'jpg';
      const presignRes = await axios.post(
        `${GATEWAY_BASE_URL}/api/media/upload-url`,
        {
          filename: file.name,
          contentType: file.type || 'image/jpeg',
          extension: ext,
          mediaType: 'image'
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'X-USER-ID': user?.userId
          }
        }
      );

      const { mediaId, uploadUrl } = presignRes.data.data || presignRes.data;

      // Step 2: Upload file directly to S3 / MinIO
      await fetch(uploadUrl, {
        method: 'PUT',
        headers: {
          'Content-Type': file.type || 'image/jpeg'
        },
        body: file
      });

      // Step 3: Complete upload
      await axios.post(
        `${GATEWAY_BASE_URL}/api/media/${mediaId}/complete`,
        {},
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'X-USER-ID': user?.userId
          }
        }
      );

      // Step 4: Dispatch chat message with mediaId
      sendMessage(activeRoom.roomId, '', 'IMAGE', mediaId);
    } catch (err) {
      console.error('Failed to upload image message:', err);
    } finally {
      setUploadingImage(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    return new Date(timestamp).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Helper to resolve contact profile from followers/following by userId
  const findContactByUserId = (targetId) => {
    if (!targetId) return null;
    return (
      following.find((u) => (u.userId || u.id) === targetId) ||
      followers.find((u) => (u.userId || u.id) === targetId)
    );
  };

  // Filter list based on search and active tab
  const getDisplayList = () => {
    let list = [];
    if (activeTab === 'following') list = following;
    else if (activeTab === 'followers') list = followers;
    else if (activeTab === 'rooms') {
      return rooms.filter(r => {
        const otherUser = r.otherUserId ? findContactByUserId(r.otherUserId) : null;
        const searchTarget = `${r.name || ''} ${otherUser?.firstName || ''} ${otherUser?.lastName || ''} ${otherUser?.userName || ''} ${r.roomId}`.toLowerCase();
        return searchTarget.includes(searchInput.toLowerCase());
      });
    }

    return list.filter(item => {
      const name = `${item.firstName || ''} ${item.lastName || ''}`.toLowerCase();
      const uName = (item.userName || item.username || '').toLowerCase();
      const q = searchInput.toLowerCase();
      return name.includes(q) || uName.includes(q);
    });
  };

  const displayList = getDisplayList();

  return (
    <div className="max-w-7xl mx-auto p-4">
      <div className="bg-white rounded-lg shadow-md overflow-hidden h-[calc(100vh-8rem)]">
        <div className="flex h-full">
          {/* Sidebar */}
          <div className="w-1/3 border-r border-gray-200 flex flex-col">
            {/* Header & Search */}
            <div className="p-4 border-b border-gray-200">
              <div className="flex items-center justify-between mb-3">
                <h2 className="text-xl font-semibold text-gray-900">Messages</h2>
                <div className={`px-2.5 py-1 rounded-full text-xs font-medium ${connected ? 'text-green-700 bg-green-100' : 'text-red-700 bg-red-100'}`}>
                  {connected ? '🟢 Connected' : '🔴 Connecting...'}
                </div>
              </div>
              <div className="relative">
                <input
                  type="text"
                  placeholder="Search people or rooms..."
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  className="w-full pl-4 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                />
              </div>
            </div>

            {/* Navigation Tabs */}
            <div className="flex border-b border-gray-200 bg-gray-50 text-xs font-medium text-gray-600">
              <button
                onClick={() => setActiveTab('following')}
                className={`flex-1 py-2.5 text-center border-b-2 transition-colors ${
                  activeTab === 'following' ? 'border-blue-600 text-blue-600 bg-white font-semibold' : 'border-transparent hover:text-gray-900'
                }`}
              >
                Following ({following.length})
              </button>
              <button
                onClick={() => setActiveTab('followers')}
                className={`flex-1 py-2.5 text-center border-b-2 transition-colors ${
                  activeTab === 'followers' ? 'border-blue-600 text-blue-600 bg-white font-semibold' : 'border-transparent hover:text-gray-900'
                }`}
              >
                Followers ({followers.length})
              </button>
              <button
                onClick={() => setActiveTab('rooms')}
                className={`flex-1 py-2.5 text-center border-b-2 transition-colors ${
                  activeTab === 'rooms' ? 'border-blue-600 text-blue-600 bg-white font-semibold' : 'border-transparent hover:text-gray-900'
                }`}
              >
                Chats ({rooms.length})
              </button>
            </div>

            {/* Contact / Room List */}
            <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
              {followersLoading || followingLoading ? (
                <div className="flex justify-center py-8">
                  <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
                </div>
              ) : displayList.length === 0 ? (
                <div className="text-center py-8 text-gray-400 text-sm">
                  No {activeTab} found
                </div>
              ) : (
                displayList.map((item) => {
                  if (activeTab === 'rooms') {
                    const isSelected = activeRoom?.roomId === item.roomId;
                    const otherUser = item.otherUserId ? findContactByUserId(item.otherUserId) : null;
                    const title = item.name || (otherUser ? `${otherUser.firstName} ${otherUser.lastName}` : (item.roomType === 'ONE_TO_ONE' ? 'Direct Chat' : 'Group Chat'));
                    const subtitle = otherUser ? `@${otherUser.userName || otherUser.username}` : `ID: ${item.roomId.substring(0, 8)}...`;
                    const unread = item.unreadCount || 0;
                    const lastMsg = item.lastMessage;
                    const lastPreview = lastMsg
                      ? (lastMsg.type === 'IMAGE' ? '📷 Image' : (lastMsg.content || ''))
                      : null;

                    return (
                      <button
                        key={item.id || item.roomId}
                        onClick={() => {
                          setSelectedContact(otherUser || { firstName: title, lastName: '', _fallback: true });
                          openExistingRoom(item);
                        }}
                        className={`w-full p-3.5 flex items-center space-x-3 hover:bg-gray-50 transition-colors text-left ${
                          isSelected ? 'bg-blue-50 border-r-2 border-r-blue-600' : ''
                        }`}
                      >
                        <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full flex items-center justify-center text-white font-semibold text-sm flex-shrink-0">
                          {otherUser?.firstName?.charAt(0) || title.charAt(0)}
                          {otherUser?.lastName?.charAt(0) || ''}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between">
                            <p className={`truncate text-sm ${unread > 0 ? 'font-semibold text-gray-900' : 'font-medium text-gray-900'}`}>
                              {title}
                            </p>
                            {unread > 0 && (
                              <span className="ml-2 px-1.5 min-w-[1.25rem] h-5 bg-blue-600 text-white text-[10px] font-bold rounded-full flex items-center justify-center flex-shrink-0">
                                {unread > 99 ? '99+' : unread}
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-gray-500 truncate">
                            {lastPreview || subtitle}
                          </p>
                        </div>
                      </button>
                    );
                  }

                  const contactId = item.userId || item.id;
                  const isSelected = selectedContact && (selectedContact.userId === contactId || selectedContact.id === contactId);

                  return (
                    <button
                      key={contactId}
                      onClick={() => handleSelectContact(item)}
                      className={`w-full p-3.5 flex items-center space-x-3 hover:bg-gray-50 transition-colors text-left ${
                        isSelected ? 'bg-blue-50 border-r-2 border-r-blue-600' : ''
                      }`}
                    >
                      <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full flex items-center justify-center text-white font-medium text-sm flex-shrink-0">
                        {item.firstName?.charAt(0) || item.userName?.charAt(0) || 'U'}
                        {item.lastName?.charAt(0) || ''}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-gray-900 truncate text-sm">
                          {item.firstName} {item.lastName}
                        </p>
                        <p className="text-xs text-gray-500 truncate">@{item.userName || item.username}</p>
                      </div>
                    </button>
                  );
                })
              )}
            </div>
          </div>

          {/* Main Chat Window */}
          <div className="flex-1 flex flex-col bg-gray-50">
            {activeRoom ? (
              <>
                {/* Chat Header */}
                <div className="p-4 border-b border-gray-200 bg-white flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full flex items-center justify-center text-white font-medium">
                      {selectedContact?.firstName?.charAt(0) || 'C'}
                      {selectedContact?.lastName?.charAt(0) || ''}
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900 text-sm">
                        {selectedContact?.firstName ? `${selectedContact.firstName} ${selectedContact.lastName || ''}` : activeRoom.name || 'Direct Conversation'}
                      </h3>
                      {selectedContact?.userName && (
                        <p className="text-xs text-gray-500">@{selectedContact.userName}</p>
                      )}
                    </div>
                  </div>
                </div>

                {/* Messages Feed */}
                <div
                  ref={messagesScrollRef}
                  onScroll={handleScroll}
                  className="flex-1 overflow-y-auto p-4 space-y-3"
                >
                  {chatLoading ? (
                    <div className="flex justify-center py-8">
                      <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
                    </div>
                  ) : messages.length === 0 ? (
                    <div className="text-center text-gray-400 mt-12 text-sm">
                      <ChatBubbleLeftRightIcon className="h-10 w-10 mx-auto mb-2 text-gray-300" />
                      <p>No messages yet. Say hello!</p>
                    </div>
                  ) : (
                    messages.map((message, index) => {
                      const isOwn = message.senderId === user?.userId;
                      const isImage = message.type === 'IMAGE';

                      return (
                        <div
                          key={message.id || message.clientMessageId || index}
                          className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}
                        >
                          <div
                            className={`max-w-xs lg:max-w-md px-4 py-2.5 rounded-2xl shadow-sm ${
                              isOwn
                                ? 'bg-blue-600 text-white rounded-br-none'
                                : 'bg-white text-gray-900 border border-gray-200 rounded-bl-none'
                            }`}
                          >
                            {isImage ? (
                              <ChatImage mediaId={message.mediaId} />
                            ) : (
                              <p className="text-sm leading-relaxed">{message.content}</p>
                            )}

                            <div className="flex items-center justify-end space-x-1 mt-1">
                              <span className={`text-[10px] ${isOwn ? 'text-blue-200' : 'text-gray-400'}`}>
                                {formatTime(message.timestamp)}
                              </span>
                              {isOwn && (
                                <span className="text-[11px]">
                                  {message.status === 'READ' ? (
                                    <span className="text-blue-200 font-bold" title="Read">✓✓</span>
                                  ) : message.status === 'DELIVERED' ? (
                                    <span className="text-blue-300" title="Delivered">✓✓</span>
                                  ) : (
                                    <span className="text-blue-300" title="Sent">✓</span>
                                  )}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Message Input Bar */}
                <form onSubmit={handleSendMessage} className="p-3 bg-white border-t border-gray-200">
                  <div className="flex items-center space-x-2">
                    <input
                      type="file"
                      ref={fileInputRef}
                      onChange={handleImageUpload}
                      accept="image/*"
                      className="hidden"
                    />
                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      disabled={uploadingImage || !connected}
                      title="Attach image"
                      className="text-gray-400 hover:text-blue-600 p-2 rounded-full hover:bg-gray-100 transition-colors disabled:opacity-50"
                    >
                      <PaperClipIcon className="h-5 w-5" />
                    </button>

                    <div className="flex-1 relative">
                      <input
                        type="text"
                        value={messageInput}
                        onChange={(e) => setMessageInput(e.target.value)}
                        placeholder={uploadingImage ? 'Uploading image...' : 'Type a message...'}
                        disabled={uploadingImage || !connected}
                        className="w-full px-4 py-2.5 border border-gray-300 rounded-full focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm pr-10"
                      />
                    </div>

                    <button
                      type="submit"
                      disabled={!messageInput.trim() || !connected || uploadingImage}
                      className="bg-blue-600 text-white p-2.5 rounded-full hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shadow"
                    >
                      <PaperAirplaneIcon className="h-4 w-4 transform rotate-45" />
                    </button>
                  </div>
                </form>
              </>
            ) : (
              /* Empty state */
              <div className="flex-1 flex items-center justify-center">
                <div className="text-center">
                  <div className="w-16 h-16 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
                    <ChatBubbleLeftRightIcon className="h-8 w-8" />
                  </div>
                  <h3 className="text-lg font-medium text-gray-900 mb-1">Select a conversation</h3>
                  <p className="text-gray-500 text-sm">Choose someone from your following or followers to chat</p>
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