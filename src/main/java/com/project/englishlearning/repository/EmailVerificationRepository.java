package com.project.englishlearning.repository;

import com.project.englishlearning.entity.EmailVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from EmailVerification v where v.email = :email")
    Optional<EmailVerification> findForUpdate(@Param("email") String email);
}
