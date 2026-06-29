package com.dis.workshopticketing.identityservice.controller;

import com.dis.workshopticketing.identityservice.dto.CreateInstructorRequest;
import com.dis.workshopticketing.identityservice.dto.CreateUserRequest;
import com.dis.workshopticketing.identityservice.dto.ExistenceResponse;
import com.dis.workshopticketing.identityservice.dto.InstructorResponse;
import com.dis.workshopticketing.identityservice.dto.UserResponse;
import com.dis.workshopticketing.identityservice.security.CustomOAuth2UserService;
import com.dis.workshopticketing.identityservice.security.OAuth2LoginSuccessHandler;
import com.dis.workshopticketing.identityservice.service.InstructorService;
import com.dis.workshopticketing.identityservice.service.UserService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:identity_controller_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.jwt.secret=change-me-change-me-change-me-change-me"
})
@AutoConfigureMockMvc
class IdentityControllerSecurityTest {

    private static final String JWT_SECRET = "change-me-change-me-change-me-change-me";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private InstructorService instructorService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    void keepsHealthEndpointPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void keepsUserExistsEndpointPublic() throws Exception {
        when(userService.exists(7L)).thenReturn(new ExistenceResponse(7L, true));

        mockMvc.perform(get("/users/7/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.exists").value(true));

        verify(userService).exists(7L);
    }

    @Test
    void keepsInstructorExistsEndpointPublic() throws Exception {
        when(instructorService.exists(5L)).thenReturn(new ExistenceResponse(5L, true));

        mockMvc.perform(get("/instructors/5/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.exists").value(true));

        verify(instructorService).exists(5L);
    }

    @Test
    void rejectsAuthMeWithoutJwt() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, instructorService);
    }

    @Test
    void returnsAuthUserFromJwtClaims() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7", "ana@example.com", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void rejectsUserMutationWithoutJwt() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ana\",\"lastName\":\"Ivic\",\"email\":\"ana@example.com\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void rejectsInstructorMutationWithoutJwt() throws Exception {
        mockMvc.perform(post("/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Mila\",\"lastName\":\"Markovic\",\"email\":\"mila@example.com\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(instructorService);
    }

    @Test
    void createsUserWithJwt() throws Exception {
        when(userService.create(any(CreateUserRequest.class))).thenReturn(user());

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("99", "admin@example.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ana\",\"lastName\":\"Ivic\",\"email\":\"ana@example.com\",\"phone\":\"123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("ana@example.com"));

        verify(userService).create(any(CreateUserRequest.class));
    }

    @Test
    void createsInstructorWithJwt() throws Exception {
        when(instructorService.create(any(CreateInstructorRequest.class))).thenReturn(instructor());

        mockMvc.perform(post("/instructors")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("99", "admin@example.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Mila\",\"lastName\":\"Markovic\",\"email\":\"mila@example.com\",\"bio\":\"Bio\",\"expertise\":\"Java\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("mila@example.com"));

        verify(instructorService).create(any(CreateInstructorRequest.class));
    }

    private UserResponse user() {
        return new UserResponse(
                7L,
                "Ana",
                "Ivic",
                "ana@example.com",
                "123",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private InstructorResponse instructor() {
        return new InstructorResponse(
                5L,
                "Mila",
                "Markovic",
                "mila@example.com",
                "Bio",
                "Java",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private String bearerToken(String subject, String email, String role) throws JOSEException {
        return "Bearer " + jwt(subject, email, role);
    }

    private String jwt(String subject, String email, String role) throws JOSEException {
        JWSSigner signer = new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("email", email)
                .claim("role", role)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();

        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(signer);
        return signedJwt.serialize();
    }
}
