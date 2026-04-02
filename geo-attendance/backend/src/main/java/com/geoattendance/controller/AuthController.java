package com.geoattendance.controller;

import com.geoattendance.config.JwtUtil;
import com.geoattendance.model.Admin;
import com.geoattendance.model.Student;
import com.geoattendance.repository.AdminRepository;
import com.geoattendance.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    // ─── Admin Login ───────────────────────────────────────────────────
    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");

        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        Admin admin = adminOpt.get();
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(admin.getUsername(), "ADMIN", admin.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", "ADMIN");
        response.put("name", admin.getUsername());
        response.put("id", admin.getId());
        return ResponseEntity.ok(response);
    }

    // ─── Student Register ──────────────────────────────────────────────
    @PostMapping("/student/register")
    public ResponseEntity<?> studentRegister(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String studentId = req.get("studentId");

        if (studentRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        }
        if (studentRepository.existsByStudentId(studentId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Student ID already exists"));
        }

        // Validate studentId: allow alphanumeric only
        if (!studentId.matches("^[a-zA-Z0-9]+$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Student ID must be alphanumeric (letters and/or numbers only)"));
        }

        Student student = new Student();
        student.setName(req.get("name"));
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode(req.get("password")));
        student.setStudentId(studentId);
        student.setPhone(req.getOrDefault("phone", ""));
        student.setStatus(Student.StudentStatus.pending);

        studentRepository.save(student);
        return ResponseEntity.ok(Map.of("message", "Registration successful! Awaiting admin approval."));
    }

    // ─── Student Login ─────────────────────────────────────────────────
    @PostMapping("/student/login")
    public ResponseEntity<?> studentLogin(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String password = req.get("password");

        Optional<Student> studentOpt = studentRepository.findByEmail(email);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        Student student = studentOpt.get();

        if (!passwordEncoder.matches(password, student.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        if (student.getStatus() == Student.StudentStatus.pending) {
            return ResponseEntity.status(403).body(Map.of("message", "Your account is pending approval by admin"));
        }

        if (student.getStatus() == Student.StudentStatus.rejected) {
            return ResponseEntity.status(403).body(Map.of("message", "Your account has been rejected. Contact admin."));
        }

        String token = jwtUtil.generateToken(student.getEmail(), "STUDENT", student.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", "STUDENT");
        response.put("name", student.getName());
        response.put("studentId", student.getStudentId());
        response.put("id", student.getId());
        return ResponseEntity.ok(response);
    }
}
