# AttendMark Web Edition

A companion web application for AttendMark, designed for teachers and educators to manage student attendance, exam records, consolidated multi-subject report cards, and automated WhatsApp parent communication on any desktop or mobile browser.

---

## 🚀 Features

1. **Dashboard & Summary**: Real-time overview of active class rosters, today's attendance percentage, absent counts, and exam assessments.
2. **Attendance Register**:
   - Single-click daily status marking (`Present`, `Absent`, `Late`, `Excused`).
   - Quick **"Mark All Present"** button.
   - **WhatsApp Absentees Dispatch**: Direct WhatsApp Web parent notification generator.
   - **Export CSV**: Download clean attendance logs for any selected date.
3. **Marks & Assessment Manager**:
   - Create exams by type (Unit Test, Midterm, Final, Quiz) with customizable Max Marks.
   - In-line student marks input with automatic percentage and letter grade calculation (`A+`, `A`, `B+`, `B`, `C`, `D`, `E`, `F`).
   - Class average, highest score, and pass rate statistics.
   - **Single Exam CSV Export** and **WhatsApp Results Dispatch**.
   - **Master Subject-Wise Matrix CSV**: Exports a comprehensive spreadsheet with all subjects side-by-side, cumulative totals, percentages, and final grades.
   - **Consolidated Multi-Subject Report Card**: Printable official A4-format report card with full subject breakdown, overall metrics, and teacher signature blocks (compatible with Print to PDF).
4. **Student Directory**:
   - Search by student name or roll number.
   - Add/Edit/Delete students with Guardian contact and WhatsApp consent.
   - **CSV Import**: Fast bulk student onboarding with auto column mapping.
   - **Downloadable Sample CSV Template**: Ready-to-fill `.csv` starter template.
   - **Export Roster CSV**: Single-click student list download.
5. **Data Backup & Privacy**:
   - 100% offline and client-side via browser `localStorage`.
   - Download full JSON database backup at any time.
   - Restore database from JSON backup file.

---

## 💻 How to Run

1. Open `index.html` in any modern web browser (Chrome, Edge, Firefox, Safari).
2. Or serve it using any local static file server:
   ```bash
   npx serve .
   # or
   python3 -m http.server 8000
   ```
3. No build tools or Node.js server dependencies required — it uses modern Tailwind CSS CDN and standalone JavaScript.
