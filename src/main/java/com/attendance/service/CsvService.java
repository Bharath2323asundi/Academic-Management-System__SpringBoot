package com.attendance.service;

import com.attendance.entity.Attendance;
import com.attendance.entity.Session;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CsvService {

    public byte[] generateAttendanceCsv(Session session, List<Attendance> attendanceList) {
        StringBuilder csv = new StringBuilder();
        csv.append("Subject,Teacher,Date,Student Name,VTU No,Time,IP Address\n");

        String subject = session.getSubject();
        String teacher = (session.getTeacher() != null && session.getTeacher().getUser() != null) 
            ? session.getTeacher().getUser().getName() : "Unknown";
        String date = (session.getStartTime() != null) 
            ? session.getStartTime().toLocalDate().toString() : "Unknown";

        for (Attendance att : attendanceList) {
            String studentName = (att.getStudent() != null && att.getStudent().getUser() != null) 
                ? att.getStudent().getUser().getName() : "Unknown";
            String vtuNo = (att.getStudent() != null) ? att.getStudent().getVtuNo() : "N/A";
            String time = (att.getMarkedAt() != null) ? att.getMarkedAt().toLocalTime().toString() : "N/A";
            String ip = att.getStudentIp() != null ? att.getStudentIp() : "N/A";

            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    subject, teacher, date, studentName, vtuNo, time, ip));
        }

        return csv.toString().getBytes();
    }

    public byte[] generateDailyCsv(String teacherName, String date, List<Attendance> attendanceList) {
        StringBuilder csv = new StringBuilder();
        csv.append("Teacher,Date,Subject,Student Name,VTU No,Time,IP Address\n");

        for (Attendance att : attendanceList) {
            String subject = (att.getSession() != null) ? att.getSession().getSubject() : "Unknown";
            String studentName = (att.getStudent() != null && att.getStudent().getUser() != null) 
                ? att.getStudent().getUser().getName() : "Unknown";
            String vtuNo = (att.getStudent() != null) ? att.getStudent().getVtuNo() : "N/A";
            String time = (att.getMarkedAt() != null) ? att.getMarkedAt().toLocalTime().toString() : "N/A";
            String ip = att.getStudentIp() != null ? att.getStudentIp() : "N/A";

            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    teacherName, date, subject, studentName, vtuNo, time, ip));
        }

        return csv.toString().getBytes();
    }
}
