package com.geoattendance.controller;

import com.geoattendance.model.Attendance;
import com.geoattendance.model.Student;
import com.geoattendance.model.LeaveRequest;
import com.geoattendance.repository.AdminSettingsRepository;
import com.geoattendance.repository.AttendanceRepository;
import com.geoattendance.repository.LeaveRequestRepository;
import com.geoattendance.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private AdminSettingsRepository settingsRepository;
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private Student getStudentFromAuth(Authentication auth) {
        String email = auth.getName();
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    // ─── Student Profile ───────────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        Student student = getStudentFromAuth(auth);
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", student.getId());
        profile.put("name", student.getName());
        profile.put("email", student.getEmail());
        profile.put("studentId", student.getStudentId());
        profile.put("phone", student.getPhone());
        profile.put("status", student.getStatus().toString());

        long totalPresent = attendanceRepository.countPresentByStudent(student);
        // Calculate from registration date or last 30 days
        long totalDays = 30;
        double percentage = totalDays > 0 ? (totalPresent * 100.0 / totalDays) : 0;

        profile.put("totalPresent", totalPresent);
        profile.put("attendancePercentage", Math.min(Math.round(percentage), 100));
        return ResponseEntity.ok(profile);
    }

    // ─── Mark Attendance ───────────────────────────────────────────────
    @PostMapping("/attendance/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> req,
            Authentication auth, HttpServletRequest request) {
        Student student = getStudentFromAuth(auth);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Check already marked
        if (attendanceRepository.existsByStudentAndDate(student, today)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Attendance already marked for today"));
        }

        // Verify submitted student ID
        String submittedId = (String) req.get("studentId");
        if (submittedId == null || !submittedId.equalsIgnoreCase(student.getStudentId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid Student ID"));
        }

        // Validate student ID format (alphanumeric)
        if (!submittedId.matches("^[a-zA-Z0-9]+$")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Student ID must contain only letters and/or numbers"));
        }

        // Get admin settings
        Map<String, String> settings = new HashMap<>();
        settingsRepository.findAll().forEach(s -> settings.put(s.getSettingKey(), s.getSettingValue()));

        if (!settings.containsKey("latitude")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Admin has not configured attendance location"));
        }

        double adminLat = Double.parseDouble(settings.get("latitude"));
        double adminLng = Double.parseDouble(settings.get("longitude"));
        double radius = Double.parseDouble(settings.get("radius"));
        LocalTime startTime = LocalTime.parse(settings.get("start_time"));
        LocalTime endTime = LocalTime.parse(settings.get("end_time"));

        // Check time window
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Attendance can only be marked between " + startTime + " and " + endTime));
        }

        // IP Network Fencing
        if (settings.containsKey("allowed_ips")) {
            String allowedIpsStr = settings.get("allowed_ips");
            if (allowedIpsStr != null && !allowedIpsStr.trim().isEmpty()) {
                String remoteIp = request.getHeader("X-Forwarded-For");
                if (remoteIp == null || remoteIp.isEmpty()) {
                    remoteIp = request.getRemoteAddr();
                }
                // Handle IPv6 localhost
                if ("0:0:0:0:0:0:0:1".equals(remoteIp)) remoteIp = "127.0.0.1";
                
                List<String> allowedIps = Arrays.asList(allowedIpsStr.split(","));
                boolean isAllowed = false;
                for (String ip : allowedIps) {
                    if (remoteIp.startsWith(ip.trim())) {
                        isAllowed = true;
                        break;
                    }
                }
                if (!isAllowed) {
                     return ResponseEntity.badRequest().body(Map.of(
                        "message", "You must be connected to the campus Wi-Fi to mark attendance. Your IP: " + remoteIp));
                }
            }
        }

        // Get student location
        double studentLat = Double.parseDouble(req.get("latitude").toString());
        double studentLng = Double.parseDouble(req.get("longitude").toString());

        // Haversine distance calculation
        double distance = haversineDistance(adminLat, adminLng, studentLat, studentLng);

        if (distance > radius) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", String.format("You are %.0f meters away from the allowed zone (radius: %.0f m)",
                            distance, radius)));
        }

        // Mark attendance
        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setDate(today);
        attendance.setTime(now);
        attendance.setStatus(Attendance.AttendanceStatus.present);
        attendance.setLatitude(studentLat);
        attendance.setLongitude(studentLng);
        attendance.setDistanceFromCenter(distance);
        attendanceRepository.save(attendance);

        return ResponseEntity.ok(Map.of(
                "message", "Attendance marked successfully!",
                "date", today.toString(),
                "time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                "distance", Math.round(distance)));
    }

    // ─── Attendance History ────────────────────────────────────────────
    @GetMapping("/attendance/history")
    public ResponseEntity<?> getHistory(Authentication auth) {
        Student student = getStudentFromAuth(auth);
        List<Attendance> records = attendanceRepository.findStudentHistory(student);

        List<Map<String, Object>> result = records.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("date", a.getDate().toString());
            map.put("time", a.getTime().toString());
            map.put("status", a.getStatus().toString());
            map.put("distance", a.getDistanceFromCenter() != null ? Math.round(a.getDistanceFromCenter()) : 0);
            return map;
        }).collect(Collectors.toList());

        long totalPresent = records.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.present)
                .count();

        Map<String, Object> response = new HashMap<>();
        response.put("records", result);
        response.put("totalPresent", totalPresent);
        response.put("totalRecords", result.size());

        return ResponseEntity.ok(response);
    }

    // ─── Check Today's Status ──────────────────────────────────────────
    @GetMapping("/attendance/today")
    public ResponseEntity<?> getTodayStatus(Authentication auth) {
        Student student = getStudentFromAuth(auth);
        Optional<Attendance> record = attendanceRepository.findByStudentAndDate(student, LocalDate.now());

        Map<String, Object> response = new HashMap<>();
        response.put("marked", record.isPresent());
        if (record.isPresent()) {
            response.put("time", record.get().getTime().toString());
            response.put("status", record.get().getStatus().toString());
        }
        return ResponseEntity.ok(response);
    }

    // ─── Leave Applications ────────────────────────────────────────────
    @PostMapping("/leave/apply")
    public ResponseEntity<?> applyForLeave(@RequestBody Map<String, Object> req, Authentication auth) {
        Student student = getStudentFromAuth(auth);
        
        try {
            LocalDate startDate = LocalDate.parse(req.get("startDate").toString());
            LocalDate endDate = LocalDate.parse(req.get("endDate").toString());
            String reason = req.get("reason").toString();
            
            if (startDate.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Start date cannot be in the past"));
            }
            if (endDate.isBefore(startDate)) {
                return ResponseEntity.badRequest().body(Map.of("message", "End date must be after start date"));
            }
            
            LeaveRequest leave = new LeaveRequest();
            leave.setStudent(student);
            leave.setStartDate(startDate);
            leave.setEndDate(endDate);
            leave.setReason(reason);
            leave.setStatus(LeaveRequest.LeaveStatus.pending);
            
            leaveRequestRepository.save(leave);
            return ResponseEntity.ok(Map.of("message", "Leave application submitted successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid dates or missing fields"));
        }
    }

    @GetMapping("/leave/history")
    public ResponseEntity<?> getLeaveHistory(Authentication auth) {
        Student student = getStudentFromAuth(auth);
        List<LeaveRequest> leaves = leaveRequestRepository.findByStudentOrderByIdDesc(student);
        
        List<Map<String, Object>> result = leaves.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
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

    // ─── Haversine Formula ─────────────────────────────────────────────
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
