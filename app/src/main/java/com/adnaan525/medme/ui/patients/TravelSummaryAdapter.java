package com.adnaan525.medme.ui.patients;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adnaan525.medme.R;

import java.util.ArrayList;
import java.util.List;

public class TravelSummaryAdapter extends RecyclerView.Adapter<TravelSummaryAdapter.ViewHolder> {

    public static class Row {
        public final String medicationName;
        /** Display text for how much to pack - a computed count, or an "amount unknown" note for as-needed medications. */
        public final String quantityText;
        /** Display text for current stock vs. what's needed; null when there's nothing meaningful to compare (no inventory). */
        public final String stockText;
        public final boolean stockInsufficient;

        public Row(String medicationName, String quantityText, String stockText, boolean stockInsufficient) {
            this.medicationName = medicationName;
            this.quantityText = quantityText;
            this.stockText = stockText;
            this.stockInsufficient = stockInsufficient;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public void submitList(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_travel_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Row row = rows.get(position);
        holder.name.setText(row.medicationName);
        holder.quantity.setText(row.quantityText);

        if (row.stockText == null) {
            holder.stock.setVisibility(View.GONE);
        } else {
            holder.stock.setVisibility(View.VISIBLE);
            holder.stock.setText(row.stockText);
            android.content.res.Resources.Theme theme = holder.itemView.getContext().getTheme();
            android.util.TypedValue typedValue = new android.util.TypedValue();
            int attr = row.stockInsufficient
                    ? com.google.android.material.R.attr.colorError
                    : com.google.android.material.R.attr.colorOnSurfaceVariant;
            theme.resolveAttribute(attr, typedValue, true);
            holder.stock.setTextColor(typedValue.data);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView quantity;
        final TextView stock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textMedName);
            quantity = itemView.findViewById(R.id.textQuantity);
            stock = itemView.findViewById(R.id.textStock);
        }
    }
}
