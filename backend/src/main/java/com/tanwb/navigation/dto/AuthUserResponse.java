package com.tanwb.navigation.dto;

import java.util.Map;

public class AuthUserResponse {
    private Long userId;
    private String username;
    private Map<String, Object> navData;

    public AuthUserResponse() {
    }

    public AuthUserResponse(Long userId, String username, Map<String, Object> navData) {
        this.userId = userId;
        this.username = username;
        this.navData = navData;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, Object> getNavData() {
        return navData;
    }

    public void setNavData(Map<String, Object> navData) {
        this.navData = navData;
    }
}
