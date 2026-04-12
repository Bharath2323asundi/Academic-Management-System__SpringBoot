package com.attendance.controller;

import com.attendance.dto.MessageResponse;
import com.attendance.dto.SessionRequest;
import com.attendance.entity.*;
import com.attendance.repository.*;
import com.attendance.service.PdfService;
import com.attendance.service.QrCodeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

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
        User user = userRepository.findByEmail(email).get();
        return teacherRepository.findByUser(user).get();
    }

    @GetMapping("/pending-students")
    public List<Student> getPendingStudents() {
        return studentRepository.findByIsApprovedFalse();
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
        Session session = sessionRepository.findById(id).get();
        session.setActive(false);
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);
        return ResponseEntity.ok(new MessageResponse("Session ended."));
    }

    @GetMapping("/session/{id}/attendance")
    public List<Attendance> getSessionAttendance(@PathVariable Long id) {
        Session session = sessionRepository.findById(id).get();
        return attendanceRepository.findBySession(session);
    }

    @GetMapping("/report/{id}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
        try {
            Session session = sessionRepository.findById(id).get();
            List<Attendance> attendanceList = attendanceRepository.findBySession(session);
            byte[] pdfContents = pdfService.generateAttendanceReport(session, attendanceList);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "attendance_report.pdf");
            return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
