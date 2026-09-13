package com.adnaan525.medme.data;

import android.content.Context;
import android.net.Uri;

import com.adnaan525.medme.model.AppData;
import com.adnaan525.medme.model.DoseLog;
import com.adnaan525.medme.model.DoseStatus;
import com.adnaan525.medme.model.Inventory;
import com.adnaan525.medme.model.Medication;
import com.adnaan525.medme.model.Patient;
import com.adnaan525.medme.util.DateTimeUtils;
import com.adnaan525.medme.util.IdGenerator;
import com.adnaan525.medme.util.ScheduleUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Single in-memory source of truth for the whole app, persisted to the JSON file on every
 * mutation. The data set is tiny (a handful of patients/medications/dose logs) so writes are
 * done synchronously - there's no meaningful risk of jank and it keeps the read/write path
 * free of races between the UI and the alarm/notification receivers.
 */
public final class DataRepository {
    private static final int MISSED_GRACE_HOURS = 4;

    private static volatile DataRepository instance;

    private final JsonStorage storage;
    private AppData appData;

    private DataRepository(Context context) {
        this.storage = new JsonStorage(context);
        this.appData = storage.load();
    }

    public static DataRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (DataRepository.class) {
                if (instance == null) {
                    instance = new DataRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private void persist() {
        try {
            storage.save(appData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save MedMe data", e);
        }
    }

    // ---- Patients ----

    public List<Patient> getPatients() {
        return appData.getPatients();
    }

    public Patient getPatient(String patientId) {
        for (Patient p : appData.getPatients()) {
            if (p.getId().equals(patientId)) {
                return p;
            }
        }
        return null;
    }

    public Patient addPatient(String name) {
        Patient patient = new Patient(IdGenerator.newId(), name);
        appData.getPatients().add(patient);
        persist();
        return patient;
    }

    public void updatePatientName(String patientId, String newName) {
        Patient patient = getPatient(patientId);
        if (patient != null) {
            patient.setName(newName);
            persist();
        }
    }

    public void deletePatient(String patientId) {
        appData.getPatients().removeIf(p -> p.getId().equals(patientId));
        persist();
    }

    // ---- Medications ----

    public Medication addMedication(String patientId, Medication medication) {
        Patient patient = getPatient(patientId);
        if (patient == null) {
            return null;
        }
        medication.setId(IdGenerator.newId());
        medication.setActive(true);
        patient.getMedications().add(medication);
        persist();
        return medication;
    }

    public void updateMedication(String patientId, Medication updated) {
        Patient patient = getPatient(patientId);
        if (patient == null) {
            return;
        }
        List<Medication> meds = patient.getMedications();
        for (int i = 0; i < meds.size(); i++) {
            if (meds.get(i).getId().equals(updated.getId())) {
                meds.set(i, updated);
                break;
            }
        }
        persist();
    }

    /** Manually archives (false) or unarchives (true) a medication - separate from a FIXED_DAYS course finishing on its own by date. */
    public void setMedicationActive(String medicationId, boolean active) {
        MedicationLookup lookup = findMedication(medicationId);
        if (lookup == null) {
            return;
        }
        lookup.medication.setActive(active);
        persist();
    }

    public void deleteMedication(String patientId, String medicationId) {
        Patient patient = getPatient(patientId);
        if (patient == null) {
            return;
        }
        patient.getMedications().removeIf(m -> m.getId().equals(medicationId));
        persist();
    }

    /** Result of looking a medication up by id alone (used by alarm/notification receivers). */
    public static final class MedicationLookup {
        public final Patient patient;
        public final Medication medication;

        MedicationLookup(Patient patient, Medication medication) {
            this.patient = patient;
            this.medication = medication;
        }
    }

    public MedicationLookup findMedication(String medicationId) {
        for (Patient patient : appData.getPatients()) {
            for (Medication med : patient.getMedications()) {
                if (med.getId().equals(medicationId)) {
                    return new MedicationLookup(patient, med);
                }
            }
        }
        return null;
    }

    // ---- Dose logs ----

    /**
     * Called when a reminder alarm fires. Idempotent: if a log for this exact scheduled
     * time already exists (e.g. the user already marked the dose taken early, or the
     * receiver somehow re-fires) the existing entry is returned instead of duplicating it.
     */
    public DoseLog ensureDoseLog(String medicationId, LocalDateTime scheduledDateTime) {
        MedicationLookup lookup = findMedication(medicationId);
        if (lookup == null) {
            return null;
        }
        String scheduledIso = DateTimeUtils.formatDateTime(scheduledDateTime);
        for (DoseLog log : lookup.medication.getDoseLogs()) {
            if (log.getScheduledDateTime().equals(scheduledIso)) {
                return log;
            }
        }
        DoseLog log = new DoseLog(IdGenerator.newId(), scheduledIso);
        lookup.medication.getDoseLogs().add(log);
        persist();
        return log;
    }

    /** Creates (if needed) and immediately marks taken - used for "mark taken" on a dose that hasn't been scheduled/logged yet. */
    public DoseTakenResult markTaken(String medicationId, String doseLogId, LocalDateTime actualTime) {
        MedicationLookup lookup = findMedication(medicationId);
        if (lookup == null) {
            return null;
        }
        DoseLog target = null;
        for (DoseLog log : lookup.medication.getDoseLogs()) {
            if (log.getId().equals(doseLogId)) {
                target = log;
                break;
            }
        }
        if (target == null || target.getStatus() == DoseStatus.TAKEN) {
            return new DoseTakenResult(lookup.patient, lookup.medication, false);
        }
        target.setStatus(DoseStatus.TAKEN);
        target.setActualTakenDateTime(DateTimeUtils.formatDateTime(actualTime));

        Inventory inventory = lookup.medication.getInventory();
        boolean triggersLowStock = false;
        if (inventory != null) {
            inventory.setQuantityRemaining(Math.max(0, inventory.getQuantityRemaining() - 1));
            if (ScheduleUtils.isLowStock(lookup.medication) && !inventory.isLowStockNotified()) {
                inventory.setLowStockNotified(true);
                triggersLowStock = true;
            }
        }
        persist();
        return new DoseTakenResult(lookup.patient, lookup.medication, triggersLowStock);
    }

    /** Marks an occurrence taken even if its alarm hasn't fired yet, creating its DoseLog on the fly. */
    public DoseTakenResult markTakenForOccurrence(String medicationId, LocalDateTime scheduledDateTime, LocalDateTime actualTime) {
        DoseLog log = ensureDoseLog(medicationId, scheduledDateTime);
        if (log == null) {
            return null;
        }
        return markTaken(medicationId, log.getId(), actualTime);
    }

    /**
     * Logs one AS_NEEDED dose taken right now. Always creates a brand-new DoseLog rather than
     * going through ensureDoseLog's dedup-by-scheduled-time - that dedup is correct for a
     * *scheduled* occurrence (the alarm and an early manual tap should share one log), but here
     * every call represents a genuinely separate real-world dose. Reusing ensureDoseLog for this
     * silently collapsed repeat presses within the same clock minute into a single log (its
     * dedup key is minute-precision), so stock only ever dropped by 1 no matter how many times
     * "log dose taken" was pressed in quick succession.
     */
    public DoseTakenResult logAsNeededDose(String medicationId, LocalDateTime actualTime) {
        MedicationLookup lookup = findMedication(medicationId);
        if (lookup == null) {
            return null;
        }
        String iso = DateTimeUtils.formatDateTime(actualTime);
        DoseLog log = new DoseLog(IdGenerator.newId(), iso);
        log.setStatus(DoseStatus.TAKEN);
        log.setActualTakenDateTime(iso);
        lookup.medication.getDoseLogs().add(log);

        Inventory inventory = lookup.medication.getInventory();
        boolean triggersLowStock = false;
        if (inventory != null) {
            inventory.setQuantityRemaining(Math.max(0, inventory.getQuantityRemaining() - 1));
            if (ScheduleUtils.isLowStock(lookup.medication) && !inventory.isLowStockNotified()) {
                inventory.setLowStockNotified(true);
                triggersLowStock = true;
            }
        }
        persist();
        return new DoseTakenResult(lookup.patient, lookup.medication, triggersLowStock);
    }

    public static final class DoseTakenResult {
        public final Patient patient;
        public final Medication medication;
        public final boolean triggersLowStockNotification;

        DoseTakenResult(Patient patient, Medication medication, boolean triggersLowStockNotification) {
            this.patient = patient;
            this.medication = medication;
            this.triggersLowStockNotification = triggersLowStockNotification;
        }
    }

    /**
     * Reduces stock on hand by one unit without touching dose logs or analytics - for a dose
     * taken outside the app (e.g. before reinstalling wiped pending notifications), stock lost
     * or damaged, or any other manual correction that isn't "I just took a scheduled dose".
     */
    public DoseTakenResult reduceStockByOne(String medicationId) {
        MedicationLookup lookup = findMedication(medicationId);
        if (lookup == null || lookup.medication.getInventory() == null) {
            return null;
        }
        Inventory inventory = lookup.medication.getInventory();
        inventory.setQuantityRemaining(Math.max(0, inventory.getQuantityRemaining() - 1));
        boolean triggersLowStock = false;
        if (ScheduleUtils.isLowStock(lookup.medication) && !inventory.isLowStockNotified()) {
            inventory.setLowStockNotified(true);
            triggersLowStock = true;
        }
        persist();
        return new DoseTakenResult(lookup.patient, lookup.medication, triggersLowStock);
    }

    public void logReplenishment(String medicationId, int quantityAdded) {
        MedicationLookup lookup = findMedication(medicationId);
        if (lookup == null || lookup.medication.getInventory() == null) {
            return;
        }
        Inventory inventory = lookup.medication.getInventory();
        inventory.setQuantityRemaining(inventory.getQuantityRemaining() + quantityAdded);
        if (!ScheduleUtils.isLowStock(lookup.medication)) {
            inventory.setLowStockNotified(false);
        }
        persist();
    }

    /** Sweeps every medication's dose logs, flipping stale PENDING entries to MISSED. Call before any read that displays status/analytics. */
    public void refreshMissedStatuses() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(MISSED_GRACE_HOURS);
        boolean changed = false;
        for (Patient patient : appData.getPatients()) {
            for (Medication med : patient.getMedications()) {
                for (DoseLog log : med.getDoseLogs()) {
                    if (log.getStatus() == DoseStatus.PENDING) {
                        LocalDateTime scheduled = DateTimeUtils.parseDateTime(log.getScheduledDateTime());
                        if (scheduled.isBefore(cutoff)) {
                            log.setStatus(DoseStatus.MISSED);
                            changed = true;
                        }
                    }
                }
            }
        }
        if (changed) {
            persist();
        }
    }

    // ---- Export / Import ----

    public void exportTo(Uri destination) throws IOException {
        storage.exportTo(destination);
    }

    public void importFrom(Uri source) throws IOException {
        AppData imported = storage.readAndValidate(source);
        storage.save(imported);
        this.appData = imported;
    }

    // ---- Analytics helpers ----

    public static final class DosePoint {
        public final Patient patient;
        public final Medication medication;
        public final DoseLog doseLog;
        public final long delayMinutes;

        DosePoint(Patient patient, Medication medication, DoseLog doseLog, long delayMinutes) {
            this.patient = patient;
            this.medication = medication;
            this.doseLog = doseLog;
            this.delayMinutes = delayMinutes;
        }
    }

    /** Taken doses only, with delay (actual - scheduled) in minutes; negative means early. */
    public List<DosePoint> getTakenDosePoints(String patientId, String medicationIdOrNull) {
        refreshMissedStatuses();
        List<DosePoint> points = new ArrayList<>();
        for (Patient patient : appData.getPatients()) {
            if (patientId != null && !patient.getId().equals(patientId)) {
                continue;
            }
            for (Medication med : patient.getMedications()) {
                if (medicationIdOrNull != null && !med.getId().equals(medicationIdOrNull)) {
                    continue;
                }
                for (DoseLog log : med.getDoseLogs()) {
                    if (log.getStatus() != DoseStatus.TAKEN || log.getActualTakenDateTime() == null) {
                        continue;
                    }
                    LocalDateTime scheduled = DateTimeUtils.parseDateTime(log.getScheduledDateTime());
                    LocalDateTime actual = DateTimeUtils.parseDateTime(log.getActualTakenDateTime());
                    long delay = ChronoUnit.MINUTES.between(scheduled, actual);
                    points.add(new DosePoint(patient, med, log, delay));
                }
            }
        }
        points.sort((a, b) -> a.doseLog.getScheduledDateTime().compareTo(b.doseLog.getScheduledDateTime()));
        return points;
    }

    public static final class AdherenceStats {
        public final int takenCount;
        public final int missedCount;
        public final double averageDelayMinutes;

        AdherenceStats(int takenCount, int missedCount, double averageDelayMinutes) {
            this.takenCount = takenCount;
            this.missedCount = missedCount;
            this.averageDelayMinutes = averageDelayMinutes;
        }

        public double adherencePercent() {
            int total = takenCount + missedCount;
            return total == 0 ? 0 : (100.0 * takenCount / total);
        }
    }

    public AdherenceStats getAdherenceStats(String patientId, String medicationIdOrNull) {
        List<DosePoint> taken = getTakenDosePoints(patientId, medicationIdOrNull);
        int missed = 0;
        for (Patient patient : appData.getPatients()) {
            if (patientId != null && !patient.getId().equals(patientId)) {
                continue;
            }
            for (Medication med : patient.getMedications()) {
                if (medicationIdOrNull != null && !med.getId().equals(medicationIdOrNull)) {
                    continue;
                }
                for (DoseLog log : med.getDoseLogs()) {
                    if (log.getStatus() == DoseStatus.MISSED) {
                        missed++;
                    }
                }
            }
        }
        double average = 0;
        if (!taken.isEmpty()) {
            long sum = 0;
            for (DosePoint p : taken) {
                sum += p.delayMinutes;
            }
            average = (double) sum / taken.size();
        }
        return new AdherenceStats(taken.size(), missed, average);
    }
}
