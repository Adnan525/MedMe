package com.adnaan525.medme.ui.patients;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adnaan525.medme.R;
import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.model.DoseLog;
import com.adnaan525.medme.model.DoseStatus;
import com.adnaan525.medme.model.DurationType;
import com.adnaan525.medme.model.Inventory;
import com.adnaan525.medme.model.Medication;
import com.adnaan525.medme.model.Patient;
import com.adnaan525.medme.notifications.AlarmScheduler;
import com.adnaan525.medme.notifications.NotificationHelper;
import com.adnaan525.medme.util.DateTimeUtils;
import com.adnaan525.medme.util.InsetsUtils;
import com.adnaan525.medme.util.ScheduleUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PATIENT_ID = "patient_id";
    public static final String EXTRA_MEDICATION_ID = "medication_id";

    private DataRepository repository;
    private String patientId;
    private String medicationId;

    private Toolbar toolbar;
    private android.widget.TextView textPatientName;
    private android.widget.TextView textMedName;
    private android.widget.TextView textScheduleSummary;
    private android.widget.TextView textQuantityRemaining;
    private android.widget.TextView textLowStockThreshold;
    private RecyclerView recyclerTodayDoses;
    private View textNoDosesToday;
    private TodayDoseAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_detail);

        repository = DataRepository.getInstance(this);
        patientId = getIntent().getStringExtra(EXTRA_PATIENT_ID);
        medicationId = getIntent().getStringExtra(EXTRA_MEDICATION_ID);
        if (patientId == null || medicationId == null) {
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        InsetsUtils.applyTopInset(toolbar);
        InsetsUtils.applyBottomInset(findViewById(R.id.rootLayout));

        textPatientName = findViewById(R.id.textPatientName);
        textMedName = findViewById(R.id.textMedName);
        textScheduleSummary = findViewById(R.id.textScheduleSummary);
        textQuantityRemaining = findViewById(R.id.textQuantityRemaining);
        textLowStockThreshold = findViewById(R.id.textLowStockThreshold);
        recyclerTodayDoses = findViewById(R.id.recyclerTodayDoses);
        textNoDosesToday = findViewById(R.id.textNoDosesToday);

        adapter = new TodayDoseAdapter(this::markTaken);
        recyclerTodayDoses.setLayoutManager(new LinearLayoutManager(this));
        recyclerTodayDoses.setAdapter(adapter);

        findViewById(R.id.buttonReplenish).setOnClickListener(v -> showReplenishDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        repository.refreshMissedStatuses();
        refresh();
    }

    private Medication currentMedication() {
        DataRepository.MedicationLookup lookup = repository.findMedication(medicationId);
        return lookup != null ? lookup.medication : null;
    }

    private void refresh() {
        Patient patient = repository.getPatient(patientId);
        Medication med = currentMedication();
        if (patient == null || med == null) {
            finish();
            return;
        }

        textPatientName.setText(patient.getName());
        textMedName.setText(med.getName());
        toolbar.setTitle(med.getName());

        int doseCount = med.getDoseTimes().size();
        if (med.getDurationType() == DurationType.FIXED_DAYS) {
            textScheduleSummary.setText(getString(R.string.schedule_fixed_days_summary_full, doseCount, med.getTotalDays(),
                    DateTimeUtils.parseDate(med.getStartDate()).format(DateTimeUtils.DISPLAY_DATE_FORMAT)));
        } else {
            textScheduleSummary.setText(getResources().getQuantityString(R.plurals.dose_count_recurring, doseCount, doseCount));
        }

        Inventory inventory = med.getInventory();
        if (inventory != null) {
            textQuantityRemaining.setText(getString(R.string.quantity_remaining, inventory.getQuantityRemaining()));
            textLowStockThreshold.setText(getString(R.string.low_stock_threshold_label, inventory.getLowStockThreshold()));
        }

        List<LocalDateTime> occurrences = ScheduleUtils.occurrencesOn(med, LocalDate.now());
        List<TodayDoseAdapter.Row> rows = new ArrayList<>();
        for (LocalDateTime occurrence : occurrences) {
            String iso = DateTimeUtils.formatDateTime(occurrence);
            DoseLog match = null;
            for (DoseLog log : med.getDoseLogs()) {
                if (log.getScheduledDateTime().equals(iso)) {
                    match = log;
                    break;
                }
            }
            if (match != null) {
                LocalDateTime actual = match.getActualTakenDateTime() != null
                        ? DateTimeUtils.parseDateTime(match.getActualTakenDateTime())
                        : null;
                rows.add(new TodayDoseAdapter.Row(occurrence, match.getStatus(), actual));
            } else {
                rows.add(new TodayDoseAdapter.Row(occurrence, DoseStatus.PENDING, null));
            }
        }
        adapter.submitList(rows);
        textNoDosesToday.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerTodayDoses.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void markTaken(LocalDateTime scheduled) {
        DataRepository.DoseTakenResult result = repository.markTakenForOccurrence(medicationId, scheduled, LocalDateTime.now());
        if (result != null && result.triggersLowStockNotification) {
            NotificationHelper.showLowStock(this, result.patient, result.medication);
        }
        refresh();
    }

    private void showReplenishDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_single_input, null);
        TextInputEditText input = dialogView.findViewById(R.id.editInput);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.inputLayout);
        inputLayout.setHint(getString(R.string.replenishment_quantity));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_log_replenishment)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String text = input.getText() != null ? input.getText().toString().trim() : "";
                    try {
                        int quantity = Integer.parseInt(text);
                        if (quantity > 0) {
                            repository.logReplenishment(medicationId, quantity);
                            refresh();
                        }
                    } catch (NumberFormatException ignored) {
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_medication_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit_medication) {
            Intent intent = new Intent(this, AddEditMedicationActivity.class);
            intent.putExtra(AddEditMedicationActivity.EXTRA_PATIENT_ID, patientId);
            intent.putExtra(AddEditMedicationActivity.EXTRA_MEDICATION_ID, medicationId);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_delete_medication) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        Medication med = currentMedication();
        if (med == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_delete_medication)
                .setMessage(getString(R.string.confirm_delete_medication, med.getName()))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    AlarmScheduler.cancelAllForMedication(this, med);
                    repository.deleteMedication(patientId, medicationId);
                    finish();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
