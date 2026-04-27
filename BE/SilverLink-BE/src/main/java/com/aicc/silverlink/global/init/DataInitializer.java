package com.aicc.silverlink.global.init;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdministrativeDivisionRepository admRepo;
    private final ElderlyRepository elderlyRepo;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. 상담사 테스트 계정
        if (userRepository.findByLoginId("test").isEmpty()) {
            User testUser = User.builder()
                    .loginId("test")
                    .passwordHash(passwordEncoder.encode("1234")) // 비번 1234 암호화
                    .name("테스터")
                    .phone("010-1234-5678")
                    .role(Role.COUNSELOR)
                    .status(UserStatus.ACTIVE)
                    .phoneVerified(true)
                    .build();

            userRepository.save(testUser);
            System.out.println("✅ 테스트용 상담사 생성 완료: ID=test / PW=1234");
        }

        // 2. 행정구역 (서울시 중구)
        Long admCode = 1114000000L;
        AdministrativeDivision seoulJungGu = admRepo.findById(admCode).orElseGet(() -> {
            AdministrativeDivision adm = AdministrativeDivision.builder()
                    .admCode(admCode)
                    .sidoCode("11")
                    .sigunguCode("140")
                    .sidoName("서울특별시")
                    .sigunguName("중구")
                    .level(AdministrativeDivision.DivisionLevel.SIGUNGU)
                    .establishedAt(LocalDate.of(1990, 1, 1))
                    .build();
            return admRepo.save(adm);
        });

        // 3. 어르신 테스트 계정
        User elderlyUser = userRepository.findByLoginId("hong").orElse(null);
        
        if (elderlyUser == null) {
            elderlyUser = User.builder()
                    .loginId("hong")
                    .passwordHash(passwordEncoder.encode("1234"))
                    .name("홍길동")
                    .phone("010-9876-5432")
                    .role(Role.ELDERLY)
                    .status(UserStatus.ACTIVE)
                    .phoneVerified(true)
                    .build();
            userRepository.save(elderlyUser);
            System.out.println("✅ 테스트용 어르신 User 생성 완료: ID=" + elderlyUser.getId());
        }

        // Elderly 정보 확인 및 생성
        if (!elderlyRepo.existsById(elderlyUser.getId())) {
            Elderly elderly = Elderly.create(elderlyUser, seoulJungGu, LocalDate.of(1945, 8, 15), Elderly.Gender.M);
            elderly.updateAddress("세종대로 110", "101호", "04524");
            elderlyRepo.save(elderly);
            System.out.println("✅ 테스트용 어르신 Elderly 데이터 생성 완료: PK=" + elderlyUser.getId());
        } else {
             System.out.println("ℹ️ 테스트용 어르신 Elderly 데이터가 이미 존재합니다: PK=" + elderlyUser.getId());
        }
    }
}