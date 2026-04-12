package com.attendance.dto;

import com.attendance.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    private String role; // TEACHER or STUDENT

    // For Teacher
    private String accessKey;

    // For Student
    private String vtuNo;
    private String branch;
    private Integer semester;
}
