package com.project.englishlearning.dto.exam;

import java.util.List;
import java.util.Map;

public class ExamDTO {
    private String lessonType;
    private String mediaUrl;
    private List<PassageDTO> passages;
    private List<QuestionGroupDTO> questionGroups;

    public String getLessonType() { return lessonType; }
    public void setLessonType(String lessonType) { this.lessonType = lessonType; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    
    public List<PassageDTO> getPassages() { return passages; }
    public void setPassages(List<PassageDTO> passages) { this.passages = passages; }
    public List<QuestionGroupDTO> getQuestionGroups() { return questionGroups; }
    public void setQuestionGroups(List<QuestionGroupDTO> questionGroups) { this.questionGroups = questionGroups; }

    public static class PassageDTO {
        private Long id;
        private String label;
        private String title;
        private String content;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class QuestionGroupDTO {
        private Long id;
        private Integer passageIndex; // Link to passage
        private String questionType;
        private String instruction;
        private String questionRange;
        private Map<String, Object> metadata;
        private List<ExamQuestionDTO> questions;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getPassageIndex() { return passageIndex; }
        public void setPassageIndex(Integer passageIndex) { this.passageIndex = passageIndex; }
        public String getQuestionType() { return questionType; }
        public void setQuestionType(String questionType) { this.questionType = questionType; }
        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }
        public String getQuestionRange() { return questionRange; }
        public void setQuestionRange(String questionRange) { this.questionRange = questionRange; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        public List<ExamQuestionDTO> getQuestions() { return questions; }
        public void setQuestions(List<ExamQuestionDTO> questions) { this.questions = questions; }
    }

    public static class ExamQuestionDTO {
        private Long id;
        private String questionText;
        private String correctAnswer;
        private List<String> acceptedAnswers;
        private List<ExamAnswerDTO> answers;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public String getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
        public List<String> getAcceptedAnswers() { return acceptedAnswers; }
        public void setAcceptedAnswers(List<String> acceptedAnswers) { this.acceptedAnswers = acceptedAnswers; }
        public List<ExamAnswerDTO> getAnswers() { return answers; }
        public void setAnswers(List<ExamAnswerDTO> answers) { this.answers = answers; }
    }

    public static class ExamAnswerDTO {
        private Long id;
        private String label;
        private String answerText;
        private Boolean isCorrect;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getAnswerText() { return answerText; }
        public void setAnswerText(String answerText) { this.answerText = answerText; }
        public Boolean getIsCorrect() { return isCorrect; }
        public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    }
}
