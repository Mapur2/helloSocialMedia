// Mock chat service for development when WebSocket server is not available
export class MockChatService {
  constructor() {
    this.connected = false;
    this.subscriptions = new Map();
    this.messageHistory = new Map();
  }

  connect(headers, onConnect, onError) {
    // Simulate connection delay
    setTimeout(() => {
      this.connected = true;
      console.log('Mock chat service connected');
      if (onConnect) onConnect();
    }, 1000);
  }

  send(destination, headers, body) {
    if (destination === '/app/chat.private') {
      try {
        const message = JSON.parse(body);
        console.log('Mock: Sending message', message);
        
        // Simulate message delivery delay
        setTimeout(() => {
          // For demo purposes, we'll echo the message back
          this.simulateReceivedMessage(message);
        }, 500);
      } catch (error) {
        console.error('Failed to parse message:', error);
      }
    }
  }

  subscribe(destination, callback) {
    this.subscriptions.set(destination, callback);
    console.log('Mock: Subscribed to', destination);
    return {
      unsubscribe: () => {
        this.subscriptions.delete(destination);
        console.log('Mock: Unsubscribed from', destination);
      }
    };
  }

  disconnect() {
    this.connected = false;
    this.subscriptions.clear();
    console.log('Mock chat service disconnected');
  }

  simulateReceivedMessage(originalMessage) {
    // Create a mock response message
    const responseMessage = {
      senderId: originalMessage.receiverId,
      receiverId: originalMessage.senderId,
      content: `Echo: ${originalMessage.content}`,
      timestamp: new Date().toISOString()
    };

    // Find the subscription for this user
    const destination = `/user/${originalMessage.senderId}/queue/messages`;
    const callback = this.subscriptions.get(destination);
    
    if (callback) {
      callback({ body: JSON.stringify(responseMessage) });
    }
  }

  // Simulate some initial message history
  getInitialMessages(userId, otherUserId) {
    return [
      {
        senderId: otherUserId,
        receiverId: userId,
        content: "Hello! This is a mock message.",
        timestamp: new Date(Date.now() - 3600000).toISOString() // 1 hour ago
      },
      {
        senderId: userId,
        receiverId: otherUserId,
        content: "Hi there! Thanks for the message.",
        timestamp: new Date(Date.now() - 1800000).toISOString() // 30 minutes ago
      }
    ];
  }
}