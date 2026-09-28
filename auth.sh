#!/bin/bash
curl -s -c cookies.txt http://localhost:8080/login > login.html
CSRF_TOKEN=$(grep -o '<meta name="_csrf" content="[^"]*' login.html | cut -d'"' -f4)
curl -s -b cookies.txt -c cookies.txt -d "username=admin&password=admin" -d "_csrf=${CSRF_TOKEN}" http://localhost:8080/login > /dev/null
curl -s -b cookies.txt "http://localhost:8080/admin/lessons/3/exam-builder" | grep -i "NỘI DUNG BÀI ĐỌC"
