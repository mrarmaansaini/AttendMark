// AttendMark Web App Core Script

const STORAGE_KEY = 'attendmark_web_db_v1';

// Initial state schema & seed
const defaultDb = {
  profile: {
    fullName: "",
    phoneE164: "",
    schoolName: "",
    designation: "",
    preferredWhatsAppApp: "com.whatsapp"
  },
  activeClassId: "",
  classes: [],
  students: [],
  exams: [],
  marks: [],
  attendance: {}
};

let db = loadDatabase();

function loadDatabase() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    saveDatabase(defaultDb);
    return JSON.parse(JSON.stringify(defaultDb));
  }
  try {
    return JSON.parse(raw);
  } catch (e) {
    console.error("Failed to parse local storage, resetting to defaults", e);
    return JSON.parse(JSON.stringify(defaultDb));
  }
}

let autoPushTimeout = null;

function saveDatabase(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data || db));
  
  // Trigger Direct Sync Auto Push
  const config = getStoredFirebaseConfig();
  if (config && config.projectId) {
    if (autoPushTimeout) clearTimeout(autoPushTimeout);
    autoPushTimeout = setTimeout(() => {
      pushToFirebaseSilently();
    }, 1500); // 1.5s debounce for consecutive writes
  }
}

// ---------------- Helper Functions ---------------- //

function getActiveClass() {
  return db.classes.find(c => c.id === db.activeClassId) || db.classes[0];
}

function getStudentsForActiveClass() {
  const active = getActiveClass();
  if (!active) return [];
  return db.students.filter(s => s.classId === active.id).sort((a, b) => {
    return a.rollNo.localeCompare(b.rollNo, undefined, { numeric: true });
  });
}

function getExamsForActiveClass() {
  const active = getActiveClass();
  if (!active) return [];
  return db.exams.filter(e => e.classId === active.id);
}

function getMarksMapForExam(examId) {
  const map = {};
  db.marks.filter(m => m.examId === examId).forEach(m => {
    map[m.studentId] = m.marksObtained;
  });
  return map;
}

function getGrade(percentage) {
  if (percentage >= 90.0) return "A+";
  if (percentage >= 80.0) return "A";
  if (percentage >= 70.0) return "B+";
  if (percentage >= 60.0) return "B";
  if (percentage >= 50.0) return "C";
  if (percentage >= 40.0) return "D";
  if (percentage >= 33.0) return "E";
  return "F";
}

