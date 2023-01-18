package com.example.androidLIS;

import static com.budiyev.android.codescanner.ScanMode.*;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.example.androidLIS.model.ActionInfo;
import com.example.androidLIS.model.ActionInfoReqData;
import com.example.androidLIS.model.Alive;
import com.example.androidLIS.model.AliveReqData;
import com.example.androidLIS.model.CargoData;
import com.example.androidLIS.model.ResponseData;
import com.example.androidLIS.network.RetrofitClient;
import com.example.androidLIS.network.RetrofitService;
import com.example.androidLIS.permission.PermissionHelper;
import com.example.androidLIS.service.BluetoothService;
import com.example.androidLIS.tof.Camera;
import com.example.androidLIS.tof.DepthFrameAvailableListener;
import com.example.androidLIS.tof.DepthFrameVisualizer;
import com.example.androidLIS.model.PositionData;
import com.example.androidLIS.util.AppConfig;
import com.example.androidLIS.util.AppUtil;
import com.google.zxing.Result;
import com.orhanobut.hawk.Hawk;
import com.terabee.sdk.TerabeeSdk;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;
import timber.log.Timber;


/*  This is an example of getting and processing ToF data.

    This example will only work (correctly) on a device with a front-facing depth camera
    with output in DEPTH16. The constants can be adjusted but are made assuming this
    is being run on a Samsung S10 5G device.
 */
public class MainActivity extends AppCompatActivity implements DepthFrameVisualizer {

    private TerabeeSdk.DeviceType mCurrentType = TerabeeSdk.DeviceType.AUTO_DETECT;
    private PermissionHelper permissionHelper;


    public static final int CAM_PERMISSIONS_REQUEST = 0;
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private long backKeyTime = 0;

    //TOF 카메라
    private TextureView rawDataView;
    private Matrix defaultBitmapTransform;
    private Camera camera;


    //QR코드 스캐너
    private CodeScanner mCodeScanner;
    CodeScannerView mCodeScannerView;


    //RFID 스캐너 BLE 패킷 정보
    public ArrayList<byte[]> mBLEpacket;
    //RFID 스캐너 스캔 스레드
    public Thread mBleThread;
    //현재 스캔 유무
    public boolean mBleScan = false;


    public boolean mBleRFSet = false;
    public String mRFSetLog = "";
    public byte[] mRFSetArray = new byte[24];
    public int mRFSetCnt = 0;

    //스캐너 connect 요청 횟수
    public int mBleConnFailCnt = 0;
    public boolean mBleConnReq = false;




    //텍스트 뷰어
    TextView mCargoDepth;
    TextView mQrText;
    TextView mDisText;
    TextView mCargoFloor;
    TextView mCargoAddress;

    public TextView mRFsetLog;

    //지게차 상태 뷰어
    LinearLayout mCargoExpect;



    /**
     * mCargoStatus
     * 0 : 짐 없음 (QR 지속 스캔)
     * 1 : 짐 실음
     * 2 : 최근에 짐을 내림 (QR스캔 후 0)
     */
    public int mCargoStatus = 0;

    /**
     * 현재 지게차 위치
     * mCurrentPosition : 위치
     */
    public PositionData mCurrentPosition;
    public ArrayList<String> mBLEQueue;



    /**
     * 현재 실은 짐의 이름
     */
    public CargoData mCargo;



    /**
     * 최근에 내린 짐의 정보
     */
    public String mCurrentLoadCargo;//최근 내린 짐
    public int mLoadInCargoVolume = 0;


    /**
     * 최근 보인 짐의 부피
     */
    public int mCurrentCargoVolume = 0;
    public int[] mCurrentCargoVolumeMatrix;


    /**
     * 최근 이벤트 발생 시간
     */
    public long mCurrentLoadOut= 0;//최근 내린 시간
    public long mCurrentLoadIn = 0; // 최근 실은 시간



    /**
     * Viewmode
     * false : QR
     * true : TOF
     */
    public static boolean viewMode = false;

    //로그 다이얼로그 텍스트
    public String mLogText = "";



    //Alive 스레드
    public Thread mAliveThread;
    //백엔드 alive
    public boolean mAlive = false;

    //백엔드 alive 패킷 전송 실패 카운트
    public int mAliveFailCnt = 0;


    //백엔드 통신
    private RetrofitService mService;


    //브로드캐스트 리시버
    private NotificationReceiver mNotificationReceiver;








    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Hawk
        Hawk.init(this).build();

        /**
         * 권한 체크
         */
        onCheckPermission();
//        checkCamPermissions();
        rawDataView = findViewById(R.id.rawData);
        mCargoDepth = (TextView) findViewById(R.id.CargoDepth);
        mCargoExpect = (LinearLayout) findViewById(R.id.CargoExpect);
        mQrText = (TextView) findViewById(R.id.qrText);
        mDisText = (TextView) findViewById(R.id.distanceSensorText);
        mCargoFloor = (TextView) findViewById(R.id.cargoFloor);
        mCargoAddress = (TextView) findViewById(R.id.cargoAddress);
        mBLEpacket = new ArrayList<byte[]>();
        mCurrentPosition = new PositionData(1,"field",0);
        mBLEQueue = new ArrayList<String>();
        mCargo = new CargoData("cargo",0);
        mCodeScannerView = (CodeScannerView) findViewById(R.id.scanner_view);
        mCodeScannerView.setClickable(false);


        initSettingParams();

        if(!AppUtil.getInstance().isNetworkConnected(getApplicationContext())){
            Log.d("network","not connected");
        }else{
            Log.d("network","connected");
        }


        if(android.os.Build.VERSION.SDK_INT >= 18)
            startService(new Intent(MainActivity.this, BluetoothService.class));
        this.mNotificationReceiver = new NotificationReceiver();
        this.registerReceiver(mNotificationReceiver, new IntentFilter(BluetoothService.BLE_ACTION_RECEIVE_DATA));
        this.registerReceiver(mNotificationReceiver, new IntentFilter(BluetoothService.BLE_ACTION_GATT_CONNECTED));
        this.registerReceiver(mNotificationReceiver, new IntentFilter(BluetoothService.BLE_ACTION_GATT_DISCONNECTED));
        this.registerReceiver(mNotificationReceiver, new IntentFilter(BluetoothService.BLE_ACTION_GATT_MTU_CALLBACK));

        /**
         * 백엔드 통신 설정
         */
        mService = RetrofitClient.getHeavyClient().create(RetrofitService.class);
        platformSendAliveMessage();

        /**
         * 거리센서 SDK 초기화
         */
        TerabeeSdk.getInstance().init(this);
        TerabeeSdk.getInstance().registerDataReceive(mDataDistanceCallback);


