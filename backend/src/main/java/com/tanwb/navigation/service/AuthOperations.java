package com.tanwb.navigation.service;

import com.tanwb.navigation.dto.AuthUserResponse;
import com.tanwb.navigation.dto.LoginRequest;
import com.tanwb.navigation.dto.RegisterRequest;

public interface AuthOperations {
    AuthUserResponse register(RegisterRequest request);

    AuthUserResponse login(LoginRequest request);

    AuthUserResponse currentUser();

    void logout();
}
