package com.attendance.controller;

import com.attendance.dto.AttendanceRequest;
import com.attendance.dto.MessageResponse;
import com.attendance.entity.*;
import com.attendance.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    private Student getCurrentStudent() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).get();
        return studentRepository.findByUser(user).get();
    }

    @PostMapping("/mark-attendance")
    public ResponseEntity<?> markAttendance(@RequestBody AttendanceRequest attendanceRequest, HttpServletRequest request) {
        // Capture Public IP (handling proxies like Render)
        String studentIP = request.getHeader("X-Forwarded-For");
        if (studentIP == null || studentIP.isEmpty()) {
            studentIP = request.getRemoteAddr();
        } else {
            studentIP = studentIP.split(",")[0];
        }
        
        Student student = getCurrentStudent();

        // CHECK 2 & 4: Session Active and Token Valid
        Session session = sessionRepository.findByQrTokenAndIsActiveTrue(attendanceRequest.getQrToken())
                .orElse(null);
        
        if (session == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid or Expired QR Code / Session."));
        }

        // NEW CHECK: Does this student belong to this teacher?
        if (!student.getTeacherAccessKey().equals(session.getTeacher().getAccessKey())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: You are not authorized for this Teacher's session."));
        }

        // CHECK 1: Public IP Matching (Verifies they are on the same network/hotspot from the cloud)
        if (!studentIP.equals(session.getHotspotIp()) && !studentIP.equals("127.0.0.1") && !studentIP.equals("0:0:0:0:0:0:0:1")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Outside teacher's network! Please connect to the Teacher's Hotspot."));
        }

        // CHECK 2: Is QR Code still valid? (using dynamic duration)
        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = Duration.between(session.getStartTime(), now).toMinutes();
        int allowedDuration = session.getDurationMinutes() != null ? session.getDurationMinutes() : 10;
        
        if (minutesElapsed > allowedDuration) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: QR Code has expired (Valid for " + allowedDuration + " mins only)."));
        }

        // CHECK 3: Duplicate Check
        if (attendanceRepository.findBySessionAndStudent(session, student).isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Attendance already marked for this session."));
        }

        // All checks passed -> Mark Attendance
        Attendance attendance = Attendance.builder()
                .session(session)
                .student(student)
                .markedAt(now)
                .studentIp(studentIP)
                .build();
        
        attendanceRepository.save(attendance);

        return ResponseEntity.ok(new MessageResponse("Attendance Marked Successfully! ✅"));
    }

    @GetMapping("/history")
    public List<Attendance> getHistory() {
        Student student = getCurrentStudent();
        return attendanceRepository.findByStudentOrderByMarkedAtDesc(student);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Student student = getCurrentStudent();
        List<Attendance> history = attendanceRepository.findByStudentOrderByMarkedAtDesc(student);
        
        // Group by subject and count
        Map<String, Long> stats = history.stream()
                .collect(Collectors.groupingBy(a -> a.getSession().getSubject(), Collectors.counting()));
        
        List<Map<String, Object>> result = new ArrayList<>();
        stats.forEach((subject, count) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("subject", subject);
            map.put("count", count);
            result.add(map);
        });
        
        return ResponseEntity.ok(result);
    }
}
