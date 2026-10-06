package com.project.englishlearning.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String level;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id")
    private LearningPath learningPath;

    @Column(name = "target_band_min")
    private Double targetBandMin;

    @Column(name = "target_band_max")
    private Double targetBandMax;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "price")
    private java.math.BigDecimal price;

    @Column(name = "sale_price")
    private java.math.BigDecimal salePrice;

    @Column(name = "is_free")
    private Boolean isFree = true;

    @Column(name = "access_duration_months")
    private Integer accessDurationMonths;

    @Column(name = "sale_start_date")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime saleStartDate;

    @Column(name = "sale_end_date")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime saleEndDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LearningPath getLearningPath() {
        return learningPath;
    }

    public void setLearningPath(LearningPath learningPath) {
        this.learningPath = learningPath;
    }

    public Double getTargetBandMin() {
        return targetBandMin;
    }

    public void setTargetBandMin(Double targetBandMin) {
        this.targetBandMin = targetBandMin;
    }

    public Double getTargetBandMax() {
        return targetBandMax;
    }

    public void setTargetBandMax(Double targetBandMax) {
        this.targetBandMax = targetBandMax;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public java.math.BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(java.math.BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public void setIsFree(Boolean isFree) {
        this.isFree = isFree;
    }

    public Integer getAccessDurationMonths() {
        return accessDurationMonths;
    }

    public void setAccessDurationMonths(Integer accessDurationMonths) {
        this.accessDurationMonths = accessDurationMonths;
    }

    public LocalDateTime getSaleStartDate() {
        return saleStartDate;
    }

    public void setSaleStartDate(LocalDateTime saleStartDate) {
        this.saleStartDate = saleStartDate;
    }

    public LocalDateTime getSaleEndDate() {
        return saleEndDate;
    }

    public void setSaleEndDate(LocalDateTime saleEndDate) {
        this.saleEndDate = saleEndDate;
    }
}