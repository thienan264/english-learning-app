package com.project.englishlearning.dto;

public class QuestionDTO {
    private Long id;
    private String content;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    public QuestionDTO(Long id, String content, String optionA, String optionB, String optionC, String optionD) {
        this.id = id;
        this.content = content;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
    }

    public Long getId() { return id; }
    public String getContent() { return content; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
}