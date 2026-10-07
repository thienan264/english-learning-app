package com.project.englishlearning.controller;

import com.project.englishlearning.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class AccountNavbarAdvice {
    private final UserRepository users;

    public AccountNavbarAdvice(UserRepository users) {
        this.users = users;
    }

    @ModelAttribute
    public void avatar(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            users.findByUsername(authentication.getName())
                .ifPresent(user -> model.addAttribute("accountAvatarUrl", user.getAvatarUrl()));
        }
    }
}
