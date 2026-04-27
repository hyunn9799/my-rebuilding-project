//package com.aicc.silverlink.domain.system.entity;
//
//import com.aicc.silverlink.domain.user.entity.User;
//import jakarta.persistence.*;
//import lombok.AccessLevel;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "offline_registration_logs")
//@Getter
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//public class OfflineRegistrationLog {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "reg_log_id")
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "registrar_user_id", nullable = false)
//    private User registrar;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "target_user_id", nullable = false)
//    private User target;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "channel", nullable = false)
//    private Channel channel;
//
//    @Column(name = "memo", length = 500)
//    private String memo;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @PrePersist
//    protected void onCreate() {
//        this.createdAt = LocalDateTime.now();
//        if (this.channel == null) {
//            this.channel = Channel.CENTER_VISIT;
//        }
//    }
//
//    public enum Channel {
//        CENTER_VISIT
//    }
//}
