package com.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // nullable=true to allow Hibernate to add this column to existing DB tables on PostgreSQL
    // Business logic in AuthController ensures all new teachers always provide an access key
    @Column(name = "access_key", nullable = true)
    private String accessKey;

    @Column(nullable = false)
    private boolean isActive = true; // Default active until Admin says otherwise
}
