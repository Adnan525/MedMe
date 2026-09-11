package com.adnaan525.medme.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.adnaan525.medme.model.AppData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Reads/writes the single-file JSON store. Writes go to a temp file and are then renamed
 * into place so a crash or kill mid-write can never leave a half-written data file behind.
 */
public class JsonStorage {
    private static final String FILE_NAME = "medme_data.json";

    private final Context appContext;
    private final Gson gson;

    public JsonStorage(Context context) {
        this.appContext = context.getApplicationContext();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    private File dataFile() {
        return new File(appContext.getFilesDir(), FILE_NAME);
    }

    public AppData load() {
        File file = dataFile();
        if (!file.exists()) {
            return new AppData();
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            AppData data = gson.fromJson(reader, AppData.class);
            return sanitize(data);
        } catch (IOException | JsonSyntaxException e) {
            return new AppData();
        }
    }

    private AppData sanitize(AppData data) {
        if (data == null) {
            return new AppData();
        }
        if (data.getPatients() == null) {
            data.setPatients(new java.util.ArrayList<>());
        }
        return data;
    }

    public synchronized void save(AppData data) throws IOException {
        File target = dataFile();
        File tempFile = new File(appContext.getFilesDir(), FILE_NAME + ".tmp");
        try (Writer writer = new java.io.OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }
        Files.move(tempFile.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public void exportTo(Uri destination) throws IOException {
        ContentResolver resolver = appContext.getContentResolver();
        try (InputStream input = new FileInputStream(dataFile());
             OutputStream output = resolver.openOutputStream(destination)) {
            if (output == null) {
                throw new IOException("Could not open destination for writing");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
    }

    /** Parses and validates a backup file without touching the live data file. */
    public AppData readAndValidate(Uri source) throws IOException {
        ContentResolver resolver = appContext.getContentResolver();
        try (InputStream input = resolver.openInputStream(source)) {
            if (input == null) {
                throw new IOException("Could not open selected file");
            }
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            AppData data = gson.fromJson(reader, AppData.class);
            if (data == null || data.getPatients() == null || data.getSchemaVersion() <= 0) {
                throw new IOException("This file doesn't look like a valid MedMe backup");
            }
            return data;
        } catch (JsonSyntaxException e) {
            throw new IOException("This file doesn't look like a valid MedMe backup", e);
        }
    }
}
