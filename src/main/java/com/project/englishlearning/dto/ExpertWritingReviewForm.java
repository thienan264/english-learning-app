package com.project.englishlearning.dto;

public class ExpertWritingReviewForm {
    private Double task1Band;
    private Double task2Band;
    private String task1Feedback;
    private String task2Feedback;
    private String task1Corrections;
    private String task2Corrections;
    private String task1Suggested;
    private String task2Suggested;
    private String annotationsJson = "[]";
    public Double getTask1Band() { return task1Band; }
    public void setTask1Band(Double value) { task1Band=value; }
    public Double getTask2Band() { return task2Band; }
    public void setTask2Band(Double value) { task2Band=value; }
    public String getTask1Feedback() { return task1Feedback; }
    public void setTask1Feedback(String value) { task1Feedback=value; }
    public String getTask2Feedback() { return task2Feedback; }
    public void setTask2Feedback(String value) { task2Feedback=value; }
    public String getTask1Corrections() { return task1Corrections; }
    public void setTask1Corrections(String value) { task1Corrections=value; }
    public String getTask2Corrections() { return task2Corrections; }
    public void setTask2Corrections(String value) { task2Corrections=value; }
    public String getTask1Suggested() { return task1Suggested; }
    public void setTask1Suggested(String value) { task1Suggested=value; }
    public String getTask2Suggested() { return task2Suggested; }
    public void setTask2Suggested(String value) { task2Suggested=value; }
    public String getAnnotationsJson() { return annotationsJson; }
    public void setAnnotationsJson(String value) { annotationsJson=value; }
}
