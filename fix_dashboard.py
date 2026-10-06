import re

with open("src/main/resources/templates/admin/dashboard.html", "r", encoding="utf-8") as f:
    html = f.read()

# Fix the duplicate div class="row g-4"
html = html.replace('<div class="row g-4">\n    <!-- Stat Cards Row -->\n    <div class="row g-4 mb-5">', '    <!-- Stat Cards Row -->\n    <div class="row g-4 mb-5">')

# Replace the Mock Chart with Revenue Chart
html = html.replace('<!-- System Status Chart (Mock) -->', '<!-- Revenue Chart -->')
html = html.replace('<h4 class="fw-bold mb-4"><i class="bi bi-activity text-success me-2"></i>Biểu đồ Hoạt động</h4>', '<h4 class="fw-bold mb-4"><i class="bi bi-graph-up-arrow text-success me-2"></i>Doanh thu năm nay</h4>')
html = html.replace('<canvas id="activityChart" style="max-height: 250px; width: 100%;"></canvas>\n                    <p class="text-muted small mt-3 mb-0 text-center">Tương tác người dùng 7 ngày qua</p>', '<canvas id="revenueChart" style="max-height: 250px; width: 100%;"></canvas>\n                    <p class="text-muted small mt-3 mb-0 text-center">Tổng doanh thu theo tháng</p>')

# Add Top Courses table
top_courses_html = """
        <!-- Top Courses -->
        <div class="col-lg-12 mt-5">
            <h4 class="fw-bold mb-4"><i class="bi bi-trophy text-warning me-2"></i>Top Khóa học Bán chạy</h4>
            <div class="card action-card shadow-sm">
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th class="ps-4">Tên khóa học</th>
                                    <th class="text-end pe-4">Số lượt mua</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr th:each="tc : ${topCourses}">
                                    <td class="ps-4 fw-semibold" th:text="${tc[0]}"></td>
                                    <td class="text-end pe-4"><span class="badge bg-success rounded-pill px-3 py-2" th:text="${tc[1]} + ' lượt'">0</span></td>
                                </tr>
                                <tr th:if="${#lists.isEmpty(topCourses)}">
                                    <td colspan="2" class="text-center text-muted py-4">Chưa có dữ liệu</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
"""
# Insert Top Courses before the closing div of the main row
html = html.replace('    </div>\n</div>\n\n<script', f'{top_courses_html}    </div>\n</div>\n\n<script')

# Replace script
script_replacement = """
    // Date
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    document.getElementById('currentDate').textContent = new Date().toLocaleDateString('vi-VN', options);

    // Revenue Chart
    const revData = [[${monthlyRevenue}]];
    const ctx = document.getElementById('revenueChart').getContext('2d');
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'],
            datasets: [{
                label: 'Doanh thu (₫)',
                data: revData,
                backgroundColor: 'rgba(13, 110, 253, 0.7)',
                borderColor: '#0d6efd',
                borderWidth: 1,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: { 
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            if (value >= 1000000) return (value / 1000000) + 'tr';
                            if (value >= 1000) return (value / 1000) + 'k';
                            return value;
                        }
                    }
                },
                x: { grid: { display: false } }
            }
        }
    });
"""

# replace the chart script block
import re
html = re.sub(r'// Date.*}\);', script_replacement.strip(), html, flags=re.DOTALL)

with open("src/main/resources/templates/admin/dashboard.html", "w", encoding="utf-8") as f:
    f.write(html)
