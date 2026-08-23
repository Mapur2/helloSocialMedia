package com.recommendationservice.dto;

import lombok.Data;

@Data
public class UserDTO {

    private String id;

    private String email;

    private String firstName;

    private String lastName;

    private String userName;
}