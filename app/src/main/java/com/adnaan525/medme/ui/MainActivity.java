package com.adnaan525.medme.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.adnaan525.medme.R;
import com.adnaan525.medme.notifications.AlarmScheduler;
import com.adnaan525.medme.ui.about.AboutFragment;
import com.adnaan525.medme.ui.analysis.AnalysisFragment;
import com.adnaan525.medme.ui.patients.PatientListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AlarmScheduler.rescheduleAll(this);
        maybeRequestNotificationPermission();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_patients) {
                showFragment(new PatientListFragment());
                return true;
            } else if (id == R.id.nav_analysis) {
                showFragment(new AnalysisFragment());
                return true;
            } else if (id == R.id.nav_about) {
                showFragment(new AboutFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            showFragment(new PatientListFragment());
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