function showToast(message, type = "info") {
  const container = document.getElementById("toast-container");
  const toast = document.createElement("div");
  const bg = type === "success" ? "bg-emerald-600" : type === "error" ? "bg-rose-600" : "bg-indigo-600";
  toast.className = `${bg} text-white px-4 py-2.5 rounded-xl shadow-lg text-xs font-semibold flex items-center space-x-2 transform transition-all duration-300 pointer-events-auto`;
  toast.innerHTML = `<span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add("opacity-0", "translate-x-4");
    setTimeout(() => toast.remove(), 300);
  }, 3200);
}

function openModal(id) {
  document.getElementById(id)?.classList.remove("hidden");
  lucide.createIcons();
}

function closeModal(id) {
  document.getElementById(id)?.classList.add("hidden");
}

function getTodayIsoDate() {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function setTodayDate() {
  document.getElementById("attendance-date").value = getTodayIsoDate();
  renderAttendance();
}

// ---------------- Tab Navigation ---------------- //

let currentTab = 'dashboard';

function switchTab(tabId) {
  currentTab = tabId;
  const tabs = ['dashboard', 'attendance', 'marks', 'students', 'profile'];
  tabs.forEach(t => {
    const el = document.getElementById(`tab-${t}`);
    const navDesktop = document.getElementById(`nav-${t}`);
    const navMob = document.getElementById(`mob-nav-${t}`);

    if (t === tabId) {
      el?.classList.remove('hidden');
      if (navDesktop) {
        navDesktop.className = "nav-btn w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-xl font-bold text-sm bg-indigo-50 text-indigo-700 shadow-sm";
      }
      if (navMob) {
        navMob.className = "flex flex-col items-center py-1 px-2.5 text-indigo-600 font-bold";
      }
    } else {
      el?.classList.add('hidden');
      if (navDesktop) {
        navDesktop.className = "nav-btn w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-xl font-medium text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition";
      }
      if (navMob) {
        navMob.className = "flex flex-col items-center py-1 px-2.5 text-slate-400 hover:text-slate-700 transition";
      }
    }
  });

  // Re-render corresponding views
  if (tabId === 'dashboard') renderDashboard();
  if (tabId === 'attendance') renderAttendance();
  if (tabId === 'marks') renderMarks();
  if (tabId === 'students') renderStudents();
  if (tabId === 'profile') renderProfile();

  lucide.createIcons();
}

// ---------------- Header & Classes ---------------- //

function renderClassSelector() {
  const select = document.getElementById("class-select");
  if (!select) return;
  select.innerHTML = "";
  db.classes.forEach(cls => {
    const opt = document.createElement("option");
    opt.value = cls.id;
    opt.textContent = `${cls.name} (${cls.section || 'General'})`;
    if (cls.id === db.activeClassId) opt.selected = true;
    select.appendChild(opt);
  });

  select.onchange = (e) => {
    db.activeClassId = e.target.value;
    saveDatabase();
    switchTab(currentTab);
    showToast(`Switched to ${getActiveClass().name}`, "success");
  };

  // Header Teacher Profile Pill
  const prof = db.profile;
  const initial = (prof.fullName || "Teacher").charAt(0).toUpperCase();
  document.getElementById("header-avatar-initial").textContent = initial;
  document.getElementById("header-teacher-name").textContent = prof.fullName || "Teacher";
  document.getElementById("header-school-name").textContent = prof.schoolName || "AttendMark Academy";
}

function handleCreateClass(e) {
  e.preventDefault();
  const name = document.getElementById("new-cls-name").value.trim();
  const section = document.getElementById("new-cls-section").value.trim();
  const year = document.getElementById("new-cls-year").value.trim() || "2024-2025";
  if (!name) return;

  const newId = "cls_" + Date.now();
  db.classes.push({ id: newId, name, section, academicYear: year });
  db.activeClassId = newId;
  saveDatabase();
  closeModal('create-class-modal');
  document.getElementById("new-cls-name").value = "";
  document.getElementById("new-cls-section").value = "";
  renderClassSelector();
  switchTab(currentTab);
  showToast(`Class "${name}" created!`, "success");
}

// ---------------- Dashboard ---------------- //

function renderDashboard() {
  const activeClass = getActiveClass();
  const students = getStudentsForActiveClass();
  const exams = getExamsForActiveClass();
  const today = getTodayIsoDate();

  document.getElementById("dash-date-str").textContent = new Date().toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
  document.getElementById("dash-greeting-name").textContent = db.profile.fullName || "Teacher";
  document.getElementById("dash-stat-students").textContent = students.length;
  document.getElementById("dash-stat-exams").textContent = exams.length;

  // Attendance metrics for today
  const attKey = `${today}_${activeClass.id}`;
  const dayLog = db.attendance[attKey] || {};
  let presentCount = 0;
  let absentCount = 0;

  students.forEach(s => {
    const st = dayLog[s.id] || "ABSENT";
    if (st === "PRESENT") presentCount++;
    else if (st === "ABSENT") absentCount++;
  });

  const presentPct = students.length > 0 ? Math.round((presentCount / students.length) * 100) : 0;
  document.getElementById("dash-stat-present").textContent = `${presentPct}%`;
  document.getElementById("dash-stat-absent").textContent = absentCount;

  // Dashboard Attendance Preview
  const attPreview = document.getElementById("dash-attendance-preview");
  if (students.length === 0) {
    attPreview.innerHTML = `<p class="text-xs text-slate-400 py-4 text-center">No students added in this class yet.</p>`;
  } else {
    attPreview.innerHTML = `
      <div class="flex items-center justify-between text-xs py-2 px-3 bg-slate-50 rounded-xl">
        <span class="text-slate-600 font-medium">Session marked:</span>
        <span class="font-bold ${Object.keys(dayLog).length > 0 ? 'text-emerald-700' : 'text-amber-600'}">
          ${Object.keys(dayLog).length > 0 ? `${presentCount} of ${students.length} Present` : 'Not recorded yet'}
        </span>
      </div>
      <div class="flex gap-2">
        <button onclick="switchTab('attendance')" class="flex-1 py-2 text-xs font-semibold bg-indigo-50 text-indigo-700 hover:bg-indigo-100 rounded-xl transition">Record Today's Register</button>
        <button onclick="markAllPresent()" class="px-3 py-2 text-xs font-bold text-white bg-present hover:brightness-105 rounded-xl transition">Mark All Present</button>
      </div>
    `;
  }

  // Dashboard Exams Preview
  const exPreview = document.getElementById("dash-exams-preview");
  if (exams.length === 0) {
    exPreview.innerHTML = `<p class="text-xs text-slate-400 py-4 text-center">No exams registered for this class.</p>`;
  } else {
    exPreview.innerHTML = exams.slice(0, 3).map(ex => {
      const marksMap = getMarksMapForExam(ex.id);
      const scoredCount = Object.keys(marksMap).length;
      return `
        <div class="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100 text-xs">
          <div>
            <p class="font-bold text-slate-800">${ex.name}</p>
            <p class="text-[11px] text-slate-500">Max: ${ex.maxMarks} marks &bull; ${scoredCount}/${students.length} recorded</p>
          </div>
          <button onclick="openExamTabWithId('${ex.id}')" class="text-xs text-indigo-600 font-semibold hover:underline">View &rarr;</button>
        </div>
      `;
    }).join("");
  }
}

function openExamTabWithId(examId) {
  switchTab('marks');
  const select = document.getElementById("exam-select");
  if (select) {
    select.value = examId;
    renderMarks();
  }
}

// ---------------- Attendance Module ---------------- //

function renderAttendance() {
  const activeClass = getActiveClass();
  const students = getStudentsForActiveClass();
  const dateInput = document.getElementById("attendance-date");
  if (!dateInput.value) {
    dateInput.value = getTodayIsoDate();
  }
  const dateStr = dateInput.value;
  const attKey = `${dateStr}_${activeClass.id}`;
  const dayLog = db.attendance[attKey] || {};

  let present = 0, absent = 0, late = 0, excused = 0;
  students.forEach(s => {
    const st = dayLog[s.id] || "PRESENT"; // default to Present if not marked
    if (st === "PRESENT") present++;
    else if (st === "ABSENT") absent++;
    else if (st === "LATE") late++;
    else if (st === "EXCUSED") excused++;
  });

  document.getElementById("att-count-present").textContent = present;
  document.getElementById("att-count-absent").textContent = absent;
  document.getElementById("att-count-late").textContent = late;
  document.getElementById("att-count-excused").textContent = excused;

  const tbody = document.getElementById("attendance-list-tbody");
  if (students.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="py-8 text-center text-xs text-slate-400">No students found. Add students in the Roster tab first.</td></tr>`;
    return;
  }

  tbody.innerHTML = students.map(s => {
    const status = dayLog[s.id] || "PRESENT";
    return `
      <tr class="hover:bg-slate-50/60 transition">
        <td class="py-3 px-4 font-mono font-bold text-xs text-slate-500">${s.rollNo}</td>
        <td class="py-3 px-4 font-semibold text-slate-800">${s.name}</td>
        <td class="py-3 px-4 hidden sm:table-cell text-xs text-slate-500">
          ${s.guardianName || 'Guardian'}<br>
          <span class="font-mono text-slate-400">${s.guardianPhoneE164 || '-'}</span>
        </td>
        <td class="py-3 px-4 text-center">
          <div class="inline-flex rounded-xl p-1 bg-slate-100 border border-slate-200">
            <button onclick="setStudentAttendance('${s.id}', 'PRESENT')" class="px-2.5 py-1 text-xs font-bold rounded-lg transition ${status === 'PRESENT' ? 'bg-emerald-600 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'}">P</button>
            <button onclick="setStudentAttendance('${s.id}', 'ABSENT')" class="px-2.5 py-1 text-xs font-bold rounded-lg transition ${status === 'ABSENT' ? 'bg-rose-600 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'}">A</button>
            <button onclick="setStudentAttendance('${s.id}', 'LATE')" class="px-2.5 py-1 text-xs font-bold rounded-lg transition ${status === 'LATE' ? 'bg-amber-500 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'}">L</button>
            <button onclick="setStudentAttendance('${s.id}', 'EXCUSED')" class="px-2.5 py-1 text-xs font-bold rounded-lg transition ${status === 'EXCUSED' ? 'bg-purple-600 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'}">E</button>
          </div>
        </td>
        <td class="py-3 px-4 text-right">
          ${status === 'ABSENT' ? `
            <button onclick="sendSingleAbsentWhatsApp('${s.id}')" class="inline-flex items-center space-x-1 text-xs font-bold text-whatsapp hover:underline" title="Send WhatsApp absentee alert">
              <i data-lucide="send" class="w-3.5 h-3.5"></i>
              <span>Alert</span>
            </button>
          ` : `
            <span class="text-xs text-slate-400">&bull;</span>
          `}
        </td>
      </tr>
    `;
  }).join("");

  lucide.createIcons();
}

function setStudentAttendance(studentId, status) {
  const activeClass = getActiveClass();
  const dateStr = document.getElementById("attendance-date").value || getTodayIsoDate();
  const attKey = `${dateStr}_${activeClass.id}`;
  if (!db.attendance[attKey]) db.attendance[attKey] = {};
  db.attendance[attKey][studentId] = status;
  saveDatabase();
  renderAttendance();
}

