package com.example.auth_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.auth_service.dto.AppConfig;
import com.example.auth_service.dto.ResponseWrapper;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    AppConfig appConfig;
    @GetMapping("/check")
    public String login() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .reduce("",(a,b)->a + " " +b);
        return "User logged in successfully ";
    }

    @GetMapping("admin")
    public String admin()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .reduce("",(a,b)-> a + " " +b);

        return "admin login with the:-" +roles.trim().toLowerCase();
    }

    @GetMapping("meta-info")
    public ResponseWrapper metaInfo()
    {
        return new ResponseWrapper(appConfig.toKeycloakInfo());
    }






}
