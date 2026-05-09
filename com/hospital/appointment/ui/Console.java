package com.hospital.appointment.ui;

import java.util.Scanner;

/**
 * All console rendering: banners, headers, boxes, alerts, tables.
 * No business logic whatsoever.
 *
 * Design philosophy: clean ASCII borders that look great in any terminal.
 * The width constant W drives all box sizing for consistency.
 */
public class Console {

    public static final int W = 70;  // Inner width of all boxes
    private static final String LINE  = "=".repeat(W);
    private static final String DASH  = "-".repeat(W);
    private static final String THICK = "#".repeat(W + 4);

    // =========================================================================
    // SYSTEM BANNERS
    // =========================================================================

    public static void welcomeBanner() {
        System.out.println();
        System.out.println("  " + THICK);
        System.out.println("  ##" + center("", W) + "##");
        System.out.println("  ##" + center("H O S P I T A L   M A N A G E M E N T   S Y S T E M", W) + "##");
        System.out.println("  ##" + center("Professional Appointment & Records Platform  v3.0", W) + "##");
        System.out.println("  ##" + center("", W) + "##");
        System.out.println("  " + THICK);
        System.out.println();
    }

    public static void goodbye() {
        System.out.println();
        System.out.println("  " + THICK);
        System.out.println("  ##" + center("", W) + "##");
        System.out.println("  ##" + center("Thank you for using Hospital Management System", W) + "##");
        System.out.println("  ##" + center("All records saved.  Goodbye!", W) + "##");
        System.out.println("  ##" + center("", W) + "##");
        System.out.println("  " + THICK);
        System.out.println();
    }

    // =========================================================================
    // ROLE DASHBOARDS
    // =========================================================================

    public static void adminDashboard(String name) {
        dashboardHeader("ADMINISTRATOR DASHBOARD", name, "Full System Access");
        System.out.println("  |  APPOINTMENTS                   |  MANAGEMENT                       |");
        System.out.println("  |  [1]  Book Appointment          |  [9]  Manage Doctors              |");
        System.out.println("  |  [2]  View All Appointments     |  [10] Manage Patients             |");
        System.out.println("  |  [3]  Appointment Detail        |  [11] View Waitlist               |");
        System.out.println("  |  [4]  Approve / Reschedule      |  [12] View Report Summary         |");
        System.out.println("  |  [5]  Cancel Appointment        |                                   |");
        System.out.println("  |  [6]  Complete Appointment      |  SEARCH & FILTER                  |");
        System.out.println("  |  [7]  View Doctor Schedule      |  [13] Search All Records          |");
        System.out.println("  |  [8]  Manage Waitlist           |  [14] Filter by Status            |");
        System.out.println("  |                                 |  [15] Filter by Department        |");
        System.out.println("  |                                 |                                   |");
        System.out.println("  |                                 |  [0]  Logout                      |");
        dashboardFooter();
    }

    public static void doctorDashboard(String name) {
        dashboardHeader("DOCTOR DASHBOARD", "Dr. " + name, "Medical Staff Portal");
        System.out.println("  |  MY SCHEDULE                    |  PATIENT RECORDS                  |");
        System.out.println("  |  [1]  My Appointments Today     |  [5]  Patient Record Lookup       |");
        System.out.println("  |  [2]  My Full Schedule          |  [6]  Appointment Detail          |");
        System.out.println("  |  [3]  Mark Appointment Done     |                                   |");
        System.out.println("  |  [4]  Set My Available Slots    |  [0]  Logout                      |");
        dashboardFooter();
    }

    public static void patientDashboard(String name) {
        dashboardHeader("PATIENT PORTAL", name, "My Health Dashboard");
        System.out.println("  |  MY APPOINTMENTS                |  MY ACCOUNT                       |");
        System.out.println("  |  [1]  Book New Appointment      |  [5]  Update My Profile           |");
        System.out.println("  |  [2]  My Upcoming Appointments  |  [6]  Change Password             |");
        System.out.println("  |  [3]  Cancel My Appointment     |                                   |");
        System.out.println("  |  [4]  My Appointment History    |  [0]  Logout                      |");
        System.out.println("  |       Join Waitlist -> [1]      |                                   |");
        dashboardFooter();
    }

