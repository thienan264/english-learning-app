package com.project.englishlearning.service;

import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class GraderAccountService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final WritingAssignmentService assignments;
    public GraderAccountService(UserRepository users,PasswordEncoder passwords,WritingAssignmentService assignments) { this.users=users;this.passwords=passwords;this.assignments=assignments; }
    @Transactional
    public void create(String username,String email,String fullName,String password) {
        username=username.strip();email=email.strip().toLowerCase(java.util.Locale.ROOT);fullName=fullName.strip();
        if(!username.matches("[A-Za-z0-9_]{3,50}") || email.length()>100 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || fullName.isBlank() || fullName.length()>100 || password.length()<8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nhập tên tài khoản 3–50 ký tự, email hợp lệ, họ tên và mật khẩu tối thiểu 8 ký tự (tối đa 72 byte).");
        if(users.existsByUsername(username) || users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Tên tài khoản hoặc email đã được sử dụng.");
        assignments.lockDispatch();User u=new User();u.setUsername(username);u.setEmail(email);u.setFullName(fullName);u.setPassword(passwords.encode(password));u.setRole("ROLE_GRADER");u.setStatus("ACTIVE");users.saveAndFlush(u);assignments.dispatch();
    }
    @Transactional
    public void change(Long id,String action) {
        assignments.lockDispatch();var u=users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!"ROLE_GRADER".equals(u.getRole())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tài khoản này không phải người chấm Writing.");
        switch(action) { case "lock" -> u.setStatus("LOCKED");case "unlock" -> u.setStatus("ACTIVE");case "revoke" -> u.setRole("ROLE_STUDENT");default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
        users.saveAndFlush(u);assignments.dispatch();
    }
}
