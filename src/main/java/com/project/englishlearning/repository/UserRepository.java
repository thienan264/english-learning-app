package com.project.englishlearning.repository;

import com.project.englishlearning.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Query("select month(u.createdAt), count(u.id) from User u where year(u.createdAt)=year(current_date) group by month(u.createdAt)")
    java.util.List<Object[]> monthlyRegistrations();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> lockForWriting(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
}