        connectToDevice();
        /**
         * QR스캔 시작
         */
        startScanning();

        /**
         * TOF 카메라 시작
         */
        camera = new Camera(this, this);
        camera.openFrontDepthCamera();

    }


    public void isconn(boolean is){
        if(is){
            runOnUiThread(new Runnable() {
                public void run() {
                    viewShortToast("sensor connected");
                }
            });
        }else{
            runOnUiThread(new Runnable() {
                public void run() {
                    viewShortToast("sensor disconnected");
                }
            });        }

    }

    /**
     * 거리센서 콜백함수
     */
    private final TerabeeSdk.DataDistanceCallback mDataDistanceCallback = new
            TerabeeSdk.DataDistanceCallback() {
                @Override
                public void onDistanceReceived(int distance, int dataBandwidth, int
                        dataSpeed) {
                    setDistext(distance+"");
                    setCurrentFloor(distance);

                    if(distance <= AppConfig.FLOOR_HEIGHT){ //1층
                        setCargoFloor("1층");
                    }else if(distance > AppConfig.FLOOR_HEIGHT && distance <= (AppConfig.FLOOR_HEIGHT*2)){ //2층
                        setCargoFloor("2층");
                    }else if(distance > (AppConfig.FLOOR_HEIGHT*2) && distance <= (AppConfig.FLOOR_HEIGHT*3)){ //3층
                        setCargoFloor("3층");
                    }else if(distance > (AppConfig.FLOOR_HEIGHT*3) && distance <= (AppConfig.FLOOR_HEIGHT*4)) { //4층
                        setCargoFloor("4층");
                    }else{ //그 외
                        setCargoFloor("1층");
                    }


                    // received distance from the sensor
                }

                @Override
                public void onReceivedData(byte[] bytes, int i, int i1) {
                    // received raw data from the sensor
                    Log.d("TerabeeLog",AppUtil.getInstance().byteArrayToHex(bytes));
                }
            };


    private void connectToDevice() {
        Thread connectThread = new Thread(() -> {
            try {
                TerabeeSdk.getInstance().connect(new TerabeeSdk.IUsbConnect() {
                    @Override
                    public void connected(boolean success, TerabeeSdk.DeviceType
                            deviceType) {
                        isconn(success);
                        Log.d("Terabee", success + "");
                    }

                    @Override
                    public void disconnected() {

                    }

                    @Override
                    public void permission(boolean granted) {

                    }
//                }, TerabeeSdk.DeviceType.EVO_60M);
                }, mCurrentType);
            } catch (Exception e) {
                Log.e("Terabee", e.getMessage());
            }
        });

        connectThread.start();
    }


    private void disconnectDevice() {
        try {
            TerabeeSdk.getInstance().disconnect();
        } catch (Exception e) {
            Log.e("Terabee", e.getMessage());
        }
    }

    private void clearDataReceivers() {
        TerabeeSdk.getInstance().unregisterDataReceive(mDataDistanceCallback);
    }



    private void checkCamPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAM_PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onRawDataAvailable(Bitmap bitmap) {
        renderBitmapToTextureView(bitmap, rawDataView);
    }

    @Override
    public void onNoiseReductionAvailable(Bitmap bitmap) {
//        renderBitmapToTextureView(bitmap, noiseReductionView);
    }

    @Override
    public void onMovingAverageAvailable(Bitmap bitmap) {
//        renderBitmapToTextureView(bitmap, movingAverageView);
    }

    @Override
    public void onBlurredMovingAverageAvailable(Bitmap bitmap) {
//        renderBitmapToTextureView(bitmap, blurredAverageView);
    }

    /**
     * 지게차 포크의 짐 유무 판단
     * @param status
     */
    @Override
    public void onCargoExpect(boolean status) {
        String text = "";
        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String formatTime = dateFormat.format(now);
        switch (getCargoStatus()){
            case 0://짐이 없는 상황
                if(status){//짐이 없었는데 생김
                    //짐의 이름 가져오기
                    if(now - mCargo.getTime() < 5000){
                        mCurrentLoadCargo = mCargo.getName();
                        setQRtext(mCurrentLoadCargo);
                    }else{
                        mCurrentLoadCargo = "cargo";
                        setQRtext(mCurrentLoadCargo);
                    }
                    this.mLoadInCargoVolume = mCurrentCargoVolume;
                    this.mCurrentLoadIn = now;
                    this.mCargoStatus = 1;
                }
                break;

            case 1://짐이 있던 상황
                if ((now - mCurrentLoadIn) > 5000 && (now - mCurrentPosition.time) < 5000 && mCurrentLoadIn != 0) {
                    this.mCurrentLoadIn = 0;
                    //선반에 있는 짐을 실은 상황
                    /**
                     * 짐의 이름 : cargoName
                     * 현재 위치 : mCurrentPosition.address
                     * 현재 높이 : mCurrentPosition.floor
                     * 현재 짐의 부피 : mCurrentCargoVolume
                     */
                    text = "( IN )\nadr:" + mCurrentPosition.address + "\nName :" + mCurrentLoadCargo + "\nHeight:" + mCurrentPosition.floor + "\nvolume:" + mLoadInCargoVolume;
                    viewShortToast(text);
                    mLogText = mLogText + "\n[" + formatTime + "]\n" + text + "\n";
                    int[] defaultMatrix ={0,0,0,0,0,0,0,0,0,0};

                    //백엔드 연동 추가
                    platformActionInfoMessage(new ActionInfoReqData(new ActionInfo(AppConfig.WORK_LOCATION_ID
                            , AppConfig.GET_IN
                            , AppConfig.VEHICLE_ID
                            , mCurrentPosition.address
                            , String.valueOf(mCurrentPosition.floor)
                            , mCurrentLoadCargo
                            , "0"
                            , String.valueOf(AppConfig.MATRIX_X)
                            , String.valueOf(AppConfig.MATRIX_Y)
                            , defaultMatrix)));
                    setCargoAddress(mCurrentPosition.address);
                } else if (mCurrentLoadIn != 0 && (now - mCurrentLoadIn) > 4000) {//선반이 아닌 곳의 짐을 실음
                    int[] defaultMatrix ={0,0,0,0,0,0,0,0,0,0};
                    this.mCurrentLoadIn = 0;
                    setCargoAddress("field");
                    text = "( IN )\nadr:" + "field" + "\nName :" + mCurrentLoadCargo + "\nHeight:" + "1" + "\nvolume:" + mLoadInCargoVolume;
                    viewShortToast(text);
                    mLogText = mLogText + "\n[" + formatTime + "]\n" + text + "\n";
                    //백엔드 연동 추가
                    platformActionInfoMessage(new ActionInfoReqData(new ActionInfo(AppConfig.WORK_LOCATION_ID
                            , AppConfig.GET_IN
                            , AppConfig.VEHICLE_ID
                            , "field"
                            , "1"
                            , mCurrentLoadCargo
                            , String.valueOf(mLoadInCargoVolume)
                            , String.valueOf(AppConfig.MATRIX_X)
                            , String.valueOf(AppConfig.MATRIX_Y)
                            , defaultMatrix)));
                }


                if (!status) {
                    if (mCurrentLoadIn == 0) {//짐이 있다가 없어진 상황
                        this.mCurrentLoadOut = now;
                        this.mCargoStatus = 2;
                    } else if (mCurrentLoadIn != 0) {
                        this.mCurrentLoadIn = 0;
                        this.mCargoStatus = 0;
                    }
                }

                break;

            case 2://최근에 짐을 내린 상황
                if ((now - mCurrentPosition.time) < 5000) {
                    //내린 선반 입력''''
                    text = "( OUT )\nadr:" + mCurrentPosition.address + "\nName :" + mCurrentLoadCargo + "\nHeight:" + mCurrentPosition.floor + "\nvolume:" + mLoadInCargoVolume;
                    viewShortToast(text);
                    mLogText = mLogText + "\n[" + formatTime + "]\n" + text + "\n";
                    //백엔드 연동 추가
                    platformActionInfoMessage(new ActionInfoReqData(new ActionInfo(AppConfig.WORK_LOCATION_ID
                            , AppConfig.GET_OUT
                            , AppConfig.VEHICLE_ID
                            , mCurrentPosition.address
                            , String.valueOf(mCurrentPosition.floor)
                            , mCargo.getName()
                            , String.valueOf(mLoadInCargoVolume)
                            , String.valueOf(AppConfig.MATRIX_X)
                            , String.valueOf(AppConfig.MATRIX_Y)
                            , mCurrentCargoVolumeMatrix)));

                    mLoadInCargoVolume = 0;
                    this.mCargoStatus = 0;
                    mCurrentLoadCargo = "cargo";
                } else if ((now - mCurrentLoadOut) > 2000) {
                    //선반이 아닌곳에 내림
//                    mCurrentPosition = new PositionData(1,"field",now);
                    setCargoAddress("field");
                    text = "( OUT )\nadr:" + "field"+ "\nName :" + mCurrentLoadCargo + "\nHeight:" + "1" + "\nvolume:" + mLoadInCargoVolume;
                    viewShortToast(text);
                    mLogText = mLogText + "\n[" + formatTime + "]\n" + text + "\n";


                    //백엔드 연동 추가
                    platformActionInfoMessage(new ActionInfoReqData(new ActionInfo(AppConfig.WORK_LOCATION_ID
                            , AppConfig.GET_OUT
                            , AppConfig.VEHICLE_ID
                            , "field"
                            , "1"
                            , mCargo.getName()
                            , String.valueOf(mLoadInCargoVolume)
                            , String.valueOf(AppConfig.MATRIX_X)
                            , String.valueOf(AppConfig.MATRIX_Y)
                            , mCurrentCargoVolumeMatrix)));
                    mLoadInCargoVolume = 0;
                    this.mCargoStatus = 0;
                    mCurrentLoadCargo = "cargo";
                }else if(status){
                    this.mCargoStatus = 1;
                }
                break;
        }

        //현재 포크의 짐 상태에 따른 색변경
        if(status) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mCargoExpect.setBackgroundColor(Color.parseColor("#993300"));
                }
            });
        }else{
            runOnUiThread(new Runnable() {
                public void run() {
                    mCargoExpect.setBackgroundColor(Color.parseColor("#339900"));
                }
            });
        }
    }

    @Override
    public void onSampleDepth(String depth) {
        final String textview = depth;
        runOnUiThread(new Runnable() {
            public void run() {
                mCargoDepth.setText(textview);
            }
        });

    }

    @Override
    public void onCargoVolume(int volume, int[] matrix) {
        if(mCargoStatus == 0) {
            mCurrentCargoVolume = volume;
            mCurrentCargoVolumeMatrix = matrix;
            Log.e("matrix10x1", Arrays.toString(mCurrentCargoVolumeMatrix));
        }

    }


    /* We don't want a direct camera preview since we want to get the frames of data directly
        from the camera and process.

        This takes a converted bitmap and renders it onto the surface, with a basic rotation
        applied.
     */
    private void renderBitmapToTextureView(Bitmap bitmap, TextureView textureView) {
        Canvas canvas = textureView.lockCanvas();
        canvas.drawBitmap(bitmap, defaultBitmapTransform(textureView), null);
        textureView.unlockCanvasAndPost(canvas);
    }

    private Matrix defaultBitmapTransform(TextureView view) {
        if (defaultBitmapTransform == null || view.getWidth() == 0 || view.getHeight() == 0) {
            Matrix matrix = new Matrix();
            int centerX = view.getWidth() / 2;
            int centerY = view.getHeight() / 2;

            int bufferWidth = DepthFrameAvailableListener.WIDTH;
            int bufferHeight = DepthFrameAvailableListener.HEIGHT;

            RectF bufferRect = new RectF(0, 0, bufferWidth, bufferHeight);
            RectF viewRect = new RectF(0, 0, view.getWidth(), view.getHeight());
            matrix.setRectToRect(bufferRect, viewRect, Matrix.ScaleToFit.CENTER);
            matrix.postRotate(270, centerX, centerY);

            defaultBitmapTransform = matrix;
        }
        return defaultBitmapTransform;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
    }


    /**
     * QR 스캔 start
     */
    private void startScanning() {
        if(mCodeScanner == null) {
            final CodeScannerView scannerView = findViewById(R.id.scanner_view);

            mCodeScanner = new CodeScanner(this,scannerView,0);
//            mCodeScanner.setView(scannerView);
            mCodeScanner.setScanMode(CONTINUOUS);
            mCodeScanner.setAutoFocusEnabled(true);
//            mCodeScanner.setTouchFocusEnabled(true);
            mCodeScanner.setAutoFocusInterval(500);
            mCodeScanner.setZoom(0);
//            mCodeScanner.setCamera(-1);
            Log.d("scanner", "set");
            mCodeScanner.setDecodeCallback(new DecodeCallback() {
                @Override
                public void onDecoded(@NonNull final Result result) {
                    if(mCargoStatus == 0) {
                        mCargo.setName(result.getText());
                        mCargo.setTime(System.currentTimeMillis());
                        Log.d("scanner", result.getText());
                        setQRtext(result.getText());
                    }

/**QR코드 층 구분 로직 --> 160cm 이하의 선반 크기만 가능함**/
//                    int height = scannerView.getHeight();
//                    String[] data = result.getText().split(":");
//                    if(data[0].contains("wata")){
//                        long now = System.currentTimeMillis();
//                        if(result.getResultPoints()[0].getY() > ((height*1.0f)/2.0f)){
//                            switch (Integer.parseInt(data[1])){
//                                case 1:
//                                    setCurrentPosition(new PositionData(2,data[2],now));
//                                    setQRtext(data[2]+" "+"2층");
//                                    break;
//                                case 2:
//                                    setCurrentPosition(new PositionData(3,data[2],now));
//                                    setQRtext(data[2]+" "+"3층");
//                                    break;
//                                case 3:
//                                    setCurrentPosition(new PositionData(4,data[2],now));
//                                    setQRtext(data[2]+" "+"4층");
//                                    break;
//                            }
//                        }else if(result.getResultPoints()[2].getY() < ((height*1.0f)/2.0f)){
//                            switch (Integer.parseInt(data[1])){
//                                case 1:
//                                    setCurrentPosition(new PositionData(1,data[2],now));
//                                    setQRtext(data[2]+" "+"1층");
//                                    break;
//                                case 2:
//                                    setCurrentPosition(new PositionData(2,data[2],now));
//                                    setQRtext(data[2]+" "+"2층");
//                                    break;
//                                case 3:
//                                    setCurrentPosition(new PositionData(3,data[2],now));
//                                    setQRtext(data[2]+" "+"3층");
//                                    break;
//                            }
//
//                        }else{
//                            if((result.getResultPoints()[0].getY() - ((height*1.0f)/2.0f)) > (result.getResultPoints()[2].getY() - ((height*1.0f)/2.0f))){
//                                switch (Integer.parseInt(data[1])){
//                                    case 1:
//                                        setCurrentPosition(new PositionData(1,data[2],now));
//                                        setQRtext(data[2]+" "+"1층");
//                                        break;
//                                    case 2:
//                                        setCurrentPosition(new PositionData(2,data[2],now));
//                                        setQRtext(data[2]+" "+"2층");
//                                        break;
//                                    case 3:
//                                        setCurrentPosition(new PositionData(3,data[2],now));
//                                        setQRtext(data[2]+" "+"3층");
//                                        break;
//                                }
//                            }else{
//                                switch (Integer.parseInt(data[1])){
//                                    case 1:
//                                        setCurrentPosition(new PositionData(2,data[2],now));
//                                        setQRtext(data[2]+" "+"2층");
//                                        break;
//                                    case 2:
//                                        setCurrentPosition(new PositionData(3,data[2],now));
//                                        setQRtext(data[2]+" "+"3층");
//                                        break;
//                                    case 3:
//                                        setCurrentPosition(new PositionData(4,data[2],now));
//                                        setQRtext(data[2]+" "+"4층");
//                                        break;
//                                }
//                            }
//                        }
//                    }


                }
            });
//            scannerView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    mCodeScanner.startPreview();
//                    Log.d("qr","priview");
//
//                }
//            });
        }else{
            Log.d("scanner", "alreadyset");
        }
    }

    //QR 스캔 데이터 텍스트 뷰
    public void setQRtext(String text){
        final String textview = text;
        runOnUiThread(new Runnable() {
            public void run() {
                mQrText.setText(textview);
            }
        });
    }

    // TOF 거리 텍스트 뷰
    public void setDistext(String text){
        final String textview = text;
        runOnUiThread(new Runnable() {
            public void run() {
                mDisText.setText(textview);
            }
        });
    }

    //현재 짐 상태
    public int getCargoStatus() {
        return mCargoStatus;
    }

    public PositionData getCurrentPosition() {
        return mCurrentPosition;
    }

