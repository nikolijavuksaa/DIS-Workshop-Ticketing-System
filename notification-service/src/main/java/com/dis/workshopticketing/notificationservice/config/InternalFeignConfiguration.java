package com.dis.workshopticketing.notificationservice.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Configuration
public class InternalFeignConfiguration {

    @Bean
    RequestInterceptor internalJwtRequestInterceptor(@Value("${app.security.jwt.secret}") String secret) {
        return template -> template.header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken(secret));
    }

    private String internalToken(String secret) {
        try {
            JWSSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("notification-service")
                    .claim("role", "SYSTEM")
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build();

            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJwt.sign(signer);
            return signedJwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Could not sign internal JWT", exception);
        }
    }
}
