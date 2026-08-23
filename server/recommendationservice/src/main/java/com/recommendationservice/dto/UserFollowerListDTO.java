package com.recommendationservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserFollowerListDTO {

    private List<UserDTO> followers;
}