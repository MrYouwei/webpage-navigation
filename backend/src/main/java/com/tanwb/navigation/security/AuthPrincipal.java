package com.tanwb.navigation.security;

import java.io.Serializable;

public class AuthPrincipal implements Serializable {
    private final Long userId;
    private final String username;

    public AuthPrincipal(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
