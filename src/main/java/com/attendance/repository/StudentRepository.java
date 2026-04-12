package com.attendance.repository;

import com.attendance.entity.Student;
import com.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser(User user);
    Optional<Student> findByVtuNo(String vtuNo);
    List<Student> findByIsApprovedFalse();
}
