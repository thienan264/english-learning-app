package com.project.englishlearning.controller;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/writing")
public class AdminWritingController {

    private final WritingTaskRepository writingTaskRepository;
    private final WritingTaskImageRepository writingTaskImageRepository;
    private final WritingExamRepository writingExamRepository;
    private final LessonRepository lessonRepository;

    public AdminWritingController(WritingTaskRepository writingTaskRepository,
                                  WritingTaskImageRepository writingTaskImageRepository,
                                  WritingExamRepository writingExamRepository,
                                  LessonRepository lessonRepository) {
        this.writingTaskRepository = writingTaskRepository;
        this.writingTaskImageRepository = writingTaskImageRepository;
        this.writingExamRepository = writingExamRepository;
        this.lessonRepository = lessonRepository;
    }

    // ==================== TASK BANK ====================

    @GetMapping("/tasks")
    public String taskBank(Model model) {
        List<WritingTask> task1List = writingTaskRepository.findByTaskTypeOrderByCreatedAtDesc("TASK_1");
        List<WritingTask> task2List = writingTaskRepository.findByTaskTypeOrderByCreatedAtDesc("TASK_2");
        model.addAttribute("task1List", task1List);
        model.addAttribute("task2List", task2List);
        return "admin/writing-task-bank";
    }

    @GetMapping("/tasks/create")
    public String createTaskForm(@RequestParam(required = false, defaultValue = "TASK_1") String type, Model model) {
        model.addAttribute("taskType", type);
        return "admin/writing-task-form";
    }

    @PostMapping("/tasks/save")
    public String saveTask(@RequestParam String taskType,
                           @RequestParam String title,
                           @RequestParam String instruction,
                           @RequestParam(required = false) Integer minWords,
                           @RequestParam(required = false) Integer timeLimitMinutes,
                           @RequestParam(required = false) MultipartFile[] images,
                           RedirectAttributes redirectAttributes) {
        try {
            WritingTask task = new WritingTask();
            task.setTaskType(taskType);
            task.setTitle(title);
            task.setInstruction(instruction);
            task.setMinWords(minWords != null ? minWords : ("TASK_1".equals(taskType) ? 150 : 250));
            task.setTimeLimitMinutes(timeLimitMinutes != null ? timeLimitMinutes : ("TASK_1".equals(taskType) ? 20 : 40));
            writingTaskRepository.save(task);

            // Handle image uploads for Task 1
            if (images != null && "TASK_1".equals(taskType)) {
                String uploadsDir = System.getProperty("user.dir") + "/uploads/writing/";
                File dir = new File(uploadsDir);
                if (!dir.exists()) dir.mkdirs();

                int orderIdx = 1;
                for (MultipartFile img : images) {
                    if (img.isEmpty()) continue;
                    String fileName = System.currentTimeMillis() + "_" + img.getOriginalFilename().replaceAll("[^a-zA-Z0-9.\\-]", "_");
                    File dest = new File(dir, fileName);
                    Files.copy(img.getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    WritingTaskImage taskImage = new WritingTaskImage();
                    taskImage.setWritingTask(task);
                    taskImage.setImageUrl("/uploads/writing/" + fileName);
                    taskImage.setOrderIndex(orderIdx++);
                    writingTaskImageRepository.save(taskImage);
                }
            }

            redirectAttributes.addFlashAttribute("success", "Đã tạo đề bài thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo đề: " + e.getMessage());
        }
        return "redirect:/admin/writing/tasks";
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        writingTaskRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa đề bài.");
        return "redirect:/admin/writing/tasks";
    }

    // Compatibility for bookmarks: compose Writing from the course lesson now.
    @GetMapping("/exams")
    public String examList() {
        return "redirect:/admin/courses";
    }

    // ==================== SPECIFIC WRITING BUILDER ====================
    @GetMapping("/lessons/{lessonId}/writing-builder")
    public String writingBuilder(@PathVariable Long lessonId, Model model) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        model.addAttribute("lesson", lesson);
        Course course=lesson.getCourse()!=null?lesson.getCourse():lesson.getModule()!=null?lesson.getModule().getCourse():null;
        model.addAttribute("backUrl",course!=null?"/admin/courses/"+course.getId()+"/builder":"/admin/exams");
        model.addAttribute("backLabel",course!=null?"Quay lại lộ trình khóa học":"Quay lại Ngân hàng đề");
        
        WritingExam exam = writingExamRepository.findByLessonId(lessonId).orElse(null);
        model.addAttribute("exam", exam);
        
        List<WritingTask> task1List = writingTaskRepository.findByTaskTypeOrderByCreatedAtDesc("TASK_1");
        List<WritingTask> task2List = writingTaskRepository.findByTaskTypeOrderByCreatedAtDesc("TASK_2");
        model.addAttribute("task1List", task1List);
        model.addAttribute("task2List", task2List);
        
        return "admin/writing-exam-builder";
    }

    @PostMapping("/lessons/{lessonId}/writing-builder/save")
    public String saveSpecificExam(@PathVariable Long lessonId,
                                   @RequestParam Long task1Id,
                                   @RequestParam Long task2Id,
                                   @RequestParam(required = false) Integer totalTimeMinutes,
                                   RedirectAttributes redirectAttributes) {
        try {
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
            WritingTask task1 = writingTaskRepository.findById(task1Id).orElseThrow();
            WritingTask task2 = writingTaskRepository.findById(task2Id).orElseThrow();

            WritingExam exam = writingExamRepository.findByLessonId(lessonId).orElse(new WritingExam());
            exam.setLesson(lesson);
            // Default to lesson title if writing exam title is needed, or just let it be the same
            exam.setTitle(lesson.getTitle()); 
            exam.setTask1(task1);
            exam.setTask2(task2);
            exam.setTotalTimeMinutes(totalTimeMinutes != null ? totalTimeMinutes : 60);
            writingExamRepository.save(exam);

            redirectAttributes.addFlashAttribute("success", "Đã lưu đề thi Writing thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/writing/lessons/"+lessonId+"/writing-builder";
    }
}
