package com.example.androidLIS.tof;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import com.example.androidLIS.MainActivity;
import com.example.androidLIS.util.AppConfig;

import java.nio.ShortBuffer;
import java.util.Arrays;

public class DepthFrameAvailableListener implements ImageReader.OnImageAvailableListener {
    private static final String TAG = DepthFrameAvailableListener.class.getSimpleName();

    /**
     * 설정 파라미터 상수
     * WIDTH : TOF 이미지 너비
     * HEIGHT : TOF 이미지 높이
     * mResamplingIndexY : 물체 탐지 샘플링 Y
     * mResamplingIndexX : 물체 탐지 샘플링 X
     * mMaxResamplingNum : 샘플링 비트 수
     *
     */
    public static int WIDTH = AppConfig.TOF_WIDTH; //320
    public static int HEIGHT = AppConfig.TOF_HEIGHT; //240
    public int[] mResamplingIndexY = {AppConfig.TOF_RESAMPLING_HEIGHT_MIN, AppConfig.TOF_RESAMPLING_HEIGHT_MAX};
    public int[] mResamplingIndexX = {AppConfig.TOF_RESAMPLING_WIDTH_MIN, AppConfig.TOF_RESAMPLING_WIDTH_MAX};
    public int mMaxResamplingNum = (mResamplingIndexX[1]-mResamplingIndexX[0])*(mResamplingIndexY[1]-mResamplingIndexY[0]);

    public int mForkLength = AppConfig.TOF_FORK_LENGTH;
    public int mForkThickness = AppConfig.TOF_FORK_THICKNESS;
    public int mForkGap = AppConfig.TOF_FORK_GAP;

    public int mResamplingHeight =AppConfig.TOF_RESAMPLING_VOLUME_HEIGHT;
    public int mResamplingWidth =AppConfig.TOF_RESAMPLING_VOLUME_WIDTH;

    private static float RANGE_MIN = 0.0f;
    private static float RANGE_MAX = 4000.0f;
    private static float CONFIDENCE_FILTER = 0.1f;



    public int mMatrixWidth;
    public int mMatrixHeight;



    /**
     * 현재 샘플링 거리
     */
    public int mSamplingDistance = 0;




    private DepthFrameVisualizer depthFrameVisualizer;
    private int[] rawMask;

    /**
     * currentStatus : 현재 적재 상태
     * currentChangeTime : 상태 변화 시간
     */
    public boolean currentStatus = false;
    public long currentChangeTime = 0;


    public DepthFrameAvailableListener(DepthFrameVisualizer depthFrameVisualizer) {
        this.depthFrameVisualizer = depthFrameVisualizer;
        int size = WIDTH * HEIGHT;
        rawMask = new int[size];
        mMatrixWidth = Math.floorDiv(WIDTH,10);
        mMatrixHeight =  Math.floorDiv(HEIGHT,10);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        try {
            Image image = reader.acquireNextImage();
//            if (image != null && image.getFormat() == ImageFormat.DEPTH16) {
            if (image != null && image.getFormat() == ImageFormat.DEPTH16) {
                processImage(image);
                //publishRawData();
            }
            image.close();
        }
        catch (Exception e) {
            Log.e("error", "Failed to acquireNextImage: " + e.getMessage());
        }
    }

    private void publishRawData() {
        if (depthFrameVisualizer != null) {
            Bitmap bitmap = convertToRGBBitmap(rawMask);
            depthFrameVisualizer.onRawDataAvailable(bitmap);
            bitmap.recycle();
        }
    }

    private void sendDepth(String data){
        if (depthFrameVisualizer != null) {
            depthFrameVisualizer.onSampleDepth(data);
        }
    }

    private void sendCargoStatus(boolean cargo){
        if (depthFrameVisualizer != null) {
            depthFrameVisualizer.onCargoExpect(cargo);
        }
    }

    private void sendCargoVolume(int v, int[] matrix){
        if (depthFrameVisualizer != null) {
            depthFrameVisualizer.onCargoVolume(v, matrix);
        }
    }

