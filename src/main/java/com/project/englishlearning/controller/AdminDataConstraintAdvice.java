package com.project.englishlearning.controller;
import com.project.englishlearning.service.DataConstraintViolation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@ControllerAdvice(assignableTypes={AdminCourseController.class,AdminCourseBuilderController.class})
public class AdminDataConstraintAdvice {
 @ExceptionHandler({DataConstraintViolation.class,org.springframework.dao.DataIntegrityViolationException.class})
 public String invalid(RuntimeException error,HttpServletRequest request,RedirectAttributes flash){
  flash.addFlashAttribute("error",error instanceof DataConstraintViolation?error.getMessage():"Không thể lưu hoặc xóa vì dữ liệu đang được sử dụng hoặc không hợp lệ. Hãy tải lại trang và kiểm tra thông tin.");
  var match=java.util.regex.Pattern.compile("^/admin/courses/(\\d+)/builder(?:/.*)?$").matcher(request.getRequestURI());
  return "redirect:"+(match.matches()?"/admin/courses/"+match.group(1)+"/builder":"/admin/courses");
 }
}
