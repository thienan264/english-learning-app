import re

with open('src/main/java/com/project/englishlearning/controller/AdminCourseController.java', 'r') as f:
    content = f.read()

# Add dependencies
if 'import com.project.englishlearning.repository.NotificationRepository;' not in content:
    content = content.replace('import org.springframework.stereotype.Controller;', 
                              'import org.springframework.stereotype.Controller;\nimport com.project.englishlearning.repository.NotificationRepository;\nimport com.project.englishlearning.repository.UserRepository;\nimport com.project.englishlearning.entity.Notification;\nimport java.util.List;')

# Add to constructor
constructor_pattern = r'(public AdminCourseController\(CourseService courseService,\s*com.project.englishlearning.repository.CourseOrderRepository courseOrderRepository\)\s*\{)'
new_constructor = r'''private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public AdminCourseController(CourseService courseService,
                                 com.project.englishlearning.repository.CourseOrderRepository courseOrderRepository,
                                 NotificationRepository notificationRepository,
                                 UserRepository userRepository) {
        this.courseService = courseService;
        this.courseOrderRepository = courseOrderRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
'''
content = re.sub(constructor_pattern, new_constructor, content, flags=re.DOTALL)

# Add notification generation in addCourse
add_course_method = r'(courseService\.saveCourse\(course\);)'
add_course_notif = r'''courseService.saveCourse(course);
        
        // Notify all users about the new course
        List<com.project.englishlearning.entity.User> allUsers = userRepository.findAll();
        for (com.project.englishlearning.entity.User u : allUsers) {
            Notification n = new Notification();
            n.setUser(u);
            n.setTitle("Khóa học mới: " + course.getTitle());
            n.setMessage("Hệ thống vừa ra mắt khóa học mới: " + course.getTitle() + ". Hãy khám phá ngay!");
            n.setUrl("/courses/" + course.getId());
            notificationRepository.save(n);
        }
'''
content = re.sub(add_course_method, add_course_notif, content, count=1)

with open('src/main/java/com/project/englishlearning/controller/AdminCourseController.java', 'w') as f:
    f.write(content)

print("Updated AdminCourseController")
