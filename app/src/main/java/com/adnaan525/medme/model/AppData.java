package com.adnaan525.medme.model;

import java.util.ArrayList;
import java.util.List;

public class AppData {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int schemaVersion;
    private List<Patient> patients;

    public AppData() {
        this.schemaVersion = CURRENT_SCHEMA_VERSION;
        this.patients = new ArrayList<>();
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<Patient> getPatients() {
        return patients;
    }

    public void setPatients(List<Patient> patients) {
        this.patients = patients;
    }
}
