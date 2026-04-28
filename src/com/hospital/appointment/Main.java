package com.hospital.appointment;

import com.hospital.appointment.model.SessionContext;
import com.hospital.appointment.service.AppointmentManager;
import com.hospital.appointment.service.AuthService;
import com.hospital.appointment.service.AuthServiceImpl;
import com.hospital.appointment.storage.FileHandler;
import com.hospital.appointment.ui.MenuHandler;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            FileHandler fileHandler = new FileHandler("data/appointments.txt");
            AuthService authService = new AuthServiceImpl(fileHandler);
            AppointmentManager appointmentManager = new AppointmentManager(fileHandler, authService);
            
            Scanner scanner = new Scanner(System.in);
            
            // Login flow
            SessionContext session = null;
            while (session == null) {
                session = promptLogin(authService, scanner);
            }
            
            // Route to appropriate menu based on role
            MenuHandler menuHandler = new MenuHandler(appointmentManager, session);
            menuHandler.start();
            
            scanner.close();
        } catch (Exception exception) {
            System.out.println("Fatal error: " + exception.getMessage());
        }
    }

    private static SessionContext promptLogin(AuthService authService, Scanner scanner) {
        try {
            System.out.println();
            System.out.println("=======================================");
            System.out.println("   HOSPITAL APPOINTMENT SYSTEM");
            System.out.println("=======================================");
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            
            System.out.print("Password: ");
            String password = scanner.nextLine();
            
            Optional<SessionContext> result = authService.login(username, password);
            if (result.isPresent()) {
                SessionContext session = result.get();
                System.out.println("Login successful. Welcome, " + session.getUsername() + "!");
                return session;
            } else {
                System.out.println("Invalid username or password. Please try again.");
                return null;
            }
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            return null;
        }
    }
}