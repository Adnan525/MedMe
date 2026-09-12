package com.adnaan525.medme.model;

public enum DurationType {
    RECURRING,
    FIXED_DAYS,
    /** No dose times, no reminders - stock is only ever adjusted by manually logging a dose. */
    AS_NEEDED
}
