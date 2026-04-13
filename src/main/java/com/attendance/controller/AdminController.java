package com.attendance.controller;

import com.attendance.dto.MessageResponse;
import com.attendance.entity.Attendance;
import com.attendance.entity.Role;
import com.attendance.entity.Teacher;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.TeacherRepository;
import com.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    @GetMapping("/teachers")
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    @PostMapping("/teachers/{id}/toggle")
    public ResponseEntity<?> toggleTeacherStatus(@PathVariable Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Teacher not found."));
        teacher.setActive(!teacher.isActive());
        teacherRepository.save(teacher);
        return ResponseEntity.ok(new MessageResponse("Teacher status updated to: " + (teacher.isActive() ? "Active" : "Inactive")));
    }

    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<?> deleteTeacher(@PathVariable Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Teacher not found."));
        
        // Associated user and records will be deleted if cascading is set up, 
        // otherwise we might need manual cleanup. 
        // Let's assume standard JPA delete for now.
        teacherRepository.delete(teacher);
        userRepository.delete(teacher.getUser());
        
        return ResponseEntity.ok(new MessageResponse("Teacher deleted successfully."));
    }

    @GetMapping("/generate-key")
    public ResponseEntity<?> generateAccessKey() {
        // Generating a unique key. Admin gives this to a teacher.
        String key = "TCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, String> response = new HashMap<>();
        response.put("key", key);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long teacherCount = teacherRepository.count();
        long studentCount = userRepository.countByRole(Role.STUDENT);
        long totalAttendance = attendanceRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("teachers", teacherCount);
        stats.put("students", studentCount);
        stats.put("attendanceCount", totalAttendance);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/attendance-logs")
    public List<Attendance> getGlobalLogs() {
        return attendanceRepository.findAll();
    }
}
