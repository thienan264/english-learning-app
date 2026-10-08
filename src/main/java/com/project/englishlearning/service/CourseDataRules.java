package com.project.englishlearning.service;
import com.project.englishlearning.entity.Course;
public final class CourseDataRules {
 private CourseDataRules() {}
 public static void validate(Course c){
  if(c.getTitle()==null || c.getTitle().isBlank() || c.getTitle().strip().length()>200) fail("Tên khóa học phải có nội dung và tối đa 200 ký tự.");
  if(c.getPrice()!=null && c.getPrice().signum()<0) fail("Giá khóa học không được âm.");
  if(Boolean.FALSE.equals(c.getIsFree()) && (c.getPrice()==null || c.getPrice().signum()<=0)) fail("Khóa trả phí phải có giá lớn hơn 0. Chọn Miễn phí nếu không thu phí.");
  if(c.getSalePrice()!=null && (c.getSalePrice().signum()<0 || c.getPrice()==null || c.getSalePrice().compareTo(c.getPrice())>=0)) fail("Giá ưu đãi phải từ 0 và nhỏ hơn giá gốc.");
  if(c.getSaleStartDate()!=null && c.getSaleEndDate()!=null && c.getSaleEndDate().isBefore(c.getSaleStartDate())) fail("Ngày kết thúc ưu đãi không được trước ngày bắt đầu.");
  if(c.getAccessDurationMonths()!=null && c.getAccessDurationMonths()<0) fail("Thời hạn học không được âm; để trống hoặc 0 nếu không giới hạn.");
 }
 private static void fail(String message){throw new DataConstraintViolation(message);}
}