//    public void setCurrentPosition(PositionData mCurrentPosition) {
//        this.mCurrentPosition = mCurrentPosition;
//    }

    //현재 RFID 스캔 위치
    public void setCurrentAddress(String adr){
        this.mCurrentPosition.setAddress(adr);
        this.mCurrentPosition.setTime(System.currentTimeMillis());
    }

    //현재 거리센서 높이
    public void setCurrentFloor(int f){
        this.mCurrentPosition.setFloor(f);
    }

    //짐 높이 텍스트뷰
    public void setCargoFloor(String s){
        runOnUiThread(new Runnable() {
            public void run() {
                mCargoFloor.setText(s);
            }
        });
    }

    //짐 위치 텍스트뷰
    public void setCargoAddress(String s){
        runOnUiThread(new Runnable() {
            public void run() {
                mCargoAddress.setText(s);
            }
        });
    }


    /**
     * 뷰모드
     * TOF <-> QR
     * @param v
     */
    public void setViewMode(View v){
        Button viewChanger = (Button) findViewById(R.id.viewChanger);
        if(!viewMode){
            rawDataView.setVisibility(View.VISIBLE);
            mCodeScannerView.setVisibility(View.INVISIBLE);
            viewChanger.setText("QR");
            viewMode = true;
        }else{
            rawDataView.setVisibility(View.INVISIBLE);
            mCodeScannerView.setVisibility(View.VISIBLE);
            viewChanger.setText("TOF");
            viewMode = false;
        }

    }

    /**
     * 로그 데이터 뷰어
     * @param v
     */
    public void callLogViewer(View v) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_log_message, null);
        final TextView logtext = dialogView.findViewById(R.id.logMessageText);
        final TextView btn = dialogView.findViewById(R.id.btn_ok);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        logtext.setText(mLogText);


        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

    }

    /**
     * BLE 연결 관련
     */



    public void connBLE(){
        if(!mBleScan) {
            Log.e("connBle" , "start");
            mBleConnReq = true;
            Intent _intent = new Intent(BluetoothService.BLE_ACTION_CONNECT);
            _intent.putExtra(BluetoothService.DEVICE_ADDRESS, AppConfig.RFID_MAC);
            sendBroadcast(_intent);
        }
    }

    public void setRFTag(View v){
        mBleRFSet = true;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rfid_setting, null);
        final TextView rflogtext = dialogView.findViewById(R.id.RFLogText);
        final EditText inputtext = dialogView.findViewById(R.id.inputText);
        final TextView btn_ok = dialogView.findViewById(R.id.btn_ok);
        final TextView btn_set = dialogView.findViewById(R.id.btn_set);
        final TextView btn_check = dialogView.findViewById(R.id.btn_check);
        final TextView inputtextSize = dialogView.findViewById(R.id.inputTextSize);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        inputtext.setText("000000000000000000000000");

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
        inputtext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputtextSize.setText(""+inputtext.getText().toString().length());

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputtext.getText().toString().length() == 24) {
                    requestWrite(inputtext.getText().toString());
                }else{
                    viewShortToast("글자수를 24자로 맞춰주세요");
                }


            }
        });

        btn_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestScan();
                rflogtext.setText(mRFSetLog);
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBleRFSet = false;
                alertDialog.dismiss();
            }
        });

    }

    //스캐너 스캔 시작
    public void startScanRFID(){
        mBleScan = true;
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        if (mBleThread != null) mBleThread.interrupt();
        mBleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mBleScan) {
                    if(!mBleRFSet) {
                        requestScan();
                        try {
                            Thread.sleep(AppConfig.RFID_SCAN_INTERVAL);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    }
                }

            }

        });

        mBleThread.start();
    }

    //스캔 요청
    public void requestScan(){
//        byte[] bytearray = {(byte) 0x0A,
//                (byte) 0x55,
//                (byte) 0x0D
//        };
        byte[] bytearray = {(byte) 0x0A,
                (byte) 0x52,
                (byte) 0x31,
                (byte) 0x2C,
                (byte) 0x31,
                (byte) 0x2C,
                (byte) 0x39,
                (byte) 0x0D
        };
        Log.d("BLE_TX_DATA", AppUtil.getInstance().byteArrayToHex(bytearray));
        Intent _intent = new Intent(BluetoothService.BLE_ACTION_SEND_DATA);
        _intent.putExtra(BluetoothService.BYTES_DATA, bytearray);
        sendBroadcast(_intent);
    }

    //스캔 요청
    public void requestWrite(String str) {
        if(str != null) {
            mRFSetArray = str.getBytes();
            mRFSetCnt = 0;
        }

                ;
        byte[] bytearray = {(byte) 0x0A,
                (byte) 0x57,
                (byte) 0x31,
                (byte) 0x2C,

                (byte) ((2+(mRFSetCnt*2))|0x30),
                (byte) 0x2C,

                (byte) 0x32,
                (byte) 0x2C,

                mRFSetArray[(mRFSetCnt*8)],
                mRFSetArray[(mRFSetCnt*8)+1],
                mRFSetArray[(mRFSetCnt*8)+2],
                mRFSetArray[(mRFSetCnt*8)+3],

                mRFSetArray[(mRFSetCnt*8)+4],
                mRFSetArray[(mRFSetCnt*8)+5],
                mRFSetArray[(mRFSetCnt*8)+6],
                mRFSetArray[(mRFSetCnt*8)+7],

                (byte) 0x0D
        };
        Log.d("BLE_TX_DATA", AppUtil.getInstance().byteArrayToHex(bytearray));
        Intent _intent = new Intent(BluetoothService.BLE_ACTION_SEND_DATA);
        _intent.putExtra(BluetoothService.BYTES_DATA, bytearray);
        sendBroadcast(_intent);
        if(mRFSetCnt < 3){
            mRFSetCnt++;
        }


    }




    /**
     * 브로드 캐스트 리시버
     * BLE 스캐너 데이터 리시버
     * */
    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] _bData;

            switch (intent.getAction()) {
                case BluetoothService.BLE_ACTION_RECEIVE_DATA:
                    _bData = intent.getExtras().getByteArray(BluetoothService.BYTES_DATA);
                    Log.d("BLE_RX_DATA", AppUtil.getInstance().byteArrayToHex(_bData));
                    if(mBleRFSet){
                        if(_bData[1] == 0x57 && mRFSetCnt < 3){
                            requestWrite(null);
                        }else if(_bData[1] == 0x58){
                            mRFSetCnt = 0;
                        }
                    }



                    if (mBLEpacket.size() == 0) {
                        if(_bData.length >= 20){
                            int len = _bData.length;
                            if (_bData[0] == 0x0a) {
                                byte[] _data = Arrays.copyOfRange(_bData, 6, len);
                                mBLEpacket.add(_data);
                            }
                        }else{
                            if(mBLEQueue.size() < 20){
                                mBLEQueue.add(null);
                            }else{
                                mBLEQueue.remove(0);
                                mBLEQueue.add(null);
                            }
                        }
                    } else if (mBLEpacket.size() == 1 ) {
                        if( _bData.length >= 20){
                            int len = _bData.length;
                            if (_bData[len - 1] == 0x0a) {
                                byte[] _data = Arrays.copyOfRange(_bData, 0, len - 10);
                                mBLEpacket.add(_data);
                                String adrressID = new String(mBLEpacket.get(0)) + new String(mBLEpacket.get(1));
                                Log.d("BLE_DATA_ID", adrressID);
                                if(mBleRFSet){
                                    mRFSetLog = adrressID;
                                }

                                if(mBLEQueue.size() < 20){
                                    mBLEQueue.add(adrressID);
                                }else{
                                    mBLEQueue.remove(0);
                                    mBLEQueue.add(adrressID);
                                }
                                mBLEpacket.clear();
                            }
                        }

                    }



                    if(mBLEQueue.size() > 0) {
                        String loca = guessLocation(mBLEQueue);
                        if(loca!= null) {
                            setCurrentAddress(loca);
                        }
                    }


//                    if(mBLEQueue.size() == 20){
//                        for(int i = 0 ; i < 20 ; i++){
//                            if(mBLEQueue.get(i)!= null) {
//                                Log.e("qdata", mBLEQueue.get(i));
//                            }else{
//                                Log.e("qdata", "null");
//                            }
//                        }
//                        Log.e("qdata", "==================================");
//
//                        mBLEQueue.clear();
//                    }
                    break;

                case BluetoothService.BLE_ACTION_GATT_CONNECTED:
                    String connectdata = intent.getExtras().getString(BluetoothService.STRING_DATA);
                    Log.d("BLE_CONNECT_RESULT", connectdata);
                    startScanRFID();
                    mBleConnFailCnt = 0;
                    mBleConnReq = false;
                    break;


                case BluetoothService.BLE_ACTION_GATT_DISCONNECTED:
                    String disconnectdata = intent.getExtras().getString(BluetoothService.STRING_DATA);
                    Log.d("BLE_DISCONNECT_RESULT", disconnectdata);

                    if(mBleConnFailCnt < 3 && mBleConnReq){
                        mBleConnFailCnt++;
                        Intent _intent = new Intent(BluetoothService.BLE_ACTION_CONNECT);
                        _intent.putExtra(BluetoothService.DEVICE_ADDRESS, "34:86:5D:71:98:7A");
                        sendBroadcast(_intent);
                    }else if(mBleConnReq){
                        mBleConnReq = false;
                        mBleConnFailCnt = 0;
                        Log.d("BLE_DISCONNECT_FAIL", disconnectdata);
                    }

                    break;
                case BluetoothService.BLE_ACTION_GATT_MTU_CALLBACK:
                    String mtudata = intent.getExtras().getString(BluetoothService.STRING_DATA);
                    Log.d("RX_MTU_DATA", mtudata);
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * RFID 스캔 데이터를 기반으로 현재 선반 위치 추측
     * @param list
     * @return
     */

    public String guessLocation(ArrayList<String> list){
        ArrayList<String> dataLocation = new ArrayList<String>();
        ArrayList<Integer> dataCount = new ArrayList<Integer>();

        for(int i = 0 ; i < list.size() ; i++){
            if(list.get(i) != null){
                int index = dataLocation.indexOf(list.get(i));
                if(index != -1){
                    dataCount.set(index,dataCount.get(index)+1);
                }else{
                    dataLocation.add(list.get(i));
                    dataCount.add(1);
                }
            }
        }

        int max = 0;
        int maxIndex = 0;
        for(int i =0 ; i < dataCount.size() ; i++){
            if(dataCount.get(i) > max){
                max = dataCount.get(i);
                maxIndex = i;
            }
        }
//        if(dataLocation.size() > 0) {
//            Log.e("qdata_count", dataCount.get(maxIndex) + "");
//        }

        if(dataLocation.size() > 0 && dataCount.get(maxIndex) >= AppConfig.RFID_SCAN_CNT_THRESHOLD){
            setCargoAddress(dataCount.get(maxIndex) +"/ "+ dataLocation.get(maxIndex));
            return dataLocation.get(maxIndex);
        }else{
            return null;
        }

    }

    /**
     * 백엔드 연동 테스트
     * Alive 패킷 전송
     * @param data
     */
    public void platformAliveMessage(AliveReqData data){
        ResponseData res;
        mService.ALIVE_MESSAGE(data).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Response<ResponseData>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }
                    @Override
                    public void onSuccess(Response<ResponseData>response) {
                        ResponseData res = response.body();
                        if(res != null) {
                            mAliveFailCnt = 0;
                            connBLE();
                            Log.d("resdata", res.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if(mAliveFailCnt > 3){
                            e.printStackTrace();
                        }else {
                            mAliveFailCnt++;
                        }
                    }
                });
    }


    /**
     * 백엔드 연동
     * Action 패킷 전송
     * @param data
     */
    public void platformActionInfoMessage(ActionInfoReqData data){
        ResponseData res;
        mService.ACTION_INFO_MESSAGE(data).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Response<ResponseData>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }
                    @Override
                    public void onSuccess(Response<ResponseData>response) {
                        ResponseData res = response.body();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    public void testpp(View v ){
        int[] array = new int[100];
        Arrays.fill(array, 1);
        platformActionInfoMessage(new ActionInfoReqData(new ActionInfo(AppConfig.WORK_LOCATION_ID
                , AppConfig.GET_IN
                , AppConfig.VEHICLE_ID
                , "DA00010001000000006D00ED"
                , "1"
                , mCurrentLoadCargo
                , "90"
                , String.valueOf(AppConfig.MATRIX_X)
                , String.valueOf(AppConfig.MATRIX_Y)
                , array)));
    }


    //플랫폼 alive 신호 전송
    public void platformSendAliveMessage(){
        mAlive = true;
        if (mAliveThread != null) mAliveThread.interrupt();
        mAliveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mAlive) {
                    platformAliveMessage(new AliveReqData(new Alive(AppConfig.WORK_LOCATION_ID,AppConfig.VEHICLE_ID,"0000")));
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }

            }

        });
        mAliveThread.start();
    }


    //플랫폼 alive 신호 전송
    public void platformSendErrorMessage(String errorCode){
        platformAliveMessage(new AliveReqData(new Alive(AppConfig.WORK_LOCATION_ID,AppConfig.VEHICLE_ID,errorCode)));
    }




    public void initSettingParams(){
        if (Hawk.contains("WATA_PLATFORM_URL")) {
            AppConfig.WATA_PLATFORM_URL = Hawk.get("WATA_PLATFORM_URL");
        }
        if (Hawk.contains("WORK_LOCATION_ID")) {
            AppConfig.WORK_LOCATION_ID = Hawk.get("WORK_LOCATION_ID");
        }
        if (Hawk.contains("VEHICLE_ID")) {
            AppConfig.VEHICLE_ID = Hawk.get("VEHICLE_ID");
        }
        if (Hawk.contains("RFID_MAC")) {
            AppConfig.RFID_MAC = Hawk.get("RFID_MAC");
        }
        if (Hawk.contains("RFID_SCAN_CNT_THRESHOLD")) {
            AppConfig.RFID_SCAN_CNT_THRESHOLD = Integer.parseInt(Hawk.get("RFID_SCAN_CNT_THRESHOLD").toString());
        }
        if (Hawk.contains("RFID_SCAN_INTERVAL")) {
            AppConfig.RFID_SCAN_INTERVAL = Integer.parseInt(Hawk.get("RFID_SCAN_INTERVAL").toString());
        }
        if (Hawk.contains("FLOOR_HEIGHT")) {
            AppConfig.FLOOR_HEIGHT = Integer.parseInt(Hawk.get("FLOOR_HEIGHT").toString());
        }
        if (Hawk.contains("PICK_THRESHOLD")) {
            AppConfig.PICK_THRESHOLD = Integer.parseInt(Hawk.get("PICK_THRESHOLD").toString());
        }
        if (Hawk.contains("TOF_WIDTH")) {
            AppConfig.TOF_WIDTH = Integer.parseInt(Hawk.get("TOF_WIDTH").toString());
        }
        if (Hawk.contains("TOF_HEIGHT")) {
            AppConfig.TOF_HEIGHT = Integer.parseInt(Hawk.get("TOF_HEIGHT").toString());
        }
        if (Hawk.contains("TOF_RESAMPLING_WIDTH_MIN")) {
            AppConfig.TOF_RESAMPLING_WIDTH_MIN = Integer.parseInt(Hawk.get("TOF_RESAMPLING_WIDTH_MIN").toString());
        }

        if (Hawk.contains("TOF_RESAMPLING_WIDTH_MAX")) {
            AppConfig.TOF_RESAMPLING_WIDTH_MAX = Integer.parseInt(Hawk.get("TOF_RESAMPLING_WIDTH_MAX").toString());
        }
        if (Hawk.contains("TOF_RESAMPLING_HEIGHT_MIN")) {
            AppConfig.TOF_RESAMPLING_HEIGHT_MIN = Integer.parseInt(Hawk.get("TOF_RESAMPLING_HEIGHT_MIN").toString());
        }
        if (Hawk.contains("TOF_RESAMPLING_HEIGHT_MAX")) {
            AppConfig.TOF_RESAMPLING_HEIGHT_MAX = Integer.parseInt(Hawk.get("TOF_RESAMPLING_HEIGHT_MAX").toString());
        }
        if (Hawk.contains("MATRIX_X")) {
            AppConfig.MATRIX_X = Integer.parseInt(Hawk.get("MATRIX_X").toString());
        }
        if (Hawk.contains("MATRIX_Y")) {
            AppConfig.MATRIX_Y = Integer.parseInt(Hawk.get("MATRIX_Y").toString());
        }

        // 포크 샘플링
        if (Hawk.contains("TOF_FORK_LENGTH")) {
            AppConfig.TOF_FORK_LENGTH = Integer.parseInt(Hawk.get("TOF_FORK_LENGTH").toString());
        }
        if (Hawk.contains("TOF_FORK_THICKNESS")) {
            AppConfig.TOF_FORK_THICKNESS = Integer.parseInt(Hawk.get("TOF_FORK_THICKNESS").toString());
        }
        if (Hawk.contains("TOF_FORK_GAP")) {
            AppConfig.TOF_FORK_GAP = Integer.parseInt(Hawk.get("TOF_FORK_GAP").toString());
        }

        //부피 샘플링
        if (Hawk.contains("TOF_RESAMPLING_VOLUME_WIDTH")) {
            AppConfig.TOF_RESAMPLING_VOLUME_WIDTH = Integer.parseInt(Hawk.get("TOF_RESAMPLING_VOLUME_WIDTH").toString());
        }
        if (Hawk.contains("TOF_RESAMPLING_VOLUME_HEIGHT")) {
            AppConfig.TOF_RESAMPLING_VOLUME_HEIGHT = Integer.parseInt(Hawk.get("TOF_RESAMPLING_VOLUME_HEIGHT").toString());
        }

    }

    public void settingParameter(View v){
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_setting, null);
        final EditText serverID = dialogView.findViewById(R.id.platformURL);
        final EditText centerID = dialogView.findViewById(R.id.locationID);
        final EditText vehicleID = dialogView.findViewById(R.id.vehicleID);
        final EditText rfidMac = dialogView.findViewById(R.id.rfMac);
        final EditText rfidThres = dialogView.findViewById(R.id.rfThres);
        final EditText rfidInterval = dialogView.findViewById(R.id.rfInterval);
        final EditText floorHeight = dialogView.findViewById(R.id.floorHeight);
        final EditText forkLength = dialogView.findViewById(R.id.forkLength);
        final EditText pickLength = dialogView.findViewById(R.id.pickThres);
        final EditText tofWidth = dialogView.findViewById(R.id.tofWidth);
        final EditText tofHeight = dialogView.findViewById(R.id.tofHeight);
        final EditText tofWidthResampleMin = dialogView.findViewById(R.id.resampleWidthMin);
        final EditText tofWidthResampleMax = dialogView.findViewById(R.id.resampleWidthMax);
        final EditText tofHeightResampleMin = dialogView.findViewById(R.id.resampleHeightMin);
        final EditText tofHeightResampleMax  = dialogView.findViewById(R.id.resampleHeightMax);
        final EditText matrixX  = dialogView.findViewById(R.id.matrixX);
        final EditText matrixY  = dialogView.findViewById(R.id.matrixY);

        final TextView btn_ok = dialogView.findViewById(R.id.btn_ok);
        final TextView btn_cancel = dialogView.findViewById(R.id.btn_cancel);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        serverID.setText(AppConfig.WATA_PLATFORM_URL);
        centerID.setText(AppConfig.WORK_LOCATION_ID);
        vehicleID.setText(AppConfig.VEHICLE_ID);
        rfidMac.setText(AppConfig.RFID_MAC);
        rfidThres.setText(String.valueOf(AppConfig.RFID_SCAN_CNT_THRESHOLD));
        rfidInterval.setText(String.valueOf(AppConfig.RFID_SCAN_INTERVAL));
        floorHeight.setText(String.valueOf(AppConfig.FLOOR_HEIGHT));
        forkLength.setText(String.valueOf(AppConfig.FORK_LENGTH));
        pickLength.setText(String.valueOf(AppConfig.PICK_THRESHOLD));
        tofWidth.setText(String.valueOf(AppConfig.TOF_WIDTH));
        tofHeight.setText(String.valueOf(AppConfig.TOF_HEIGHT));
        tofWidthResampleMin.setText(String.valueOf(AppConfig.TOF_RESAMPLING_WIDTH_MIN));
        tofWidthResampleMax.setText(String.valueOf(AppConfig.TOF_RESAMPLING_WIDTH_MAX));
        tofHeightResampleMin.setText(String.valueOf(AppConfig.TOF_RESAMPLING_HEIGHT_MIN));
        tofHeightResampleMax.setText(String.valueOf(AppConfig.TOF_RESAMPLING_HEIGHT_MAX));
        matrixX.setText(String.valueOf(AppConfig.MATRIX_X));
        matrixY.setText(String.valueOf(AppConfig.MATRIX_Y));


        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Hawk.put("WATA_PLATFORM_URL",serverID.getText().toString());
                Hawk.put("WORK_LOCATION_ID",centerID.getText().toString());
                Hawk.put("VEHICLE_ID",vehicleID.getText().toString());
                Hawk.put("RFID_MAC",rfidMac.getText().toString());
                Hawk.put("RFID_SCAN_CNT_THRESHOLD",rfidThres.getText().toString());
                Hawk.put("RFID_SCAN_INTERVAL",rfidInterval.getText().toString());
                Hawk.put("FLOOR_HEIGHT",floorHeight.getText().toString());
                Hawk.put("FORK_LENGTH",forkLength.getText().toString());
                Hawk.put("PICK_THRESHOLD",pickLength.getText().toString());
                Hawk.put("TOF_WIDTH",tofWidth.getText().toString());
                Hawk.put("TOF_HEIGHT",tofHeight.getText().toString());
                Hawk.put("TOF_RESAMPLING_WIDTH_MIN",tofWidthResampleMin.getText().toString());
                Hawk.put("TOF_RESAMPLING_WIDTH_MAX",tofWidthResampleMax.getText().toString());
                Hawk.put("TOF_RESAMPLING_HEIGHT_MIN",tofHeightResampleMin.getText().toString());
                Hawk.put("TOF_RESAMPLING_HEIGHT_MAX",tofHeightResampleMax.getText().toString());
                Hawk.put("MATRIX_X",matrixX.getText().toString());
                Hawk.put("MATRIX_Y",matrixY.getText().toString());

                AppConfig.WATA_PLATFORM_URL = serverID.getText().toString();
                AppConfig.WORK_LOCATION_ID = centerID.getText().toString();
                AppConfig.VEHICLE_ID = vehicleID.getText().toString();
                AppConfig.RFID_MAC = rfidMac.getText().toString();
                AppConfig.RFID_SCAN_CNT_THRESHOLD = Integer.parseInt(rfidThres.getText().toString());
                AppConfig.RFID_SCAN_INTERVAL = Integer.parseInt(rfidInterval.getText().toString());
                AppConfig.FLOOR_HEIGHT = Integer.parseInt(floorHeight.getText().toString());
                AppConfig.FORK_LENGTH = Integer.parseInt(forkLength.getText().toString());
                AppConfig.PICK_THRESHOLD = Integer.parseInt(pickLength.getText().toString());
                AppConfig.TOF_WIDTH = Integer.parseInt(tofWidth.getText().toString());
                AppConfig.TOF_HEIGHT = Integer.parseInt(tofHeight.getText().toString());
                AppConfig.TOF_RESAMPLING_WIDTH_MIN = Integer.parseInt(tofWidthResampleMin.getText().toString());
                AppConfig.TOF_RESAMPLING_WIDTH_MAX = Integer.parseInt(tofWidthResampleMax.getText().toString());
                AppConfig.TOF_RESAMPLING_HEIGHT_MIN = Integer.parseInt(tofHeightResampleMin.getText().toString());
                AppConfig.TOF_RESAMPLING_HEIGHT_MAX = Integer.parseInt(tofHeightResampleMax.getText().toString());
                AppConfig.MATRIX_X = Integer.parseInt(matrixX.getText().toString());
                AppConfig.MATRIX_Y = Integer.parseInt(matrixY.getText().toString());
                alertDialog.dismiss();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }

    public void viewShortToast(String str){
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }



    public void settingSampling(View v){
        switch (v.getId()){
            case R.id.sampling_fork_gap_add:
                AppConfig.TOF_FORK_GAP++;
                Hawk.put("TOF_FORK_GAP", AppConfig.TOF_FORK_GAP);
                break;
            case R.id.sampling_fork_gap_reduce:
                AppConfig.TOF_FORK_GAP--;
                Hawk.put("TOF_FORK_GAP", AppConfig.TOF_FORK_GAP);
                break;
            case R.id.sampling_fork_length_add:
                AppConfig.TOF_FORK_LENGTH++;
                Hawk.put("TOF_FORK_LENGTH", AppConfig.TOF_FORK_LENGTH);
                break;
            case R.id.sampling_fork_length_reduce:
                AppConfig.TOF_FORK_LENGTH--;
                Hawk.put("TOF_FORK_LENGTH", AppConfig.TOF_FORK_LENGTH);
                break;
            case R.id.sampling_fork_thickness_add:
                AppConfig.TOF_FORK_THICKNESS++;
                Hawk.put("TOF_FORK_THICKNESS", AppConfig.TOF_FORK_THICKNESS);
                break;
            case R.id.sampling_fork_thickness_reduce:
                AppConfig.TOF_FORK_THICKNESS--;
                Hawk.put("TOF_FORK_THICKNESS", AppConfig.TOF_FORK_THICKNESS);
                break;

            case R.id.sampling_volume_height_add:
                AppConfig.TOF_RESAMPLING_VOLUME_HEIGHT++;
                Hawk.put("TOF_RESAMPLING_VOLUME_HEIGHT", AppConfig.TOF_RESAMPLING_VOLUME_HEIGHT);
                break;
            case R.id.sampling_volume_height_reduce:
                AppConfig.TOF_RESAMPLING_VOLUME_HEIGHT--;
                Hawk.put("TOF_RESAMPLING_VOLUME_HEIGHT", AppConfig.TOF_RESAMPLING_VOLUME_HEIGHT);
                break;
            case R.id.sampling_volume_width_add:
                AppConfig.TOF_RESAMPLING_VOLUME_WIDTH++;
                Hawk.put("TOF_RESAMPLING_VOLUME_WIDTH", AppConfig.TOF_RESAMPLING_VOLUME_WIDTH);
                break;
            case R.id.sampling_volume_width_reduce:
                AppConfig.TOF_RESAMPLING_VOLUME_WIDTH--;
                Hawk.put("TOF_RESAMPLING_VOLUME_WIDTH", AppConfig.TOF_RESAMPLING_VOLUME_WIDTH);
                break;
        }
    }



    /**
     * 뒤로가기 버튼 클릭 이벤트
     */
    @Override
    public void onBackPressed() {
            if (System.currentTimeMillis() > backKeyTime + 2000) {
                backKeyTime = System.currentTimeMillis();
                viewShortToast("한번 더 누르면 종료됩니다");

                return;
            } else {
                mCodeScanner.releaseResources();
                finishAndRemoveTask();
            }

    }


    @Override
    protected void onResume() {
        super.onResume();
        if(camera != null){
            camera.openFrontDepthCamera();
        }

        if(mCodeScanner != null) {
            mCodeScanner.startPreview();
        }
    }

    @Override
    protected void onPause() {
        if(mCodeScanner != null) {
            mCodeScanner.releaseResources();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mBleScan = false;
        mAlive = false;
        if(mCodeScanner != null) {
            mCodeScanner.releaseResources();
        }
        TerabeeSdk.getInstance().unregisterDataReceive(mDataDistanceCallback);
        disconnectDevice();
    // release Terabee SDK
        TerabeeSdk.getInstance().dispose();

        stopService(new Intent(MainActivity.this, BluetoothService.class));
        unregisterReceiver(this.mNotificationReceiver);
        super.onDestroy();
    }

    //권한 확인
    private void onCheckPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            permissionHelper = new PermissionHelper(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.CAMERA}, 100);
            permissionHelper.request(new PermissionHelper.PermissionCallback() {
                @Override
                public void onPermissionGranted() {
                    Timber.d("onPermissionGranted() called");
                }

                @Override
                public void onIndividualPermissionGranted(String[] grantedPermission) {
                    Timber.d("onIndividualPermissionGranted() called with: grantedPermission = [" + TextUtils.join(",", grantedPermission) + "]");
                }

                @Override
                public void onPermissionDenied() {
                    Timber.d("onPermissionDenied() called");
                    Toast.makeText(getApplicationContext(), "권한 허용 후 해당 서비스를 이용하실 수 있습니다.\\n\\'설정\\'을 통해 권한 허용을 해주시기 바랍니다.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onPermissionDeniedBySystem() {
                    Timber.d("onPermissionDeniedBySystem() called");
                    permissionHelper.openAppDetailsActivity();
                }
            });
        } else {
            //onStartMain();
            //checkBluetooth();
        }
    }

}






