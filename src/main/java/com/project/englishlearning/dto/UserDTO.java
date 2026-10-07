package com.project.englishlearning.dto;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String role; // Chỉ mang ra những thông tin an toàn
    private String fullName;

    public UserDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    private String phone;
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    private java.time.LocalDate dateOfBirth;
    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    private String city;
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    private String learningGoal;
    public String getLearningGoal() { return learningGoal; }
    public void setLearningGoal(String learningGoal) { this.learningGoal = learningGoal; }

    private String avatarUrl;
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

}