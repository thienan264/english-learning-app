package com.project.englishlearning.service;

import org.springframework.stereotype.Service;

@Service
public class IeltsScoringService {

    // Thang quy đổi 40 câu hỏi sang Band 9.0 (Chuẩn IELTS)
    public double calculateBandScore(int correctAnswers, int totalQuestions) {
        if (totalQuestions == 0) return 0.0;
        
        // Trong môi trường test, nếu giảng viên chỉ nhập 4 câu hỏi, 
        // ta cần tính tỷ lệ tương đương với 40 câu thực tế để quy đổi đúng.
        int equivalentScore = (int) Math.round((double) correctAnswers / totalQuestions * 40.0);

        if (equivalentScore >= 39) return 9.0;
        if (equivalentScore >= 37) return 8.5;
        if (equivalentScore >= 35) return 8.0;
        if (equivalentScore >= 32) return 7.5;
        if (equivalentScore >= 30) return 7.0;
        if (equivalentScore >= 27) return 6.5;
        if (equivalentScore >= 23) return 6.0;
        if (equivalentScore >= 19) return 5.5;
        if (equivalentScore >= 15) return 5.0;
        if (equivalentScore >= 13) return 4.5;
        if (equivalentScore >= 10) return 4.0;
        if (equivalentScore >= 8) return 3.5;
        if (equivalentScore >= 6) return 3.0;
        return 0.0;
    }
}