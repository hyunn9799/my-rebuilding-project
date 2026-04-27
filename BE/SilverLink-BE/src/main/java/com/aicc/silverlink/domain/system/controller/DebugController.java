package com.aicc.silverlink.domain.system.controller;

import com.aicc.silverlink.domain.notification.dto.NotificationDto.SummaryResponse;
import com.aicc.silverlink.domain.notification.entity.Notification;
import com.aicc.silverlink.domain.notification.repository.NotificationRepository;
import com.aicc.silverlink.domain.notification.service.NotificationService;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @GetMapping("/emergency-test")
    public ResponseEntity<?> testEmergencyNotification(@RequestParam(defaultValue = "1") Long userId) {
        Map<String, Object> result = new HashMap<>();
        log.info("📢 [DEBUG API] Starting Emergency Notification Check for userId={}...", userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        // 1. Create
        Long fakeAlertId = System.currentTimeMillis(); // Unique ID
        String elderlyName = "DebugUser";
        String severity = "Critical";
        String linkUrl = "/guardian/alerts/" + fakeAlertId;

        Notification created = notificationService.createEmergencyNewNotification(
                userId, fakeAlertId, elderlyName, severity, linkUrl);

        result.put("created_id", created.getId());
        result.put("created_ref_id", created.getReferenceId());
        result.put("created_type", created.getNotificationType());

        // 2. Fetch Recent
        List<SummaryResponse> recentList = notificationService.getRecentNotifications(userId, 10);

        boolean found = false;
        SummaryResponse foundDto = null;
        for (SummaryResponse dto : recentList) {
            if (dto.getNotificationId().equals(created.getId())) {
                found = true;
                foundDto = dto;
                break;
            }
        }

        result.put("found_in_recent", found);
        result.put("found_dto", foundDto);

        if (found) {
            result.put("ref_id_match", fakeAlertId.equals(foundDto.getReferenceId()));
        }

        return ResponseEntity.ok(result);
    }
}
