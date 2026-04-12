package com.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "vtu_no", nullable = false, unique = true)
    private String vtuNo;

    @Column(nullable = false)
    private String branch;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "is_approved", nullable = false)
    private boolean isApproved = false;

    @Column(name = "teacher_access_key", nullable = false)
    private String teacherAccessKey;
}
