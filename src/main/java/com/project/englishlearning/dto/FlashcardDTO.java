package com.project.englishlearning.dto;

public class FlashcardDTO {
    private Long id;
    private String word;
    private String meaning;
    private String example;

    public FlashcardDTO(Long id, String word, String meaning, String example) {
        this.id = id;
        this.word = word;
        this.meaning = meaning;
        this.example = example;
    }

    public Long getId() { return id; }
    public String getWord() { return word; }
    public String getMeaning() { return meaning; }
    public String getExample() { return example; }
}