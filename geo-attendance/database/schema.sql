-- ============================================
-- Geo-Fencing Attendance Management System
-- Database Schema for MySQL (XAMPP)
-- ============================================

CREATE DATABASE IF NOT EXISTS geo_attendance;
USE geo_attendance;

-- Admin table
CREATE TABLE IF NOT EXISTS admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Students table
CREATE TABLE IF NOT EXISTS students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    student_id VARCHAR(50) NOT NULL UNIQUE,
    phone VARCHAR(20),
    status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Attendance table
CREATE TABLE IF NOT EXISTS attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    date DATE NOT NULL,
    time TIME NOT NULL,
    status ENUM('present', 'absent') DEFAULT 'present',
    latitude DOUBLE,
    longitude DOUBLE,
    distance_from_center DOUBLE COMMENT 'Distance in meters from admin-set location',
    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    UNIQUE KEY unique_student_date (student_id, date)
);

-- Admin settings table (geo-fence & time window)
CREATE TABLE IF NOT EXISTS admin_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================
-- Default Admin Account
-- Password: admin123 (BCrypt hashed)
-- ============================================
INSERT INTO admin (username, password, email) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6ZBMS', 'admin@geoattendance.com')
ON DUPLICATE KEY UPDATE username = username;

-- Default geo-fence settings
INSERT INTO admin_settings (setting_key, setting_value) VALUES
('latitude', '13.0827'),
('longitude', '80.2707'),
('radius', '100'),
('start_time', '09:00:00'),
('end_time', '17:00:00')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- ============================================
-- Sample Student (for testing - approved)
-- Password: student123
-- ============================================
INSERT INTO students (name, email, password, student_id, phone, status) VALUES
('John Doe', 'john@test.com', '$2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAZPaD4GlBnZ0XDg6r.9EOCJK', 'STU001', '9876543210', 'approved')
ON DUPLICATE KEY UPDATE email = email;

-- Indexes for performance
CREATE INDEX idx_attendance_date ON attendance(date);
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_students_status ON students(status);
