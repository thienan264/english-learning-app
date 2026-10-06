import re

with open('src/main/resources/templates/admin/course-list.html', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update Add Form
add_form_pattern = re.compile(r'(<form th:action="@\{/admin/courses/add\}" method="post">.*?)(<button type="submit" class="btn btn-success w-100">Lưu khóa học</button>)', re.DOTALL)

add_pricing_html = """
                            <div class="mb-3 form-check form-switch">
                                <input class="form-check-input" type="checkbox" role="switch" name="isFree" id="isFreeAdd" value="true" checked onchange="togglePricing('Add')">
                                <label class="form-check-label fw-bold text-success" for="isFreeAdd">Khóa học Miễn phí</label>
                            </div>
                            <div id="pricingSectionAdd" style="display: none;">
                                <div class="mb-3">
                                    <label class="form-label">Giá bán gốc (VNĐ)</label>
                                    <input type="number" name="price" id="priceAdd" class="form-control" min="0" step="1000">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Giá khuyến mãi (VNĐ)</label>
                                    <input type="number" name="salePrice" class="form-control" min="0" step="1000">
                                </div>
                            </div>
                            """

content = add_form_pattern.sub(r'\1' + add_pricing_html + r'\2', content)


# 2. Update Edit Form
edit_form_pattern = re.compile(r'(<form th:action="@\{\'/admin/courses/edit/\' \+ \$\{course\.id\}\}" method="post">.*?)(<div class="modal-footer">)', re.DOTALL)

edit_pricing_html = """
                                                      <div class="mb-3 form-check form-switch">
                                                          <input class="form-check-input" type="checkbox" role="switch" name="isFree" th:id="'isFreeEdit' + ${course.id}" value="true" th:checked="${course.isFree}" th:onchange="'togglePricingEdit(' + ${course.id} + ')'">
                                                          <label class="form-check-label fw-bold text-success" th:for="'isFreeEdit' + ${course.id}">Khóa học Miễn phí</label>
                                                      </div>
                                                      <div th:id="'pricingSectionEdit' + ${course.id}" th:style="${course.isFree} ? 'display: none;' : 'display: block;'">
                                                          <div class="mb-3">
                                                              <label class="form-label fw-bold">Giá bán gốc (VNĐ)</label>
                                                              <input type="number" name="price" th:id="'priceEdit' + ${course.id}" class="form-control" th:value="${course.price}" min="0" step="1000">
                                                          </div>
                                                          <div class="mb-3">
                                                              <label class="form-label fw-bold">Giá khuyến mãi (VNĐ)</label>
                                                              <input type="number" name="salePrice" class="form-control" th:value="${course.salePrice}" min="0" step="1000">
                                                          </div>
                                                      </div>
                                                  </div>
                                                  """

# The match group 1 includes `<div class="modal-body text-start">` ... up to `</textarea></div></div>`
# Wait, let's just insert it before `</div>\n<div class="modal-footer">`
edit_form_pattern = re.compile(r'(<textarea name="description" class="form-control" rows="3" th:text="\$\{course\.description\}"></textarea>\s*</div>)\s*(</div>\s*<div class="modal-footer">)', re.DOTALL)
content = edit_form_pattern.sub(r'\1' + '\n' + edit_pricing_html + r'\2', content)
# Wait, I already added `</div>` in `edit_pricing_html` so I should be careful not to duplicate it.
# Let's fix that.
edit_pricing_html_2 = """
                                                      <div class="mb-3 form-check form-switch">
                                                          <input class="form-check-input" type="checkbox" role="switch" name="isFree" th:id="'isFreeEdit' + ${course.id}" value="true" th:checked="${course.isFree == null || course.isFree}" th:onchange="'togglePricingEdit(' + ${course.id} + ')'">
                                                          <label class="form-check-label fw-bold text-success" th:for="'isFreeEdit' + ${course.id}">Khóa học Miễn phí</label>
                                                      </div>
                                                      <div th:id="'pricingSectionEdit' + ${course.id}" th:style="${course.isFree != null && !course.isFree} ? 'display: block;' : 'display: none;'">
                                                          <div class="mb-3">
                                                              <label class="form-label fw-bold">Giá bán gốc (VNĐ)</label>
                                                              <input type="number" name="price" th:id="'priceEdit' + ${course.id}" class="form-control" th:value="${course.price}" min="0" step="1000">
                                                          </div>
                                                          <div class="mb-3">
                                                              <label class="form-label fw-bold">Giá khuyến mãi (VNĐ)</label>
                                                              <input type="number" name="salePrice" class="form-control" th:value="${course.salePrice}" min="0" step="1000">
                                                          </div>
                                                      </div>
"""

content = edit_form_pattern.sub(r'\1' + edit_pricing_html_2 + r'\2', content)

# 3. Add toggle scripts
script_html = """
<script>
    function togglePricing(formType) {
        const isFree = document.getElementById('isFree' + formType).checked;
        const pricingSection = document.getElementById('pricingSection' + formType);
        const priceInput = document.getElementById('price' + formType);
        if (isFree) {
            pricingSection.style.display = 'none';
            priceInput.removeAttribute('required');
        } else {
            pricingSection.style.display = 'block';
            priceInput.setAttribute('required', 'required');
        }
    }
    
    function togglePricingEdit(courseId) {
        const isFree = document.getElementById('isFreeEdit' + courseId).checked;
        const pricingSection = document.getElementById('pricingSectionEdit' + courseId);
        const priceInput = document.getElementById('priceEdit' + courseId);
        if (isFree) {
            pricingSection.style.display = 'none';
            priceInput.removeAttribute('required');
        } else {
            pricingSection.style.display = 'block';
            priceInput.setAttribute('required', 'required');
        }
    }
</script>
"""

content = content.replace('</body>', script_html + '</body>')

# 4. Add "Loại phí" column to table
th_tr_pattern = re.compile(r'(<th>Trình độ</th>)\s*(<th>Thao tác</th>)', re.DOTALL)
content = th_tr_pattern.sub(r'\1\n                                    <th>Loại phí</th>\n                                    \2', content)

td_tr_pattern = re.compile(r'(<td[^>]*>\s*<span class="badge"[^>]*th:text="\$\{course\.level\}"></span>\s*</td>)\s*(<td>\s*<button)', re.DOTALL)
td_fee_html = """
                                    <td>
                                        <span class="badge bg-success" th:if="${course.isFree == null || course.isFree}">Miễn phí</span>
                                        <span class="badge bg-danger" th:if="${course.isFree != null && !course.isFree}" th:text="${#numbers.formatDecimal(course.price, 0, 'COMMA', 0, 'POINT')} + ' VNĐ'"></span>
                                    </td>
"""
content = td_tr_pattern.sub(r'\1' + td_fee_html + r'                                    \2', content)

with open('src/main/resources/templates/admin/course-list.html', 'w', encoding='utf-8') as f:
    f.write(content)

