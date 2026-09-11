package com.adnaan525.medme.ui.patients;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adnaan525.medme.R;
import com.adnaan525.medme.model.DoseStatus;
import com.adnaan525.medme.util.DateTimeUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TodayDoseAdapter extends RecyclerView.Adapter<TodayDoseAdapter.ViewHolder> {

    public static class Row {
        public final LocalDateTime scheduled;
        public final DoseStatus status;
        public final LocalDateTime actualTaken;

        public Row(LocalDateTime scheduled, DoseStatus status, LocalDateTime actualTaken) {
            this.scheduled = scheduled;
            this.status = status;
            this.actualTaken = actualTaken;
        }
    }

    public interface OnMarkTakenListener {
        void onMarkTaken(LocalDateTime scheduled);
    }

    private final List<Row> rows = new ArrayList<>();
    private final OnMarkTakenListener listener;

    public TodayDoseAdapter(OnMarkTakenListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dose_occurrence, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Row row = rows.get(position);
        android.content.res.Resources res = holder.itemView.getResources();

        holder.time.setText(row.scheduled.format(DateTimeUtils.DISPLAY_TIME_FORMAT));

        switch (row.status) {
            case TAKEN:
                String takenAt = row.actualTaken != null
                        ? row.actualTaken.format(DateTimeUtils.DISPLAY_TIME_FORMAT)
                        : "";
                holder.status.setText(res.getString(R.string.dose_status_taken, takenAt));
                holder.markTaken.setVisibility(View.GONE);
                break;
            case MISSED:
                holder.status.setText(R.string.dose_status_missed);
                holder.markTaken.setVisibility(View.VISIBLE);
                holder.markTaken.setOnClickListener(v -> listener.onMarkTaken(row.scheduled));
                break;
            case PENDING:
            default:
                holder.status.setText(R.string.dose_status_pending);
                holder.markTaken.setVisibility(View.VISIBLE);
                holder.markTaken.setOnClickListener(v -> listener.onMarkTaken(row.scheduled));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView time;
        final TextView status;
        final Button markTaken;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.textDoseTime);
            status = itemView.findViewById(R.id.textDoseStatus);
            markTaken = itemView.findViewById(R.id.buttonMarkTaken);
        }
    }
}
