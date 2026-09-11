package com.adnaan525.medme.ui.patients;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.adnaan525.medme.R;
import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.model.DurationType;
import com.adnaan525.medme.model.Inventory;
import com.adnaan525.medme.model.Medication;
import com.adnaan525.medme.model.Patient;
import com.adnaan525.medme.notifications.AlarmScheduler;
import com.adnaan525.medme.util.DateTimeUtils;
import com.adnaan525.medme.util.InsetsUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddEditMedicationActivity extends AppCompatActivity {

    public static final String EXTRA_PATIENT_ID = "patient_id";
    public static final String EXTRA_MEDICATION_ID = "medication_id";

    private DataRepository repository;
    private String patientId;
    private String medicationId;
    private Medication existingMedication;

    private TextInputEditText editMedName;
    private RadioGroup radioGroupDuration;
    private View layoutTotalDays;
    private TextInputEditText editTotalDays;
    private Spinner spinnerDurationUnit;
    private Button buttonPickStartDate;
    private android.widget.LinearLayout containerDoseTimes;
    private View layoutInitialQuantity;
    private TextInputEditText editInitialQuantity;
    private TextInputEditText editLowStockThreshold;

    private LocalDate startDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medication);

        repository = DataRepository.getInstance(this);
        patientId = getIntent().getStringExtra(EXTRA_PATIENT_ID);
        medicationId = getIntent().getStringExtra(EXTRA_MEDICATION_ID);
        if (patientId == null || repository.getPatient(patientId) == null) {
            finish();
            return;
        }

        bindViews();
        InsetsUtils.applySystemAndImeInsets(findViewById(R.id.rootLayout));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        radioGroupDuration.setOnCheckedChangeListener((group, checkedId) ->
                layoutTotalDays.setVisibility(checkedId == R.id.radioFixedDays ? View.VISIBLE : View.GONE));

        if (medicationId != null) {
            existingMedication = findMedication(medicationId);
            if (existingMedication == null) {
                finish();
                return;
            }
            toolbar.setTitle(R.string.edit_medication);
            layoutInitialQuantity.setVisibility(View.GONE);
            populateForEdit(existingMedication);
        } else {
            toolbar.setTitle(R.string.add_medication);
            startDate = LocalDate.now();
            addTimeRow(LocalTime.of(8, 0));
        }
        updateStartDateLabel();

        findViewById(R.id.buttonPickStartDate).setOnClickListener(v -> pickStartDate());
        findViewById(R.id.buttonAddTimeSlot).setOnClickListener(v -> addTimeRow(LocalTime.of(9, 0)));
        findViewById(R.id.buttonSave).setOnClickListener(v -> save());
    }

    private Medication findMedication(String id) {
        Patient patient = repository.getPatient(patientId);
        if (patient == null) {
            return null;
        }
        for (Medication m : patient.getMedications()) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        return null;
    }

    private void bindViews() {
        editMedName = findViewById(R.id.editMedName);
        radioGroupDuration = findViewById(R.id.radioGroupDuration);
        layoutTotalDays = findViewById(R.id.layoutTotalDays);
        editTotalDays = findViewById(R.id.editTotalDays);
        spinnerDurationUnit = findViewById(R.id.spinnerDurationUnit);
        buttonPickStartDate = findViewById(R.id.buttonPickStartDate);
        containerDoseTimes = findViewById(R.id.containerDoseTimes);
        layoutInitialQuantity = findViewById(R.id.layoutInitialQuantity);
        editInitialQuantity = findViewById(R.id.editInitialQuantity);
        editLowStockThreshold = findViewById(R.id.editLowStockThreshold);
    }

    private void populateForEdit(Medication med) {
        editMedName.setText(med.getName());
        if (med.getDurationType() == DurationType.FIXED_DAYS) {
            radioGroupDuration.check(R.id.radioFixedDays);
            layoutTotalDays.setVisibility(View.VISIBLE);
            editTotalDays.setText(String.valueOf(med.getTotalDays()));
        } else {
            radioGroupDuration.check(R.id.radioRecurring);
        }
        startDate = DateTimeUtils.parseDate(med.getStartDate());
        for (String time : med.getDoseTimes()) {
            addTimeRow(DateTimeUtils.parseTime(time));
        }
        if (med.getInventory() != null) {
            editLowStockThreshold.setText(String.valueOf(med.getInventory().getLowStockThreshold()));
        }
    }

    private void pickStartDate() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startDate = LocalDate.of(year, month + 1, dayOfMonth);
            updateStartDateLabel();
        }, startDate.getYear(), startDate.getMonthValue() - 1, startDate.getDayOfMonth()).show();
    }

    private void updateStartDateLabel() {
        buttonPickStartDate.setText(startDate.format(DateTimeUtils.DISPLAY_DATE_FORMAT));
    }

    private void addTimeRow(LocalTime time) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_dose_time_row, containerDoseTimes, false);
        row.setTag(time);
        bindTimeRow(row);
        containerDoseTimes.addView(row);
        updateRemoveButtonsEnabled();
    }

    private void bindTimeRow(View row) {
        TextView textTimeValue = row.findViewById(R.id.textTimeValue);
        Button buttonPickTime = row.findViewById(R.id.buttonPickTime);
        ImageButton buttonRemoveTime = row.findViewById(R.id.buttonRemoveTime);

        LocalTime time = (LocalTime) row.getTag();
        textTimeValue.setText(time.format(DateTimeUtils.DISPLAY_TIME_FORMAT));

        buttonPickTime.setOnClickListener(v -> {
            LocalTime current = (LocalTime) row.getTag();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                LocalTime updated = LocalTime.of(hourOfDay, minute);
                row.setTag(updated);
                textTimeValue.setText(updated.format(DateTimeUtils.DISPLAY_TIME_FORMAT));
            }, current.getHour(), current.getMinute(), true).show();
        });

        buttonRemoveTime.setOnClickListener(v -> {
            if (containerDoseTimes.getChildCount() > 1) {
                containerDoseTimes.removeView(row);
                updateRemoveButtonsEnabled();
            }
        });
    }

    private void updateRemoveButtonsEnabled() {
        boolean canRemove = containerDoseTimes.getChildCount() > 1;
        for (int i = 0; i < containerDoseTimes.getChildCount(); i++) {
            View row = containerDoseTimes.getChildAt(i);
            ImageButton button = row.findViewById(R.id.buttonRemoveTime);
            button.setEnabled(canRemove);
            button.setAlpha(canRemove ? 1f : 0.3f);
        }
    }

    /** Converts a duration entered as days/weeks/months into a day count, anchored to startDate so month lengths are exact. */
    private int totalDaysFor(int quantity, int unitPosition) {
        switch (unitPosition) {
            case 1: // Weeks
                return quantity * 7;
            case 2: // Months
                return (int) ChronoUnit.DAYS.between(startDate, startDate.plusMonths(quantity));
            case 0: // Days
            default:
                return quantity;
        }
    }

    private void save() {
        String name = editMedName.getText() != null ? editMedName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            editMedName.setError(getString(R.string.error_name_required));
            return;
        }

        boolean isFixedDays = radioGroupDuration.getCheckedRadioButtonId() == R.id.radioFixedDays;
        int totalDays = 0;
        if (isFixedDays) {
            int quantity;
            try {
                quantity = Integer.parseInt(editTotalDays.getText().toString().trim());
                if (quantity <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                editTotalDays.setError(getString(R.string.error_total_days_required));
                return;
            }
            totalDays = totalDaysFor(quantity, spinnerDurationUnit.getSelectedItemPosition());
        }

        List<LocalTime> times = new ArrayList<>();
        for (int i = 0; i < containerDoseTimes.getChildCount(); i++) {
            times.add((LocalTime) containerDoseTimes.getChildAt(i).getTag());
        }
        if (times.isEmpty()) {
            Toast.makeText(this, R.string.error_time_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.sort(times);
        List<String> doseTimeStrings = new ArrayList<>();
        for (LocalTime t : times) {
            doseTimeStrings.add(DateTimeUtils.formatTime(t));
        }

        int lowStockThreshold;
        try {
            lowStockThreshold = Integer.parseInt(editLowStockThreshold.getText().toString().trim());
            if (lowStockThreshold < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            editLowStockThreshold.setError(getString(R.string.error_threshold_required));
            return;
        }

        if (existingMedication == null) {
            int initialQuantity;
            try {
                initialQuantity = Integer.parseInt(editInitialQuantity.getText().toString().trim());
                if (initialQuantity < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                editInitialQuantity.setError(getString(R.string.error_quantity_required));
                return;
            }

            Medication med = new Medication();
            med.setName(name);
            med.setDurationType(isFixedDays ? DurationType.FIXED_DAYS : DurationType.RECURRING);
            med.setTotalDays(totalDays);
            med.setStartDate(DateTimeUtils.formatDate(startDate));
            med.setDoseTimes(doseTimeStrings);
            med.setInventory(new Inventory(initialQuantity, lowStockThreshold));

            Medication saved = repository.addMedication(patientId, med);
            if (saved != null) {
                AlarmScheduler.scheduleAllForMedication(this, saved);
            }
        } else {
            AlarmScheduler.cancelAllForMedication(this, existingMedication);

            existingMedication.setName(name);
            existingMedication.setDurationType(isFixedDays ? DurationType.FIXED_DAYS : DurationType.RECURRING);
            existingMedication.setTotalDays(totalDays);
            existingMedication.setStartDate(DateTimeUtils.formatDate(startDate));
            existingMedication.setDoseTimes(doseTimeStrings);
            if (existingMedication.getInventory() != null) {
                existingMedication.getInventory().setLowStockThreshold(lowStockThreshold);
            }

            repository.updateMedication(patientId, existingMedication);
            AlarmScheduler.scheduleAllForMedication(this, existingMedication);
        }

        finish();
    }
}
