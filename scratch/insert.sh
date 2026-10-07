#!/bin/bash

# Extract line 1 to 134 of index.html
head -n 134 src/main/resources/templates/index.html > scratch/new_index.html

# Append My Courses container start
cat << 'INNER_EOF' >> scratch/new_index.html
    <!-- My Courses Section -->
    <th:block sec:authorize="isAuthenticated()">
        <div class="container mb-5 mt-5" id="my-courses" th:with="enrolledCourses=${courses.?[isEnrolled]}">
            <th:block th:if="${not #lists.isEmpty(enrolledCourses)}">
                <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-end mb-4 border-bottom pb-3">
                    <div class="mb-3 mb-md-0">
                        <h2 class="fw-bold text-primary mb-0"><i class="bi bi-journal-bookmark-fill me-2"></i>Khóa Học Của Tôi</h2>
                        <p class="text-muted mb-0 mt-1">Các khóa học bạn đang tham gia</p>
                    </div>
                </div>
                
                <div class="row g-4 mb-5">
                    <div class="col-lg-4 col-md-6 course-item" th:each="course : ${enrolledCourses}">
INNER_EOF

# Append the card
cat scratch/card.txt >> scratch/new_index.html

# Append My Courses container end
cat << 'INNER_EOF' >> scratch/new_index.html
                    </div>
                </div>
            </th:block>
        </div>
    </th:block>

INNER_EOF

# Append the rest of index.html from line 135 onwards
tail -n +135 src/main/resources/templates/index.html >> scratch/new_index.html

# Overwrite index.html
mv scratch/new_index.html src/main/resources/templates/index.html
