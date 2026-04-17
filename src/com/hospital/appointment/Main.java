package com.hospital.appointment;

import com.hospital.appointment.service.AppointmentManager;
import com.hospital.appointment.storage.FileHandler;
import com.hospital.appointment.ui.MenuHandler;

public class Main {
    public static void main(String[] args) {
        try {
            FileHandler fileHandler = new FileHandler("data/appointments.txt");
            AppointmentManager appointmentManager = new AppointmentManager(fileHandler);
            MenuHandler menuHandler = new MenuHandler(appointmentManager);
            menuHandler.start();
        } catch (Exception exception) {
            System.out.println("Fatal error: " + exception.getMessage());
        }
    }
}