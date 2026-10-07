package com.project.englishlearning.dto;

public class CourseDTO {
    
    private Long id;
    private String title;
    private String description;
    private String level;
    
    // Progress fields
    private Boolean isEnrolled = false;
    private Double completionPercentage = 0.0;
    private Integer completedLessons = 0;
    private Integer totalLessons = 0;
    private Integer totalFlashcards = 0;
    private Integer completedFlashcards = 0;
    
    private java.math.BigDecimal price;
    private java.math.BigDecimal salePrice;
    private java.time.LocalDateTime saleStartDate;
    private java.time.LocalDateTime saleEndDate;
    private Boolean isFree;
    
    private String thumbnailUrl;
    private Integer accessDurationMonths;
    private java.time.LocalDateTime expiresAt;
    private java.time.LocalDateTime createdAt;
    private Boolean isNewCourse = false;
    private Long daysUntilExpiration;
    private Boolean isExpired = false;

    // ModelMapper bắt buộc phải có Constructor rỗng
    public CourseDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Boolean getIsEnrolled() { return isEnrolled; }
    public void setIsEnrolled(Boolean enrolled) { isEnrolled = enrolled; }
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    public Integer getCompletedLessons() { return completedLessons; }
    public void setCompletedLessons(Integer completedLessons) { this.completedLessons = completedLessons; }
    public Integer getTotalLessons() { return totalLessons; }
    public void setTotalLessons(Integer totalLessons) { this.totalLessons = totalLessons; }
    public Integer getTotalFlashcards() { return totalFlashcards; }
    public void setTotalFlashcards(Integer totalFlashcards) { this.totalFlashcards = totalFlashcards; }
    public Integer getCompletedFlashcards() { return completedFlashcards; }
    public void setCompletedFlashcards(Integer completedFlashcards) { this.completedFlashcards = completedFlashcards; }
    
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    
    public java.math.BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(java.math.BigDecimal salePrice) { this.salePrice = salePrice; }
    
    public java.time.LocalDateTime getSaleStartDate() { return saleStartDate; }
    public void setSaleStartDate(java.time.LocalDateTime saleStartDate) { this.saleStartDate = saleStartDate; }
    
    public java.time.LocalDateTime getSaleEndDate() { return saleEndDate; }
    public void setSaleEndDate(java.time.LocalDateTime saleEndDate) { this.saleEndDate = saleEndDate; }
    
    public Boolean getIsFree() { return isFree; }
    public void setIsFree(Boolean isFree) { this.isFree = isFree; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public Integer getAccessDurationMonths() { return accessDurationMonths; }
    public void setAccessDurationMonths(Integer accessDurationMonths) { this.accessDurationMonths = accessDurationMonths; }
    
    public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getIsNewCourse() { return isNewCourse; }
    public void setIsNewCourse(Boolean isNewCourse) { this.isNewCourse = isNewCourse; }
    public void setExpiresAt(java.time.LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public Long getDaysUntilExpiration() { return daysUntilExpiration; }
    public void setDaysUntilExpiration(Long daysUntilExpiration) { this.daysUntilExpiration = daysUntilExpiration; }
    
    public Boolean getIsExpired() { return isExpired; }
    public void setIsExpired(Boolean isExpired) { this.isExpired = isExpired; }
    private long purchaseCount;
    private double averageRating;
    private long reviewCount;
    public long getPurchaseCount() { return purchaseCount; }
    public void setPurchaseCount(long v) { purchaseCount = v; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double v) { averageRating = v; }
    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long v) { reviewCount = v; }
}