    private void processImage(Image image) {
        ShortBuffer shortDepthBuffer = image.getPlanes()[0].getBuffer().asShortBuffer();
//        int[] mask = new int[WIDTH * HEIGHT];
//        int[] noiseReducedMask = new int[WIDTH * HEIGHT];
        int[][] matrix = {  {0,0,0,0,0,0,0,0,0,0},
                            {0,0,0,0,0,0,0,0,0,0},
                            {0,0,0,0,0,0,0,0,0,0},
                            {0,0,0,0,0,0,0,0,0,0},
                            {0,0,0,0,0,0,0,0,0,0},
                            {0,0,0,0,0,0,0,0,0,0},
                            {0,0,0,0,0,0,0,0,0,0},
                            {0,0,0,0,0,0,0,0,0,0},
                            {0,0,0,0,0,0,0,0,0,0},
                            {0,0,0,0,0,0,0,0,0,0}};

        long now = System.currentTimeMillis();
        int totalDepth = 0;
        int avgDepth = 0;
        int depthNum = 0;
//        int sampleVolume = 0;
//        int totalVolume = 0;
        int nearestDistance = Integer.MAX_VALUE;
        int nearestDistanceIndex = 0;

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = y * WIDTH + x;
                short depthSample = shortDepthBuffer.get(index);
                int depth = extractRange(depthSample, CONFIDENCE_FILTER);
                if(MainActivity.viewMode) {
                    rawMask[(HEIGHT - y - 1) * WIDTH + (WIDTH - x - 1)] = (int) normalizeRange(depth);
                }

//                if (x < (WIDTH-mResamplingIndexX[0]) && x > (WIDTH-mResamplingIndexX[1])) {
//                    if (y < (HEIGHT-mResamplingIndexY[0]) && y > (HEIGHT-mResamplingIndexY[1])) {
//                        if (depth != 0) {
//                            totalDepth += depth;
//                            depthNum++;
//                        }
//                    }
//                }

                if (x >= (WIDTH - AppConfig.TOF_FORK_LENGTH)) {
                    if ((y >= ((HEIGHT / 2) - (AppConfig.TOF_FORK_GAP + AppConfig.TOF_FORK_THICKNESS)) &&
                            y <= (HEIGHT / 2) - (AppConfig.TOF_FORK_GAP)) ||
                            (y <= ((HEIGHT / 2) + (AppConfig.TOF_FORK_GAP + AppConfig.TOF_FORK_THICKNESS)) &&
                                    y >= (HEIGHT / 2) + (AppConfig.TOF_FORK_GAP))) {
                        if (depth != 0) {
                            totalDepth += depth;
                            depthNum++;
                        }
                    }
                }


                if(y == (int)(HEIGHT/2)){
                    if(depth != 0 && depth < nearestDistance){
                        nearestDistance = depth;
                        nearestDistanceIndex = (WIDTH-x-1);
                    }
                }

//                if (mSamplingDistance != 0) {
//                    totalVolume++;
////                    if (depth > (mSamplingDistance - 150) && depth < (mSamplingDistance + 150)) {
//                    if(depth < mSamplingDistance){
//                        sampleVolume++;
//                        matrix[Math.floorDiv(x,mMatrixWidth)][Math.floorDiv(HEIGHT -y-1,mMatrixHeight)]++;
//                    }
//                }
            }
        }

        if (MainActivity.viewMode) {
            publishRawData();
        }

        avgDepth = getAverageDepth(totalDepth,depthNum);
        mSamplingDistance =  (int)(avgDepth/10)*10;

