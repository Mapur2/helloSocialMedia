package com.chatservice.controller;

import com.chatservice.dto.Response;
import com.chatservice.entity.Message;
import com.chatservice.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/chats/{receiverId}")
    public ResponseEntity<Response<List<Message>>> getMessages(@PathVariable String receiverId, @RequestHeader("X-USER-ID")String senderId){
        try{
            return new ResponseEntity<>(new Response<>(true,"All messages",messageService.messages(senderId,receiverId)), HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(new Response<>(false,"Something went wrong",null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
