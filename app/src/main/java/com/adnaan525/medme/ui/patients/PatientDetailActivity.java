package com.adnaan525.medme.ui.patients;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

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
import com.adnaan525.medme.util.InsetsUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class PatientDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PATIENT_ID = "patient_id";

    private DataRepository repository;
    private String patientId;
    private MedicationAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyState;
    private Toolbar toolbar;
    private TextView toolbarTitle;

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
        InsetsUtils.applyTopInset(toolbar);
        InsetsUtils.applyBottomInset(findViewById(R.id.rootLayout));

        ImageButton buttonMoreOptions = findViewById(R.id.buttonMoreOptions);
        buttonMoreOptions.setOnClickListener(this::showOverflowMenu);

        recyclerView = findViewById(R.id.recyclerMedications);
        emptyState = findViewById(R.id.textEmptyState);
        adapter = new MedicationAdapter(this::openMedication);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

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
        Patient patient = repository.getPatient(patientId);
        if (patient == null) {
            finish();
            return;
        }
        toolbarTitle.setText(patient.getName());
        adapter.submitList(patient.getMedications());
        emptyState.setVisibility(patient.getMedications().isEmpty() ? View.VISIBLE : View.GONE);
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
            if (item.getItemId() == R.id.action_delete_patient) {
                confirmDeletePatient();
                return true;
            }
            return false;
        });
        popup.show();
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
