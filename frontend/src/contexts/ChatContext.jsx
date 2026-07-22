import React, { createContext, useContext, useReducer, useEffect, useRef } from 'react';
import axios from 'axios';
import { useAuth } from './AuthContext';
import { MockChatService } from '../utils/mockChatService';

// Alternative WebSocket implementation without SockJS
class StompOverWebSocket {
  constructor(url) {
    this.url = url;
    this.websocket = null;
    this.connected = false;
    this.subscriptions = new Map();
    this.messageQueue = [];
  }

  connect(headers = {}, onConnect, onError) {
    try {
      // Convert http/https URL to ws/wss for direct WebSocket connection
      const wsUrl = this.url.replace('http://', 'ws://').replace('https://', 'wss://');
      this.websocket = new WebSocket(wsUrl);
      
      this.websocket.onopen = () => {
        this.connected = true;
        console.log('WebSocket connected');
        
        // Send CONNECT frame
        this.send('CONNECT', headers);
        
        // Process queued messages
        while (this.messageQueue.length > 0) {
          const message = this.messageQueue.shift();
          this.websocket.send(message);
        }
        
        if (onConnect) onConnect();
      };
      
      this.websocket.onmessage = (event) => {
        this.handleMessage(event.data);
      };
      
      this.websocket.onclose = () => {
        this.connected = false;
        console.log('WebSocket disconnected');
      };
      
      this.websocket.onerror = (error) => {
        console.error('WebSocket error:', error);
        if (onError) onError(error);
      };
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      if (onError) onError(error);
    }
  }

  send(command, headers = {}, body = '') {
    const frame = this.buildFrame(command, headers, body);
    if (this.connected && this.websocket.readyState === WebSocket.OPEN) {
      this.websocket.send(frame);
    } else {
      this.messageQueue.push(frame);
    }
  }

  subscribe(destination, callback) {
    const id = 'sub-' + Math.random().toString(36).substr(2, 9);
    this.subscriptions.set(destination, { id, callback });
    this.send('SUBSCRIBE', { destination, id });
    return { unsubscribe: () => this.unsubscribe(id) };
  }

  unsubscribe(id) {
    this.send('UNSUBSCRIBE', { id });
    for (const [destination, sub] of this.subscriptions.entries()) {
      if (sub.id === id) {
        this.subscriptions.delete(destination);
        break;
      }
    }
  }

  disconnect() {
    if (this.websocket) {
      this.send('DISCONNECT');
      this.websocket.close();
      this.connected = false;
    }
  }

  buildFrame(command, headers, body) {
    let frame = command + '\n';
    for (const [key, value] of Object.entries(headers)) {
      frame += `${key}:${value}\n`;
    }
    frame += '\n' + body + '\0';
    return frame;
  }

  handleMessage(data) {
    // Parse STOMP frame
    const lines = data.split('\n');
    const command = lines[0];
    const headers = {};
    let bodyIndex = 1;
    
    for (let i = 1; i < lines.length; i++) {
      if (lines[i] === '') {
        bodyIndex = i + 1;
        break;
      }
      const [key, value] = lines[i].split(':');
      headers[key] = value;
    }
    
    const body = lines.slice(bodyIndex).join('\n').replace(/\0$/, '');
    
    if (command === 'MESSAGE') {
      const destination = headers.destination;
      const subscription = this.subscriptions.get(destination);
      if (subscription && subscription.callback) {
        subscription.callback({ body });
      }
    }
  }
}

const ChatContext = createContext();

const chatReducer = (state, action) => {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    case 'SET_MESSAGES':
      return { 
        ...state, 
        messages: { 
          ...state.messages, 
          [action.chatId]: action.payload 
        }, 
        loading: false 
      };
    case 'ADD_MESSAGE':
      const chatId = action.chatId;
      return {
        ...state,
        messages: {
          ...state.messages,
          [chatId]: [...(state.messages[chatId] || []), action.payload]
        }
      };
    case 'SET_ONLINE_USERS':
      return { ...state, onlineUsers: action.payload };
    case 'SET_CONNECTED':
      return { ...state, connected: action.payload };
    case 'SET_ERROR':
      return { ...state, error: action.payload, loading: false };
    case 'CLEAR_ERROR':
      return { ...state, error: null };
    default:
      return state;
  }
};

