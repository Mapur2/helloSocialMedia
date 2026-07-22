package com.userservice.service;

import com.userservice.dto.*;
import com.userservice.entity.User;
import com.userservice.repository.UserRepo;
import jakarta.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private EmailService emailService;

    public UserResponse getUserProfile(String userId) {
        User user = userRepo.findById(userId).orElseThrow(()->new RuntimeException("User not found"));
        UserResponse res = new UserResponse();
        res.setEmail(user.getEmail());
        res.setId(user.getId());
        res.setPassword(user.getPassword());
        res.setFirstName(user.getFirstName());
        res.setLastName(user.getLastName());
        res.setCreatedAt(user.getCreatedAt());
        res.setUpdatedAt(user.getUpdatedAt());
        return  res;
    }

    public  User getUserByEmailId(String userId){
        return userRepo.findByEmail(userId);
    }

    public UserResponse register(RegisterRequest request) {
        if(userRepo.existsByEmail(request.getEmail()))
            throw  new IllegalArgumentException("Email already exists");
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUserName(request.getUserName());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        userRepo.save(user);

        UserResponse res = new UserResponse();
        res.setEmail(user.getEmail());
        res.setId(user.getId());
        res.setUserName(user.getUserName());
        res.setPassword(user.getPassword());
        res.setFirstName(user.getFirstName());
        res.setLastName(user.getLastName());
        res.setCreatedAt(user.getCreatedAt());
        res.setUpdatedAt(user.getUpdatedAt());

        emailService.sendSimpleEmail(request.getEmail(),"Welcome to Hello Media","Let's embark on a new journey");

        return res;
    }

    public UserResponse mapToUserResponse(User user){
        UserResponse res = new UserResponse();
        res.setEmail(user.getEmail());
        res.setId(user.getId());
        res.setPassword(user.getPassword());
        res.setFirstName(user.getFirstName());
        res.setLastName(user.getLastName());
        res.setCreatedAt(user.getCreatedAt());
        res.setUpdatedAt(user.getUpdatedAt());
        return res;
    }

    public Boolean exitsByUserId(String userId) {

        return  userRepo.existsById(userId);
    }

    public UserFollowerList getUsers(List<String> userIds){
        List<User> users =  userRepo.findAllById(userIds);
        List<UserFollowerResponse> userFollowerResponses = new ArrayList<>();
        for(User u:users){
            UserFollowerResponse userResponse = new UserFollowerResponse();
            userResponse.setEmail(u.getEmail());
            userResponse.setId(u.getId());
            userResponse.setUserName(u.getUserName());
            userResponse.setFirstName(u.getFirstName());
            userResponse.setLastName(u.getLastName());
            userFollowerResponses.add(userResponse);
        }
        return new UserFollowerList(userFollowerResponses);
    }

    public Usernames getUsernames(List<String> userIds){
        HashMap<String,String> h = new HashMap<>();

        for (String id:userIds){
            User username = userRepo.findById(id).orElse(null);
            assert username != null;
            h.put(id,username.getUserName());
        }
        Usernames usernames =new Usernames();
        usernames.setUsers(h);
        return usernames;
    }
}
