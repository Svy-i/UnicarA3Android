package com.svyatogor.appcaronaa3.Activities;

import android.app.Application;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class Cloudinary extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Configurações do Cloudinary
        Map config = new HashMap();

        config.put("cloud_name", "dljy91pou");
        config.put("api_key", "236263628567759");
        config.put("api_secret", "lLYg-1T0bFwnQQ5fIOfI2fYXg90@dljy91pou");

        MediaManager.init(this, config);
    }
}
