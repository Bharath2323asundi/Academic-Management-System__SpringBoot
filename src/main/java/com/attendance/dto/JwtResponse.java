package com.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private Long id;
    private String name;
    private String email;
    private List<String> roles;
    private String studentStatus; // Approved/Pending (for student)
    private Boolean teacherActive; // Active/Inactive (for teacher)
}