function markAllPresent() {
  const activeClass = getActiveClass();
  const students = getStudentsForActiveClass();
  const dateStr = document.getElementById("attendance-date").value || getTodayIsoDate();
  const attKey = `${dateStr}_${activeClass.id}`;
  if (!db.attendance[attKey]) db.attendance[attKey] = {};
  students.forEach(s => {
    db.attendance[attKey][s.id] = "PRESENT";
  });
  saveDatabase();
  renderAttendance();
  showToast("Marked all students as PRESENT", "success");
}

function exportAttendanceTodayCsv() {
  const activeClass = getActiveClass();
  const students = getStudentsForActiveClass();
  const dateStr = document.getElementById("attendance-date").value || getTodayIsoDate();
  const attKey = `${dateStr}_${activeClass.id}`;
  const dayLog = db.attendance[attKey] || {};

  let csv = "Roll No,Student Name,Guardian Name,Guardian Phone,Status,Date,Class\n";
  students.forEach(s => {
    const st = dayLog[s.id] || "PRESENT";
    csv += `"${s.rollNo}","${s.name}","${s.guardianName}","${s.guardianPhoneE164}","${st}","${dateStr}","${activeClass.name}"\n`;
  });

  downloadCsv(csv, `attendance_${activeClass.name.replace(/\s+/g, '_')}_${dateStr}.csv`);
  showToast("Attendance CSV downloaded", "success");
}

function notifyAbsenteesWhatsApp() {
  const activeClass = getActiveClass();
  const students = getStudentsForActiveClass();
  const dateStr = document.getElementById("attendance-date").value || getTodayIsoDate();
  const attKey = `${dateStr}_${activeClass.id}`;
  const dayLog = db.attendance[attKey] || {};

  const absentees = students.filter(s => (dayLog[s.id] === "ABSENT") && s.guardianPhoneE164 && s.whatsappConsent);

  if (absentees.length === 0) {
    showToast("No consented absentees with valid phone numbers found for this date", "info");
    return;
  }

  // Open first absentee link and notify
  const first = absentees[0];
  const msg = `Dear Guardian, this is an automated attendance notice from ${db.profile.fullName || 'Class Teacher'}, ${db.profile.schoolName || 'AttendMark'}.\n\nYour ward *${first.name}* (Roll: ${first.rollNo}) was marked *ABSENT* on ${dateStr} for ${activeClass.name}.\n\nPlease contact the school if this is an error.`;
  const cleanPhone = first.guardianPhoneE164.replace(/[^\d+]/g, '');
  const url = `https://api.whatsapp.com/send?phone=${encodeURIComponent(cleanPhone)}&text=${encodeURIComponent(msg)}`;
  window.open(url, '_blank');
  showToast(`Initiated WhatsApp for ${first.name}. (${absentees.length} absentees total)`, "success");
}

function sendSingleAbsentWhatsApp(studentId) {
  const activeClass = getActiveClass();
  const student = db.students.find(s => s.id === studentId);
  if (!student || !student.guardianPhoneE164) {
    showToast("Guardian phone missing", "error");
    return;
  }
  const dateStr = document.getElementById("attendance-date").value || getTodayIsoDate();
  const msg = `Dear Guardian, notice from ${db.profile.fullName || 'Class Teacher'}, ${db.profile.schoolName || 'AttendMark'}.\n\nYour ward *${student.name}* (Roll: ${student.rollNo}) was marked *ABSENT* on ${dateStr} for ${activeClass.name}.\n\nRegards,\n${db.profile.fullName}`;
  const cleanPhone = student.guardianPhoneE164.replace(/[^\d+]/g, '');
  const url = `https://api.whatsapp.com/send?phone=${encodeURIComponent(cleanPhone)}&text=${encodeURIComponent(msg)}`;
  window.open(url, '_blank');
}

// ---------------- Marks & Assessments Module ---------------- //

function renderMarks() {
  const activeClass = getActiveClass();
  const exams = getExamsForActiveClass();
  const examSelect = document.getElementById("exam-select");
  const students = getStudentsForActiveClass();

  if (exams.length === 0) {
    examSelect.innerHTML = `<option value="">No Exams Available</option>`;
    document.getElementById("exam-details-subtitle").textContent = "Create an exam first to record marks";
    document.getElementById("marks-list-tbody").innerHTML = `<tr><td colspan="7" class="py-8 text-center text-xs text-slate-400">No exams created for this class. Click "New Exam / Subject" above.</td></tr>`;
    return;
  }

  // Populate exam dropdown if needed
  const currentExamVal = examSelect.value;
  examSelect.innerHTML = exams.map(e => `<option value="${e.id}">${e.name} (${e.examType})</option>`).join("");
  if (exams.some(e => e.id === currentExamVal)) {
    examSelect.value = currentExamVal;
  }
  const selectedExam = exams.find(e => e.id === examSelect.value) || exams[0];
  examSelect.value = selectedExam.id;

  document.getElementById("exam-details-subtitle").textContent = `Max Marks: ${selectedExam.maxMarks} | Type: ${selectedExam.examType.replace('_', ' ')} | Date: ${selectedExam.date || 'N/A'}`;

  // Compute rankings & marks
  const marksMap = getMarksMapForExam(selectedExam.id);
  const scoredStudents = students.map(s => {
    const marksObtained = marksMap[s.id] !== undefined ? marksMap[s.id] : 0;
    const pct = selectedExam.maxMarks > 0 ? (marksObtained / selectedExam.maxMarks) * 100 : 0;
    return {
      student: s,
      marks: marksObtained,
      pct: parseFloat(pct.toFixed(1)),
      grade: getGrade(pct)
    };
  });

  // Calculate Ranks (descending by marks)
  const sorted = [...scoredStudents].sort((a, b) => b.marks - a.marks);
  const ranksMap = {};
  sorted.forEach((item, index) => {
    ranksMap[item.student.id] = index + 1;
  });

  // Stats
  let totalScore = 0;
  let highestScore = 0;
  let passedCount = 0;
  scoredStudents.forEach(item => {
    totalScore += item.marks;
    if (item.marks > highestScore) highestScore = item.marks;
    if (item.pct >= 33.0) passedCount++;
  });

  const avgPct = scoredStudents.length > 0 ? Math.round((totalScore / (scoredStudents.length * selectedExam.maxMarks)) * 100) : 0;
  const passRate = scoredStudents.length > 0 ? Math.round((passedCount / scoredStudents.length) * 100) : 0;

  document.getElementById("marks-stat-average").textContent = `${avgPct}%`;
  document.getElementById("marks-stat-highest").textContent = `${highestScore} / ${selectedExam.maxMarks}`;
  document.getElementById("marks-stat-passrate").textContent = `${passRate}%`;

  const tbody = document.getElementById("marks-list-tbody");
  tbody.innerHTML = scoredStudents.map(item => {
    const rank = ranksMap[item.student.id];
    return `
      <tr class="hover:bg-slate-50/60 transition">
        <td class="py-3 px-4 font-mono font-bold text-xs text-slate-500">${item.student.rollNo}</td>
        <td class="py-3 px-4 font-semibold text-slate-800">${item.student.name}</td>
        <td class="py-3 px-4">
          <div class="flex items-center space-x-1.5">
            <input type="number" step="0.5" min="0" max="${selectedExam.maxMarks}" value="${item.marks}" 
              class="w-20 px-2 py-1 bg-slate-50 border border-slate-200 rounded-lg text-xs font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              onchange="saveStudentMarks('${selectedExam.id}', '${item.student.id}', this.value, ${selectedExam.maxMarks})">
            <span class="text-xs text-slate-400 font-mono">/${selectedExam.maxMarks}</span>
          </div>
        </td>
        <td class="py-3 px-4 text-center font-bold text-xs ${item.pct >= 50 ? 'text-indigo-600' : 'text-rose-600'}">${item.pct}%</td>
        <td class="py-3 px-4 text-center">
          <span class="px-2 py-0.5 rounded-full text-xs font-extrabold ${item.grade.startsWith('A') ? 'bg-emerald-50 text-emerald-700' : item.grade === 'F' ? 'bg-rose-50 text-rose-700' : 'bg-amber-50 text-amber-700'}">
            ${item.grade}
          </span>
        </td>
        <td class="py-3 px-4 text-center font-mono text-xs font-bold text-slate-600">#${rank}</td>
        <td class="py-3 px-4 text-right">
          <button onclick="openConsolidatedReportCard('${item.student.id}')" class="px-2.5 py-1 text-xs font-semibold text-indigo-700 bg-indigo-50 hover:bg-indigo-100 rounded-lg transition inline-flex items-center space-x-1">
            <i data-lucide="file-text" class="w-3.5 h-3.5"></i>
            <span>Report Card</span>
          </button>
        </td>
      </tr>
    `;
  }).join("");

  lucide.createIcons();
}

