package com.geoattendance.controller;

import com.geoattendance.model.AdminSettings;
import com.geoattendance.model.Attendance;
import com.geoattendance.model.Student;
import com.geoattendance.model.LeaveRequest;
import com.geoattendance.repository.AdminSettingsRepository;
import com.geoattendance.repository.AttendanceRepository;
import com.geoattendance.repository.LeaveRequestRepository;
import com.geoattendance.repository.AttendanceRepository;
import com.geoattendance.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private AdminSettingsRepository settingsRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;

    // ─── Dashboard Stats ───────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        LocalDate today = LocalDate.now();
        long totalStudents = studentRepository.countByStatus(Student.StudentStatus.approved);
        long presentToday = attendanceRepository.countPresentByDate(today);
        long absentToday = totalStudents - presentToday;
        long pendingApprovals = studentRepository.countByStatus(Student.StudentStatus.pending);
        long pendingLeaves = leaveRequestRepository.findByStatusOrderByIdDesc(LeaveRequest.LeaveStatus.pending).size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("presentToday", presentToday);
        stats.put("absentToday", Math.max(absentToday, 0));
        stats.put("pendingApprovals", pendingApprovals);
        stats.put("pendingLeaves", pendingLeaves);
        stats.put("date", today.toString());

        // Last 7 days chart data
        List<Map<String, Object>> chartData = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long present = attendanceRepository.countPresentByDate(date);
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(DateTimeFormatter.ofPattern("MMM dd")));
            dayData.put("present", present);
            chartData.add(dayData);
        }
        stats.put("chartData", chartData);
        return ResponseEntity.ok(stats);
    }

    // ─── Today's Attendance ────────────────────────────────────────────
    @GetMapping("/attendance/today")
    public ResponseEntity<?> getTodayAttendance() {
        List<Attendance> records = attendanceRepository.findByDateWithStudent(LocalDate.now());
        List<Map<String, Object>> result = records.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("studentName", a.getStudent().getName());
            map.put("studentId", a.getStudent().getStudentId());
            map.put("time", a.getTime().toString());
            map.put("status", a.getStatus().toString());
            map.put("distance", a.getDistanceFromCenter());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─── Attendance by Date ────────────────────────────────────────────
    @GetMapping("/attendance/date/{date}")
    public ResponseEntity<?> getAttendanceByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<Attendance> records = attendanceRepository.findByDateWithStudent(localDate);
        List<Map<String, Object>> result = records.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("studentName", a.getStudent().getName());
            map.put("studentId", a.getStudent().getStudentId());
            map.put("time", a.getTime().toString());
            map.put("status", a.getStatus().toString());
            map.put("distance", a.getDistanceFromCenter() != null ? Math.round(a.getDistanceFromCenter()) : 0);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─── All Students ──────────────────────────────────────────────────
    @GetMapping("/students")
    public ResponseEntity<?> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        List<Map<String, Object>> result = students.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getName());
            map.put("email", s.getEmail());
            map.put("studentId", s.getStudentId());
            map.put("phone", s.getPhone());
            map.put("status", s.getStatus().toString());
            map.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─── Pending Approvals ─────────────────────────────────────────────
    @GetMapping("/students/pending")
    public ResponseEntity<?> getPendingStudents() {
        List<Student> students = studentRepository.findByStatus(Student.StudentStatus.pending);
        List<Map<String, Object>> result = students.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getName());
            map.put("email", s.getEmail());
            map.put("studentId", s.getStudentId());
            map.put("phone", s.getPhone());
            map.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─── Approve/Reject Student ────────────────────────────────────────
    @PutMapping("/students/{id}/status")
    public ResponseEntity<?> updateStudentStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, String> req) {
        Optional<Student> studentOpt = studentRepository.findById(id);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Student student = studentOpt.get();
        String status = req.get("status");
        try {
            student.setStatus(Student.StudentStatus.valueOf(status));
            studentRepository.save(student);
            return ResponseEntity.ok(Map.of("message", "Student status updated to " + status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid status"));
        }
    }

    // ─── Get Settings ──────────────────────────────────────────────────
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        Map<String, String> settings = new HashMap<>();
        settingsRepository.findAll().forEach(s -> settings.put(s.getSettingKey(), s.getSettingValue()));
        return ResponseEntity.ok(settings);
    }

    // ─── Update Settings ───────────────────────────────────────────────
    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> req) {
        String[] keys = {"latitude", "longitude", "radius", "start_time", "end_time", "allowed_ips"};
        for (String key : keys) {
            if (req.containsKey(key)) {
                Optional<AdminSettings> opt = settingsRepository.findBySettingKey(key);
                AdminSettings setting;
                if (opt.isPresent()) {
                    setting = opt.get();
                } else {
                    setting = new AdminSettings();
                    setting.setSettingKey(key);
                }
                setting.setSettingValue(req.get(key));
                settingsRepository.save(setting);
            }
        }
        return ResponseEntity.ok(Map.of("message", "Settings updated successfully"));
    }

    // ─── Leave Management ──────────────────────────────────────────────
    @GetMapping("/leaves")
    public ResponseEntity<?> getAllLeaves() {
        List<LeaveRequest> leaves = leaveRequestRepository.findAll();
        List<Map<String, Object>> result = leaves.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("studentName", l.getStudent().getName());
            map.put("studentId", l.getStudent().getStudentId());
            map.put("startDate", l.getStartDate().toString());
            map.put("endDate", l.getEndDate().toString());
            map.put("reason", l.getReason());
            map.put("status", l.getStatus().toString());
            map.put("adminRemarks", l.getAdminRemarks());
            map.put("appliedOn", l.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/leaves/{id}/status")
    public ResponseEntity<?> updateLeaveStatus(@PathVariable Long id, @RequestBody Map<String, String> req) {
        Optional<LeaveRequest> opt = leaveRequestRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LeaveRequest leave = opt.get();
        String statusStr = req.get("status");
        String remarks = req.get("adminRemarks");
        
        try {
            leave.setStatus(LeaveRequest.LeaveStatus.valueOf(statusStr));
            if (remarks != null) {
                leave.setAdminRemarks(remarks);
            }
            leaveRequestRepository.save(leave);
            return ResponseEntity.ok(Map.of("message", "Leave status updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid leave status"));
        }
    }
}
