package com.marketplace.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AuthRegisterRequest {
    private String email;
    private String password;
    private Boolean isSeller;
}