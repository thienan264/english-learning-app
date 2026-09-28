package com.project.englishlearning.controller;

import com.project.englishlearning.dto.exam.ExamDTO;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/lessons/{lessonId}/exam-builder")
public class AdminExamController {

    private final LessonRepository lessonRepository;
    private final ExamPassageRepository passageRepository;
    private final QuestionGroupRepository groupRepository;
    private final QuestionRepository questionRepository;

    public AdminExamController(LessonRepository lessonRepository,
                               ExamPassageRepository passageRepository,
                               QuestionGroupRepository groupRepository,
                               QuestionRepository questionRepository) {
        this.lessonRepository = lessonRepository;
        this.passageRepository = passageRepository;
        this.groupRepository = groupRepository;
        this.questionRepository = questionRepository;
    }

    @GetMapping
    public String examBuilderPage(@PathVariable Long lessonId, Model model) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        model.addAttribute("lesson", lesson);
        // We can pass existing passages to the model later. For now, the Vue/Alpine frontend will fetch via REST if needed.
        return "admin/exam-builder";
    }

    @GetMapping("/exam-data")
    @ResponseBody
    public ExamDTO getExamData(@PathVariable Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        List<ExamPassage> passages = passageRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        List<QuestionGroup> groups = groupRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);

        ExamDTO dto = new ExamDTO();
        dto.setLessonType(lesson.getSkillType());
        dto.setMediaUrl(lesson.getMediaUrl());
        List<ExamDTO.PassageDTO> pDtoList = new ArrayList<>();
        
        for (ExamPassage p : passages) {
            ExamDTO.PassageDTO pDto = new ExamDTO.PassageDTO();
            pDto.setId(p.getId());
            pDto.setLabel(p.getLabel());
            pDto.setTitle(p.getTitle());
            pDto.setContent(p.getContent());
            pDtoList.add(pDto);
        }

        List<ExamDTO.QuestionGroupDTO> gDtoList = new ArrayList<>();
        for (QuestionGroup g : groups) {
            ExamDTO.QuestionGroupDTO gDto = new ExamDTO.QuestionGroupDTO();
            gDto.setId(g.getId());
            gDto.setQuestionType(g.getQuestionType().name());
            gDto.setInstruction(g.getInstruction());
            gDto.setQuestionRange(g.getQuestionRange());
            gDto.setMetadata(g.getMetadata());
            if (g.getMetadata() != null && g.getMetadata().containsKey("passageIndex")) {
                Object pIdx = g.getMetadata().get("passageIndex");
                if (pIdx instanceof Number) {
                    gDto.setPassageIndex(((Number) pIdx).intValue());
                }
            }
            
            List<ExamDTO.ExamQuestionDTO> qDtoList = new ArrayList<>();
            for (Question q : g.getQuestions()) {
                ExamDTO.ExamQuestionDTO qDto = new ExamDTO.ExamQuestionDTO();
                qDto.setId(q.getId());
                qDto.setQuestionText(q.getQuestionText());
                qDto.setCorrectAnswer(q.getCorrectAnswer());
                qDto.setAcceptedAnswers(q.getAcceptedAnswers());
                
                List<ExamDTO.ExamAnswerDTO> aDtoList = new ArrayList<>();
                for (Answer a : q.getAnswers()) {
                    ExamDTO.ExamAnswerDTO aDto = new ExamDTO.ExamAnswerDTO();
                    aDto.setId(a.getId());
                    aDto.setLabel(a.getLabel());
                    aDto.setAnswerText(a.getAnswerText());
                    aDto.setIsCorrect(a.getIsCorrect());
                    aDtoList.add(aDto);
                }
                qDto.setAnswers(aDtoList);
                qDtoList.add(qDto);
            }
            gDto.setQuestions(qDtoList);
            gDtoList.add(gDto);
        }

        dto.setPassages(pDtoList);
        dto.setQuestionGroups(gDtoList);
        return dto;
    }

    @PostMapping("/upload-audio")
    @ResponseBody
    public ResponseEntity<?> uploadAudio(@PathVariable Long lessonId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Lesson"));
            
            // Validate file
            if (file.isEmpty() || file.getOriginalFilename() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "File không hợp lệ"));
            }

            // Save file locally using absolute path to project root
            String uploadsDir = System.getProperty("user.dir") + "/uploads/";
            java.io.File dir = new java.io.File(uploadsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            java.io.File dest = new java.io.File(dir, fileName);
            
            // Use nio Files.copy instead of transferTo for safety with relative paths
            java.nio.file.Files.copy(file.getInputStream(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Set URL
            String mediaUrl = "/uploads/" + fileName;
            lesson.setMediaUrl(mediaUrl);
            lessonRepository.save(lesson);
            
            return ResponseEntity.ok(Map.of("message", "Upload thành công", "mediaUrl", mediaUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi upload: " + e.getMessage()));
        }
    }

    @PostMapping("/save")
    @ResponseBody
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> saveExamStructure(@PathVariable Long lessonId, @RequestBody ExamDTO examDTO) {
        try {
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Lesson ID: " + lessonId));
            
            List<QuestionGroup> oldGroups = groupRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
            groupRepository.deleteAll(oldGroups);

            List<ExamPassage> oldPassages = passageRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
            passageRepository.deleteAll(oldPassages);

            List<ExamPassage> savedPassages = new ArrayList<>();
            if (examDTO.getPassages() != null) {
                int pIndex = 1;
                for (ExamDTO.PassageDTO pDto : examDTO.getPassages()) {
                    ExamPassage passage = new ExamPassage();
                    passage.setLesson(lesson);
                    passage.setLabel(pDto.getLabel());
                    passage.setTitle(pDto.getTitle());
                    passage.setContent(pDto.getContent());
                    passage.setOrderIndex(pIndex++);
                    savedPassages.add(passageRepository.save(passage));
                }
            }

            if (examDTO.getQuestionGroups() != null) {
                int gIndex = 1;
                int globalQIndex = 1;
                for (ExamDTO.QuestionGroupDTO gDto : examDTO.getQuestionGroups()) {
                    QuestionGroup group = new QuestionGroup();
                    group.setLesson(lesson);
                    
                    if (gDto.getPassageIndex() != null && gDto.getPassageIndex() >= 0 && gDto.getPassageIndex() < savedPassages.size()) {
                        Map<String, Object> metadata = gDto.getMetadata() != null ? gDto.getMetadata() : new java.util.HashMap<>();
                        metadata.put("passageIndex", gDto.getPassageIndex());
                        metadata.put("passageId", savedPassages.get(gDto.getPassageIndex()).getId());
                        gDto.setMetadata(metadata);
                    }
                    
                    if (gDto.getQuestionType() == null || gDto.getQuestionType().isEmpty()) {
                        throw new IllegalArgumentException("Dạng câu hỏi không được để trống ở nhóm " + gIndex);
                    }
                    group.setQuestionType(QuestionType.valueOf(gDto.getQuestionType()));
                    group.setInstruction(gDto.getInstruction());
                    group.setQuestionRange(gDto.getQuestionRange());
                    group.setMetadata(gDto.getMetadata());
                    group.setOrderIndex(gIndex++);
                    
                    if (gDto.getQuestions() != null) {
                        for (ExamDTO.ExamQuestionDTO qDto : gDto.getQuestions()) {
                            Question q = new Question();
                            q.setLesson(lesson);
                            q.setQuestionGroup(group);
                            q.setQuestionText(qDto.getQuestionText());
                            q.setQuestionType(group.getQuestionType().name());
                            q.setCorrectAnswer(qDto.getCorrectAnswer());
                            q.setAcceptedAnswers(qDto.getAcceptedAnswers());
                            q.setOrderIndex(globalQIndex++);
                            
                            if (qDto.getAnswers() != null) {
                                for (ExamDTO.ExamAnswerDTO aDto : qDto.getAnswers()) {
                                    Answer a = new Answer();
                                    a.setQuestion(q);
                                    a.setLabel(aDto.getLabel());
                                    a.setAnswerText(aDto.getAnswerText());
                                    a.setIsCorrect(aDto.getIsCorrect() != null ? aDto.getIsCorrect() : false);
                                    q.getAnswers().add(a);
                                }
                            }
                            group.getQuestions().add(q);
                        }
                    }
                    groupRepository.save(group);
                }
            }

            // Force flush to catch DB constraint exceptions inside this try-catch block!
            passageRepository.flush();
            groupRepository.flush();

            return ResponseEntity.ok(Map.of("message", "Đã lưu đề thi thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi lưu: " + e.getMessage()));
        }
    }
}
