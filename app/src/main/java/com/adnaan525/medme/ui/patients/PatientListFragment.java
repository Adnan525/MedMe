package com.adnaan525.medme.ui.patients;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adnaan525.medme.R;
import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.model.Patient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class PatientListFragment extends Fragment {

    private DataRepository repository;
    private PatientAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = DataRepository.getInstance(requireContext());

        recyclerView = view.findViewById(R.id.recyclerPatients);
        emptyState = view.findViewById(R.id.textEmptyState);
        adapter = new PatientAdapter(this::openPatient);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fabAddPatient);
        fab.setOnClickListener(v -> showAddPatientDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        java.util.List<Patient> patients = repository.getPatients();
        adapter.submitList(patients);
        emptyState.setVisibility(patients.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openPatient(Patient patient) {
        Intent intent = new Intent(requireContext(), PatientDetailActivity.class);
        intent.putExtra(PatientDetailActivity.EXTRA_PATIENT_ID, patient.getId());
        startActivity(intent);
    }

    private void showAddPatientDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_single_input, null);
        TextInputEditText input = dialogView.findViewById(R.id.editInput);
        com.google.android.material.textfield.TextInputLayout inputLayout = dialogView.findViewById(R.id.inputLayout);
        inputLayout.setHint(getString(R.string.patient_name));
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_patient)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!name.isEmpty()) {
                        repository.addPatient(name);
                        refresh();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
