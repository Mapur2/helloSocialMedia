import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import axios from 'axios';
import { useAuth } from './AuthContext';

// Robust STOMP-over-WebSocket client
class StompOverWebSocket {
  constructor(url) {
    this.url = url;
    this.websocket = null;
    this.connected = false;
    this._everConnected = false;
    this.subscriptions = new Map();
    this.messageQueue = [];
    this.reconnectTimeout = null;
  }

  connect(headers = {}, onConnect, onError) {
    try {
      this._onConnectCb = onConnect || null;
      this._onErrorCb = onError || null;
      const wsUrl = this.url.replace(/^http:\/\//, 'ws://').replace(/^https:\/\//, 'wss://');
      this.websocket = new WebSocket(wsUrl);

      // Spring's STOMP server REQUIRES accept-version and host headers, and
      // a heart-beat pair, otherwise it sends an ERROR frame and closes the
      // connection. The custom frame builder previously omitted them, which
      // made the WS connect succeed at the transport level but the STOMP
      // handshake never completed - hence "Connecting..." forever.
      const connectHeaders = {
        'accept-version': '1.2',
        'heart-beat': '10000,10000',
        host: this.url.replace(/^ws(s)?:\/\//, '').replace(/\/.*$/, ''),
        ...headers
      };

      this.websocket.onopen = () => {
        // TCP socket is up; we still need a STOMP CONNECTED frame before we
        // can call ourselves "connected" - the handleMessage() method flips
        // this.connected = true on CONNECTED.
        this.send('CONNECT', connectHeaders);

        // Process queued messages
        while (this.messageQueue.length > 0) {
          const item = this.messageQueue.shift();
          this.websocket.send(item);
        }
      };

      this.websocket.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.websocket.onclose = () => {
        this.connected = false;
        // Don't fire onError on a graceful close (we explicitly called
        // disconnect() ourselves). Only report to the caller if we never
        // got a CONNECTED frame - otherwise the fallback path would spin
        // forever for a transient network blip.
        if (!this._everConnected && this._onErrorCb) {
          const cb = this._onErrorCb;
          this._onErrorCb = null;
          cb(new Error('WebSocket closed before CONNECTED'));
        }
      };

      this.websocket.onerror = (error) => {
        console.error('WebSocket error:', error);
        // Don't double-fire: onerror is always followed by onclose.
      };
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      if (onError) onError(error);
    }
  }

  send(command, headers = {}, body = '') {
    const frame = this.buildFrame(command, headers, body);
    if (this.connected && this.websocket && this.websocket.readyState === WebSocket.OPEN) {
      this.websocket.send(frame);
    } else {
      this.messageQueue.push(frame);
    }
  }

  subscribe(destination, callback) {
    const id = 'sub-' + Math.random().toString(36).substr(2, 9);
    this.subscriptions.set(destination, { id, callback, sent: false });
    if (this.connected && this.websocket && this.websocket.readyState === WebSocket.OPEN) {
      this.send('SUBSCRIBE', { destination, id });
      const sub = this.subscriptions.get(destination);
      if (sub) sub.sent = true;
    }
    return {
      unsubscribe: () => this.unsubscribe(destination)
    };
  }

  unsubscribe(destination) {
    const sub = this.subscriptions.get(destination);
    if (sub) {
      if (sub.sent) this.send('UNSUBSCRIBE', { id: sub.id });
      this.subscriptions.delete(destination);
    }
  }

  /**
   * Flush any subscriptions that were registered before the STOMP
   * handshake completed. Without this, React effects that run during the
   * CONNECT/CONNECTED round-trip will have stored their subscribe callback
   * in this.subscriptions but never actually sent a SUBSCRIBE frame to the
   * server - the user appears connected but receives no messages.
   */
  flushPendingSubscriptions() {
    this.subscriptions.forEach((sub, destination) => {
      if (!sub.sent) {
        this.send('SUBSCRIBE', { destination, id: sub.id });
        sub.sent = true;
      }
    });
  }

  disconnect() {
    if (this.websocket) {
      try {
        this.send('DISCONNECT');
        this.websocket.close();
      } catch (e) {}
      this.connected = false;
      this._everConnected = false;
      this.subscriptions.clear();
      this.messageQueue = [];
    }
  }

  buildFrame(command, headers, body) {
    let frame = command + '\n';
    for (const [key, value] of Object.entries(headers)) {
      frame += `${key}:${value}\n`;
    }
    // Add content-length if body is non-empty to avoid parsing issues
    if (body) {
      frame += `content-length:${body.length}\n`;
    }
    frame += '\n' + (body || '') + '\0';
    return frame;
  }

  handleMessage(data) {
    const lines = data.split('\n');
    const command = lines[0];
    const headers = {};
    let bodyIndex = 1;

    for (let i = 1; i < lines.length; i++) {
      if (lines[i] === '') {
        bodyIndex = i + 1;
        break;
      }
      const [key, ...rest] = lines[i].split(':');
      if (key) {
        headers[key.trim()] = rest.join(':').trim();
      }
    }

    const body = lines.slice(bodyIndex).join('\n').replace(/\0$/, '');

    if (command === 'CONNECTED') {
      // Server confirmed the STOMP handshake. We treat this as the point
      // where the connection is "actually" usable, not when the TCP socket
      // opened. Also notify the caller so React can flip the badge/UI.
      this._everConnected = true;
      this.connected = true;
      // Flush any subscriptions that were registered between WS open and
      // CONNECTED - they were stored in this.subscriptions but never sent.
      this.flushPendingSubscriptions();
      if (this._onConnectCb) {
        this._onConnectCb();
        this._onConnectCb = null;
      }
    } else if (command === 'MESSAGE') {
      const destination = headers.destination;
      const sub = this.subscriptions.get(destination);
      if (sub && sub.callback) {
        sub.callback({ headers, body });
      }
    } else if (command === 'ERROR') {
      console.error('[STOMP] server error:', body || headers.message || '(no body)');
    }
  }
}

const ChatContext = createContext();

const GATEWAY_BASE_URL = 'http://localhost:8079';
const GATEWAY_WS_URL = 'ws://localhost:8079/chat';

export const ChatProvider = ({ children }) => {
  const { user, token, isAuthenticated } = useAuth();
  const currentUserId = user?.userId || user?.id;

  const [activeRoom, setActiveRoom] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [messages, setMessages] = useState({}); // { [roomId]: [Message, ...] }
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const stompClientRef = useRef(null);

  const getAuthHeaders = useCallback(() => {
    const headers = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (currentUserId) headers['X-USER-ID'] = currentUserId;
    return { headers };
  }, [token, currentUserId]);

  // Connect WebSocket when authenticated
  useEffect(() => {
    if (isAuthenticated && currentUserId) {
      const client = new StompOverWebSocket(GATEWAY_WS_URL);
      stompClientRef.current = client;

      client.connect(
        { 'X-USER-ID': currentUserId },
        () => {
          setConnected(true);
          console.log('[Chat] Connected to STOMP Gateway');
        },
        (err) => {
          console.warn('[Chat] Gateway WebSocket failed, attempting direct fallback to :8084:', err);
          // Fallback directly to 8084 if Gateway WS routing has an issue
          const fallbackClient = new StompOverWebSocket('ws://localhost:8084/chat');
          stompClientRef.current = fallbackClient;
          fallbackClient.connect(
            { 'X-USER-ID': currentUserId },
            () => {
              setConnected(true);
              console.log('[Chat] Connected to direct chat WebSocket');
            },
            (fallbackErr) => {
              console.error('[Chat] All WebSocket connection attempts failed:', fallbackErr);
              setConnected(false);
            }
          );
        }
      );

      return () => {
        if (stompClientRef.current) {
          stompClientRef.current.disconnect();
        }
      };
    } else {
      if (stompClientRef.current) {
        stompClientRef.current.disconnect();
      }
      setConnected(false);
    }
  }, [isAuthenticated, currentUserId]);

  // Fetch active rooms on mount or user login
  const fetchMyRooms = useCallback(async () => {
    if (!currentUserId) return;
    try {
      const res = await axios.get(`${GATEWAY_BASE_URL}/api/rooms/my-rooms`, getAuthHeaders());
      if (res.data?.success) {
        // Dedupe rooms: prefer ONE_TO_ONE entries and keep only the most-recent
        // one per (otherUserId). GROUP rooms are kept by roomId since they
        // have no single counterpart.
        const raw = res.data.data || [];
        const seen = new Set();
        const deduped = [];
        // Sort by updatedAt desc so we keep the newest of any duplicates.
        const sorted = [...raw].sort((a, b) => {
          const ta = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
          const tb = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
          return tb - ta;
        });
        for (const room of sorted) {
          const key = room.roomType === 'ONE_TO_ONE'
            ? `u:${room.otherUserId || ''}`
            : `r:${room.roomId}`;
          if (seen.has(key)) continue;
          seen.add(key);
          deduped.push(room);
        }
        setRooms(deduped.map((r) => ({ ...r, unreadCount: 0 })));
      }
    } catch (err) {
      console.error('[Chat] Failed to fetch user rooms:', err);
    }
  }, [currentUserId, getAuthHeaders]);

  useEffect(() => {
    if (isAuthenticated && currentUserId) {
      fetchMyRooms();
    }
  }, [isAuthenticated, currentUserId, fetchMyRooms]);

  // Track which rooms we already subscribed to so we don't double-subscribe
  // when the active room changes.
  const subscribedRoomIdsRef = useRef(new Set());

  // Subscribe to ALL known rooms so we receive messages even when the
  // conversation isn't currently active. Re-subscribes when the room list
  // changes (new chat created, old one removed).
  useEffect(() => {
    const client = stompClientRef.current;
    if (!client || !connected || !rooms || rooms.length === 0) return;

    const localUserId = user?.userId || user?.id;
    const subscribed = subscribedRoomIdsRef.current;
    const newSubs = [];

    for (const room of rooms) {
      const roomId = room.roomId;
      if (!roomId || subscribed.has(roomId)) continue;

      // 1. New messages
      const messageSub = client.subscribe(`/topic/room/${roomId}`, (frame) => {
        try {
          const payload = JSON.parse(frame.body);
          const newMsg = payload.data || payload;

          setMessages((prev) => {
            const currentList = prev[roomId] || [];
            // Already confirmed (echo of our own message or already present)
            if (currentList.some((m) => m.id === newMsg.id)) {
              return prev;
            }
            // Reconcile an optimistic message by clientMessageId
            const optimisticIdx = newMsg.clientMessageId
              ? currentList.findIndex((m) => m._optimistic && m.clientMessageId === newMsg.clientMessageId)
              : -1;
            if (optimisticIdx !== -1) {
              const next = currentList.slice();
              next[optimisticIdx] = { ...next[optimisticIdx], ...newMsg, _optimistic: false };
              return { ...prev, [roomId]: next };
            }
            // Brand new message
            return {
              ...prev,
              [roomId]: [...currentList, newMsg]
            };
          });

          // If message is from other user, send read receipt - but only
          // when this room is the one the user is actively looking at.
          if (newMsg.senderId !== localUserId) {
            // bump unread counter optimistically; the actual mark-read
            // happens when the user opens the conversation.
            setRooms((prevRooms) => prevRooms.map((r) =>
              r.roomId === roomId
                ? { ...r, unreadCount: (r.unreadCount || 0) + 1, lastMessage: newMsg }
                : r
            ));
            // Auto-READ only for the currently open room.
            if (activeRoomRef.current && activeRoomRef.current.roomId === roomId) {
              markMessageStatus(roomId, newMsg.id, 'READ');
            }
          }
        } catch (e) {
          console.error('[Chat] Failed to parse incoming message:', e);
        }
      });

      // 2. Status updates (read receipts, delivery)
      const statusSub = client.subscribe(`/topic/room/${roomId}/status`, (frame) => {
        try {
          const payload = JSON.parse(frame.body);
          const updatedMsg = payload.data || payload;

          setMessages((prev) => {
            const currentList = prev[roomId] || [];
            return {
              ...prev,
              [roomId]: currentList.map((m) =>
                m.id === updatedMsg.id || m.clientMessageId === updatedMsg.clientMessageId
                  ? { ...m, status: updatedMsg.status }
                  : m
              )
            };
          });
        } catch (e) {
          console.error('[Chat] Failed to parse status update:', e);
        }
      });

      subscribed.add(roomId);
      newSubs.push({ roomId, messageSub, statusSub });
    }

    return () => {
      // Note: we deliberately do NOT unsubscribe on rooms-change cleanup.
      // Subscriptions live for the lifetime of the STOMP session. Only the
      // active-room effect (below) triggers a re-subscribe if needed.
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rooms, connected, user?.userId]);

  // Keep an up-to-date ref of the activeRoom so the message handler above
  // can read the latest value without re-subscribing on every change.
  const activeRoomRef = useRef(activeRoom);
  useEffect(() => {
    activeRoomRef.current = activeRoom;
  }, [activeRoom]);

  // Fetch message history for a room
  const fetchRoomMessages = async (roomId, page = 0) => {
    setLoading(true);
    try {
      const res = await axios.get(`${GATEWAY_BASE_URL}/api/rooms/${roomId}/messages?page=${page}&size=50`, getAuthHeaders());
      if (res.data?.success) {
        const history = (res.data.data || []).reverse(); // Sort chronologically (oldest to newest)
        setMessages((prev) => ({
          ...prev,
          [roomId]: page === 0 ? history : [...history, ...(prev[roomId] || [])]
        }));

        // Mark room as read on server
        await axios.put(`${GATEWAY_BASE_URL}/api/rooms/${roomId}/messages/read`, {}, getAuthHeaders()).catch(() => {});
      }
      setLoading(false);
    } catch (err) {
      console.error('[Chat] Failed to fetch room messages:', err);
      setError('Failed to fetch messages');
      setLoading(false);
    }
  };

  // Open or create 1-to-1 conversation
  // CRITICAL: targetUserId MUST be a user id, not a room id. If you pass a
  // room id the backend will happily create a phantom direct room between
  // the current user and the room id, which is the "two chats" bug.
  const openDirectChat = async (targetUserId) => {
    if (!targetUserId || typeof targetUserId !== 'string') {
      console.warn('[Chat] openDirectChat called with invalid targetUserId:', targetUserId);
      return null;
    }
    if (targetUserId === currentUserId) {
      console.warn('[Chat] openDirectChat called with self id, ignoring');
      return null;
    }
    try {
      const res = await axios.post(`${GATEWAY_BASE_URL}/api/rooms/direct`, { userId: targetUserId }, getAuthHeaders());
      if (res.data?.success) {
        const room = res.data.data;
        setActiveRoom(room);
        await fetchRoomMessages(room.roomId);
        fetchMyRooms();
        return room;
      }
      return null;
    } catch (err) {
      console.error('[Chat] Failed to open direct room:', err);
      return null;
    }
  };

  // Switch to an existing room by its roomId without creating a new one.
  // Used by the Chats tab when clicking an existing conversation row.
  const openExistingRoom = async (room) => {
    if (!room || !room.roomId) {
      console.warn('[Chat] openExistingRoom called with invalid room:', room);
      return null;
    }
    // Clear unread badge when entering the room
    setRooms((prev) => prev.map((r) =>
      r.roomId === room.roomId ? { ...r, unreadCount: 0 } : r
    ));
    setActiveRoom(room);
    // Only fetch if we don't already have a populated list
    if (!messages[room.roomId] || messages[room.roomId].length === 0) {
      await fetchRoomMessages(room.roomId);
    }
    return room;
  };

  // Send message
  const sendMessage = (target, content, type = 'TEXT', mediaId = null) => {
    const roomId = typeof target === 'string' && target.length > 20 ? target : activeRoom?.roomId;
    if (!roomId) {
      console.warn('[Chat] Cannot send message: No active room');
      return null;
    }

    const clientMessageId = (crypto.randomUUID && crypto.randomUUID()) || ('msg-' + Date.now() + '-' + Math.random().toString(36).slice(2));

    const payload = {
      clientMessageId,
      senderId: currentUserId,
      type: type || 'TEXT',
      content: content || '',
      mediaId: mediaId || null
    };

    // Optimistic insert so the sender sees their own message instantly.
    const optimistic = {
      id: clientMessageId,        // temp id, server echo replaces this
      clientMessageId,
      senderId: currentUserId,
      type: payload.type,
      content: payload.content,
      mediaId: payload.mediaId,
      status: 'SENT',
      timestamp: new Date().toISOString(),
      _optimistic: true
    };
    setMessages((prev) => {
      const list = prev[roomId] || [];
      // Guard against accidental double-fire
      if (list.some((m) => m.clientMessageId === clientMessageId)) return prev;
      return { ...prev, [roomId]: [...list, optimistic] };
    });

    if (stompClientRef.current && connected) {
      stompClientRef.current.send('SEND', { destination: `/app/send-message/${roomId}` }, JSON.stringify(payload));
    } else {
      console.warn('[Chat] Not connected: dropped SEND frame');
    }

    return optimistic;
  };

  // Send read / delivery receipt
  const markMessageStatus = (roomId, messageId, status = 'READ') => {
    if (!stompClientRef.current || !connected) return;

    stompClientRef.current.send(
      'SEND',
      { destination: `/app/message-status/${roomId}` },
      JSON.stringify({ messageId, roomId, status })
    );
  };

  const getChatHistory = async (receiverId) => {
    const room = await openDirectChat(receiverId);
    return { success: true, data: room };
  };

  const clearError = () => setError(null);

  const activeMessages = activeRoom ? messages[activeRoom.roomId] || [] : [];

  const value = {
    rooms,
    activeRoom,
    setActiveRoom,
    messages: activeMessages,
    allMessages: messages,
    connected,
    loading,
    error,
    openDirectChat,
    openExistingRoom,
    fetchMyRooms,
    fetchRoomMessages,
    sendMessage,
    markMessageStatus,
    getChatHistory,
    clearError
  };

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
};

export const useChat = () => {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error('useChat must be used within a ChatProvider');
  }
  return context;
};
