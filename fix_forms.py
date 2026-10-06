import re

with open("src/main/resources/templates/admin/course-list.html", "r", encoding="utf-8") as f:
    html = f.read()

# Add enctype to both forms
html = html.replace('<form action="/admin/courses/add" method="post">', '<form action="/admin/courses/add" method="post" enctype="multipart/form-data">')
html = html.replace('<form th:action="@{\'/admin/courses/edit/\' + ${course.id}}" method="post">', '<form th:action="@{\'/admin/courses/edit/\' + ${course.id}}" method="post" enctype="multipart/form-data">')

# Replace thumbnail inputs
add_thumb_old = '<input type="text" name="thumbnailUrl" class="form-control" placeholder="VD: /images/course1.jpg hoặc https://...">'
add_thumb_new = '<input type="file" name="thumbnailImage" class="form-control" accept="image/*">\n                            <small class="text-muted">Chọn ảnh từ máy (Để trống nếu không đổi)</small>'

edit_thumb_old = '<input type="text" name="thumbnailUrl" class="form-control" th:value="${course.thumbnailUrl}" placeholder="VD: /images/course1.jpg hoặc https://...">'
edit_thumb_new = '<div class="d-flex align-items-center gap-2 mb-2">\n                                                              <img th:if="${course.thumbnailUrl != null}" th:src="${course.thumbnailUrl}" style="height:40px; border-radius:4px; object-fit:cover;">\n                                                              <input type="file" name="thumbnailImage" class="form-control" accept="image/*">\n                                                          </div>\n                                                          <small class="text-muted">Chọn ảnh từ máy (Để trống nếu không muốn đổi ảnh cũ)</small>'

html = html.replace(add_thumb_old, add_thumb_new)
html = html.replace(edit_thumb_old, edit_thumb_new)

with open("src/main/resources/templates/admin/course-list.html", "w", encoding="utf-8") as f:
    f.write(html)
