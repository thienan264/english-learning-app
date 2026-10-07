import re

with open('src/main/resources/templates/index.html', 'r') as f:
    content = f.read()

style_to_add = """
    <style>
        /* Prevent carousel layout shift by setting a fixed minimum height */
        #saleCoursesCarousel .carousel-item .card {
            min-height: 540px;
            display: flex;
            flex-direction: column;
        }
        #saleCoursesCarousel .carousel-item .card-body {
            display: flex;
            flex-direction: column;
            justify-content: center;
            flex: 1;
        }
    </style>
"""

# Insert the style just before the </head> tag, or before the offers section
# Let's put it right above <div id="saleCoursesCarousel"
if '<div id="saleCoursesCarousel"' in content:
    content = content.replace('<div id="saleCoursesCarousel"', style_to_add + '\n            <div id="saleCoursesCarousel"')

with open('src/main/resources/templates/index.html', 'w') as f:
    f.write(content)

print("Fixed carousel height")