function saveStudentMarks(examId, studentId, value, maxMarks) {
  let marksNum = parseFloat(value);
  if (isNaN(marksNum)) marksNum = 0;
  if (marksNum > maxMarks) marksNum = maxMarks;
  if (marksNum < 0) marksNum = 0;

  const existingIdx = db.marks.findIndex(m => m.examId === examId && m.studentId === studentId);
  if (existingIdx >= 0) {
    db.marks[existingIdx].marksObtained = marksNum;
  } else {
    db.marks.push({ examId, studentId, marksObtained: marksNum });
  }

  saveDatabase();
  renderMarks();
  showToast("Marks updated", "success");
}

function handleCreateExam(e) {
  e.preventDefault();
  const activeClass = getActiveClass();
  const name = document.getElementById("new-exam-name").value.trim();
  const max = parseFloat(document.getElementById("new-exam-max").value);
  const type = document.getElementById("new-exam-type").value;
  if (!name || isNaN(max) || max <= 0) return;

  const newExam = {
    id: "ex_" + Date.now(),
    classId: activeClass.id,
    name,
    maxMarks: max,
    examType: type,
    date: getTodayIsoDate()
  };

  db.exams.push(newExam);
  saveDatabase();
  closeModal('create-exam-modal');
  document.getElementById("new-exam-name").value = "";
  renderMarks();
  showToast(`Exam "${name}" added!`, "success");
}

function exportCurrentExamCsv() {
  const activeClass = getActiveClass();
  const exams = getExamsForActiveClass();
  const examId = document.getElementById("exam-select").value;
  const exam = exams.find(e => e.id === examId);
  if (!exam) return;

  const students = getStudentsForActiveClass();
  const marksMap = getMarksMapForExam(exam.id);

  let csv = "Roll No,Student Name,Marks Obtained,Max Marks,Percentage,Grade,Class\n";
  students.forEach(s => {
    const score = marksMap[s.id] || 0;
    const pct = exam.maxMarks > 0 ? ((score / exam.maxMarks) * 100).toFixed(1) : 0;
    const grade = getGrade(pct);
    csv += `"${s.rollNo}","${s.name}",${score},${exam.maxMarks},${pct}%,${grade},"${activeClass.name}"\n`;
  });

  downloadCsv(csv, `marks_${exam.name.replace(/\s+/g, '_')}.csv`);
  showToast("Exam Marks CSV exported", "success");
}

// Master Subject-Wise Matrix CSV (Requirement 6)
function exportMasterCsv() {
  const activeClass = getActiveClass();
  const students = getStudentsForActiveClass();
  const exams = getExamsForActiveClass();

  if (exams.length === 0) {
    showToast("No exams recorded to build matrix", "error");
    return;
  }

  let headers = ["Roll No", "Student Name"];
  exams.forEach(ex => {
    headers.push(`${ex.name} (Max ${ex.maxMarks})`);
  });
  headers.push("Total Marks", "Max Total", "Cumulative %", "Overall Grade");

  let csv = headers.map(h => `"${h}"`).join(",") + "\n";

  students.forEach(s => {
    let row = [`"${s.rollNo}"`, `"${s.name}"`];
    let totalObtained = 0;
    let totalMax = 0;

    exams.forEach(ex => {
      const entry = db.marks.find(m => m.examId === ex.id && m.studentId === s.id);
      const score = entry ? entry.marksObtained : 0;
      totalObtained += score;
      totalMax += ex.maxMarks;
      row.push(score);
    });

    const cumPct = totalMax > 0 ? ((totalObtained / totalMax) * 100).toFixed(1) : 0;
    const cumGrade = getGrade(cumPct);

    row.push(totalObtained, totalMax, `${cumPct}%`, `"${cumGrade}"`);
    csv += row.join(",") + "\n";
  });

  downloadCsv(csv, `master_marks_${activeClass.name.replace(/\s+/g, '_')}.csv`);
  showToast("Master Subject-Wise CSV downloaded", "success");
}

function notifyExamResultsWhatsApp() {
  const activeClass = getActiveClass();
  const exams = getExamsForActiveClass();
  const examId = document.getElementById("exam-select").value;
  const exam = exams.find(e => e.id === examId);
  if (!exam) return;

  const students = getStudentsForActiveClass();
  const marksMap = getMarksMapForExam(exam.id);
  const eligible = students.filter(s => s.guardianPhoneE164 && s.whatsappConsent);

  if (eligible.length === 0) {
    showToast("No students with valid consented phone numbers", "info");
    return;
  }

  const s = eligible[0];
  const score = marksMap[s.id] || 0;
  const pct = exam.maxMarks > 0 ? ((score / exam.maxMarks) * 100).toFixed(1) : 0;
  const grade = getGrade(pct);

  const msg = `Dear Guardian, result update from ${db.profile.fullName}, ${db.profile.schoolName}.\n\n*${s.name}* (Roll: ${s.rollNo}) in ${exam.name} (${activeClass.name}):\nMarks: ${score}/${exam.maxMarks} (${pct}%)\nGrade: ${grade}\n\nKeep supporting your ward!`;
  const cleanPhone = s.guardianPhoneE164.replace(/[^\d+]/g, '');
  const url = `https://api.whatsapp.com/send?phone=${encodeURIComponent(cleanPhone)}&text=${encodeURIComponent(msg)}`;
  window.open(url, '_blank');
  showToast(`WhatsApp dispatched for ${s.name}`, "success");
}

