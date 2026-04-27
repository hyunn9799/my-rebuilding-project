package com.aicc.silverlink;

import com.aicc.silverlink.domain.notification.dto.NotificationDto.SummaryResponse;
import com.aicc.silverlink.domain.notification.entity.Notification;
import com.aicc.silverlink.domain.notification.repository.NotificationRepository;
import com.aicc.silverlink.domain.notification.service.NotificationService;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebugEmergencyNotificationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info(
                "====================================================================================================");
        log.info("📢 [DEBUG] Starting Emergency Notification Persistence Check...");

        // 1. Find a test user (Guardian or Admin)
        // Adjust ID as needed. Assuming ID 1 exists.
        Long userId = 1L;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("❌ Test user with ID {} not found. Skipping debug.", userId);
            return;
        }
        log.info("✅ Test User found: id={}, name={}, role={}", user.getId(), user.getName(), user.getRole());

        // 2. Simulate Emergency Notification Creation
        // We simulate what EmergencyAlertEventListener does: call
        // notificationService.createEmergencyNewNotification
        Long fakeAlertId = 9999L; // Arbitrary ID
        String elderlyName = "테스트어르신";
        String severity = "응급";
        String linkUrl = "/guardian/alerts/" + fakeAlertId;

        log.info("🚀 Creating Test Emergency Notification...");
        Notification created = notificationService.createEmergencyNewNotification(
                userId, fakeAlertId, elderlyName, severity, linkUrl);
        log.info("✅ Notification Created: id={}, type={}, refId={}, refType={}",
                created.getId(), created.getNotificationType(), created.getReferenceId(), created.getReferenceType());

        // 3. Verify Persistence immediately via Repository
        Notification persisted = notificationRepository.findById(created.getId()).orElse(null);
        if (persisted != null) {
            log.info("✅ Persistence Verified: Database contains notification ID {}", persisted.getId());
        } else {
            log.error("❌ Persistence Failed: Notification ID {} not found in DB immediately after save!",
                    created.getId());
        }

        // 4. Test Query used by Frontend (getRecentNotifications ->
        // findRecentByReceiverId)
        log.info("🔍 Testing getRecentNotifications API logic...");
        List<SummaryResponse> recentList = notificationService.getRecentNotifications(userId, 10);

        boolean found = false;
        for (SummaryResponse dto : recentList) {
            if (dto.getNotificationId().equals(created.getId())) {
                found = true;
                log.info("🎯 Found in Recent List! verifying DTO fields:");
                log.info("   - notificationId: {}", dto.getNotificationId());
                log.info("   - notificationType: {}", dto.getNotificationType());
                log.info("   - referenceId: {} (Expected: {})", dto.getReferenceId(), fakeAlertId);
                log.info("   - referenceType: {}", dto.getReferenceType());

                if (fakeAlertId.equals(dto.getReferenceId())) {
                    log.info("✅ DTO referenceId matches!");
                } else {
                    log.error("❌ DTO referenceId MISMATCH!");
                }
            }
        }

        if (!found) {
            log.error("❌ Created notification NOT found in getRecentNotifications list!");
            log.info("   Current list size: {}", recentList.size());
            recentList.forEach(n -> log.info("   - id={}, type={}, date={}", n.getNotificationId(),
                    n.getNotificationType(), n.getCreatedAt()));
        }

        // Cleanup (Optional - comment out if you want to see it in UI)
        // notificationRepository.delete(created);
        // log.info("🧹 Test notification deleted.");

        log.info(
                "====================================================================================================");
    }
}
