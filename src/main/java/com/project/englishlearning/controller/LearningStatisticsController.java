package com.project.englishlearning.controller;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.service.LearningStatisticsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Controller
@RequestMapping("/profile/statistics")
public class LearningStatisticsController {
 private final UserRepository users;private final LearningStatisticsService statistics;
 public LearningStatisticsController(UserRepository u,LearningStatisticsService s){users=u;statistics=s;}
 @GetMapping public String page(){return "student/learning-statistics";}
 @GetMapping("/data") @ResponseBody public LearningStatisticsService.Statistics data(Authentication auth){
  if(auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  var user=users.findByUsername(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));return statistics.build(user);
 }
}