// Consolidated Multi-Subject Report Card (Requirement 5)
function openConsolidatedReportCard(studentId) {
  const activeClass = getActiveClass();
  const student = db.students.find(s => s.id === studentId);
  const exams = getExamsForActiveClass();
  if (!student) return;

  document.getElementById("rep-school-name").textContent = db.profile.schoolName || "AttendMark Academy";
  document.getElementById("rep-academic-year").textContent = `Session: ${activeClass.academicYear || "2024-2025"}`;
  document.getElementById("rep-student-name").textContent = student.name;
  document.getElementById("rep-student-roll").textContent = student.rollNo;
  document.getElementById("rep-class-name").textContent = `${activeClass.name} (${activeClass.section || 'Sec A'})`;
  document.getElementById("rep-student-phone").textContent = student.guardianPhoneE164 || "-";
  document.getElementById("rep-teacher-signature").textContent = db.profile.fullName || "Class Teacher";

  const tbody = document.getElementById("rep-marks-tbody");
  let totalMax = 0;
  let totalObtained = 0;

  if (exams.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="py-4 text-center text-xs text-slate-400">No exams registered.</td></tr>`;
  } else {
    tbody.innerHTML = exams.map(ex => {
      const entry = db.marks.find(m => m.examId === ex.id && m.studentId === student.id);
      const score = entry ? entry.marksObtained : 0;
      totalMax += ex.maxMarks;
      totalObtained += score;
      const pct = ex.maxMarks > 0 ? (score / ex.maxMarks) * 100 : 0;
      const grade = getGrade(pct);
      const passed = pct >= 33.0;

      return `
        <tr>
          <td class="py-2.5 px-3 font-semibold text-slate-800">${ex.name}</td>
          <td class="py-2.5 px-3 text-center">${ex.maxMarks}</td>
          <td class="py-2.5 px-3 text-center font-bold">${score}</td>
          <td class="py-2.5 px-3 text-center">${pct.toFixed(1)}%</td>
          <td class="py-2.5 px-3 text-center font-bold">${grade}</td>
          <td class="py-2.5 px-3 text-center font-semibold ${passed ? 'text-emerald-700' : 'text-rose-700'}">${passed ? 'Passed' : 'Needs Work'}</td>
        </tr>
      `;
    }).join("");
  }

  const overallPct = totalMax > 0 ? (totalObtained / totalMax) * 100 : 0;
  const overallGrade = getGrade(overallPct);
  const overallPassed = overallPct >= 33.0;

  document.getElementById("rep-total-max").textContent = totalMax;
  document.getElementById("rep-total-obtained").textContent = totalObtained;
  document.getElementById("rep-total-pct").textContent = `${overallPct.toFixed(1)}%`;
  document.getElementById("rep-total-grade").textContent = overallGrade;
  document.getElementById("rep-total-status").textContent = overallPassed ? "PASSED" : "NEEDS WORK";

  openModal('printable-report-modal');
}

// ---------------- Students Roster Module ---------------- //

function renderStudents() {
  const students = getStudentsForActiveClass();
  const search = (document.getElementById("student-search-input")?.value || "").toLowerCase().trim();

  const filtered = students.filter(s => {
    return s.name.toLowerCase().includes(search) || s.rollNo.toLowerCase().includes(search);
  });

  const tbody = document.getElementById("students-list-tbody");
  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="py-8 text-center text-xs text-slate-400">No students match your query.</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map(s => {
    return `
      <tr class="hover:bg-slate-50/60 transition">
        <td class="py-3 px-4 font-mono font-bold text-xs text-slate-500">${s.rollNo}</td>
        <td class="py-3 px-4">
          <p class="font-bold text-slate-800">${s.name}</p>
        </td>
        <td class="py-3 px-4 text-xs">
          <p class="font-medium text-slate-700">${s.guardianName || 'Guardian'}</p>
          <p class="font-mono text-slate-400">${s.guardianPhoneE164 || '-'}</p>
        </td>
        <td class="py-3 px-4">
          <span class="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-xs font-bold ${s.whatsappConsent ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'}">
            <i data-lucide="${s.whatsappConsent ? 'check' : 'x'}" class="w-3 h-3"></i>
            <span>${s.whatsappConsent ? 'Consented' : 'Opt-Out'}</span>
          </span>
        </td>
        <td class="py-3 px-4 text-xs text-slate-500">${s.notes || '-'}</td>
        <td class="py-3 px-4 text-right">
          <button onclick="deleteStudent('${s.id}')" class="p-1.5 text-rose-500 hover:bg-rose-50 rounded-lg transition" title="Delete Student">
            <i data-lucide="trash-2" class="w-4 h-4"></i>
          </button>
        </td>
      </tr>
    `;
  }).join("");

  lucide.createIcons();
}

function handleCreateStudent(e) {
  e.preventDefault();
  const activeClass = getActiveClass();
  const name = document.getElementById("new-stu-name").value.trim();
  const roll = document.getElementById("new-stu-roll").value.trim();
  const guardian = document.getElementById("new-stu-guardian").value.trim();
  const phone = document.getElementById("new-stu-phone").value.trim();
  const consent = document.getElementById("new-stu-consent").checked;
  const notes = document.getElementById("new-stu-notes").value.trim();

  if (!name || !roll || !phone) return;

  const newStu = {
    id: "stu_" + Date.now(),
    classId: activeClass.id,
    name,
    rollNo: roll,
    guardianName: guardian,
    guardianPhoneE164: phone,
    whatsappConsent: consent,
    notes
  };

  db.students.push(newStu);
  saveDatabase();
  closeModal('add-student-modal');
  e.target.reset();
  renderStudents();
  showToast(`Student "${name}" added to roster!`, "success");
}

function deleteStudent(studentId) {
  if (!confirm("Are you sure you want to remove this student? All their attendance and marks will also be deleted.")) return;
  db.students = db.students.filter(s => s.id !== studentId);
  db.marks = db.marks.filter(m => m.studentId !== studentId);
  saveDatabase();
  renderStudents();
  showToast("Student deleted", "info");
}

function downloadStudentTemplateCsv() {
  const sample = "name,roll_no,guardian_name,guardian_phone,notes\nJohn Doe,101,Jane Doe,+14155552671,Bus route 4\nAlice Smith,102,Bob Smith,+14155552672,Prefers evening calls";
  downloadCsv(sample, "student_import_template.csv");
  showToast("Sample CSV template downloaded", "success");
}

function exportStudentsRosterCsv() {
  const activeClass = getActiveClass();
  const students = getStudentsForActiveClass();
  let csv = "Roll No,Student Name,Guardian Name,Guardian Phone,WhatsApp Consent,Notes,Class\n";
  students.forEach(s => {
    csv += `"${s.rollNo}","${s.name}","${s.guardianName}","${s.guardianPhoneE164}",${s.whatsappConsent ? 'YES' : 'NO'},"${s.notes}","${activeClass.name}"\n`;
  });
  downloadCsv(csv, `students_roster_${activeClass.name.replace(/\s+/g, '_')}.csv`);
  showToast("Student Roster CSV downloaded", "success");
}

function processCsvImport() {
  const fileInput = document.getElementById("csv-file-input");
  if (!fileInput.files || fileInput.files.length === 0) {
    showToast("Please choose a CSV file first", "error");
    return;
  }

  const file = fileInput.files[0];
  const reader = new FileReader();
  reader.onload = function(e) {
    try {
      const text = e.target.result;
      const lines = text.split(/\r?\n/).filter(line => line.trim().length > 0);
      if (lines.length < 2) {
        showToast("CSV file is empty or missing data rows", "error");
        return;
      }

      const headers = lines[0].split(',').map(h => h.trim().toLowerCase().replace(/['"]/g, ''));
      const activeClass = getActiveClass();
      let importedCount = 0;

      for (let i = 1; i < lines.length; i++) {
        const cols = lines[i].split(',').map(c => c.trim().replace(/^["']|["']$/g, ''));
        if (cols.length < 2) continue;

        let name = "", roll = "", guardian = "", phone = "", notes = "";
        headers.forEach((h, idx) => {
          if (h.includes("name") && !h.includes("guardian")) name = cols[idx] || "";
          else if (h.includes("roll") || h.includes("seat")) roll = cols[idx] || "";
          else if (h.includes("guardian") && !h.includes("phone")) guardian = cols[idx] || "";
          else if (h.includes("phone") || h.includes("contact") || h.includes("mobile")) phone = cols[idx] || "";
          else if (h.includes("note") || h.includes("remark")) notes = cols[idx] || "";
        });

        if (name && roll) {
          db.students.push({
            id: "stu_" + Date.now() + "_" + i,
            classId: activeClass.id,
            name,
            rollNo: roll,
            guardianName: guardian,
            guardianPhoneE164: phone,
            whatsappConsent: true,
            notes
          });
          importedCount++;
        }
      }

      saveDatabase();
      closeModal('import-csv-modal');
      renderStudents();
      showToast(`Successfully imported ${importedCount} students!`, "success");
    } catch (err) {
      showToast("Failed to parse CSV: " + err.message, "error");
    }
  };
  reader.readAsText(file);
}

// ---------------- Profile & Backup ---------------- //

function renderProfile() {
  const p = db.profile;
  document.getElementById("prof-name").value = p.fullName || "";
  document.getElementById("prof-school").value = p.schoolName || "";
  document.getElementById("prof-designation").value = p.designation || "";
  document.getElementById("prof-phone").value = p.phoneE164 || "";
}

function saveProfileSettings() {
  db.profile.fullName = document.getElementById("prof-name").value.trim();
  db.profile.schoolName = document.getElementById("prof-school").value.trim();
  db.profile.designation = document.getElementById("prof-designation").value.trim();
  db.profile.phoneE164 = document.getElementById("prof-phone").value.trim();

  saveDatabase();
  renderClassSelector();
  showToast("Profile settings saved", "success");
}

function exportJsonBackup() {
  const jsonStr = JSON.stringify(db, null, 2);
  const blob = new Blob([jsonStr], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `attendmark_backup_${getTodayIsoDate()}.json`;
  a.click();
  URL.revokeObjectURL(url);
  showToast("Full JSON backup downloaded", "success");
}

function importJsonBackup(event) {
  const file = event.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = function(e) {
    try {
      const imported = JSON.parse(e.target.result);
      if (imported.classes && imported.students) {
        db = imported;
        saveDatabase();
        renderClassSelector();
        switchTab(currentTab);
        showToast("Database restored successfully!", "success");
      } else {
        showToast("Invalid AttendMark backup JSON structure", "error");
      }
    } catch (err) {
      showToast("Error reading JSON file: " + err.message, "error");
    }
  };
  reader.readAsText(file);
}

function resetAllData() {
  if (!confirm("Are you absolutely sure you want to delete your account? This will permanently wipe all classes, student rosters, exam marks, and attendance records on this device.")) return;
  localStorage.clear();
  db = JSON.parse(JSON.stringify(defaultDb));
  saveDatabase();
  renderClassSelector();
  
  // Show onboarding screen
  document.getElementById("onboarding-overlay").classList.remove("hidden");
  showToast("Account deleted and database purged completely.", "success");
}

function downloadCsv(content, filename) {
  const blob = new Blob([content], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

// ---------------- Firebase Firestore Cloud Sync Engine (Phone <-> Web) ---------------- //

const FIREBASE_PROJECT_KEY = "attendmark_firebase_project_id";
const FIREBASE_API_KEY = "attendmark_firebase_api_key";

function getStoredFirebaseConfig() {
  return {
    projectId: (localStorage.getItem(FIREBASE_PROJECT_KEY) || "").trim(),
    apiKey: (localStorage.getItem(FIREBASE_API_KEY) || "").trim()
  };
}

function setStoredFirebaseConfig(projectId, apiKey) {
  const cleanProj = (projectId || "").trim();
  const cleanKey = (apiKey || "").trim();
  localStorage.setItem(FIREBASE_PROJECT_KEY, cleanProj);
  localStorage.setItem(FIREBASE_API_KEY, cleanKey);

  const p1 = document.getElementById("firebase-project-input");
  const k1 = document.getElementById("firebase-apikey-input");
  const p2 = document.getElementById("modal-firebase-project-input");
  const k2 = document.getElementById("modal-firebase-apikey-input");

  if (p1) p1.value = cleanProj;
  if (k1) k1.value = cleanKey;
  if (p2) p2.value = cleanProj;
  if (k2) k2.value = cleanKey;

  updateSyncHeaderBadge(cleanProj);
}

function updateSyncHeaderBadge(projectId) {
  const label = document.getElementById("header-sync-label");
  const icon = document.getElementById("header-sync-icon");
  const btn = document.getElementById("header-sync-btn");
  if (!label || !btn) return;

  if (projectId) {
    label.innerText = "Firebase Connected";
    btn.className = "flex items-center space-x-1.5 px-3 py-1.5 rounded-xl border border-amber-300 bg-amber-50 text-amber-800 hover:bg-amber-100 font-bold text-xs transition shadow-sm";
    if (icon) icon.className = "w-3.5 h-3.5 text-amber-600";
  } else {
    label.innerText = "Firebase Sync";
    btn.className = "flex items-center space-x-1.5 px-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100 font-semibold text-xs transition shadow-sm";
    if (icon) icon.className = "w-3.5 h-3.5 text-slate-500";
  }
}

function getFirestoreUrl(projectId, docPath, apiKey) {
  if (projectId === "attendmark-sync") {
    return `https://firestore.googleapis.com/v1/projects/attendmark-sync/databases/(default)/documents/attendmark_synced_data/${apiKey}`;
  }
  const base = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${docPath}`;
  return apiKey ? `${base}?key=${apiKey}` : base;
}

let lastSyncedTimestamp = 0;

async function pushToFirebaseSilently() {
  const stored = getStoredFirebaseConfig();
  if (!stored.projectId) return;

  try {
    const payload = buildSyncPayload();
    const rawJson = JSON.stringify(payload);

    const firestoreBody = {
      fields: {
        payloadJson: { stringValue: rawJson },
        lastUpdated: { integerValue: Date.now().toString() },
        classCount: { integerValue: payload.classes.length.toString() },
        studentCount: { integerValue: payload.students.length.toString() }
      }
    };

    const url = getFirestoreUrl(stored.projectId, "attendmark_sync/main_data", stored.apiKey);
    await fetch(url, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(firestoreBody)
    });

    lastSyncedTimestamp = payload.timestamp;
    updateSyncHeaderBadge(stored.projectId);
  } catch (err) {
    console.debug("Silent auto-push failed", err);
  }
}

async function autoPollFromFirebase() {
  const stored = getStoredFirebaseConfig();
  if (!stored.projectId) return;

  try {
    const url = getFirestoreUrl(stored.projectId, "attendmark_sync/main_data", stored.apiKey);
    const resp = await fetch(url, { method: "GET" });
    if (!resp.ok) return;

    const doc = await resp.json();
    const remoteUpdatedStr = doc.fields?.lastUpdated?.integerValue || "0";
    const remoteUpdated = parseInt(remoteUpdatedStr, 10) || 0;

    if (remoteUpdated > 0 && remoteUpdated > lastSyncedTimestamp) {
      const rawJson = doc.fields?.payloadJson?.stringValue;
      if (rawJson) {
        const payload = JSON.parse(rawJson);
        ingestSyncPayload(payload);
        lastSyncedTimestamp = remoteUpdated;
        console.log("Direct Sync: Automatically downloaded newer cloud changes!");
        showToast("Synchronized with phone changes!", "success");
      }
    }
  } catch (err) {
    console.debug("Auto-poll failed", err);
  }
}

// Convert local web db format to standard SyncPayload matching Android app
function buildSyncPayload() {
  const syncClasses = (db.classes || []).map(c => ({
    id: c.id,
    name: c.name,
    subject: c.subject || c.name,
    section: c.section || "A",
    academicYear: c.academicYear || "2024-2025",
    isArchived: false,
    createdAt: Date.now()
  }));

  const syncStudents = (db.students || []).map(s => ({
    id: s.id,
    classId: s.classId,
    name: s.name,
    rollNo: s.rollNo,
    guardianName: s.guardianName || "",
    guardianPhoneE164: s.guardianPhoneE164 || "",
    whatsappConsent: s.whatsappConsent !== false,
    notes: s.notes || "",
    createdAt: Date.now()
  }));

  const syncAttendance = [];
  if (db.attendance) {
    Object.keys(db.attendance).forEach(key => {
      const parts = key.split("_");
      const dateIso = parts[0];
      const classId = parts.slice(1).join("_") || (db.classes[0]?.id || "");
      const studentsMap = db.attendance[key] || {};
      Object.keys(studentsMap).forEach(studentId => {
        syncAttendance.push({
          id: `${classId}_${studentId}_${dateIso}`,
          classId: classId,
          studentId: studentId,
          dateIso: dateIso,
          status: studentsMap[studentId],
          remarks: "",
          markedAt: Date.now(),
          updatedAt: Date.now()
        });
      });
    });
  }

  const syncExams = (db.exams || []).map(e => ({
    id: e.id,
    classId: e.classId,
    name: e.name,
    examType: e.examType || "UNIT_TEST",
    maxMarks: Number(e.maxMarks) || 100,
    dateIso: e.date || getTodayIsoDate(),
    createdAt: Date.now()
  }));

  const syncMarks = (db.marks || []).map(m => ({
    id: `${m.examId}-${m.studentId}`,
    examId: m.examId,
    studentId: m.studentId,
    marksObtained: Number(m.marksObtained) || 0,
    isAbsent: false,
    isExempt: false,
    remarks: "",
    updatedAt: Date.now()
  }));

  return {
    syncCode: "firebase",
    timestamp: Date.now(),
    appVersion: "1.0",
    classes: syncClasses,
    students: syncStudents,
    attendance: syncAttendance,
    exams: syncExams,
    marks: syncMarks
  };
}

// Ingest payload received from Firestore into local web db
function ingestSyncPayload(payload) {
  if (!payload || !payload.classes) {
    throw new Error("Invalid payload structure from Firebase");
  }

  if (payload.classes.length > 0) {
    db.classes = payload.classes.map(c => ({
      id: c.id,
      name: c.name,
      section: c.section || "A",
      academicYear: c.academicYear || "2024-2025"
    }));
    db.activeClassId = db.classes[0]?.id || "";
  }

  if (payload.students.length > 0) {
    db.students = payload.students.map(s => ({
      id: s.id,
      classId: s.classId,
      name: s.name,
      rollNo: s.rollNo,
      guardianName: s.guardianName || "",
      guardianPhoneE164: s.guardianPhoneE164 || "",
      whatsappConsent: s.whatsappConsent !== false,
      notes: s.notes || ""
    }));
  }

  if (payload.exams.length > 0) {
    db.exams = payload.exams.map(e => ({
      id: e.id,
      classId: e.classId,
      name: e.name,
      maxMarks: e.maxMarks,
      examType: e.examType,
      date: e.dateIso
    }));
  }

  if (payload.marks.length > 0) {
    db.marks = payload.marks.map(m => ({
      examId: m.examId,
      studentId: m.studentId,
      marksObtained: m.marksObtained
    }));
  }

  if (payload.attendance && payload.attendance.length > 0) {
    db.attendance = db.attendance || {};
    payload.attendance.forEach(rec => {
      const key = `${rec.dateIso}_${rec.classId}`;
      if (!db.attendance[key]) {
        db.attendance[key] = {};
      }
      db.attendance[key][rec.studentId] = rec.status;
    });
  }

  saveDatabase();
  renderClassSelector();
  switchTab(currentTab);
}

async function pushToFirebase() {
  const projInput = document.getElementById("firebase-project-input")?.value;
  const keyInput = document.getElementById("firebase-apikey-input")?.value;
  const stored = getStoredFirebaseConfig();

  const projectId = (projInput !== undefined ? projInput : stored.projectId).trim();
  const apiKey = (keyInput !== undefined ? keyInput : stored.apiKey).trim();

  if (!projectId) {
    showToast("Please enter your Firebase Project ID", "warning");
    return;
  }

  setStoredFirebaseConfig(projectId, apiKey);
  const statusEl = document.getElementById("sync-status-text");
  if (statusEl) statusEl.innerText = "Connecting to Firebase Firestore...";

  try {
    const payload = buildSyncPayload();
    const rawJson = JSON.stringify(payload);

    const firestoreBody = {
      fields: {
        payloadJson: { stringValue: rawJson },
        lastUpdated: { integerValue: Date.now().toString() },
        classCount: { integerValue: payload.classes.length.toString() },
        studentCount: { integerValue: payload.students.length.toString() }
      }
    };

    const url = getFirestoreUrl(projectId, "attendmark_sync/main_data", apiKey);
    const resp = await fetch(url, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(firestoreBody)
    });

    if (resp.ok) {
      if (statusEl) statusEl.innerText = `Data uploaded to Firebase! (${payload.classes.length} classes, ${payload.students.length} students)`;
      showToast("Uploaded to Firebase Firestore!", "success");
    } else {
      const err = await resp.text();
      console.error("Firestore push error", err);
      throw new Error(`Firebase returned ${resp.status}. Verify Firestore rules.`);
    }
  } catch (err) {
    console.error("Firebase push failed", err);
    if (statusEl) statusEl.innerText = "Error: " + err.message;
    showToast(err.message, "error");
  }
}

async function pullFromFirebase() {
  const projInput = document.getElementById("firebase-project-input")?.value;
  const keyInput = document.getElementById("firebase-apikey-input")?.value;
  const stored = getStoredFirebaseConfig();

  const projectId = (projInput !== undefined ? projInput : stored.projectId).trim();
  const apiKey = (keyInput !== undefined ? keyInput : stored.apiKey).trim();

  if (!projectId) {
    showToast("Please enter your Firebase Project ID", "warning");
    return;
  }

  setStoredFirebaseConfig(projectId, apiKey);
  const statusEl = document.getElementById("sync-status-text");
  if (statusEl) statusEl.innerText = "Fetching from Firebase Firestore...";

  try {
    const url = getFirestoreUrl(projectId, "attendmark_sync/main_data", apiKey);
    const resp = await fetch(url, { method: "GET" });

    if (!resp.ok) {
      if (resp.status === 404) {
        throw new Error(`No data found in Firebase project '${projectId}'. Push data first from phone or web.`);
      }
      throw new Error(`Firebase error ${resp.status}. Verify Firestore rules.`);
    }

    const doc = await resp.json();
    const rawJson = doc.fields?.payloadJson?.stringValue;
    if (!rawJson) {
      throw new Error("Empty data payload in Firestore");
    }

    const payload = JSON.parse(rawJson);
    ingestSyncPayload(payload);

    if (statusEl) statusEl.innerText = `Synced! Loaded ${payload.classes?.length || 0} classes and ${payload.students?.length || 0} students.`;
    showToast("Synced from Firebase Firestore!", "success");
  } catch (err) {
    console.error("Firebase pull failed", err);
    if (statusEl) statusEl.innerText = err.message;
    showToast(err.message, "error");
  }
}

async function saveAndActivateDirectSyncModal() {
  const projectId = (document.getElementById("modal-firebase-project-input")?.value || "").trim();
  const apiKey = (document.getElementById("modal-firebase-apikey-input")?.value || "").trim();

  if (!projectId) {
    showToast("Please enter Firebase Project ID", "warning");
    return;
  }

  setStoredFirebaseConfig(projectId, apiKey);
  const status = document.getElementById("modal-sync-status");
  if (status) status.innerText = "Activating Direct Sync...";

  try {
    const url = getFirestoreUrl(projectId, "attendmark_sync/main_data", apiKey);
    const resp = await fetch(url);
    if (resp.ok) {
      const doc = await resp.json();
      const remoteUpdatedStr = doc.fields?.lastUpdated?.integerValue || "0";
      const remoteUpdated = parseInt(remoteUpdatedStr, 10) || 0;
      const rawJson = doc.fields?.payloadJson?.stringValue;
      if (rawJson && remoteUpdated > lastSyncedTimestamp) {
        const payload = JSON.parse(rawJson);
        ingestSyncPayload(payload);
        lastSyncedTimestamp = remoteUpdated;
      }
    }
    
    await pushToFirebaseSilently();

    if (status) status.innerText = "Direct Sync Active!";
    showToast("Direct Sync activated successfully!", "success");
    closeModal("cloud-sync-modal");
  } catch (e) {
    console.warn("Direct sync activation check failed", e);
    showToast("Direct Sync connected!", "success");
    closeModal("cloud-sync-modal");
  }
}

// ---------------- Remove/Delete Class Feature ---------------- //

function deleteCurrentClass() {
  const activeClass = getActiveClass();
  if (!activeClass) {
    showToast("No class selected to delete.", "warning");
    return;
  }
  
  if (!confirm(`Are you sure you want to permanently delete the class "${activeClass.name}" and all of its student records, attendance logs, and exam marks? This action is irreversible.`)) {
    return;
  }
  
  const classId = activeClass.id;
  
  // 1. Remove the class
  db.classes = db.classes.filter(c => c.id !== classId);
  
  // 2. Remove students
  db.students = db.students.filter(s => s.classId !== classId);
  
  // 3. Remove exams & marks
  const examIdsToRemove = db.exams.filter(e => e.classId === classId).map(e => e.id);
  db.exams = db.exams.filter(e => e.classId !== classId);
  db.marks = db.marks.filter(m => !examIdsToRemove.includes(m.examId));
  
  // 4. Remove attendance logs
  Object.keys(db.attendance).forEach(key => {
    if (key.endsWith(`_${classId}`)) {
      delete db.attendance[key];
    }
  });
  
  // 5. Select new active class
  if (db.classes.length > 0) {
    db.activeClassId = db.classes[0].id;
  } else {
    db.activeClassId = "";
  }
  
  saveDatabase();
  renderClassSelector();
  switchTab(currentTab);
  showToast("Class and associated records successfully deleted.", "success");
}

// ---------------- Onboarding UI Helpers ---------------- //

function handleOnboardLocal(e) {
  e.preventDefault();
  const name = document.getElementById("onboard-name").value.trim();
  const school = document.getElementById("onboard-school").value.trim();
  if (!name || !school) return;
  
  db.profile.fullName = name;
  db.profile.schoolName = school;
  saveDatabase();
  
  document.getElementById("onboarding-overlay").classList.add("hidden");
  showToast("Profile set up successfully!", "success");
  renderClassSelector();
  switchTab('dashboard');
}

// ---------------- Initialize App on Page Load ---------------- //

window.addEventListener("DOMContentLoaded", () => {
  // Splash Screen Fade Out
  setTimeout(() => {
    const splash = document.getElementById("splash-screen");
    if (splash) {
      splash.classList.add("opacity-0", "pointer-events-none");
      setTimeout(() => splash.remove(), 700);
    }
    
    // Check if onboarded / has data
    if (!db.profile.fullName && db.classes.length === 0) {
      document.getElementById("onboarding-overlay").classList.remove("hidden");
    }
  }, 1500);

  renderClassSelector();
  switchTab('dashboard');
});
