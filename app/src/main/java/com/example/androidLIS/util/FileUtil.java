package com.example.androidLIS.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtil {

    public static File createFileWithDate(String directoryName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH_mm_ss");
        String currentDate = dateFormat.format(new Date());
        String fileName = "LIS_LOG_" + currentDate + ".txt";
        Log.e("createFileWithDate", directoryName+"/"+fileName);

        File directory = new File(directoryName);
        if (!directory.exists()) {
            Log.e("createFileWithDate", "mkdirs");

            directory.mkdirs();
        }

        return new File(directory, fileName);
    }

    public static void writeDataToFile(File file, String data) {
        Log.e("writeDataToFile", data);

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(data.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFileContainingString(String directoryName, String string) {
        File directory = new File(directoryName);
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.getName().contains(string)) {
                file.delete();
            }
        }
    }
}