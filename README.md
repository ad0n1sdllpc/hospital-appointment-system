# 🏥 Hospital Appointment and Patient Record Management System

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Platform](https://img.shields.io/badge/Platform-Console-blue?style=for-the-badge)
![OOP](https://img.shields.io/badge/OOP-Principles-green?style=for-the-badge)
![RBAC](https://img.shields.io/badge/Access-Role--Based-purple?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Project Structure](#-project-structure)
- [OOP Concepts](#-oop-concepts)
- [Getting Started](#-getting-started)
- [Default Credentials](#-default-credentials)
- [Role Permissions](#-role-permissions)
- [Data Storage](#-data-storage)
- [Appointment Lifecycle](#-appointment-lifecycle)
- [Doctor Availability System](#-doctor-availability-system)
- [Doctors Roster](#-doctors-roster)
- [Architecture Decisions](#-architecture-decisions)
- [Technologies Used](#-technologies-used)

---

## 🔍 Overview

The **Hospital Management System** is a fully console-based Java application implementing a complete **Role-Based Access Control (RBAC)** system across three distinct user roles: **Administrator**, **Doctor**, and **Patient**.

Each role gets its own dashboard with scoped permissions. Doctors can approve or reject patient appointment requests. Patients book appointments that start as `PENDING` and require doctor approval. Admins have full system control.

All data is saved automatically to plain-text files after every action and fully restored on restart — no database required.

---

## ✨ Features

### 🔐 Authentication System

- Secure login with username and password validation
- Patient self-registration — creates a login account and patient profile in one step
- Password change for all roles
- Account deactivation by admin (soft-delete, data preserved)
- Role-based session routing — login lands you automatically in the correct dashboard

### 📅 Appointment Management

- Book appointments with doctor, date, and time slot selection
- Patient bookings start as **PENDING** — await doctor approval
- Admin bookings are **BOOKED** (auto-approved, no review needed)
- Approve / Reject / Cancel / Complete / Reschedule workflows
- Full appointment detail view with all patient and doctor info
- Search by patient name, patient ID, date, doctor name, or appointment ID
- Filter by status (7 states) or by department (12 departments)

### 👨‍⚕️ Doctor Approval System

- Doctors review all PENDING requests in a color-coded table (`[7] Review Appt Requests`)
- Approve → status becomes `APPROVED`
- Reject with optional reason → status becomes `REJECTED`, slot freed automatically
- Rejected slots become immediately available for other patients

### 🗓️ Doctor Availability Manager

- Doctors set available slots **per date** via `[4] Set My Available Slots`
- Three slot states: `FREE` · `UNAVAILABLE` · `RESERVED`
- `FREE` — visible and bookable by patients
- `UNAVAILABLE` — doctor blocked it; hidden from patients
- `RESERVED` — has an active appointment; read-only
- Toggle interactively; state persists in `data/availability.txt`

### ⏳ Waitlist System

- Offered automatically when all slots on a date are full
- Queue position tracked per doctor + date
- When a slot is cancelled, the #1 waitlist patient is auto-promoted to `PENDING`

### 📊 Reports & Analytics

- Total appointments broken down by all 7 statuses with ANSI colors
- Per-department count and percentage share
- Top 5 busiest dates
- Waitlist count included

### 🎨 ANSI Color Terminal UI

| Color         | Meaning                                       |
| ------------- | --------------------------------------------- |
| 🔵 **Cyan**   | Section headers, Doctor Dashboard borders     |
| 🟢 **Green**  | APPROVED / FREE slots / success messages      |
| 🔴 **Red**    | REJECTED / UNAVAILABLE slots / error messages |
| 🟡 **Yellow** | PENDING status / warnings                     |
| ⚫ **Gray**   | RESERVED slots / dimmed prompts               |

---

## 📸 Screenshots

### Welcome Banner

```
##########################################################################
##         HOSPITAL APPOINTMENT & PATIENT RECORD                      ##
##                    MANAGEMENT SYSTEM                                ##
##          Professional Healthcare Platform                           ##
##########################################################################
```

### Doctor Dashboard

```
+======================================================================+
|                          DOCTOR DASHBOARD                            |
|         Logged in as: Dr. Santos  |  Medical Staff Portal           |
+----------------------------------------------------------------------+
|  MY SCHEDULE                    |  PATIENT RECORDS                  |
|  [1]  My Appointments Today     |  [5]  Patient Record Lookup       |
|  [2]  My Full Schedule          |  [6]  Appointment Detail          |
|  [3]  Mark Appointment Done     |                                   |
|  [4]  Set My Available Slots    |  [7]  Review Appt Requests        |
|                                 |  [0]  Logout                      |
+======================================================================+
```

### Pending Requests Table

```
========================================================================
                    PENDING APPOINTMENT REQUESTS
========================================================================
Appt ID          Patient Name           Date         Time     Status
------------------------------------------------------------------------
APT-2026-002     Juan Cruz              2026-08-10   10:00    [ PENDING ]
APT-2026-005     Ana Reyes              2026-08-11   14:00    [ PENDING ]
========================================================================
Showing 2 pending request(s)
```

### Slot Availability Manager

```
========================================================================
                         SET AVAILABLE SLOTS
------------------------------------------------------------------------
Doctor : Dr. Santos
Date   : August 10, 2026
========================================================================
 1.  7:00 AM  - 8:00 AM          [  RESERVED   ]
 2.  8:01 AM  - 9:00 AM          [ UNAVAILABLE ]
 3.  9:01 AM  - 10:00 AM         [  FREE       ]
 4.  10:01 AM - 11:00 AM         [  FREE       ]
 5.  1:00 PM  - 2:00 PM          [  FREE       ]
========================================================================
Legend: [  FREE       ]  [ UNAVAILABLE ]  [  RESERVED   ]
```

### Administrator Dashboard

```
+======================================================================+
|                      ADMINISTRATOR DASHBOARD                         |
|      Logged in as: System Administrator  |  Full System Access       |
+----------------------------------------------------------------------+
|  APPOINTMENTS                   |  MANAGEMENT                       |
|  [1]  Book Appointment          |  [9]  Manage Doctors              |
|  [2]  View All Appointments     |  [10] Manage Patients             |
|  [3]  Appointment Detail        |  [11] View Waitlist               |
|  [4]  Approve / Reschedule      |  [12] View Report Summary         |
|  [5]  Cancel Appointment        |                                   |
|  [6]  Complete Appointment      |  SEARCH & FILTER                  |
|  [7]  View Doctor Schedule      |  [13] Search All Records          |
|  [8]  Manage Waitlist           |  [14] Filter by Status            |
|                                 |  [15] Filter by Department        |
|                                 |  [0]  Logout                      |
+======================================================================+
```

---

## 📁 Project Structure

```
HospitalRBAS/
│
├── data/                                    ← Auto-created on first run
│   ├── users.txt                            ← Login accounts (all roles)
│   ├── patients.txt                         ← Patient domain records
│   ├── doctors.txt                          ← Doctor domain records
│   ├── appointments.txt                     ← Appointment records
│   ├── waitlist.txt                         ← Waitlist entries
│   └── availability.txt                     ← Doctor per-date slot states
│
└── src/com/hospital/appointment/
    │
    ├── Main.java                            ← Entry point: bootstrap + role router
    │
    ├── auth/
    │   └── AuthService.java                 ← Login, registration, password change
    │
    ├── dashboard/
    │   ├── AdminDashboard.java              ← Admin menu loop (15 options)
    │   ├── DoctorDashboard.java             ← Doctor menu loop (7 options)
    │   └── PatientDashboard.java            ← Patient menu loop (6 options)
    │
    ├── enums/
    │   ├── AppointmentStatus.java           ← 7 states: PENDING|APPROVED|BOOKED|CANCELLED|COMPLETED|REJECTED|NO_SHOW
    │   ├── Department.java                  ← 12 departments
    │   ├── UserRole.java                    ← ADMIN | DOCTOR | PATIENT
    │   └── WaitlistStatus.java              ← WAITING | PROMOTED | REMOVED | EXPIRED
    │
    ├── model/
    │   ├── Person.java                      ← Abstract base class
    │   ├── Patient.java                     ← extends Person
    │   ├── Doctor.java                      ← Doctor domain record
    │   ├── User.java                        ← Login account linked to Patient/Doctor
    │   ├── Appointment.java                 ← Full appointment record
    │   ├── DoctorAvailability.java          ← Per-date slot state map (FREE/UNAVAIL/RESERVED)
    │   └── WaitlistEntry.java               ← Waitlist record with queue position
    │
    ├── service/
    │   └── AppointmentService.java          ← All business logic, shared by all dashboards
    │
    ├── storage/
    │   └── DataStore.java                   ← Central in-memory store + all file I/O
    │
    ├── ui/
    │   └── Console.java                     ← All rendering: banners, tables, ANSI colors
    │
    └── util/
        ├── DateUtils.java                   ← Date parsing, formatting, timestamps
        ├── IdGenerator.java                 ← ID generation + counter sync after load
        └── InputValidator.java              ← Input reading with validation retry loops
```

**22 source files · ~3,300 lines of Java**

---

## 🧠 OOP Concepts

| Concept                             | Where & How                                                                                                                                          |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Encapsulation**                   | Every model field is `private`. Access is only through getters and setters. No class reaches into another class's data directly.                     |
| **Inheritance**                     | `Patient extends Person` — inherits `name`, `age`, `address`, `contactNumber`, `email` from the abstract base class.                                 |
| **Abstraction**                     | `Person` is declared `abstract` — it defines the contract but cannot be instantiated on its own.                                                     |
| **Polymorphism**                    | Role routing in `Main.java` dispatches to the correct dashboard at runtime based on `UserRole` without cascading if-else chains.                     |
| **Enums**                           | `UserRole` (3 values), `AppointmentStatus` (7 values), `Department` (12 values), `WaitlistStatus` (4 values) — each carries a display name and icon. |
| **Separation of Concerns**          | Auth / Service / Dashboard / Storage / UI / Util are fully separate layers. No class does two jobs.                                                  |
| **Single Responsibility Principle** | `Console.java` only renders. `DataStore.java` only persists. `AppointmentService.java` only applies rules.                                           |
| **DRY Principle**                   | `AppointmentService` is written once and called by all three dashboards — no duplicated booking or cancellation logic.                               |

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher (`java --version` to check)
- Any terminal — Windows CMD / PowerShell, macOS Terminal, Linux bash

### Clone

```bash
git clone https://github.com/your-username/hospital-management-system.git
cd hospital-management-system
```

### Compile

```bash
# Create output folder
mkdir -p out

# Linux / macOS
javac -d out $(find com -name "*.java")

# Windows CMD
for /r com %f in (*.java) do javac -d out "%f"

# Windows PowerShell
Get-ChildItem -Recurse -Filter "*.java" com | ForEach-Object { javac -d out $_.FullName }
```

### Run

```bash
java -cp out com.hospital.appointment.Main
```

> **First run:** The `data/` folder is created automatically and all seed accounts (admin + 12 doctors) are written. No manual setup needed.

---

## 🔑 Default Credentials

### Administrator

| Username | Password   |
| -------- | ---------- |
| `admin`  | `admin123` |

### Doctors — all use password `doc123`

| Username        | Doctor         | Department              |
| --------------- | -------------- | ----------------------- |
| `dr.santos`     | Dr. Santos     | Cardiology              |
| `dr.reyes`      | Dr. Reyes      | Pediatrics              |
| `dr.cruz`       | Dr. Cruz       | Orthopedics             |
| `dr.garcia`     | Dr. Garcia     | Neurology               |
| `dr.mendoza`    | Dr. Mendoza    | Dermatology             |
| `dr.torres`     | Dr. Torres     | Ophthalmology           |
| `dr.villanueva` | Dr. Villanueva | General Medicine        |
| `dr.aquino`     | Dr. Aquino     | Obstetrics & Gynecology |
| `dr.bautista`   | Dr. Bautista   | Oncology                |
| `dr.lim`        | Dr. Lim        | Psychiatry              |
| `dr.ramos`      | Dr. Ramos      | Pulmonology             |
| `dr.delacreuz`  | Dr. Dela Cruz  | Endocrinology           |

### Patients

Select **[2] Register as New Patient** from the main menu to create your own account.

---

## 🛡️ Role Permissions

### 👔 Administrator

| Feature                                        | ✅  |
| ---------------------------------------------- | --- |
| Book appointment (walk-in or existing patient) | ✅  |
| View / detail / search all appointments        | ✅  |
| Reschedule any appointment                     | ✅  |
| Cancel any appointment                         | ✅  |
| Mark completed / no-show                       | ✅  |
| Add, edit, deactivate doctors                  | ✅  |
| View, search, deactivate patients              | ✅  |
| View and manage waitlist                       | ✅  |
| View report summary + department breakdown     | ✅  |
| View any doctor's daily schedule               | ✅  |
| Filter by status / department                  | ✅  |

### 👨‍⚕️ Doctor

| Feature                                        | Scope                 |
| ---------------------------------------------- | --------------------- |
| View today's appointments                      | Own patients only     |
| View full schedule (any date)                  | Own patients only     |
| Mark appointment completed                     | Own appointments only |
| Set available date & time slots                | Own schedule only     |
| View patient records                           | Own patients only     |
| **Review & approve / reject PENDING requests** | Own appointments only |

### 🧑‍🤝‍🧑 Patient

| Feature                    | Notes                            |
| -------------------------- | -------------------------------- |
| Register account           | Creates login + patient profile  |
| Book new appointment       | Starts as PENDING                |
| Join waitlist              | Auto-offered when slots are full |
| View upcoming appointments | Own only                         |
| Cancel appointment         | Own only                         |
| View full history          | Own only                         |
| Update profile             | Name, address, contact, email    |
| Change password            | —                                |

---

## 💾 Data Storage

Six plain-text pipe-delimited files. One record per line. No database required.

### File Format Reference

**`users.txt`** (8 fields)

```
USR-001|admin|admin123|ADMIN||System Administrator|true|2026-05-10 09:00:00
```

**`patients.txt`** (10 fields)

```
PAT-2026-001|USR-014|Juan Cruz|28|Manila|09171234567|juan@email.com|O+|Maria Cruz|2026-05-10 09:30:00
```

**`doctors.txt`** (8 fields)

```
D-01|USR-002|Santos|CARDIOLOGY|Interventional Cardiology|18|Mon-Fri|DEFAULT
```

**`appointments.txt`** (8 fields)

```
APT-2026-001|PAT-2026-001|D-01|2026-08-10|08:00|BOOKED|Hypertension check|2026-05-10 09:00:00
APT-2026-002|PAT-2026-002|D-01|2026-08-10|10:00|PENDING|Back pain|2026-05-10 09:30:00
```

**`waitlist.txt`** (8 fields)

```
WL-2026-001|PAT-2026-003|D-01|2026-08-10|WAITING|2026-05-10 09:45:00|1|Chest pain
```

**`availability.txt`** (doctor + date + comma-separated slot:state pairs)

```
D-01|2026-08-10|08:00:RESERVED,09:00:UNAVAILABLE,10:00:FREE,11:00:FREE
```

### ID Format Reference

| Entity         | Format         | Example        |
| -------------- | -------------- | -------------- |
| Appointment    | `APT-YYYY-###` | `APT-2026-007` |
| Patient        | `PAT-YYYY-###` | `PAT-2026-003` |
| Waitlist Entry | `WL-YYYY-###`  | `WL-2026-001`  |
| User Account   | `USR-###`      | `USR-015`      |
| Doctor         | `D-##`         | `D-01`         |

---

## 🔄 Appointment Lifecycle

```
Patient books
      │
      ▼
  [ PENDING ]  ──── Doctor reviews via [7] ────►  [ APPROVED ]
      │                                                  │
      │                                         Doctor marks done
      │                                                  │
      │                                            [ COMPLETED ]
      │
      ├──── Doctor rejects ──────────────────►  [ REJECTED ]
      │                                    (slot freed → next waitlist patient promoted)
      │
      └──── Patient/Admin cancels ──────────►  [ CANCELLED ]
                                          (slot freed → waitlist auto-promotes to PENDING)

Admin books walk-in
      │
      ▼
   [ BOOKED ]  (bypasses PENDING, no doctor review needed)
      │
      └──── Mark done / cancel → [ COMPLETED ] / [ CANCELLED ]

Doctor/Admin marks no-show
      │
      ▼
  [ NO_SHOW ]
```

---

## 📅 Doctor Availability System

### Time Slots

| #   | Display             | System Key |
| --- | ------------------- | ---------- |
| 1   | 7:00 AM – 8:00 AM   | `08:00`    |
| 2   | 8:01 AM – 9:00 AM   | `09:00`    |
| 3   | 9:01 AM – 10:00 AM  | `10:00`    |
| 4   | 10:01 AM – 11:00 AM | `11:00`    |
| 5   | 1:00 PM – 2:00 PM   | `13:00`    |
| 6   | 2:01 PM – 3:00 PM   | `14:00`    |
| 7   | 3:01 PM – 4:00 PM   | `15:00`    |
| 8   | 4:01 PM – 5:00 PM   | `16:00`    |
| 9   | 5:01 PM – 6:00 PM   | `17:00`    |

### Slot State Rules

| State         | Patient can book? | Doctor can change? | How it's set                                          |
| ------------- | ----------------- | ------------------ | ----------------------------------------------------- |
| `FREE`        | ✅ Yes            | ✅ → UNAVAILABLE   | Default; restored when appointment cancelled/rejected |
| `UNAVAILABLE` | ❌ Hidden         | ✅ → FREE          | Doctor manually blocks it                             |
| `RESERVED`    | ❌ Hidden         | ❌ Read-only       | Set automatically when appointment is created         |

---

## 🏥 Doctors Roster

| ID   | Name       | Department              | Specialization            | Schedule | Exp    |
| ---- | ---------- | ----------------------- | ------------------------- | -------- | ------ |
| D-01 | Santos     | Cardiology              | Interventional Cardiology | Mon–Fri  | 18 yrs |
| D-02 | Reyes      | Pediatrics              | Neonatology               | Mon–Sat  | 12 yrs |
| D-03 | Cruz       | Orthopedics             | Sports Medicine           | Tue–Sat  | 15 yrs |
| D-04 | Garcia     | Neurology               | Stroke & Epilepsy         | Mon–Fri  | 20 yrs |
| D-05 | Mendoza    | Dermatology             | Cosmetic Dermatology      | Mon–Fri  | 9 yrs  |
| D-06 | Torres     | Ophthalmology           | Retina Specialist         | Mon–Thu  | 14 yrs |
| D-07 | Villanueva | General Medicine        | Family Medicine           | Mon–Sun  | 11 yrs |
| D-08 | Aquino     | Obstetrics & Gynecology | Maternal-Fetal Medicine   | Mon–Fri  | 16 yrs |
| D-09 | Bautista   | Oncology                | Medical Oncology          | Mon–Fri  | 22 yrs |
| D-10 | Lim        | Psychiatry              | Clinical Psychology       | Mon–Fri  | 10 yrs |
| D-11 | Ramos      | Pulmonology             | Critical Care             | Mon–Sat  | 17 yrs |
| D-12 | Dela Cruz  | Endocrinology           | Diabetes & Metabolism     | Mon–Fri  | 13 yrs |

---

## 📐 Architecture Decisions

**Why flat files instead of a database?**
Flat-file storage means zero setup — just compile and run. The `DataStore` class is the single source of truth for all in-memory data and handles all I/O in one place, making it straightforward to swap in SQLite or MySQL later without touching any business logic.

**Why a shared `AppointmentService`?**
All three dashboards perform many of the same operations. Writing the logic once in `AppointmentService` and calling it from Admin, Doctor, and Patient dashboards eliminates duplication and guarantees the same validation rules apply everywhere, regardless of who is logged in.

**Why PENDING for patient bookings?**
In real clinical workflows, walk-ins confirmed by staff are immediately valid, but online/self-requested appointments require review. The `PENDING → APPROVED / REJECTED` flow reflects actual clinical practice and gives doctors meaningful control over their own schedule.

**Why is `DoctorAvailability` a separate model/file?**
Mixing per-date slot states into the `Doctor` model would bloat it with unbounded map data. Keeping it in its own class and its own file (`availability.txt`) keeps each entity clean and makes the data easy to audit.

---

## 🛠️ Technologies Used

| Technology                                                  | Purpose                            |
| ----------------------------------------------------------- | ---------------------------------- |
| Java 21                                                     | Core language                      |
| OOP (Encapsulation, Inheritance, Abstraction, Polymorphism) | Architecture                       |
| ANSI Escape Codes                                           | Terminal colors                    |
| `java.time` API                                             | Date parsing and formatting        |
| Plain-text flat files                                       | Persistence (no external DB)       |
| `javac`                                                     | Compilation (no build tool needed) |

---

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first.

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add your feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with ☕ and Java

**Hospital Management System — Role-Based Access Control Edition**

_22 source files · ~3,300 lines · Zero dependencies_

</div>
