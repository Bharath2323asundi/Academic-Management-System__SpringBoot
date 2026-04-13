package com.attendance.repository;

import com.attendance.entity.Attendance;
import com.attendance.entity.Session;
import com.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findBySession(Session session);
    List<Attendance> findByStudentOrderByMarkedAtDesc(Student student);
    Optional<Attendance> findBySessionAndStudent(Session session, Student student);
    List<Attendance> findBySessionTeacherIdAndMarkedAtBetween(Long teacherId, java.time.LocalDateTime start, java.time.LocalDateTime end);
    void deleteAllByStudent(Student student);
}
