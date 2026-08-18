package com.tanwb.navigation.controller;

import com.tanwb.navigation.common.ApiResponse;
import com.tanwb.navigation.dto.SaveNavRequest;
import com.tanwb.navigation.dto.SaveNavResponse;
import com.tanwb.navigation.security.AuthPrincipal;
import com.tanwb.navigation.service.NavDataOperations;
import com.tanwb.navigation.service.SecurityService;
import javax.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nav")
public class NavDataController {
    private final NavDataOperations navDataService;
    private final SecurityService securityService;

    public NavDataController(NavDataOperations navDataService, SecurityService securityService) {
        this.navDataService = navDataService;
        this.securityService = securityService;
    }

    @GetMapping("/data")
    public ApiResponse<Map<String, Object>> data() {
        AuthPrincipal user = securityService.requireUser();
        return ApiResponse.ok("成功", navDataService.getOrCreateData(user.getUserId()));
    }

    @PostMapping("/data")
    public ApiResponse<SaveNavResponse> save(@Valid @RequestBody SaveNavRequest request) {
        AuthPrincipal user = securityService.requireUser();
        return ApiResponse.ok("保存成功", navDataService.saveData(user.getUserId(), request.getNavData()));
    }

    @PutMapping("/data")
    public ApiResponse<SaveNavResponse> update(@Valid @RequestBody SaveNavRequest request) {
        AuthPrincipal user = securityService.requireUser();
        return ApiResponse.ok("保存成功", navDataService.saveData(user.getUserId(), request.getNavData()));
    }
}
