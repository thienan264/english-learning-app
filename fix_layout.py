import re

with open('src/main/resources/templates/admin/course-list.html', 'r') as f:
    content = f.read()

# Replace the layout wrapper
# 1. We have:
# <div class="row">
#     <div class="col-xl-3 col-lg-4 col-md-12 mb-4">
#         <div class="card shadow">
#             <div class="card-header bg-primary text-white">Thêm Khóa Học Mới</div>
#             <div class="card-body">
#                 <form ...>
#                     ...
#                     <button type="submit" class="btn btn-success w-100">Lưu khóa học</button>
#                 </form>
#             </div>
#         </div>
#     </div>

# 2. We have:
#     <div class="col-xl-9 col-lg-8 col-md-12">
#         <div class="card shadow">
#             <div class="card-header bg-dark text-white">Danh sách Khóa học</div>

# Let's use regex to extract the form contents
form_match = re.search(r'(<form th:action="@\{/admin/courses/add\}" method="post" enctype="multipart/form-data">.*?)</form>', content, re.DOTALL)
form_html = form_match.group(1)

# Modify the form submit button to be in modal footer
form_html = form_html.replace('<button type="submit" class="btn btn-success w-100">Lưu khóa học</button>', '')

# Replace the entire row structure
new_structure = f"""        <div class="card shadow mb-4">
            <div class="card-header bg-dark text-white d-flex justify-content-between align-items-center">
                <span class="fw-bold">Danh sách Khóa học</span>
                <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#addCourseModal"><i class="bi bi-plus-circle"></i> Thêm Khóa Học Mới</button>
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-bordered table-hover align-middle">"""

# Let's replace from <div class="row"> to <table class="table ...">
content = re.sub(r'<div class="row">.*<table class="table table-bordered table-hover align-middle">', new_structure, content, flags=re.DOTALL)

# Add the modal before the script tags at the bottom
modal_html = f"""
    <!-- Add Course Modal -->
    <div class="modal fade" id="addCourseModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title">Thêm Khóa Học Mới</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                {form_html}
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-success">Lưu khóa học</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
"""
content = content.replace('</body>', modal_html + '\n</body>')

# Remove the extra closing divs from the old row layout
content = content.replace('''                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>''',
'''                        </div>
                    </div>
                </div>
    </div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>''')

# Fix the button grid
old_buttons = '''                                        <div class="d-flex flex-wrap gap-1" style="min-width: 150px;">
                                            <button class="btn btn-sm btn-outline-warning fw-bold text-nowrap flex-grow-1" data-bs-toggle="modal" th:data-bs-target="'#editCourseModal' + ${course.id}">
                                                <i class="bi bi-pencil-square"></i> Sửa
                                            </button>
                                            <a th:href="@{'/admin/courses/' + ${course.id} + '/builder'}" class="btn btn-sm btn-outline-primary fw-bold text-nowrap flex-grow-1">
                                                <i class="bi bi-bricks"></i> Builder
                                            </a>
                                            <a th:href="@{'/admin/courses/' + ${course.id} + '/flashcards'}" class="btn btn-sm btn-outline-success fw-bold text-nowrap flex-grow-1">
                                                <i class="bi bi-card-text"></i> Từ vựng
                                            </a>
                                            <form th:action="@{'/admin/courses/delete/' + ${course.id}}" method="post" class="m-0 flex-grow-1 d-flex" onsubmit="return confirm('Bạn có chắc chắn muốn xóa khóa học này và tất cả dữ liệu bên trong?');">
                                                <button type="submit" class="btn btn-sm btn-outline-danger fw-bold text-nowrap w-100">
                                                    <i class="bi bi-trash"></i> Xóa
                                                </button>
                                            </form>
                                        </div>'''

new_buttons = '''                                        <div class="d-grid gap-1" style="grid-template-columns: 1fr 1fr; min-width: 180px;">
                                            <button class="btn btn-sm btn-outline-warning fw-bold text-nowrap" data-bs-toggle="modal" th:data-bs-target="'#editCourseModal' + ${course.id}">
                                                <i class="bi bi-pencil-square"></i> Sửa
                                            </button>
                                            <a th:href="@{'/admin/courses/' + ${course.id} + '/builder'}" class="btn btn-sm btn-outline-primary fw-bold text-nowrap">
                                                <i class="bi bi-bricks"></i> Builder
                                            </a>
                                            <a th:href="@{'/admin/courses/' + ${course.id} + '/flashcards'}" class="btn btn-sm btn-outline-success fw-bold text-nowrap">
                                                <i class="bi bi-card-text"></i> Từ vựng
                                            </a>
                                            <form th:action="@{'/admin/courses/delete/' + ${course.id}}" method="post" class="m-0 d-grid" onsubmit="return confirm('Bạn có chắc chắn muốn xóa khóa học này và tất cả dữ liệu bên trong?');">
                                                <button type="submit" class="btn btn-sm btn-outline-danger fw-bold text-nowrap w-100">
                                                    <i class="bi bi-trash"></i> Xóa
                                                </button>
                                            </form>
                                        </div>'''

content = content.replace(old_buttons, new_buttons)

with open('src/main/resources/templates/admin/course-list.html', 'w') as f:
    f.write(content)

print("Done")
