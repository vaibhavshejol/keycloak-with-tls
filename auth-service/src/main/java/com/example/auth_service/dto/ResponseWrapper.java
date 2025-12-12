package com.example.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseWrapper {

     private DataWrapper data;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataWrapper {
        private KeycloakInfo keycloakInfo;
    }
    
    // Constructor to maintain backward compatibility
    public ResponseWrapper(KeycloakInfo keycloakInfo) {
        this.data = new DataWrapper(keycloakInfo);
    }
}
