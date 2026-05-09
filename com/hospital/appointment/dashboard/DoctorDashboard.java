package com.hospital.appointment.dashboard;

import com.hospital.appointment.model.*;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.storage.DataStore;
import com.hospital.appointment.ui.Console;
import com.hospital.appointment.util.InputValidator;

/**
 * Doctor Dashboard — scoped to the logged-in doctor's records only.
 *
 * Options:
 *   [1] Today's Appointments
 *   [2] Full Schedule (any date)
 *   [3] Mark Appointment Completed
 *   [4] Set My Available Slots
 *   [5] Patient Record Lookup (own patients only)
 *   [6] Appointment Detail
 */
public class DoctorDashboard {

    private final DataStore         store;
    private final AppointmentService apptSvc;
    private final InputValidator    input;
    private final User              user;
    private final Doctor            doctor;  // The domain record for this doctor

    public DoctorDashboard(DataStore store, AppointmentService apptSvc,
                            InputValidator input, User user) {
        this.store   = store;
        this.apptSvc = apptSvc;
        this.input   = input;
        this.user    = user;
        this.doctor  = store.findDoctorByUserId(user.getUserId());
    }

    /** Main loop. Returns when doctor logs out. */
    public void run(java.util.Scanner scanner) {
        if (doctor == null) {
            Console.error("No doctor profile linked to this account. Contact admin.");
            return;
        }

        boolean running = true;
        while (running) {
            Console.doctorDashboard(doctor.getName());
            int choice = input.readIntInRange("  Your choice : ", 0, 6);
            System.out.println();

            switch (choice) {
                case 1 -> apptSvc.viewDoctorAppointments(doctor.getDoctorId(), true);
                case 2 -> apptSvc.viewMySchedule(doctor);
                case 3 -> apptSvc.completeAppointment(doctor.getDoctorId());
                case 4 -> apptSvc.setDoctorSlots(doctor);
                case 5 -> apptSvc.viewPatientRecord(doctor.getDoctorId());
                case 6 -> apptSvc.viewDetail();
                case 0 -> running = false;
                default -> Console.warn("Invalid choice.");
            }
            if (running) Console.pause(scanner);
        }
    }
}
