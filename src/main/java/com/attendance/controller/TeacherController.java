package com.attendance.controller;

import com.attendance.dto.MessageResponse;
import com.attendance.dto.SessionRequest;
import com.attendance.entity.*;
import com.attendance.repository.*;
import com.attendance.service.PdfService;
import com.attendance.service.QrCodeService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private static final Logger logger = LoggerFactory.getLogger(TeacherController.class);

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    @Autowired
    QrCodeService qrCodeService;

    @Autowired
    PdfService pdfService;

    private Teacher getCurrentTeacher() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Error: Current user not found."));
        return teacherRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Error: Teacher profile not found."));
    }

    @GetMapping("/pending-students")
    public List<Student> getPendingStudents() {
        Teacher teacher = getCurrentTeacher();
        return studentRepository.findByIsApprovedFalseAndTeacherAccessKey(teacher.getAccessKey());
    }

    @PostMapping("/approve-student/{id}")
    public ResponseEntity<?> approveStudent(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Student not found."));
        student.setApproved(true);
        studentRepository.save(student);
        return ResponseEntity.ok(new MessageResponse("Student approved successfully."));
    }

    @PostMapping("/reject-student/{id}")
    public ResponseEntity<?> rejectStudent(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Student not found."));
        User user = student.getUser();
        studentRepository.delete(student);
        userRepository.delete(user);
        return ResponseEntity.ok(new MessageResponse("Student registration rejected."));
    }

    @PostMapping("/start-session")
    public ResponseEntity<?> startSession(@RequestBody SessionRequest sessionRequest, HttpServletRequest request) {
        Teacher teacher = getCurrentTeacher();
        
        // Capture Public IP (handling proxies like Render)
        String teacherIP = request.getHeader("X-Forwarded-For");
        if (teacherIP == null || teacherIP.isEmpty()) {
            teacherIP = request.getRemoteAddr();
        } else {
            teacherIP = teacherIP.split(",")[0];
        }
        
        String qrToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Session session = Session.builder()
                .teacher(teacher)
                .subject(sessionRequest.getSubject())
                .qrToken(qrToken)
                .startTime(LocalDateTime.now())
                .hotspotIp(teacherIP)
                .isActive(true)
                .build();
        
        sessionRepository.save(session);
        
        try {
            String qrBase64 = qrCodeService.generateQrCodeBase64(qrToken);
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("qrCode", qrBase64);
            response.put("startTime", session.getStartTime());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Failed to generate QR code"));
        }
    }

    @PostMapping("/end-session/{id}")
    public ResponseEntity<?> endSession(@PathVariable Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Session not found."));
        session.setActive(false);
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);
        return ResponseEntity.ok(new MessageResponse("Session ended."));
    }

    @GetMapping("/session/{id}/attendance")
    public List<Attendance> getSessionAttendance(@PathVariable Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Session not found."));
        return attendanceRepository.findBySession(session);
    }

    @GetMapping("/report/{id}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
        try {
            Session session = sessionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            List<Attendance> attendanceList = attendanceRepository.findBySession(session);
            byte[] pdfContents = pdfService.generateAttendanceReport(session, attendanceList);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance_report_" + id + ".pdf\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .contentLength(pdfContents.length)
                    .body(pdfContents);
        } catch (Exception e) {
            logger.error("Error generating session PDF report: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/report/date/{dateStr}")
    public ResponseEntity<byte[]> downloadReportByDate(@PathVariable String dateStr) {
        try {
            Teacher teacher = getCurrentTeacher();
            LocalDate date = LocalDate.parse(dateStr);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            List<Attendance> attendanceList = attendanceRepository.findBySessionTeacherIdAndMarkedAtBetween(teacher.getId(), start, end);
            
            byte[] pdfContents = pdfService.generateGeneralReport(
                "Daily Attendance Report",
                "All Subjects",
                teacher.getUser().getName(),
                dateStr,
                attendanceList
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance_report_" + dateStr + ".pdf\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .contentLength(pdfContents.length)
                    .body(pdfContents);
        } catch (Exception e) {
            logger.error("Error generating daily PDF report: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/my-students")
    public List<Student> getMyStudents() {
        Teacher teacher = getCurrentTeacher();
        return studentRepository.findByTeacherAccessKey(teacher.getAccessKey());
    }

    @GetMapping("/history")
    public List<Session> getSessionHistory() {
        Teacher teacher = getCurrentTeacher();
        return sessionRepository.findByTeacherOrderByStartTimeDesc(teacher);
    }
}
