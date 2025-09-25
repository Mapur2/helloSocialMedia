package com.chatservice.service;

import com.chatservice.dto.ChatMessage;
import com.chatservice.entity.Message;
import com.chatservice.repository.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepo messageRepo;

    public List<Message> messages(String senderId, String receiverId){
        return messageRepo.findConversation(senderId,receiverId);
    }

    public Message saveMessage(ChatMessage chatMessage){
        Message message = new Message();
        message.setSenderId(chatMessage.getSenderId());
        message.setReceiverId(chatMessage.getReceiverId());
        message.setContent(chatMessage.getContent());
        message.setTimestamp(LocalDateTime.now());
        message.setStatus("SENT");
        messageRepo.save(message);
        return  message;
    }
}
