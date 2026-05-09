package com.hospital.appointment.enums;

public enum AppointmentStatus {
    BOOKED    ("Booked",    "[ BOOKED    ]", "*"),
    CANCELLED ("Cancelled", "[ CANCELLED ]", "X"),
    COMPLETED ("Completed", "[ COMPLETED ]", "v"),
    NO_SHOW   ("No Show",   "[ NO-SHOW   ]", "-");

    private final String displayName;
    private final String label;
    private final String icon;

    AppointmentStatus(String displayName, String label, String icon) {
        this.displayName = displayName;
        this.label       = label;
        this.icon        = icon;
    }

    public String getDisplayName() { return displayName; }
    public String getLabel()       { return label; }
    public String getIcon()        { return icon; }

    @Override public String toString() { return displayName; }
}
