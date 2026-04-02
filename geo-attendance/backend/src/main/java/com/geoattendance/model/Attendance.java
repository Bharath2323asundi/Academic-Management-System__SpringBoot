package com.geoattendance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "date"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status = AttendanceStatus.present;

    private Double latitude;
    private Double longitude;

    @Column(name = "distance_from_center")
    private Double distanceFromCenter;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    @PrePersist
    protected void onCreate() {
        markedAt = LocalDateTime.now();
    }

    public enum AttendanceStatus {
        present, absent
    }
}
