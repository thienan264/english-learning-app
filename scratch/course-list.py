import re

with open('src/main/resources/templates/admin/course-list.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Add accessDurationMonths to Create Form
create_form_insert = """
                                <div class="mb-3">
                                    <label class="form-label">Thời hạn truy cập (Tháng)</label>
                                    <input type="number" name="accessDurationMonths" class="form-control" min="1" placeholder="Để trống nếu Vĩnh viễn">
                                    <small class="text-muted">Nhập số tháng khóa học sẽ hết hạn sau khi thanh toán.</small>
                                </div>
"""
content = content.replace('name="salePrice" class="form-control" min="0" step="1000">\n                                </div>\n                            </div>', 
                          'name="salePrice" class="form-control" min="0" step="1000">\n                                </div>' + create_form_insert + '\n                            </div>')

# Add accessDurationMonths to Edit Form
edit_form_insert = """
                                                          <div class="mb-3">
                                                              <label class="form-label fw-bold">Thời hạn truy cập (Tháng)</label>
                                                              <input type="number" name="accessDurationMonths" class="form-control" th:value="${course.accessDurationMonths}" min="1" placeholder="Vĩnh viễn">
                                                              <small class="text-muted">Nhập số tháng học viên được truy cập.</small>
                                                          </div>
"""
content = content.replace('name="salePrice" class="form-control" th:value="${course.salePrice}" min="0" step="1000">\n                                                          </div>\n                                                      </div>', 
                          'name="salePrice" class="form-control" th:value="${course.salePrice}" min="0" step="1000">\n                                                          </div>' + edit_form_insert + '\n                                                      </div>')

# Make modals static
content = content.replace('class="modal fade"', 'class="modal fade" data-bs-backdrop="static"')

with open('src/main/resources/templates/admin/course-list.html', 'w', encoding='utf-8') as f:
    f.write(content)
