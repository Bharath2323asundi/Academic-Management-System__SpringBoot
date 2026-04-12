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
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.setLeading(20f);
                contentStream.newLineAtOffset(50, 750);
                
                contentStream.showText("Attendance Report");
                contentStream.newLine();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.showText("Subject: " + session.getSubject());
                contentStream.newLine();
                contentStream.showText("Teacher: " + session.getTeacher().getUser().getName());
                contentStream.newLine();
                contentStream.showText("Date: " + session.getStartTime().toLocalDate());
                contentStream.newLine();
                contentStream.showText("-----------------------------------------------------------------------");
                contentStream.newLine();
                
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.showText(String.format("%-20s %-20s %-20s", "Student Name", "VTU No", "Time"));
                contentStream.newLine();
                contentStream.setFont(PDType1Font.HELVETICA, 12);

                for (Attendance att : attendanceList) {
                    String line = String.format("%-20s %-20s %-20s", 
                        att.getStudent().getUser().getName(),
                        att.getStudent().getVtuNo(),
                        att.getMarkedAt().toLocalTime().toString().substring(0, 8));
                    contentStream.showText(line);
                    contentStream.newLine();
                }

                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
