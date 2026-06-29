package com.dis.workshopticketing.notificationservice.controller;

import com.dis.workshopticketing.notificationservice.dto.NotificationResponse;
import com.dis.workshopticketing.notificationservice.dto.UnreadCountResponse;
import com.dis.workshopticketing.notificationservice.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getAll(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.getAll(userId(jwt));
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.unreadCount(userId(jwt));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return notificationService.markRead(userId(jwt), id);
    }

    @PostMapping("/read-all")
    public List<NotificationResponse> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.markAllRead(userId(jwt));
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
