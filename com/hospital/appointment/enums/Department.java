package com.hospital.appointment.enums;

public enum Department {
    CARDIOLOGY       ("Cardiology",             "[ CARD ]"),
    PEDIATRICS       ("Pediatrics",             "[ PEDI ]"),
    ORTHOPEDICS      ("Orthopedics",            "[ ORTH ]"),
    NEUROLOGY        ("Neurology",              "[ NEUR ]"),
    DERMATOLOGY      ("Dermatology",            "[ DERM ]"),
    OPHTHALMOLOGY    ("Ophthalmology",          "[ OPHT ]"),
    GENERAL_MEDICINE ("General Medicine",       "[ GENM ]"),
    OBSTETRICS       ("Obstetrics & Gynecology","[ OBGN ]"),
    ONCOLOGY         ("Oncology",               "[ ONCO ]"),
    PSYCHIATRY       ("Psychiatry",             "[ PSYC ]"),
    PULMONOLOGY      ("Pulmonology",            "[ PULM ]"),
    ENDOCRINOLOGY    ("Endocrinology",          "[ ENDO ]");

    private final String displayName;
    private final String tag;

    Department(String displayName, String tag) {
        this.displayName = displayName;
        this.tag         = tag;
    }

    public String getDisplayName() { return displayName; }
    public String getTag()         { return tag; }

    @Override public String toString() { return displayName; }
}
