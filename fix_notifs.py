import re

with open('src/main/resources/templates/student/notifications.html', 'r') as f:
    content = f.read()

# Replace ${n.isRead} with ${n.isRead == true} in classappends and onclicks
content = content.replace('${n.isRead} ?', '${n.isRead == true} ?')
content = content.replace('${n.isRead} +', '${n.isRead == true} +')

# Also, there's a missing "View All" button in the dropdown. 
# Let's fix that in navbar.html later.

with open('src/main/resources/templates/student/notifications.html', 'w') as f:
    f.write(content)
print("Fixed notifications.html")
