package com.adnaan525.medme.ui.analysis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.adnaan525.medme.R;
import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.model.Medication;
import com.adnaan525.medme.model.Patient;
import com.adnaan525.medme.util.DateTimeUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.highlight.Highlight;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnalysisFragment extends Fragment {

    private DataRepository repository;
    private Spinner spinnerPatient;
    private Spinner spinnerMedication;
    private LineChart chart;
    private View layoutSpinners;
    private View layoutStats;
    private TextView textNoData;
    private TextView textAverageDelay;
    private TextView textAdherence;
    private TextView textMissedCount;

    private List<Patient> patients = new ArrayList<>();
    private List<Medication> selectedPatientMedications = new ArrayList<>();
    private boolean suppressSpinnerCallbacks = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = DataRepository.getInstance(requireContext());

        spinnerPatient = view.findViewById(R.id.spinnerPatient);
        spinnerMedication = view.findViewById(R.id.spinnerMedication);
        chart = view.findViewById(R.id.chartDelay);
        layoutSpinners = view.findViewById(R.id.layoutSpinners);
        layoutStats = view.findViewById(R.id.layoutStats);
        textNoData = view.findViewById(R.id.textNoData);
        textAverageDelay = view.findViewById(R.id.textAverageDelay);
        textAdherence = view.findViewById(R.id.textAdherence);
        textMissedCount = view.findViewById(R.id.textMissedCount);

        configureChart();

        spinnerPatient.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (suppressSpinnerCallbacks) return;
                populateMedicationSpinner(patients.get(position));
                rebuildChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerMedication.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (suppressSpinnerCallbacks) return;
                rebuildChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        repository.refreshMissedStatuses();
        patients = repository.getPatients();

        boolean hasPatients = !patients.isEmpty();
        layoutSpinners.setVisibility(hasPatients ? View.VISIBLE : View.GONE);
        chart.setVisibility(hasPatients ? View.VISIBLE : View.GONE);
        layoutStats.setVisibility(hasPatients ? View.VISIBLE : View.GONE);
        textNoData.setVisibility(hasPatients ? View.GONE : View.VISIBLE);
        if (!hasPatients) {
            return;
        }

        List<String> names = new ArrayList<>();
        for (Patient p : patients) {
            names.add(p.getName());
        }
        suppressSpinnerCallbacks = true;
        ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
        patientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPatient.setAdapter(patientAdapter);
        suppressSpinnerCallbacks = false;

        populateMedicationSpinner(patients.get(spinnerPatient.getSelectedItemPosition() >= 0 ? spinnerPatient.getSelectedItemPosition() : 0));
        rebuildChart();
    }

    private void populateMedicationSpinner(Patient patient) {
        selectedPatientMedications = patient.getMedications();
        List<String> names = new ArrayList<>();
        names.add(getString(R.string.all_medications));
        for (Medication m : selectedPatientMedications) {
            names.add(m.getName());
        }
        suppressSpinnerCallbacks = true;
        ArrayAdapter<String> medAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
        medAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedication.setAdapter(medAdapter);
        suppressSpinnerCallbacks = false;
    }

    private void configureChart() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setExtraBottomOffset(8f);
        chart.setNoDataText(getString(R.string.no_dose_data));

        int textColor = ContextCompat.getColor(requireContext(), R.color.chart_text);
        int gridColor = ContextCompat.getColor(requireContext(), R.color.chart_grid);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(textColor);
        xAxis.setAxisLineColor(gridColor);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(textColor);
        leftAxis.setGridColor(gridColor);
        leftAxis.setAxisLineColor(gridColor);

        chart.getAxisRight().setEnabled(false);

        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e.getData() instanceof String) {
                    Toast.makeText(requireContext(), (String) e.getData(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {
            }
        });
    }

    private void rebuildChart() {
        if (patients.isEmpty()) {
            return;
        }
        int patientIndex = spinnerPatient.getSelectedItemPosition();
        if (patientIndex < 0 || patientIndex >= patients.size()) {
            return;
        }
        Patient patient = patients.get(patientIndex);

        int medIndex = spinnerMedication.getSelectedItemPosition();
        String medicationId = null;
        if (medIndex > 0 && medIndex - 1 < selectedPatientMedications.size()) {
            medicationId = selectedPatientMedications.get(medIndex - 1).getId();
        }

        List<DataRepository.DosePoint> points = repository.getTakenDosePoints(patient.getId(), medicationId);

        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            DataRepository.DosePoint point = points.get(i);
            Entry entry = new Entry(i, point.delayMinutes);
            String tooltip = point.medication.getName() + " · "
                    + DateTimeUtils.parseDateTime(point.doseLog.getScheduledDateTime()).format(DateTimeUtils.DISPLAY_DATE_TIME_FORMAT)
                    + " · " + delayLabel(point.delayMinutes);
            entry.setData(tooltip);
            entries.add(entry);
            xLabels.add(DateTimeUtils.parseDateTime(point.doseLog.getScheduledDateTime()).format(DateTimeUtils.DISPLAY_DATE_FORMAT));
        }

        int lineColor = ContextCompat.getColor(requireContext(), R.color.chart_line);
        int zeroLineColor = ContextCompat.getColor(requireContext(), R.color.chart_zero_line);
        int textColor = ContextCompat.getColor(requireContext(), R.color.chart_text);

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.chart_delay_axis_label));
        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(lineColor);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(false);
        dataSet.setHighlightEnabled(true);
        dataSet.setHighlightLineWidth(1f);
        dataSet.setHighLightColor(zeroLineColor);

        chart.setData(new LineData(dataSet));

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelCount(Math.max(1, Math.min(6, xLabels.size())), false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        LimitLine zeroLine = new LimitLine(0f, getString(R.string.on_time));
        zeroLine.setLineColor(zeroLineColor);
        zeroLine.setLineWidth(1f);
        zeroLine.enableDashedLine(6f, 4f, 0f);
        zeroLine.setTextColor(textColor);
        zeroLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        leftAxis.addLimitLine(zeroLine);

        chart.invalidate();

        DataRepository.AdherenceStats stats = repository.getAdherenceStats(patient.getId(), medicationId);
        textAverageDelay.setText(delayLabel(Math.round(stats.averageDelayMinutes)));
        textAdherence.setText(String.format(Locale.getDefault(), "%.0f%%", stats.adherencePercent()));
        textMissedCount.setText(String.valueOf(stats.missedCount));
    }

    private String delayLabel(long delayMinutes) {
        if (delayMinutes == 0) {
            return getString(R.string.on_time);
        } else if (delayMinutes > 0) {
            return getString(R.string.minutes_late, delayMinutes);
        } else {
            return getString(R.string.minutes_early, -delayMinutes);
        }
    }
}
