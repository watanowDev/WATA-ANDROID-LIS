package com.example.androidLIS.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FileManager {

    private Context mContext;

    public FileManager(Context context) {
        mContext = context;
    }

    //외부
    public File createFileWithDate(String directoryName) {
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

    public void writeDataToFile(File file, String data) {
        Log.e("writeDataToFile", data);

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(data.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFileContainingString(String directoryName, String string) {
        File directory = new File(directoryName);
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.getName().contains(string)) {
                file.delete();
            }
        }
    }

    //내부

    public File createFileIfNotExists(String filename) {
        boolean isCreated = false;
        File file = mContext.getFileStreamPath(filename);
        if (!file.exists()) {
            try {
                FileOutputStream fos = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
                fos.close();
                isCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(!isCreated) {
            return file;
        }else{
            return null;
        }
    }

    public void writeFile(File file, String data) {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput(file.getName(), Context.MODE_APPEND);
            fos.write(data.getBytes());
            fos.write(System.getProperty("line.separator").getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> readFile(File file) {
        FileInputStream fis = null;
        ArrayList<String> reserveData = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        try {
            fis = mContext.openFileInput(file.getName());
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if(!line.equals("")) {
                    reserveData.add(line);
                }
            }
            reader.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reserveData;
    }

    public void clearFile(File file) {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput(file.getName(), Context.MODE_PRIVATE);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}