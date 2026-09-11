package com.adnaan525.medme;

import android.app.Application;

import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.notifications.NotificationHelper;

public class MedMeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        DataRepository.getInstance(this);
    }
}
