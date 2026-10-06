import re

with open('src/main/resources/templates/student/learning-dashboard.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Add expiration banner in Navbar
navbar_end = content.find('</nav>')
banner_html = """
    <!-- Expiration Banner -->
    <div th:if="${isExpired}" class="bg-danger text-white text-center py-2 fw-bold w-100 position-absolute" style="z-index: 1000; top: 56px;">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>Khóa học này đã hết hạn. Bạn không thể tiếp tục học hoặc làm bài. 
        <a th:href="@{'/courses/' + ${course.id}}" class="text-white text-decoration-underline ms-2">Gia hạn ngay</a>
    </div>
    <div th:if="${!isExpired and daysUntilExpiration != null and daysUntilExpiration <= 7 and daysUntilExpiration >= 0}" class="bg-warning text-dark text-center py-2 fw-bold w-100 position-absolute" style="z-index: 1000; top: 56px;">
        <i class="bi bi-clock-history me-2"></i>Khóa học sẽ hết hạn trong <span th:text="${daysUntilExpiration}"></span> ngày nữa!
        <a th:href="@{'/courses/' + ${course.id}}" class="text-dark text-decoration-underline ms-2">Gia hạn sớm</a>
    </div>
"""
content = content[:navbar_end + 6] + banner_html + content[navbar_end + 6:]

# Add overlay in content area if expired
content_start = content.find('<div class="col-md-9 col-lg-9 content-area bg-white relative">')
if content_start == -1:
    content_start = content.find('<div class="col-md-9 col-lg-9 content-area bg-white">')

if content_start != -1:
    idx = content.find('>', content_start) + 1
    overlay = """
                <div th:if="${isExpired}" class="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-center align-items-center" style="background-color: rgba(255,255,255,0.9); z-index: 500;">
                    <i class="bi bi-lock-fill text-danger mb-3" style="font-size: 4rem;"></i>
                    <h3 class="fw-bold text-dark">Khóa học đã hết hạn</h3>
                    <p class="text-muted text-center max-w-md">Tiến trình học tập của bạn đã được lưu lại. Vui lòng gia hạn để tiếp tục mở khóa nội dung bài học.</p>
                    <a th:href="@{'/courses/' + ${course.id}}" class="btn btn-warning btn-lg rounded-pill fw-bold px-5 mt-3 shadow">Gia hạn ngay</a>
                </div>
"""
    # we need to make content area relative to contain absolute overlay
    content = content.replace('<div class="col-md-9 col-lg-9 content-area bg-white">', '<div class="col-md-9 col-lg-9 content-area bg-white position-relative">')
    content_start = content.find('<div class="col-md-9 col-lg-9 content-area bg-white position-relative">')
    idx = content.find('>', content_start) + 1
    content = content[:idx] + overlay + content[idx:]


with open('src/main/resources/templates/student/learning-dashboard.html', 'w', encoding='utf-8') as f:
    f.write(content)
