package com.geoattendance.repository;

import com.geoattendance.model.Attendance;
import com.geoattendance.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    Optional<Attendance> findByStudentAndDate(Student student, LocalDate date);
    
    boolean existsByStudentAndDate(Student student, LocalDate date);
    
    List<Attendance> findByStudentOrderByDateDesc(Student student);
    
    List<Attendance> findByDateOrderByMarkedAtDesc(LocalDate date);
    
    long countByDate(LocalDate date);
    
    @Query("SELECT COUNT(DISTINCT a.student) FROM Attendance a WHERE a.date = :date AND a.status = 'present'")
    long countPresentByDate(@Param("date") LocalDate date);
    
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student WHERE a.date = :date ORDER BY a.markedAt DESC")
    List<Attendance> findByDateWithStudent(@Param("date") LocalDate date);
    
    @Query("SELECT a FROM Attendance a WHERE a.student = :student ORDER BY a.date DESC")
    List<Attendance> findStudentHistory(@Param("student") Student student);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student = :student AND a.status = 'present'")
    long countPresentByStudent(@Param("student") Student student);
}
