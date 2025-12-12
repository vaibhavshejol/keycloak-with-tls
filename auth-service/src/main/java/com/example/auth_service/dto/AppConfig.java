package com.example.auth_service.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix="keycloak-info")
@Setter
@Getter
public class AppConfig {

    //private KeycloakInfo keycloakInfo;
     private String redirectUri;
    private int terminalBstTimeZone;
    private String url;
    private boolean keycloakEnable;
    private boolean requireHttps;
    private boolean skipIssuerCheck;
    private String realm;
    private String clientId;

    public KeycloakInfo toKeycloakInfo() {
        KeycloakInfo info = new KeycloakInfo();
        info.setRedirectUri(this.redirectUri);
        info.setTerminalBstTimeZone(this.terminalBstTimeZone);
        info.setUrl(this.url);
        info.setKeycloakEnable(this.keycloakEnable);
        info.setRequireHttps(this.requireHttps);
        info.setSkipIssuerCheck(this.skipIssuerCheck);
        info.setRealm(this.realm);
        info.setClientId(this.clientId);
        return info;
    }

}
