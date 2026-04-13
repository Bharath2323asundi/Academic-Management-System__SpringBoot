package com.attendance.controller;

import com.attendance.dto.*;
import com.attendance.entity.*;
import com.attendance.repository.*;
import com.attendance.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        User user = userRepository.findByEmail(userDetails.getUsername()).get();
        
        String studentStatus = null;
        Boolean teacherActive = null;
        String accessKey = null;

        if (user.getRole() == Role.STUDENT) {
            Optional<Student> studentOpt = studentRepository.findByUser(user);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new MessageResponse("Error: Student profile not found."));
            }
            Student student = studentOpt.get();
            studentStatus = student.isApproved() ? "APPROVED" : "PENDING";
            if (!student.isApproved()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Your account is pending approval from a teacher."));
            }
        } else if (user.getRole() == Role.TEACHER) {
            Optional<Teacher> teacherOpt = teacherRepository.findByUser(user);
            if (teacherOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new MessageResponse("Error: Teacher profile not found."));
            }
            Teacher teacher = teacherOpt.get();
            teacherActive = teacher.isActive();
            accessKey = teacher.getAccessKey();
            if (!teacher.isActive()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Your teacher account is deactivated by Admin."));
            }
        }

        return ResponseEntity.ok(new JwtResponse(jwt,
                user.getId(),
                user.getName(),
                user.getEmail(),
                roles,
                studentStatus,
                teacherActive,
                accessKey));
    }

    @Transactional
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        Role role = Role.valueOf(signUpRequest.getRole().toUpperCase());

        // Create new user's account
        User user = User.builder()
                .name(signUpRequest.getName())
                .email(signUpRequest.getEmail())
                .password(encoder.encode(signUpRequest.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        if (role == Role.TEACHER) {
            // Verify Access Key
            if (signUpRequest.getAccessKey() == null || signUpRequest.getAccessKey().trim().isEmpty()) {
                 return ResponseEntity.badRequest().body(new MessageResponse("Error: Teacher Access Key is required!"));
            }
            
            String normalizedKey = signUpRequest.getAccessKey().trim().toUpperCase();
            
            Teacher teacher = Teacher.builder()
                    .user(user)
                    .accessKey(normalizedKey)
                    .isActive(true)
                    .build();
            teacherRepository.save(teacher);
            
        } else if (role == Role.STUDENT) {
            if (signUpRequest.getVtuNo() == null || signUpRequest.getVtuNo().trim().isEmpty()) {
                 return ResponseEntity.badRequest().body(new MessageResponse("Error: VTU Number is required!"));
            }
            
            // Check VTU Unique (to prevent 500/403 errors and allow clean rollback)
            if (studentRepository.findByVtuNo(signUpRequest.getVtuNo().trim()).isPresent()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: VTU Number is already registered!"));
            }

            if (signUpRequest.getAccessKey() == null || signUpRequest.getAccessKey().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Teacher Access Key is required for students!"));
            }

            String normalizedKey = signUpRequest.getAccessKey().trim().toUpperCase();

            // Verify if teacher with this access key exists
            if (!teacherRepository.findByAccessKey(normalizedKey).isPresent()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid Teacher Access Key! Requests are case-sensitive but match after trimming."));
            }
            
            Student student = Student.builder()
                    .user(user)
                    .vtuNo(signUpRequest.getVtuNo().trim())
                    .branch(signUpRequest.getBranch())
                    .semester(signUpRequest.getSemester())
                    .teacherAccessKey(normalizedKey)
                    .isApproved(false) // Wait for teacher approval
                    .build();
            studentRepository.save(student);
        }

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
