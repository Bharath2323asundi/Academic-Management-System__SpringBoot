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
import java.util.List;

@Service
public class PdfService {

    public byte[] generateAttendanceReport(Session session, List<Attendance> attendanceList) throws IOException {
        return generateGeneralReport("Attendance Report", session.getSubject(), session.getTeacher().getUser().getName(), session.getStartTime().toLocalDate().toString(), attendanceList);
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
                    String name = att.getStudent().getUser().getName();
                    if (name.length() > 24) name = name.substring(0, 21) + "...";
                    
                    String line = String.format("%-25s %-15s %-15s", 
                        name,
                        att.getStudent().getVtuNo(),
                        att.getMarkedAt().toLocalTime().toString().substring(0, 8));
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
}