//        if(mSamplingDistance < (AppConfig.FORK_LENGTH-470) && mSamplingDistance > (AppConfig.FORK_LENGTH-530) && !currentStatus){
        if(mSamplingDistance < (AppConfig.FORK_LENGTH-250) && mSamplingDistance > (AppConfig.FORK_LENGTH-300) && !currentStatus){

//                double vol = ((sampleVolume*1.0f)/(totalVolume*1.0f))*100.0f;
            double vol = ((nearestDistanceIndex*1.0f)/(WIDTH*1.0f))*100.0f;
            if(vol > 90){
                vol = vol-15.0f;
            }else if(vol > 80){
                vol = vol-10.0f;
            }else if(vol> 70){
                vol = vol-5.0f;
            }
                //하단 20프로가 안보이는 경우******
//            sendCargoVolume((int)(20.0f+(80.0f*(vol/100.0f))), getMatrixData(matrix));
            //전체 매트릭스 추출
//            sendCargoVolume((int)vol-15, getMatrixData(matrix));
            //볼륨을 통해 매트릭스 추출
//            sendCargoVolume((int)vol-15, getMatrixDataFromVolume((int)vol-15));
            sendCargoVolume((int)vol, getMatrixDataFromVolume((int)vol));

        }




        if(mSamplingDistance < AppConfig.PICK_THRESHOLD && avgDepth != 0){
            if((now - currentChangeTime) > 1000) {
                currentStatus = true;
                sendCargoStatus(currentStatus);
            }

            if(!currentStatus){
                currentChangeTime = now;
            }
        }else if(mSamplingDistance > AppConfig.PICK_THRESHOLD || avgDepth == 0){
           if((now - currentChangeTime) > 1000) {
               currentStatus = false;
               sendCargoStatus(currentStatus);
           }

            if(currentStatus){
                currentChangeTime = now;
            }
        }

        if(MainActivity.viewMode){
//            double p = ((sampleVolume*1.0f)/(totalVolume*1.0f))*100.0f;
//            int printp = (int) (20.0f+(80.0f*(p/100.0f)));
            double p = ((nearestDistanceIndex*1.0f)/(WIDTH*1.0f))*100.0f;
            sendDepth(avgDepth + " / "+ (int)p + "%");
        }else {
            sendDepth(String.valueOf(avgDepth));
        }
    }

    private int extractRange(short sample, float confidenceFilter) {
        int depthRange = (short) (sample & 0x1FFF);
        int depthConfidence = (short) ((sample >> 13) & 0x7);
        float depthPercentage = depthConfidence == 0 ? 1.f : (depthConfidence - 1) / 7.f;
        if (depthPercentage > confidenceFilter) {
            return depthRange;
           // return normalizeRange(depthRange);
        } else {
            return 0;
        }
    }

    private int normalizeRange(int range) {
        float normalized = (float)range - RANGE_MIN;
        // Clamp to min/max
        normalized = Math.max(RANGE_MIN, normalized);
        normalized = Math.min(RANGE_MAX, normalized);
        // Normalize to 0 to 255
        normalized = normalized - RANGE_MIN;
        normalized = normalized / (RANGE_MAX - RANGE_MIN) * 255;
        return (int)normalized;
    }

    private Bitmap convertToRGBBitmap(int[] mask) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_4444);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = y * WIDTH + x;


                if(y < 120-AppConfig.TOF_FORK_GAP && y > 120-(AppConfig.TOF_FORK_GAP+AppConfig.TOF_FORK_THICKNESS)){
                    if(x == AppConfig.TOF_FORK_LENGTH ){
                        bitmap.setPixel(x, y, Color.argb(255, 255, 0, 0));
                        continue;
                    }
                }

                if(y > 120+AppConfig.TOF_FORK_GAP && y < 120+(AppConfig.TOF_FORK_GAP+AppConfig.TOF_FORK_THICKNESS)){
                    if(x == AppConfig.TOF_FORK_LENGTH ){;
                        bitmap.setPixel(x, y, Color.argb(255, 255, 0, 0));
                        continue;
                    }
                }


                if(x <= AppConfig.TOF_FORK_LENGTH){
                    if(y == 120+AppConfig.TOF_FORK_GAP || y == 120+(AppConfig.TOF_FORK_GAP+AppConfig.TOF_FORK_THICKNESS) ||
                            y == 120-AppConfig.TOF_FORK_GAP || y == 120-(AppConfig.TOF_FORK_GAP+AppConfig.TOF_FORK_THICKNESS)){
                        bitmap.setPixel(x, y, Color.argb(255, 255, 0, 0));
                        continue;
                    }
                }


                if(y <= 120+(AppConfig.TOF_RESAMPLING_VOLUME_WIDTH/2) && y >= 120-(AppConfig.TOF_RESAMPLING_VOLUME_WIDTH/2)){
                    if(x == AppConfig.TOF_RESAMPLING_VOLUME_HEIGHT ){
                        bitmap.setPixel(x, y, Color.argb(255, 0, 255, 0));
                        continue;
                    }
                }

                if(x <= AppConfig.TOF_RESAMPLING_VOLUME_HEIGHT){
                    if(y == 120+(AppConfig.TOF_RESAMPLING_VOLUME_WIDTH/2) || y == 120-(AppConfig.TOF_RESAMPLING_VOLUME_WIDTH/2) ){
                        bitmap.setPixel(x, y, Color.argb(255, 0, 255, 0));
                        continue;
                    }
                }


//
//                if(x > AppConfig.TOF_RESAMPLING_WIDTH_MIN && x <AppConfig.TOF_RESAMPLING_WIDTH_MAX){
//                    if(y == AppConfig.TOF_RESAMPLING_HEIGHT_MIN || y == AppConfig.TOF_RESAMPLING_HEIGHT_MAX){
//                        bitmap.setPixel(x, y, Color.argb(255, 255, 0, 0));
//                        continue;
//                    }
//                }
//
//                if(y > AppConfig.TOF_RESAMPLING_HEIGHT_MIN  && y <AppConfig.TOF_RESAMPLING_HEIGHT_MAX){
//                    if(x == AppConfig.TOF_RESAMPLING_WIDTH_MIN || x == AppConfig.TOF_RESAMPLING_WIDTH_MAX){
//                        bitmap.setPixel(x, y, Color.argb(255, 255, 0, 0));
//                        continue;
//                    }
//                }


                if(mask[index] == 0){
                    bitmap.setPixel(x, y, Color.argb(255, 0 ,0, 0));
                }else{
                    bitmap.setPixel(x, y, Color.argb(255, 255-mask[index] , 255-mask[index], 255-mask[index]));
                }


            }
        }
        return bitmap;
    }

    private int getAverageDepth(int total, int num){
        float avg = (total*1.0f) / (num * 1.0f);
        return (int) avg;
    }


    public int[] getMatrixData(int[][] data){
        int[] result = {0,0,0,0,0,0,0,0,0,0};
        for(int w = 0; w < 10 ; w++){
            for(int h =0; h < 10 ; h++){
                if(data[w][h] > ((mMatrixHeight*mMatrixWidth)/2)){
                    result[h]++;
                }
            }
        }
        return result;
    }

    public int[] getMatrixDataFromVolume(int v){
        int[] result = new int[10];
        Arrays.fill(result,Math.round((float) v/10));
        return result;
    }



}