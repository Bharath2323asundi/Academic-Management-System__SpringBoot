package com.attendance.service;

import com.attendance.entity.Attendance;
import com.attendance.entity.Session;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public byte[] generateAttendanceReport(Session session, List<Attendance> attendanceList) throws IOException {
        String teacherName = (session.getTeacher() != null && session.getTeacher().getUser() != null) 
            ? session.getTeacher().getUser().getName() : "Unknown";
        String date = (session.getStartTime() != null) 
            ? session.getStartTime().toLocalDate().toString() : "Unknown";
            
        return generateGeneralReport("Attendance Report", session.getSubject(), teacherName, date, attendanceList);
    }

    public byte[] generateGeneralReport(String title, String subject, String teacherName, String date, List<Attendance> attendanceList) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.COURIER_BOLD, 16);
                contentStream.setLeading(20f);
                contentStream.newLineAtOffset(50, 750);
                
                contentStream.showText(title);
                contentStream.newLine();
                contentStream.setFont(PDType1Font.COURIER, 10);
                if (subject != null) {
                    contentStream.showText("Subject: " + subject);
                    contentStream.newLine();
                }
                contentStream.showText("Teacher: " + teacherName);
                contentStream.newLine();
                contentStream.showText("Date:    " + date);
                contentStream.newLine();
                contentStream.showText("-----------------------------------------------------------------------");
                contentStream.newLine();
                
                contentStream.setFont(PDType1Font.COURIER_BOLD, 10);
                contentStream.showText(String.format("%-25s %-15s %-15s", "Student Name", "VTU No", "Time"));
                contentStream.newLine();
                contentStream.showText("-----------------------------------------------------------------------");
                contentStream.newLine();
                contentStream.setFont(PDType1Font.COURIER, 10);

                int count = 0;
                for (Attendance att : attendanceList) {
                    if (count >= 30) break; // Simple page limit for now
                    if (att.getStudent() == null || att.getStudent().getUser() == null) continue;

                    String name = att.getStudent().getUser().getName();
                    if (name == null) name = "Unknown";
                    name = sanitizeForPdf(name);
                    if (name.length() > 24) name = name.substring(0, 21) + "...";
                    
                    String timestamp = "N/A";
                    if (att.getMarkedAt() != null) {
                        timestamp = att.getMarkedAt().toLocalTime().format(TIME_FORMATTER);
                    }

                    String line = String.format("%-25s %-15s %-15s", 
                        name,
                        att.getStudent().getVtuNo() != null ? att.getStudent().getVtuNo() : "N/A",
                        timestamp);
                    contentStream.showText(line);
                    contentStream.newLine();
                    count++;
                }

                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private String sanitizeForPdf(String text) {
        if (text == null) return "";
        // PDF Standard 14 fonts (like Courier) mostly support Windows-1252 / WinAnsiEncoding.
        // For simplicity, we'll strip characters that might cause PDPageContentStream.showText to fail.
        // Replace non-ASCII and non-printable characters with '?'
        return text.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
