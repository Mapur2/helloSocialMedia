package com.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFollowerResponse {
        private String id;
        private  String email;
        private String firstName;
        private String lastName;
        private String userName;
}
