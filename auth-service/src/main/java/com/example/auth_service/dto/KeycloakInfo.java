package com.example.auth_service.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class KeycloakInfo {

    private String redirectUri;
    private int terminalBstTimeZone;
    private String url;
    private boolean keycloakEnable;
    private boolean requireHttps;
    private boolean skipIssuerCheck;
    private String realm;
    private String clientId;

}
