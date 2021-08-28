package com.example.fullstacktemplate.dto;


public class LoggedInResponse {

    public LoggedInResponse(Boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    private Boolean loggedIn;
    private String accessToken;
}
