package com.adnaan525.medme.ui.patients;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adnaan525.medme.R;
import com.adnaan525.medme.model.Patient;

import java.util.ArrayList;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {

    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
    }

    private final List<Patient> patients = new ArrayList<>();
    private final OnPatientClickListener listener;

    public PatientAdapter(OnPatientClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Patient> newPatients) {
        patients.clear();
        patients.addAll(newPatients);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient patient = patients.get(position);
        holder.name.setText(patient.getName());
        int count = patient.getMedications().size();
        holder.medicationCount.setText(holder.itemView.getResources()
                .getQuantityString(R.plurals.medication_count, count, count));
        holder.itemView.setOnClickListener(v -> listener.onPatientClick(patient));
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView medicationCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textPatientName);
            medicationCount = itemView.findViewById(R.id.textMedicationCount);
        }
    }
}
