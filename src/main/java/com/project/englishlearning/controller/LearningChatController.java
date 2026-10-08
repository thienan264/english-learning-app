package com.project.englishlearning.controller;
import com.project.englishlearning.dto.LearningChat.*;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.service.LearningChatService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;
import java.util.*;
@Controller
public class LearningChatController {
 private final UserRepository users;private final LearningChatService chat;
 public LearningChatController(UserRepository u,LearningChatService c){users=u;chat=c;}
 private User user(Authentication a){if(a==null || !a.isAuthenticated() || "anonymousUser".equals(a.getName()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return users.findByUsername(a.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 private String slot(User u){return "learning-chat-"+u.getId();}
 @SuppressWarnings("unchecked") private List<Map<String,Object>> history(HttpSession s,User u){Object h=s.getAttribute(slot(u));return h==null?new ArrayList<>():new ArrayList<>((List<Map<String,Object>>)h);}
 @GetMapping("/chat") public String page(Authentication a){user(a);return "student/learning-chat";}
 @GetMapping("/api/learning-chat") @ResponseBody public List<Map<String,Object>> get(Authentication a,HttpSession s){var u=user(a);synchronized(s){return history(s,u);}}
 @DeleteMapping("/api/learning-chat") @ResponseBody public Map<String,Boolean> clear(Authentication a,HttpSession s){var u=user(a);synchronized(s){if(Boolean.TRUE.equals(s.getAttribute(slot(u)+"busy")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Đang trả lời");s.removeAttribute(slot(u));}return Map.of("cleared",true);}
 @PostMapping("/api/learning-chat") @ResponseBody public Reply send(@RequestBody Request request,Authentication a,HttpSession s){
  var u=user(a);String text=request.message()==null?"":request.message().trim();if(text.isEmpty() || text.length()>1500)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tin nhắn cần từ 1 đến 1500 ký tự");
  List<Map<String,Object>> h;synchronized(s){if(Boolean.TRUE.equals(s.getAttribute(slot(u)+"busy")))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Hãy chờ câu trả lời trước");s.setAttribute(slot(u)+"busy",true);h=history(s,u);}
  try{var promptHistory=h.stream().map(row->Map.of("role",String.valueOf(row.get("role")),"message",String.valueOf(row.get("message")))).toList();
  Reply reply=chat.answer(u,text,promptHistory);h.add(Map.of("role","user","message",text));h.add(Map.of("role","assistant","message",reply.message(),"actions",reply.actions(),"fallback",reply.fallback()));while(h.size()>12)h.remove(0);synchronized(s){s.setAttribute(slot(u),h);}return reply;}
  finally{synchronized(s){s.removeAttribute(slot(u)+"busy");}}
 }
}
