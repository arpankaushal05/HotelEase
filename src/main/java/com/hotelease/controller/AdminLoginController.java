package com.hotelease.controller;

import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.service.AuthService;

public class AdminLoginController extends BaseLoginController {

    public AdminLoginController(AuthService authService) {
        super(authService);
    }

    @Override
    protected boolean isRoleAllowed(User user) {
        return user.getRoles().stream().map(Role::getName).anyMatch("ADMIN"::equalsIgnoreCase);
    }

    @Override
    protected String getRoleDeniedMessage() {
        return "Only administrators can access this portal.";
    }
}