    public static void guestMenu() {
        System.out.println();
        System.out.println("  +" + LINE + "+");
        System.out.println("  |" + center("WELCOME  --  Please log in or register", W) + "|");
        System.out.println("  +" + DASH + "+");
        System.out.println("  |  [1]  Login                                                        |");
        System.out.println("  |  [2]  Register as New Patient                                      |");
        System.out.println("  |  [0]  Exit Application                                             |");
        System.out.println("  +" + LINE + "+");
        System.out.println();
    }

    // =========================================================================
    // SECTION HEADERS
    // =========================================================================

    /** Bold section header inside a dashboard feature. */
    public static void header(String title) {
        System.out.println();
        System.out.println("  +" + LINE + "+");
        System.out.println("  |" + center(title, W) + "|");
        System.out.println("  +" + LINE + "+");
        System.out.println();
    }

    /** Thin divider label for sub-sections. */
    public static void section(String label) {
        System.out.println();
        System.out.printf("  -- %s %s%n", label, "-".repeat(Math.max(2, W - label.length() - 4)));
        System.out.println();
    }

    // =========================================================================
    // ALERTS
    // =========================================================================

    public static void success(String msg) {
        System.out.println();
        System.out.println("  +" + LINE + "+");
        System.out.printf ("  |  [OK] %-63s|%n", msg);
        System.out.println("  +" + LINE + "+");
        System.out.println();
    }

    public static void error(String msg) {
        System.out.println();
        System.out.println("  +" + LINE + "+");
        System.out.printf ("  |  [!!] %-63s|%n", msg);
        System.out.println("  +" + LINE + "+");
        System.out.println();
    }

    public static void info(String msg) {
        System.out.printf("  [i] %s%n", msg);
    }

    public static void warn(String msg) {
        System.out.printf("  [!] %s%n", msg);
    }

    // =========================================================================
    // CONFIRM BOX
    // =========================================================================

    public static void confirmBox(String patient, String doctor, String dept,
                                   String date, String slot) {
        System.out.println("  +" + DASH + "+");
        System.out.println("  |" + center("BOOKING SUMMARY", W) + "|");
        System.out.println("  +" + DASH + "+");
        System.out.printf ("  |  Patient    : %-55s|%n", patient);
        System.out.printf ("  |  Doctor     : Dr. %-52s|%n", doctor);
        System.out.printf ("  |  Department : %-55s|%n", dept);
        System.out.printf ("  |  Date       : %-55s|%n", date);
        System.out.printf ("  |  Time Slot  : %-55s|%n", slot);
        System.out.println("  +" + DASH + "+");
        System.out.println();
    }

    // =========================================================================
    // TABLE HEADERS
    // =========================================================================

    public static void appointmentTableHeader() {
        System.out.printf("  %-16s %-20s %-20s %-18s %-12s %-6s %-11s%n",
            "Appt ID", "Patient", "Doctor", "Department", "Date", "Time", "Status");
        System.out.println("  " + DASH);
    }

    public static void appointmentTableFooter(int count) {
        System.out.println("  " + DASH);
        System.out.printf ("  Total: %d record(s)%n", count);
    }

    public static void waitlistTableHeader() {
        System.out.printf("  %-14s %-6s %-20s %-18s %-12s %-10s%n",
            "WL ID", "Queue", "Patient", "Doctor", "Date", "Status");
        System.out.println("  " + DASH);
    }

    // =========================================================================
    // FIELD PROMPTS
    // =========================================================================

    public static void fieldLine(String label, String value) {
        System.out.printf("  %-24s: %s%n", label, value);
    }

    // =========================================================================
    // PAUSE
    // =========================================================================

    public static void pause(Scanner scanner) {
        System.out.print("\n  Press ENTER to continue...");
        scanner.nextLine();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private static void dashboardHeader(String title, String user, String subtitle) {
        System.out.println();
        System.out.println("  +" + LINE + "+");
        System.out.println("  |" + center(title, W) + "|");
        System.out.println("  |" + center("Logged in as: " + user + "  |  " + subtitle, W) + "|");
        System.out.println("  +" + DASH + "+");
    }

    private static void dashboardFooter() {
        System.out.println("  +" + LINE + "+");
        System.out.println();
    }

    /** Centers text within a fixed width, padding with spaces. */
    public static String center(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        int padL = (width - text.length()) / 2;
        int padR = width - text.length() - padL;
        return " ".repeat(padL) + text + " ".repeat(padR);
    }
}
