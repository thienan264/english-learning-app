import re

with open('src/main/resources/templates/student/course-details.html', 'r') as f:
    content = f.read()

# Add Sale Countdown Timer to the course details HTML.
# Find where the button is: <button type="submit" class="btn btn-danger btn-lg ...>
buy_button_pattern = r'(<button type="submit" class="btn btn-danger btn-lg.*?</button>)'
buy_button_match = re.search(buy_button_pattern, content, re.DOTALL)

if buy_button_match:
    sale_html = """
                    <div th:if="${course.salePrice != null}" class="mb-4 d-flex justify-content-center">
                        <div th:if="${course.saleEndDate != null}" class="sale-countdown-container text-danger fw-bold fs-5 p-3 rounded d-inline-flex align-items-center w-100 justify-content-center" style="background: rgba(220, 53, 69, 0.1); border: 2px dashed #dc3545;" th:data-sale-end="${#temporals.format(course.saleEndDate, 'yyyy-MM-dd''T''HH:mm:ss')}">
                            <i class="bi bi-stopwatch me-2 fs-3 pulse-animation"></i>
                            <span>Khuyến mãi kết thúc sau: <span class="countdown-timer ms-2">Đang tính...</span></span>
                        </div>
                        <div th:if="${course.saleEndDate == null}" class="text-danger fw-bold fs-5 p-3 rounded d-inline-flex align-items-center w-100 justify-content-center" style="background: rgba(220, 53, 69, 0.1); border: 2px dashed #dc3545;">
                            <i class="bi bi-stopwatch me-2 fs-3 pulse-animation"></i>
                            <span>Ưu đãi có giới hạn! Mua ngay!</span>
                        </div>
                    </div>
"""
    # Insert sale_html right above the form containing the buy button
    form_pattern = r'(\s*<form th:if="\${course.isFree != null and !course.isFree}".*?</form>)'
    content = re.sub(form_pattern, sale_html + r'\1', content, flags=re.DOTALL)


# Add the script to handle countdown
script_html = """
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const countdownElements = document.querySelectorAll('.sale-countdown-container');
            
            function updateCountdowns() {
                const now = new Date().getTime();
                
                countdownElements.forEach(container => {
                    const saleEndStr = container.getAttribute('data-sale-end');
                    if (!saleEndStr) return;
                    
                    const endDate = new Date(saleEndStr).getTime();
                    const distance = endDate - now;
                    
                    const timerDisplay = container.querySelector('.countdown-timer');
                    if (!timerDisplay) return;
                    
                    if (distance < 0) {
                        timerDisplay.innerHTML = "Đã kết thúc";
                        container.classList.add('opacity-50');
                        const pulse = container.querySelector('.pulse-animation');
                        if (pulse) pulse.classList.remove('pulse-animation');
                    } else {
                        const days = Math.floor(distance / (1000 * 60 * 60 * 24));
                        const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
                        const seconds = Math.floor((distance % (1000 * 60)) / 1000);
                        
                        let text = "";
                        if (days > 0) text += days + " ngày ";
                        text += (hours < 10 ? "0" + hours : hours) + "h ";
                        text += (minutes < 10 ? "0" + minutes : minutes) + "m ";
                        text += (seconds < 10 ? "0" + seconds : seconds) + "s";
                        
                        timerDisplay.innerHTML = text;
                    }
                });
            }
            
            if (countdownElements.length > 0) {
                updateCountdowns();
                setInterval(updateCountdowns, 1000);
            }
        });
    </script>
    <style>
        @keyframes pulse {
            0% { transform: scale(1); opacity: 1; }
            50% { transform: scale(1.1); opacity: 0.8; }
            100% { transform: scale(1); opacity: 1; }
        }
        .pulse-animation {
            animation: pulse 1s infinite;
        }
    </style>
</body>"""

content = content.replace('</body>', script_html)

with open('src/main/resources/templates/student/course-details.html', 'w') as f:
    f.write(content)

print("Course details updated")
