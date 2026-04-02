package com.geoattendance.repository;

import com.geoattendance.model.LeaveRequest;
import com.geoattendance.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByStudentOrderByIdDesc(Student student);

    List<LeaveRequest> findByStatusOrderByIdDesc(LeaveRequest.LeaveStatus status);
}
