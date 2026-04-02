package com.geoattendance.repository;

import com.geoattendance.model.Student;
import com.geoattendance.model.Student.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByStudentId(String studentId);
    boolean existsByEmail(String email);
    boolean existsByStudentId(String studentId);
    List<Student> findByStatus(StudentStatus status);
    long countByStatus(StudentStatus status);
}
