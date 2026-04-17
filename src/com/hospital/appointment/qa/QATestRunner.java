package com.hospital.appointment.qa;

import com.hospital.appointment.model.Appointment;
import com.hospital.appointment.model.Doctor;
import com.hospital.appointment.model.Patient;
import com.hospital.appointment.service.AppointmentManager;
import com.hospital.appointment.storage.FileHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QATestRunner {
    private static final String DEFAULT_FILE_PATH = "data/qa-phase1-appointments.txt";

    private int passed;
    private int failed;
    private final List<String> failures;

    public QATestRunner() {
        this.failures = new ArrayList<String>();
    }

    public static void main(String[] args) {
        String filePath = DEFAULT_FILE_PATH;
        boolean keepFile = false;

        for (String arg : args) {
            if (arg.startsWith("--file=")) {
                filePath = arg.substring("--file=".length()).trim();
            } else if ("--keep-file".equalsIgnoreCase(arg)) {
                keepFile = true;
            } else {
                printUsage();
                return;
            }
        }

        QATestRunner runner = new QATestRunner();
        int exitCode = runner.execute(filePath, keepFile);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private int execute(String filePath, boolean keepFile) {
        Path qaPath = Paths.get(filePath);

        try {
            Files.deleteIfExists(qaPath);
            AppointmentManager manager = new AppointmentManager(new FileHandler(filePath));
            runScenarios(manager);

            AppointmentManager reloaded = new AppointmentManager(new FileHandler(filePath));
            assertEquals(manager.viewAppointments().size(), reloaded.viewAppointments().size(),
                    "Auto-save persists appointment count");

            printSummary(filePath);
            return failed == 0 ? 0 : 1;
        } catch (Exception exception) {
            System.out.println("QA runner failed: " + exception.getMessage());
            return 1;
        } finally {
            if (!keepFile) {
                try {
                    Files.deleteIfExists(qaPath);
                } catch (Exception ignored) {
                    // Best effort cleanup for temporary QA file.
                }
            }
        }
    }

    private void runScenarios(AppointmentManager manager) {
        List<Doctor> doctors = manager.getDoctors();
        assertTrue(!doctors.isEmpty(), "Precondition: doctors are available");
        if (doctors.isEmpty()) {
            return;
        }

        Doctor doctor = doctors.get(0);

        LocalDate dayOne = LocalDate.now().plusDays(2);
        List<LocalTime> dayOneSlots = manager.getAvailableTimeSlots(doctor.getDoctorId(), dayOne);
        assertTrue(!dayOneSlots.isEmpty(), "Precondition: day one has available slots");
        if (dayOneSlots.isEmpty()) {
            return;
        }

        Appointment primary = manager.addAppointment(
                new Patient("", "QA Patient Alpha", 30, "QA Street 1"),
                doctor.getDoctorId(),
                dayOne,
                dayOneSlots.get(0)
        );
        assertNotNull(primary, "Add appointment returns created item");

        LocalDate dayTwo = LocalDate.now().plusDays(3);
        List<LocalTime> dayTwoSlots = manager.getAvailableTimeSlots(doctor.getDoctorId(), dayTwo);
        assertTrue(dayTwoSlots.size() >= 2, "Precondition: day two has at least two slots");
        if (dayTwoSlots.size() < 2) {
            return;
        }

        LocalTime rescheduleSlot = dayTwoSlots.get(0);
        Appointment rescheduled = manager.rescheduleAppointment(primary.getAppointmentId(), dayTwo, rescheduleSlot);
        assertEquals(dayTwo, rescheduled.getDate(), "Reschedule updates date");
        assertEquals(rescheduleSlot, rescheduled.getTime(), "Reschedule updates time");

        LocalTime blockedSlot = dayTwoSlots.get(1);
        Appointment blocker = manager.addAppointment(
                new Patient("", "QA Patient Beta", 31, "QA Street 2"),
                doctor.getDoctorId(),
                dayTwo,
                blockedSlot
        );
        assertNotNull(blocker, "Second appointment created to validate duplicate prevention");

        assertThrows(IllegalStateException.class, new ThrowingAction() {
            @Override
            public void run() {
                manager.rescheduleAppointment(primary.getAppointmentId(), dayTwo, blockedSlot);
            }
        }, "Reschedule rejects occupied doctor/date/time slot");

        boolean completeFuture = manager.completeAppointment(primary.getAppointmentId());
        assertTrue(!completeFuture, "Complete rejects future appointments");

        LocalDate today = LocalDate.now();
        List<LocalTime> todaySlots = manager.getAvailableTimeSlots(doctor.getDoctorId(), today);
        assertTrue(!todaySlots.isEmpty(), "Precondition: today has available slots");
        if (todaySlots.isEmpty()) {
            return;
        }

        Appointment todayAppointment = manager.addAppointment(
                new Patient("", "QA Patient Gamma", 32, "QA Street 3"),
                doctor.getDoctorId(),
                today,
                todaySlots.get(0)
        );
        assertNotNull(todayAppointment, "Today appointment created");

        boolean completeToday = manager.completeAppointment(todayAppointment.getAppointmentId());
        assertTrue(completeToday, "Complete succeeds for today appointment");

        boolean completeAgain = manager.completeAppointment(todayAppointment.getAppointmentId());
        assertTrue(!completeAgain, "Complete is idempotent for completed appointment");

        boolean cancelCompleted = manager.cancelAppointment(todayAppointment.getAppointmentId());
        assertTrue(!cancelCompleted, "Cancel rejects completed appointment");

        List<Appointment> history = manager.getPatientHistory("qa patient");
        assertTrue(history.size() >= 3, "Patient history returns matching appointments");

        boolean foundCompletedInHistory = false;
        for (Appointment appointment : history) {
            if (appointment.getStatus().name().equals("COMPLETED")) {
                foundCompletedInHistory = true;
                break;
            }
        }
        assertTrue(foundCompletedInHistory, "Patient history includes completed records");

        List<Appointment> doctorSchedule = manager.getDoctorDailySchedule(doctor.getDoctorId(), dayTwo);
        assertEquals(Integer.valueOf(2), Integer.valueOf(doctorSchedule.size()),
                "Doctor schedule filters by doctor and date");

        if (doctorSchedule.size() >= 2) {
            boolean sortedByTime = !doctorSchedule.get(0).getTime().isAfter(doctorSchedule.get(1).getTime());
            assertTrue(sortedByTime, "Doctor schedule is sorted by time ascending");
        }

        Map<String, Integer> summary = manager.getReportSummary();
        Integer completed = summary.get("completed");
        assertTrue(completed != null && completed.intValue() >= 1, "Report summary includes completed count");
    }

    private void assertTrue(boolean condition, String name) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
            return;
        }

        failed++;
        failures.add(name);
        System.out.println("[FAIL] " + name);
    }

    private void assertNotNull(Object value, String name) {
        assertTrue(value != null, name);
    }

    private void assertEquals(Object expected, Object actual, String name) {
        boolean matched = expected == null ? actual == null : expected.equals(actual);
        if (matched) {
            passed++;
            System.out.println("[PASS] " + name);
            return;
        }

        failed++;
        failures.add(name + " (expected: " + expected + ", actual: " + actual + ")");
        System.out.println("[FAIL] " + name + " (expected: " + expected + ", actual: " + actual + ")");
    }

    private void assertThrows(Class<? extends Throwable> expectedType, ThrowingAction action, String name) {
        try {
            action.run();
            failed++;
            failures.add(name + " (no exception thrown)");
            System.out.println("[FAIL] " + name + " (no exception thrown)");
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                passed++;
                System.out.println("[PASS] " + name);
            } else {
                failed++;
                failures.add(name + " (unexpected exception: " + throwable.getClass().getSimpleName() + ")");
                System.out.println("[FAIL] " + name + " (unexpected exception: " + throwable.getClass().getSimpleName() + ")");
            }
        }
    }

    private void printSummary(String filePath) {
        System.out.println();
        System.out.println("=== QA Scenario Summary ===");
        System.out.println("QA file: " + filePath);
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);

        if (!failures.isEmpty()) {
            System.out.println("Failed checks:");
            for (String item : failures) {
                System.out.println("- " + item);
            }
        }

        System.out.println();
        if (failed == 0) {
            System.out.println("Result: ALL QA SCENARIOS PASSED");
        } else {
            System.out.println("Result: QA SCENARIOS FAILED");
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp out com.hospital.appointment.qa.QATestRunner [--file=data/qa-phase1-appointments.txt] [--keep-file]");
    }

    private interface ThrowingAction {
        void run();
    }
}
