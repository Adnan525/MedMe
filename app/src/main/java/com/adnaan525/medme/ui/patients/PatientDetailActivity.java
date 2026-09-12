package com.adnaan525.medme.ui.patients;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adnaan525.medme.R;
import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.model.Medication;
import com.adnaan525.medme.model.Patient;
import com.adnaan525.medme.notifications.AlarmScheduler;
import com.adnaan525.medme.util.ScheduleUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class PatientDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PATIENT_ID = "patient_id";

    private DataRepository repository;
    private String patientId;
    private MedicationAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyState;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_detail);

        repository = DataRepository.getInstance(this);
        patientId = getIntent().getStringExtra(EXTRA_PATIENT_ID);
        if (patientId == null || repository.getPatient(patientId) == null) {
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.textToolbarTitle);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ImageButton buttonMoreOptions = findViewById(R.id.buttonMoreOptions);
        buttonMoreOptions.setOnClickListener(this::showOverflowMenu);

        recyclerView = findViewById(R.id.recyclerMedications);
        emptyState = findViewById(R.id.textEmptyState);
        adapter = new MedicationAdapter(this::openMedication);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        tabLayout = findViewById(R.id.tabLayoutMedications);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                refresh();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        FloatingActionButton fab = findViewById(R.id.fabAddMedication);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditMedicationActivity.class);
            intent.putExtra(AddEditMedicationActivity.EXTRA_PATIENT_ID, patientId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        Patient patient = repository.getPatient(patientId);
        if (patient == null) {
            finish();
            return;
        }
        toolbarTitle.setText(patient.getName());

        List<Medication> active = new ArrayList<>();
        List<Medication> archived = new ArrayList<>();
        for (Medication med : patient.getMedications()) {
            if (ScheduleUtils.isArchived(med)) {
                archived.add(med);
            } else {
                active.add(med);
            }
        }

        boolean showingArchive = tabLayout.getSelectedTabPosition() == 1;
        List<Medication> shown = showingArchive ? archived : active;
        adapter.submitList(shown);
        emptyState.setText(showingArchive ? R.string.empty_archive : R.string.empty_medications);
        emptyState.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openMedication(Medication medication) {
        Intent intent = new Intent(this, MedicationDetailActivity.class);
        intent.putExtra(MedicationDetailActivity.EXTRA_PATIENT_ID, patientId);
        intent.putExtra(MedicationDetailActivity.EXTRA_MEDICATION_ID, medication.getId());
        startActivity(intent);
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.menu_patient_detail);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_travel_summary) {
                promptTravelDays();
                return true;
            } else if (item.getItemId() == R.id.action_delete_patient) {
                confirmDeletePatient();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void promptTravelDays() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_single_input, null);
        TextInputEditText input = dialogView.findViewById(R.id.editInput);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.inputLayout);
        inputLayout.setHint(getString(R.string.hint_travel_days));
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_travel_summary)
                .setView(dialogView)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                    String text = input.getText() != null ? input.getText().toString().trim() : "";
                    try {
                        int days = Integer.parseInt(text);
                        if (days <= 0) {
                            throw new NumberFormatException();
                        }
                        Intent intent = new Intent(this, TravelSummaryActivity.class);
                        intent.putExtra(TravelSummaryActivity.EXTRA_PATIENT_ID, patientId);
                        intent.putExtra(TravelSummaryActivity.EXTRA_TRIP_DAYS, days);
                        startActivity(intent);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.error_travel_days_required, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmDeletePatient() {
        Patient patient = repository.getPatient(patientId);
        if (patient == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_delete_patient)
                .setMessage(getString(R.string.confirm_delete_patient, patient.getName()))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    for (Medication med : patient.getMedications()) {
                        AlarmScheduler.cancelAllForMedication(this, med);
                    }
                    repository.deletePatient(patientId);
                    finish();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
