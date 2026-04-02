# 📍 Geo-Fencing Attendance Management System
### Built with Spring Boot + MySQL (XAMPP) + Vanilla HTML/CSS/JS

---

## 📁 Folder Structure

```
geo-attendance/
├── database/
│   └── schema.sql                  ← Run this first in phpMyAdmin
│
├── backend/                        ← Spring Boot Maven project
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/geoattendance/
│       │   ├── GeoAttendanceApplication.java
│       │   ├── config/
│       │   │   ├── JwtUtil.java
│       │   │   ├── JwtAuthFilter.java
│       │   │   └── SecurityConfig.java
│       │   ├── model/
│       │   │   ├── Admin.java
│       │   │   ├── Student.java
│       │   │   ├── Attendance.java
│       │   │   └── AdminSettings.java
│       │   ├── repository/
│       │   │   ├── AdminRepository.java
│       │   │   ├── StudentRepository.java
│       │   │   ├── AttendanceRepository.java
│       │   │   └── AdminSettingsRepository.java
│       │   └── controller/
│       │       ├── AuthController.java
│       │       ├── AdminController.java
│       │       └── StudentController.java
│       └── resources/
│           └── application.properties
│
└── frontend/
    ├── assets/
    │   ├── css/style.css
    │   └── js/main.js
    ├── admin/
    │   ├── login.html
    │   ├── dashboard.html
    │   ├── students.html
    │   ├── attendance.html
    │   └── settings.html
    └── student/
        ├── login.html
        ├── dashboard.html
        ├── mark-attendance.html
        └── history.html
```

---

## ✅ Prerequisites

| Tool         | Version | Download |
|--------------|---------|----------|
| Java JDK     | 17+     | https://adoptium.net |
| Apache Maven | 3.8+    | https://maven.apache.org |
| XAMPP        | Latest  | https://www.apachefriends.org |
| VS Code / IntelliJ | Any | For editing code |

---

## 🚀 Step-by-Step Setup

### Step 1 — Start XAMPP
1. Open XAMPP Control Panel
2. Start **Apache** and **MySQL**
3. Open browser → http://localhost/phpmyadmin

### Step 2 — Create Database
1. In phpMyAdmin, click **"New"** in the left sidebar
2. Type database name: `geo_attendance` → click **Create**
3. Click the `geo_attendance` database
4. Go to the **SQL** tab
5. Open `database/schema.sql` from this project
6. Paste the entire contents into the SQL box
7. Click **Go** to execute

   ✅ This creates all tables and inserts default admin + sample student.

### Step 3 — Configure Backend
1. Open `backend/src/main/resources/application.properties`
2. If your MySQL has a password, change:
   ```
   spring.datasource.password=YOUR_PASSWORD_HERE
   ```
   (Leave blank if no password — default XAMPP has no password)
3. The database URL should stay as:
   ```
   spring.datasource.url=jdbc:mysql://localhost:3306/geo_attendance?...
   ```

### Step 4 — Run Spring Boot Backend
Open a terminal in the `backend/` folder:

```bash
# If Maven is installed globally:
mvn spring-boot:run

# OR use the Maven wrapper (if present):
./mvnw spring-boot:run          # macOS/Linux
mvnw.cmd spring-boot:run        # Windows
```

Wait for the message:
```
Started GeoAttendanceApplication in X seconds
```

The API will be running at: **http://localhost:8080**

### Step 5 — Open Frontend
You have two options:

**Option A — Direct file open (simplest):**
- Double-click `frontend/admin/login.html` to open in browser
- Double-click `frontend/student/login.html` for student portal

**Option B — VS Code Live Server (recommended):**
1. Install the "Live Server" extension in VS Code
2. Right-click `frontend/admin/login.html` → **Open with Live Server**
3. The app opens at `http://127.0.0.1:5500/frontend/admin/login.html`

