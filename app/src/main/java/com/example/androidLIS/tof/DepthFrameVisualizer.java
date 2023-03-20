package com.example.androidLIS.tof;

import android.graphics.Bitmap;

public interface DepthFrameVisualizer {
    void onRawDataAvailable(Bitmap bitmap);
    void onNoiseReductionAvailable(Bitmap bitmap);
    void onMovingAverageAvailable(Bitmap bitmap);
    void onBlurredMovingAverageAvailable(Bitmap bitmap);
    void onCargoExpect(boolean cargo);
    void onSampleDepth(String depth);
    void onCargoVolume(int volume, int[] matrix);
    void onTOFStatus(boolean status);
}
