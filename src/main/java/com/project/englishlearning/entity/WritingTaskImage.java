package com.project.englishlearning.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "writing_task_images")
public class WritingTaskImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writing_task_id", nullable = false)
    private WritingTask writingTask;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "caption")
    private String caption; // Optional description like "Figure 1: Energy consumption by sector"

    @Column(name = "order_index")
    private Integer orderIndex = 1;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WritingTask getWritingTask() { return writingTask; }
    public void setWritingTask(WritingTask writingTask) { this.writingTask = writingTask; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
