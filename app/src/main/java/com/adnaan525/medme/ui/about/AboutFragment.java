package com.adnaan525.medme.ui.about;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.adnaan525.medme.R;
import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.notifications.AlarmScheduler;
import com.adnaan525.medme.util.DateTimeUtils;

import java.io.IOException;

public class AboutFragment extends Fragment {

    private DataRepository repository;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            this::handleExportResult);

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::confirmImport);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = DataRepository.getInstance(requireContext());

        TextView textVersion = view.findViewById(R.id.textVersion);
        textVersion.setText(getString(R.string.version_format, versionName()));

        view.findViewById(R.id.buttonExportData).setOnClickListener(v ->
                exportLauncher.launch("medme_backup_" + DateTimeUtils.todayIso() + ".json"));

        view.findViewById(R.id.buttonImportData).setOnClickListener(v ->
                importLauncher.launch(new String[]{"application/json"}));
    }

    private String versionName() {
        try {
            PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    private void handleExportResult(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            repository.exportTo(uri);
            Toast.makeText(requireContext(), R.string.export_success, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), getString(R.string.export_failure, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmImport(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.import_confirm_title)
                .setMessage(R.string.import_confirm_message)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> doImport(uri))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void doImport(Uri uri) {
        try {
            for (com.adnaan525.medme.model.Patient patient : repository.getPatients()) {
                for (com.adnaan525.medme.model.Medication med : patient.getMedications()) {
                    AlarmScheduler.cancelAllForMedication(requireContext(), med);
                }
            }
            repository.importFrom(uri);
            AlarmScheduler.rescheduleAll(requireContext());
            Toast.makeText(requireContext(), R.string.import_success, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), getString(R.string.import_failure, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
}
