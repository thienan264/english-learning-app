package com.project.englishlearning.config;

import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseInitializer {

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Kiểm tra xem database đã có tài khoản nào chưa
            if (userRepository.count() == 0) {
                // Tạo tài khoản Admin
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@gmail.com");
                admin.setPassword(passwordEncoder.encode("123456")); // Mật khẩu là 123456
                admin.setFullName("Quản Trị Viên");
                admin.setRole("ROLE_ADMIN");
                userRepository.save(admin);

                // Tạo tài khoản Học viên
                User student = new User();
                student.setUsername("student");
                student.setEmail("student@gmail.com");
                student.setPassword(passwordEncoder.encode("123456"));
                student.setFullName("Học Viên Test");
                student.setRole("ROLE_STUDENT");
                userRepository.save(student);

                System.out.println("Đã tự động khởi tạo 2 tài khoản: admin/123456 và student/123456");
            }
        };
    }
}