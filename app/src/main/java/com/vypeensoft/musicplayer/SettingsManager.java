package com.vypeensoft.musicplayer;

import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class SettingsManager {
    private static final String FILE_PATH = "/sdcard/Vypeensoft/Music_Player/settings/folders.json";

    public static String loadScanFolders() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return "";
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = fis.read(data);
            if (read <= 0) return "";
            String jsonStr = new String(data, 0, read, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                sb.append(array.getString(i));
                if (i < array.length() - 1) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void saveScanFolders(String foldersStr) {
        try {
            File file = new File(FILE_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            JSONArray array = new JSONArray();
            String[] lines = foldersStr.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    array.put(line.trim());
                }
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
