package com.ajayaventure.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Login request payload. Uses @SerializedName so Gson reads snake_case fields
 * from clients without relying on global naming policy.
 */
public class LoginRequest {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}