const initialState = {
  messages: {},
  onlineUsers: [],
  connected: false,
  loading: false,
  error: null
};

export const ChatProvider = ({ children }) => {
  const [state, dispatch] = useReducer(chatReducer, initialState);
  const { user, isAuthenticated } = useAuth();
  const stompClient = useRef(null);

  useEffect(() => {
    if (isAuthenticated && user) {
      connectToChat();
    } else {
      disconnectFromChat();
    }

    return () => {
      disconnectFromChat();
    };
  }, [isAuthenticated, user]);

  const connectToChat = () => {
    // Try direct WebSocket connection first, fallback to mock service
    try {
      stompClient.current = new StompOverWebSocket('ws://localhost:8084/chat');
      
      stompClient.current.connect({}, 
        () => {
          dispatch({ type: 'SET_CONNECTED', payload: true });
          
          // Subscribe to private messages
          stompClient.current.subscribe(`/user/${user.userId}/queue/messages`, (message) => {
            const receivedMessage = JSON.parse(message.body);
            const chatId = getChatId(receivedMessage.senderId, user.userId);
            dispatch({ 
              type: 'ADD_MESSAGE', 
              chatId,
              payload: receivedMessage 
            });
          });
        },
        (error) => {
          console.warn('Real WebSocket failed, using mock chat service:', error);
          // Fallback to mock service
          stompClient.current = new MockChatService();
          stompClient.current.connect({},
            () => {
              dispatch({ type: 'SET_CONNECTED', payload: true });
              
              // Subscribe to private messages with mock service
              stompClient.current.subscribe(`/user/${user.userId}/queue/messages`, (message) => {
                const receivedMessage = JSON.parse(message.body);
                const chatId = getChatId(receivedMessage.senderId, user.userId);
                dispatch({ 
                  type: 'ADD_MESSAGE', 
                  chatId,
                  payload: receivedMessage 
                });
              });
            },
            (mockError) => {
              console.error('Both real and mock chat services failed:', mockError);
              dispatch({ type: 'SET_CONNECTED', payload: false });
            }
          );
        }
      );
    } catch (error) {
      console.error('Failed to initialize chat connection:', error);
      dispatch({ type: 'SET_CONNECTED', payload: false });
    }
  };

  const disconnectFromChat = () => {
    if (stompClient.current) {
      stompClient.current.disconnect();
      dispatch({ type: 'SET_CONNECTED', payload: false });
    }
  };

  const getChatId = (userId1, userId2) => {
    return [userId1, userId2].sort().join('_');
  };

  const sendMessage = (receiverId, content) => {
    if (stompClient.current && state.connected) {
      const message = {
        senderId: user.userId,
        receiverId,
        content,
        timestamp: new Date().toISOString()
      };

      stompClient.current.send('/app/chat.private', {}, JSON.stringify(message));
      
      // Add message to local state
      const chatId = getChatId(user.userId, receiverId);
      dispatch({ 
        type: 'ADD_MESSAGE', 
        chatId,
        payload: message 
      });
    } else {
      console.warn('Cannot send message: not connected to chat server');
    }
  };

  const getChatHistory = async (receiverId) => {
    dispatch({ type: 'SET_LOADING', payload: true });
    try {
      const response = await axios.get(`http://localhost:8079/messages/chats/${receiverId}`);
      const chatId = getChatId(user.userId, receiverId);
      dispatch({ 
        type: 'SET_MESSAGES', 
        chatId,
        payload: response.data.data || [] 
      });
      return { success: true, data: response.data };
    } catch (error) {
      const errorMessage = error.response?.data?.message || 'Failed to fetch chat history';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      return { success: false, error: errorMessage };
    }
  };

  const clearError = () => {
    dispatch({ type: 'CLEAR_ERROR' });
  };

  const value = {
    ...state,
    sendMessage,
    getChatHistory,
    clearError,
    getChatId
  };

  return (
    <ChatContext.Provider value={value}>
      {children}
    </ChatContext.Provider>
  );
};

export const useChat = () => {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error('useChat must be used within a ChatProvider');
  }
  return context;
};