package com.adnaan525.medme.ui.patients;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adnaan525.medme.R;
import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.model.DurationType;
import com.adnaan525.medme.model.Inventory;
import com.adnaan525.medme.model.Medication;
import com.adnaan525.medme.model.Patient;
import com.adnaan525.medme.util.ScheduleUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TravelSummaryActivity extends AppCompatActivity {

    public static final String EXTRA_PATIENT_ID = "patient_id";
    public static final String EXTRA_TRIP_DAYS = "trip_days";

    private DataRepository repository;
    private int tripDays;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel_summary);

        repository = DataRepository.getInstance(this);
        String patientId = getIntent().getStringExtra(EXTRA_PATIENT_ID);
        tripDays = getIntent().getIntExtra(EXTRA_TRIP_DAYS, 0);
        Patient patient = patientId != null ? repository.getPatient(patientId) : null;
        if (patient == null || tripDays <= 0) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        TextView toolbarTitle = findViewById(R.id.textToolbarTitle);
        toolbarTitle.setText(patient.getName());

        TextView subtitle = findViewById(R.id.textSubtitle);
        String daysLabel = getResources().getQuantityString(R.plurals.travel_days_count, tripDays, tripDays);
        subtitle.setText(getString(R.string.travel_summary_subtitle, daysLabel));

        RecyclerView recycler = findViewById(R.id.recyclerTravelItems);
        View emptyState = findViewById(R.id.textEmptyTravel);
        TravelSummaryAdapter adapter = new TravelSummaryAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        List<TravelSummaryAdapter.Row> rows = buildRows(patient);
        adapter.submitList(rows);
        emptyState.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private List<TravelSummaryAdapter.Row> buildRows(Patient patient) {
        List<TravelSummaryAdapter.Row> rows = new ArrayList<>();
        for (Medication med : patient.getMedications()) {
            if (ScheduleUtils.isArchived(med)) {
                continue;
            }

            int quantity = ScheduleUtils.packingQuantityForTrip(med, tripDays);
            String quantityText;
            if (quantity < 0) {
                quantityText = getString(R.string.travel_unknown_quantity);
            } else if (med.getDurationType() == DurationType.FIXED_DAYS) {
                long courseDaysLeft = ScheduleUtils.remainingCourseDays(med, LocalDate.now());
                quantityText = courseDaysLeft < tripDays
                        ? getString(R.string.travel_bring_quantity_course_ends, quantity, courseDaysLeft)
                        : getString(R.string.travel_bring_quantity, quantity);
            } else {
                quantityText = getString(R.string.travel_bring_quantity, quantity);
            }

            String stockText = null;
            boolean insufficient = false;
            Inventory inventory = med.getInventory();
            if (inventory != null && quantity >= 0) {
                int onHand = inventory.getQuantityRemaining();
                insufficient = onHand < quantity;
                stockText = insufficient
                        ? getString(R.string.travel_stock_insufficient, onHand, quantity - onHand)
                        : getString(R.string.travel_stock_sufficient, onHand);
            }

            rows.add(new TravelSummaryAdapter.Row(med.getName(), quantityText, stockText, insufficient));
        }
        return rows;
    }
}
