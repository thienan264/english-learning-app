import re

with open('src/main/resources/templates/layout/navbar.html', 'r') as f:
    content = f.read()

# Update badge logic to count > 2
content = content.replace('if (count > 0) {', 'if (count > 2) {')

# "khi bấm vô sẽ chuyển trang giao diện thông báo"
# Currently the bell icon has class="dropdown-toggle" maybe? No, it says:
# <a th:href="@{/notifications}" class="btn btn-primary position-relative" id="notifMenu" style="border: none;">
# It does NOT have data-bs-toggle="dropdown". It just hovers to show the list.
# But when you click it, it should go to /notifications. Since it's an <a> tag with href, it already does!
# Wait, let's make sure it doesn't have data-bs-toggle="dropdown" 
if 'data-bs-toggle="dropdown"' in content.split('id="notifMenu"')[0].split('<a ')[-1]:
    # if it has toggle, remove it
    pass

# Also, add a "Xem tất cả" button at the bottom of the dropdown
if 'Xem tất cả' not in content:
    content = content.replace('list.appendChild(li);\n                    });',
        '''list.appendChild(li);\n                    });\n                    \n                    const viewAllLi = document.createElement('li');\n                    viewAllLi.innerHTML = '<a class="dropdown-item text-center fw-bold text-primary py-2 border-top" href="/notifications">Xem tất cả thông báo</a>';\n                    list.appendChild(viewAllLi);''')

with open('src/main/resources/templates/layout/navbar.html', 'w') as f:
    f.write(content)
print("Updated navbar.html")
