package com.attendance.repository;

import com.attendance.entity.Session;
import com.attendance.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByTeacherOrderByStartTimeDesc(Teacher teacher);
    Optional<Session> findByQrTokenAndIsActiveTrue(String qrToken);
}