> ⚠️ **Important:** If using Live Server, add `http://127.0.0.1:5500` to `app.cors.allowed-origins` in `application.properties`.

---

## 🔐 Default Login Credentials

### Admin
| Field    | Value       |
|----------|-------------|
| Username | `admin`     |
| Password | `admin123`  |
| URL      | `frontend/admin/login.html` |

### Sample Student (Pre-approved)
| Field      | Value              |
|------------|--------------------|
| Email      | `john@test.com`    |
| Password   | `student123`       |
| Student ID | `STU001`           |
| URL        | `frontend/student/login.html` |

---

## 🌐 API Endpoints Reference

### Auth (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/admin/login` | Admin login |
| POST | `/api/auth/student/register` | Student registration |
| POST | `/api/auth/student/login` | Student login |

### Admin (Requires ADMIN JWT)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/dashboard` | Dashboard stats + chart data |
| GET | `/api/admin/students` | All students |
| GET | `/api/admin/students/pending` | Pending approvals |
| PUT | `/api/admin/students/{id}/status` | Approve/Reject student |
| GET | `/api/admin/attendance/today` | Today's attendance |
| GET | `/api/admin/attendance/date/{date}` | Attendance by date |
| GET | `/api/admin/settings` | Get geo-fence settings |
| PUT | `/api/admin/settings` | Update geo-fence settings |

### Student (Requires STUDENT JWT)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/student/profile` | Student profile + stats |
| POST | `/api/student/attendance/mark` | Mark attendance |
| GET | `/api/student/attendance/today` | Today's status |
| GET | `/api/student/attendance/history` | Full attendance history |

---

## 📍 Geo-Fence Setup (Admin)

1. Login as Admin → Go to **Settings**
2. Enter the **Latitude** and **Longitude** of your institution
   - Example: Chennai `13.0827, 80.2707`
   - Tip: Use the **"Use My Current Location"** button
3. Set **Radius** in meters (e.g., `100` for 100 meters)
4. Set **Attendance Time Window** (e.g., 9:00 AM to 5:00 PM)
5. Click **Save All Settings**

---

## 🎓 Student Workflow

1. Go to Student Login page → **Register**
2. Enter name, email, **Student ID** (letters and/or numbers: e.g. `STU001`, `ABC`, `12345`)
3. Admin must **approve** your registration
4. Login with email + password
5. Go to **Mark Attendance**:
   - Allow browser location access
   - Wait for GPS to acquire your position
   - Enter your Student ID
   - Click **Mark My Attendance**

---

## 🔧 Troubleshooting

| Problem | Solution |
|---------|----------|
| Backend won't start | Check Java 17+ is installed: `java -version` |
| Database error | Verify XAMPP MySQL is running and schema.sql was executed |
| CORS error in browser | Add your frontend URL to `app.cors.allowed-origins` |
| GPS not working | Use HTTPS or localhost (browsers require secure context for GPS) |
| Login fails | Check password is correct — default admin: `admin/admin123` |
| "Table doesn't exist" | Re-run the schema.sql file in phpMyAdmin |

---

## 💡 Student ID Format

The Student ID field accepts:
- **Numbers only**: `12345`, `001`  
- **Letters only**: `ABC`, `JOHN`
- **Alphanumeric**: `STU001`, `CS2024A`, `A123`
- Case-insensitive matching during attendance (e.g. `stu001` matches `STU001`)

---

## 🏗️ Technology Stack

| Layer | Technology |
|-------|------------|
| Frontend | HTML5, CSS3, Vanilla JavaScript |
| Backend | Java 17, Spring Boot 3.2, Spring Security |
| Database | MySQL via XAMPP |
| Auth | JWT (JSON Web Tokens) |
| Maps/GPS | Browser Geolocation API (free) |
| Charts | Chart.js (free CDN) |
| Fonts | Google Fonts — Syne + DM Sans (free) |

---

*Built entirely with free, open-source technologies. No paid APIs required.*
