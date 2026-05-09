package com.hospital.appointment.enums;

public enum WaitlistStatus {
    WAITING  ("Waiting",  "[ WAIT ]"),
    PROMOTED ("Promoted", "[ PROMOTED ]"),
    REMOVED  ("Removed",  "[ REMOVED ]"),
    EXPIRED  ("Expired",  "[ EXPIRED ]");

    private final String displayName;
    private final String tag;

    WaitlistStatus(String displayName, String tag) {
        this.displayName = displayName;
        this.tag         = tag;
    }

    public String getDisplayName() { return displayName; }
    public String getTag()         { return tag; }

    @Override public String toString() { return displayName; }
}
