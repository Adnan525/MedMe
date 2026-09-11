package com.adnaan525.medme.ui.patients;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adnaan525.medme.R;
import com.adnaan525.medme.model.DurationType;
import com.adnaan525.medme.model.Inventory;
import com.adnaan525.medme.model.Medication;
import com.adnaan525.medme.util.DateTimeUtils;
import com.adnaan525.medme.util.ScheduleUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.ViewHolder> {

    public interface OnMedicationClickListener {
        void onMedicationClick(Medication medication);
    }

    private final List<Medication> medications = new ArrayList<>();
    private final OnMedicationClickListener listener;

    public MedicationAdapter(OnMedicationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Medication> newMedications) {
        medications.clear();
        medications.addAll(newMedications);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medication med = medications.get(position);
        android.content.res.Resources res = holder.itemView.getResources();

        holder.name.setText(med.getName());
        int doseCount = med.getDoseTimes().size();

        String scheduleText;
        if (med.getDurationType() == DurationType.FIXED_DAYS) {
            if (ScheduleUtils.isCourseFinished(med)) {
                scheduleText = res.getString(R.string.schedule_fixed_days_ended_summary, doseCount);
            } else if (ScheduleUtils.isCourseUpcoming(med)) {
                String startLabel = DateTimeUtils.parseDate(med.getStartDate()).format(DateTimeUtils.DISPLAY_DATE_FORMAT);
                scheduleText = res.getString(R.string.schedule_fixed_days_upcoming_summary, doseCount, startLabel);
            } else {
                LocalDate start = DateTimeUtils.parseDate(med.getStartDate());
                long dayNumber = ChronoUnit.DAYS.between(start, LocalDate.now()) + 1;
                scheduleText = res.getString(R.string.schedule_fixed_days_summary, doseCount, dayNumber, med.getTotalDays());
            }
        } else {
            scheduleText = res.getString(R.string.schedule_recurring_summary, doseCount);
        }
        holder.schedule.setText(scheduleText);

        Inventory inventory = med.getInventory();
        if (inventory != null) {
            holder.inventory.setText(res.getString(R.string.inventory_summary, inventory.getQuantityRemaining()));
            holder.inventory.setVisibility(View.VISIBLE);
            holder.lowStockIndicator.setVisibility(ScheduleUtils.isLowStock(med) ? View.VISIBLE : View.GONE);
        } else {
            holder.inventory.setVisibility(View.GONE);
            holder.lowStockIndicator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onMedicationClick(med));
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView schedule;
        final TextView inventory;
        final View lowStockIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textMedName);
            schedule = itemView.findViewById(R.id.textMedSchedule);
            inventory = itemView.findViewById(R.id.textMedInventory);
            lowStockIndicator = itemView.findViewById(R.id.viewLowStockIndicator);
        }
    }
}
