import re

with open('src/main/resources/templates/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

start_marker = '<!-- Course List Section -->'
end_marker = '<!-- Teachers Section -->'

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

new_content = """<!-- Course List Section -->
    <div class="container mb-5 pb-5" id="courses">
        <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-end mb-4 border-bottom pb-3">
            <div class="mb-3 mb-md-0">
                <h2 class="fw-bold text-dark mb-0">Lộ Trình Khóa Học</h2>
                <p class="text-muted mb-0 mt-1">Chọn khóa học phù hợp với trình độ của bạn</p>
            </div>
            
            <!-- Filters -->
            <div class="d-flex gap-2 flex-wrap">
                <select id="levelFilter" class="form-select form-select-sm w-auto rounded-pill shadow-sm" onchange="filterCourses()">
                    <option value="all">Tất cả trình độ</option>
                    <option value="Beginner">Beginner</option>
                    <option value="Intermediate">Intermediate</option>
                    <option value="Advanced">Advanced</option>
                </select>
                <select id="priceFilter" class="form-select form-select-sm w-auto rounded-pill shadow-sm" onchange="filterCourses()">
                    <option value="all">Tất cả giá</option>
                    <option value="free">Miễn phí</option>
                    <option value="paid">Trả phí</option>
                </select>
            </div>
        </div>

        <div class="row g-4" id="courseList">
            <!-- Vòng lặp hiển thị các Khóa học -->
            <div class="col-lg-4 col-md-6 course-item" th:each="course, stat : ${courses}" 
                 th:data-level="${course.level}" 
                 th:data-price="${course.isFree == null or course.isFree ? 'free' : 'paid'}">
                <div class="card shadow-sm h-100 border-0 course-card bg-white overflow-hidden">
                    
                    <!-- Thumbnail -->
                    <div th:if="${course.thumbnailUrl != null}" class="position-relative" style="height: 180px; overflow: hidden;">
                        <img th:src="${course.thumbnailUrl}" class="w-100 h-100 object-fit-cover" alt="Course Thumbnail">
                    </div>
                    
                    <div class="card-body p-4">
                        <div class="d-flex justify-content-between align-items-start mb-3">
                            <div class="course-icon-wrapper" 
                                 th:classappend="${#strings.equalsIgnoreCase(course.level, 'Beginner') ? 'bg-success bg-opacity-10 text-success' : 
                                                 (#strings.equalsIgnoreCase(course.level, 'Intermediate') ? 'bg-warning bg-opacity-10 text-warning' : 
                                                 'bg-danger bg-opacity-10 text-danger')}">
                                <i th:if="${#strings.equalsIgnoreCase(course.level, 'Beginner')}" class="bi bi-egg"></i>
                                <i th:if="${#strings.equalsIgnoreCase(course.level, 'Intermediate')}" class="bi bi-bicycle"></i>
                                <i th:if="${#strings.equalsIgnoreCase(course.level, 'Advanced')}" class="bi bi-rocket-takeoff"></i>
                            </div>
                            <span class="custom-badge text-white fw-bold shadow-sm" 
                                  th:classappend="${#strings.equalsIgnoreCase(course.level, 'Beginner') ? 'bg-success' : 
                                                  (#strings.equalsIgnoreCase(course.level, 'Intermediate') ? 'bg-warning text-dark' : 
                                                  'bg-danger')}"
                                  th:text="${course.level}"></span>
                        </div>
                        
                        <h4 class="card-title fw-bold text-dark mb-2" th:text="${course.title}"></h4>
                        
                        <!-- Price Badges: Hide if enrolled -->
                        <div class="mb-3" th:unless="${course.isEnrolled}">
                            <span th:if="${course.isFree == null or course.isFree}" class="badge bg-success fs-6"><i class="bi bi-tag-fill me-1"></i>Miễn phí</span>
                            
                            <th:block th:if="${course.isFree != null and !course.isFree}">
                                <span th:if="${course.salePrice != null}" class="text-muted text-decoration-line-through me-2 small" th:text="${#numbers.formatDecimal(course.price, 0, 'COMMA', 0, 'POINT')} + ' đ'"></span>
                                <span class="badge bg-danger fs-6">
                                    <i class="bi bi-tag-fill me-1"></i>
                                    <span th:text="${#numbers.formatDecimal(course.salePrice != null ? course.salePrice : course.price, 0, 'COMMA', 0, 'POINT')} + ' đ'"></span>
                                </span>
                                
                                <span th:if="${course.accessDurationMonths != null and course.accessDurationMonths > 0}" class="d-block mt-1 small text-muted">
                                    <i class="bi bi-clock-history me-1"></i>Thời hạn: <span th:text="${course.accessDurationMonths}"></span> tháng
                                </span>
                            </th:block>
                        </div>
                        
                        <!-- Enrolled Badge: Show if enrolled -->
                        <div class="mb-3" th:if="${course.isEnrolled}">
                             <span class="badge bg-primary fs-6"><i class="bi bi-check-circle-fill me-1"></i>Đã sở hữu</span>
                        </div>

                        <p class="card-text text-muted mb-4" th:text="${course.description}" style="display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;"></p>
                        
                        <!-- Progress section (only shows if user is logged in & enrolled) -->
                        <div th:if="${course.isEnrolled}" class="progress-container p-3 bg-light rounded-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="small fw-bold text-dark">Tiến trình tổng:</span>
                                <span class="badge bg-primary rounded-pill" th:text="${course.completionPercentage} + '%'"></span>
                            </div>
                            <div class="progress mb-3 shadow-sm" style="height: 8px;">
                                <div class="progress-bar bg-primary progress-bar-striped progress-bar-animated" role="progressbar" 
                                     th:style="'width: ' + ${course.completionPercentage} + '%'"></div>
                            </div>
                            
                            <!-- Expiration warning if not free -->
                            <div th:if="${course.isFree != null and !course.isFree and course.expiresAt != null}" class="mt-3 border-top pt-2">
                                <th:block th:if="${course.isExpired}">
                                    <div class="text-danger small fw-bold"><i class="bi bi-exclamation-triangle-fill me-1"></i>Khóa học đã hết hạn</div>
                                </th:block>
                                <th:block th:unless="${course.isExpired}">
                                    <div class="small fw-bold" 
                                         th:classappend="${course.daysUntilExpiration <= 7 ? 'text-danger' : (course.daysUntilExpiration <= 30 ? 'text-warning' : 'text-success')}">
                                        <i class="bi bi-clock-history me-1"></i>Còn lại: <span th:text="${course.daysUntilExpiration}"></span> ngày
                                    </div>
                                </th:block>
                            </div>
                        </div>
                    </div>
                    <div class="card-footer bg-transparent border-0 p-4 pt-0">
                        <th:block th:if="${course.isEnrolled}">
                            <a th:if="${!course.isExpired}" th:href="@{'/learn/course/' + ${course.id}}" 
                               class="btn btn-primary w-100 fw-bold rounded-pill shadow-sm" style="padding: 12px 20px;">
                               <i class="bi bi-play-circle-fill me-2"></i>Tiếp tục học
                            </a>
                            
                            <form th:if="${course.isExpired}" th:action="@{'/payment/checkout/' + ${course.id}}" method="post">
                                <button type="submit" class="btn btn-warning w-100 fw-bold rounded-pill shadow-sm" style="padding: 12px 20px;">
                                    <i class="bi bi-arrow-clockwise me-2"></i>Gia hạn ngay
                                </button>
                            </form>
                        </th:block>
                        
                        <div th:unless="${course.isEnrolled}">
                            <a th:if="${course.isFree == null or course.isFree}" th:href="@{'/learn/course/' + ${course.id}}" 
                               class="btn btn-outline-primary w-100 fw-bold rounded-pill shadow-sm" style="padding: 12px 20px;">
                               <i class="bi bi-arrow-right-circle me-2"></i>Vào học ngay
                            </a>
                            
                            <th:block sec:authorize="isAuthenticated()">
                                <form th:if="${course.isFree != null and !course.isFree}" th:action="@{'/payment/checkout/' + ${course.id}}" method="post">
                                    <button type="submit" class="btn btn-danger w-100 fw-bold rounded-pill shadow-sm" style="padding: 12px 20px;">
                                        <i class="bi bi-cart-fill me-2"></i>Mua khóa học
                                    </button>
                                </form>
                            </th:block>
                            <th:block sec:authorize="!isAuthenticated()">
                                <a th:if="${course.isFree != null and !course.isFree}" th:href="@{/login}" class="btn btn-danger w-100 fw-bold rounded-pill shadow-sm" style="padding: 12px 20px;">
                                    <i class="bi bi-cart-fill me-2"></i>Mua khóa học
                                </a>
                            </th:block>
                        </div>
                    </div>
                </div>
            </div>

            <div th:if="${#lists.isEmpty(courses)}" class="col-12 text-center py-5">
                <img src="https://cdn-icons-png.flaticon.com/512/4076/4076432.png" alt="Empty" width="100" class="mb-3 opacity-50">
                <h4 class="text-muted fw-bold">Chưa có khóa học nào</h4>
                <p class="text-muted">Hệ thống đang cập nhật nội dung, vui lòng quay lại sau!</p>
            </div>
            
            <div id="noCourseFound" class="col-12 text-center py-5 d-none">
                <h4 class="text-muted fw-bold">Không tìm thấy khóa học phù hợp</h4>
                <p class="text-muted">Vui lòng thử lại với bộ lọc khác!</p>
            </div>
        </div>
    </div>
    
    <script>
        function filterCourses() {
            const level = document.getElementById('levelFilter').value;
            const price = document.getElementById('priceFilter').value;
            
            let visibleCount = 0;
            const items = document.querySelectorAll('.course-item');
            
            items.forEach(item => {
                const itemLevel = item.getAttribute('data-level');
                const itemPrice = item.getAttribute('data-price');
                
                const matchLevel = (level === 'all' || itemLevel === level);
                const matchPrice = (price === 'all' || itemPrice === price);
                
                if (matchLevel && matchPrice) {
                    item.classList.remove('d-none');
                    visibleCount++;
                } else {
                    item.classList.add('d-none');
                }
            });
            
            const noFoundMsg = document.getElementById('noCourseFound');
            if (noFoundMsg) {
                if (visibleCount === 0 && items.length > 0) {
                    noFoundMsg.classList.remove('d-none');
                } else {
                    noFoundMsg.classList.add('d-none');
                }
            }
        }
    </script>

    """

with open('src/main/resources/templates/index.html', 'w', encoding='utf-8') as f:
    f.write(content[:start_idx] + new_content + content[end_idx:])
