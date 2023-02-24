package com.example.androidLIS;

import static com.budiyev.android.codescanner.ScanMode.*;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
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

import com.apulsetech.lib.remote.type.ConfigValues;
import com.apulsetech.lib.remote.type.Msg;
import com.apulsetech.lib.remote.type.RemoteDevice;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.example.androidLIS.ApulseRFID.ApulseRFIDInstance;
import com.example.androidLIS.TerabeeSensor.TerabeeInstance;
import com.example.androidLIS.model.ActionInfo;
import com.example.androidLIS.model.ActionInfoReqData;
import com.example.androidLIS.model.Alive;
import com.example.androidLIS.model.AliveReqData;
import com.example.androidLIS.model.CargoData;
import com.example.androidLIS.model.LocationInfo;
import com.example.androidLIS.model.LocationInfoReqData;
import com.example.androidLIS.model.ResponseData;
import com.example.androidLIS.model.RfidScanData;
import com.example.androidLIS.network.RetrofitClient;
import com.example.androidLIS.network.RetrofitService;
import com.example.androidLIS.permission.PermissionHelper;
import com.example.androidLIS.service.BluetoothService;
import com.example.androidLIS.tof.Camera;
import com.example.androidLIS.tof.DepthFrameAvailableListener;
import com.example.androidLIS.tof.DepthFrameVisualizer;
import com.example.androidLIS.model.RackData;
import com.example.androidLIS.util.AppConfig;
import com.example.androidLIS.util.AppUtil;
import com.example.androidLIS.util.FileUtil;
import com.google.zxing.Result;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

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

    private PermissionHelper permissionHelper;
    public static final int CAM_PERMISSIONS_REQUEST = 0;
    private static final String DATE_FORMAT = "HH:mm:ss.SSS";
    private long backKeyTime = 0;

    //TOF 카메라
    private TextureView rawDataView;
    private Matrix defaultBitmapTransform;
    private Camera camera;

    //QR코드 스캐너
    private CodeScanner mCodeScanner;
    CodeScannerView mCodeScannerView;


    //Terabee sensor
    public TerabeeInstance mTerabeeSensorInstance;
    public TerabeeHandler mTerabeeHandler;

    public ApulseRFIDInstance mApulseRFIDInstance;
    private RfidHandler mRfidHandler= new RfidHandler(this);
    public boolean isRFConnected = false;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mBleSupported = false;
    private static final boolean USE_USER_INTERACTIVE_PERMISSION = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_OVERLAY = 2;

    //텍스트 뷰어
    TextView mCargoDepth;
    TextView mQrText;
    TextView mDisText;
    TextView mCargoAddress;
    TextView mTimeText;

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
     * mCurrentRack : 선반 위치
     */
    public RackData mCurrentRack;
    public ArrayList<String> mBLEQueue;
    public String LocationEPC = null;

    /**
     * 최근 10초동안 정면 안테나 rssi 최대값
     */


    public String mOnRackEpc = "";
    public long mOnRackTime = 0;


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

    //Alive 스레드
    public Thread mAliveThread;
    //백엔드 alive
    public boolean mAlive = false;


    //Alive 스레드
    public Thread mLocationThread;
    //백엔드 alive
    public boolean mLocation = false;

    //백엔드 alive 패킷 전송 실패 카운트
    public int mAliveFailCnt = 0;


    //백엔드 통신
    private RetrofitService mService;


    File externalStorageDirectory = Environment.getExternalStorageDirectory();
    String externalStorageDirectoryPath = externalStorageDirectory.getAbsolutePath();
    String mLogDirectoryName = externalStorageDirectoryPath+"/WATALIS/CommonLog";
    String mRFIDLogDirectoryName = externalStorageDirectoryPath+"/WATALIS/RfidLog";

    File mLogFile = null;
    File mRFIDLogFile = null;


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
        mCargoAddress = (TextView) findViewById(R.id.cargoAddress);
        mTimeText = (TextView)findViewById(R.id.timeText);


        mCurrentRack = new RackData(1,"field",0);
        mBLEQueue = new ArrayList<String>();
        mCargo = new CargoData("cargo",0);
        mCodeScannerView = (CodeScannerView) findViewById(R.id.scanner_view);
        mCodeScannerView.setClickable(false);

        AppUtil.getInstance().initSettingParams();
        /**
         * 로그파일 생성
         */
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
        String currentDate = dateFormat.format(new Date());
        mLogFile = FileUtil.createFileWithDate(mLogDirectoryName+"/"+currentDate);
        mRFIDLogFile = FileUtil.createFileWithDate(mRFIDLogDirectoryName+"/"+currentDate);
        if(!AppUtil.getInstance().isNetworkConnected(getApplicationContext())){
            Log.d("network","not connected");
        }else{
            Log.d("network","connected");
        }


        if(android.os.Build.VERSION.SDK_INT >= 18)
            startService(new Intent(MainActivity.this, BluetoothService.class));

        /**
         * 백엔드 통신 설정
         */
        mService = RetrofitClient.getHeavyClient().create(RetrofitService.class);
        platformSendAliveMessage();


        mTerabeeHandler = new TerabeeHandler();
        mTerabeeSensorInstance = new TerabeeInstance(getApplicationContext(),mTerabeeHandler);
        mTerabeeSensorInstance.initTerabeeSensor();


        mApulseRFIDInstance = new ApulseRFIDInstance(this,mRfidHandler);
        mApulseRFIDInstance.startScan();

        boolean supported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (supported) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }

            if (mBluetoothAdapter != null) {
                if (!mBluetoothAdapter.isEnabled()) {
                    if (USE_USER_INTERACTIVE_PERMISSION) {
                        Intent enableBtIntent =
                                new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        mBluetoothAdapter.enable();
                    }

                    Toast.makeText(this,
                            R.string.remote_scanner_alert_turning_on_bluetooth,
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this,
                        R.string.remote_scanner_alert_bluetooth_not_supported,
                        Toast.LENGTH_SHORT).show();
            }

            mBleSupported = getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
            if (!mBleSupported) {
                Toast.makeText(this,
                        R.string.remote_scanner_alert_ble_not_supported,
                        Toast.LENGTH_SHORT).show();
            }else{
                mApulseRFIDInstance.bindBtSppRemoteService();
            }
        } else {
            if (mBluetoothAdapter == null) {
                Toast.makeText(this,
                        R.string.remote_scanner_alert_bluetooth_not_supported,
                        Toast.LENGTH_SHORT).show();
            }
        }


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


    /**
     * 거리센서 데이터 핸들러
     */
    public class TerabeeHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            switch (bundle.getInt("type")){
                case 1:
                    setDistext(String.valueOf(bundle.getInt("floor")));
                    setCurrentFloor(bundle.getInt("floor"));
                    break;
                case 2:
                    isTerabeeConn(bundle.getInt("connect"));
                    break;
            }
        }
    }

    public void isTerabeeConn(int is){
        if(is == 1){
            runOnUiThread(new Runnable() {
                public void run() {
                    viewShortToast("sensor connected");
                }
            });
        }else if(is == 0){
            runOnUiThread(new Runnable() {
                public void run() {
                    viewShortToast("sensor not connected");
                }
            });
        }else if(is == 2){
            runOnUiThread(new Runnable() {
                public void run() {
                    viewShortToast("sensor disconnected");
                }
            });
        }else if(is == 3){
            runOnUiThread(new Runnable() {
                public void run() {
                    viewShortToast("sensor error");
                }
            });
        }

    }


    /**
     * RFID 스캐너 핸들러
     */
    public class RfidHandler extends Handler {
        private final WeakReference<MainActivity> mWeakActivity;

        public RfidHandler(MainActivity activity) {
            mWeakActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MainActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.d("handleMessage()", " activity is null!");
                return;
            }
            Log.d("handleMessage()", "msg=" + msg.what);
            switch (msg.what) {
                case Msg.BT_SPP_ADD_DEVICE:
                    BluetoothDevice btDevice = (BluetoothDevice) msg.obj;
                    RemoteDevice device = RemoteDevice.makeBtSppDevice(btDevice);
                    if (device.getAddress().equals(AppConfig.RFID_MAC)) {
                        Log.d("bleDevice", device.getRfidStatus() + "");
                        mApulseRFIDInstance.scanDevice = device;
                    }
                    break;

                case Msg.BT_SPP_DEVICE_INFO_RECEIVED:
                case Msg.WIFI_DEVICE_INFO_RECEIVED:
                    RemoteDevice.Detail detail = (RemoteDevice.Detail) msg.obj;
                    String deviceAddress = detail.mAddress;
                    Log.d("BT_SPP_DEVICE_INFO_RECEIVED", "info:" + deviceAddress);
                    if (deviceAddress.equals(AppConfig.RFID_MAC)) {
                        RemoteDevice remoteDevice = mApulseRFIDInstance.scanDevice;
                        if (remoteDevice != null) {
                            if ((msg.what == Msg.SERIAL_DEVICE_INFO_RECEIVED) ||
                                    (msg.what == Msg.USB_DEVICE_INFO_RECEIVED) ||
                                    (msg.what == Msg.BT_SPP_DEVICE_INFO_RECEIVED)) {
                                remoteDevice.setName((detail.mDeviceName != null) ? detail.mDeviceName : detail.mModel);
                            }
                            remoteDevice.setDetail(detail);

                            if ((remoteDevice.getStatus() != RemoteDevice.STATUS_IDLE) && !remoteDevice.forceConnectionEnabled()) {
                                Toast.makeText(MainActivity.this,
                                        R.string.remote_scanner_alert_remote_device_is_busy_or_in_unkown_state,
                                        Toast.LENGTH_LONG).show();
                                return;
                            } else {
                                Log.e("Connect", AppConfig.RFID_MAC + " conn success");
                                Toast.makeText(MainActivity.this,
                                        "Connect Success",
                                        Toast.LENGTH_LONG).show();
                                String jsonDevice = remoteDevice.toGson();
                                if (!mApulseRFIDInstance.mPreviouslyConnectedDevices.contains(jsonDevice)) {
                                    mApulseRFIDInstance.mPreviouslyConnectedDevices.add(jsonDevice);
                                    mApulseRFIDInstance.mSetting.setPreviouslyConnectedDevices(mApulseRFIDInstance.mPreviouslyConnectedDevices);
                                }
                                mApulseRFIDInstance.initialize(remoteDevice, ConfigValues.DEFAULT_REMOTE_CONNECTION_TIMEOUT_IN_MS);
                            }
                        }

                    }
                    break;
                case AppConfig.RFID_SCAN_RESULT:
                    ArrayList<RfidScanData> frontData = mApulseRFIDInstance.getFrontScanData();
                    ArrayList<RfidScanData> sideData = mApulseRFIDInstance.getSideScanData();
                    String RFIDLogText = "";
                    long now = System.currentTimeMillis();
                    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                    String formatTime = dateFormat.format(now);

                    int frontMaxRssiIndex = getMaxRssiIndex(frontData);
                    int sideMaxRssiIndex = getMaxRssiIndex(sideData);

                    if(now - mOnRackTime > AppConfig.ON_RACK_THRESHOLD){
                        mOnRackTime = 0;
                        mOnRackEpc = "";
                    }

                    if(frontData.size()>0) {
                        if (frontData.get(frontMaxRssiIndex).rssi >= AppConfig.ON_RACK_RSSI) {
                            mOnRackEpc = frontData.get(frontMaxRssiIndex).epc;
                            mOnRackTime = now;
                        }
                    }
                    setTimeText(mOnRackEpc);

                    RFIDLogText = "[" + formatTime + "]\n"+"front size:"+frontData.size() + ", side size:"+sideData.size()+"\n";
                    Log.e("scandata", "front size:"+frontData.size() + ", side size:"+sideData.size());
                    if(frontMaxRssiIndex != -1){
                        setCurrentRack(frontData.get(frontMaxRssiIndex).epc);
                        setCargoAddress(frontData.get(frontMaxRssiIndex).epc +"//"+frontData.get(frontMaxRssiIndex).rssi);
                        LocationEPC = frontData.get(frontMaxRssiIndex).epc;
                    }else if(sideMaxRssiIndex != -1){
                        setCargoAddress("field\n"+sideData.get(sideMaxRssiIndex).epc +"//"+sideData.get(sideMaxRssiIndex).rssi);
                        LocationEPC = sideData.get(sideMaxRssiIndex).epc;
                    }else{
                        setCargoAddress("field");
                        LocationEPC = "field";
                    }


                    if(frontData.size()!=0) {
                        RFIDLogText = RFIDLogText + "FRONT_DATA:\n";
                        for(int i =0 ; i < frontData.size() ; i++){
                            if(i == frontMaxRssiIndex){
                                RFIDLogText = RFIDLogText + "**";
                            }
                            RFIDLogText = RFIDLogText +frontData.get(i).epc + "(" + frontData.get(i).rssi + ")\n";
                        }
                        Log.e("scandata", "front:" + frontData.get(frontMaxRssiIndex).epc + "[" + frontData.get(frontMaxRssiIndex).rssi + "]");
                    }

                    if(sideData.size() !=0) {
                        RFIDLogText = RFIDLogText + "SIDE_DATA:\n";
                        for(int i =0 ; i < sideData.size() ; i++){
                            if(i == sideMaxRssiIndex){
                                RFIDLogText = RFIDLogText + "**";
                            }
                            RFIDLogText = RFIDLogText +sideData.get(i).epc + "(" + sideData.get(i).rssi + ")\n";
                        }
                        Log.e("scandata", "side:" + sideData.get(sideMaxRssiIndex).epc + "[" + sideData.get(sideMaxRssiIndex).rssi + "]");
                    }
                    FileUtil.writeDataToFile(mRFIDLogFile, RFIDLogText+"\n\n");

                    if(mLocationThread == null){
                        platformSendLocationMessage();
                    }

                    break;

            }
        }
    }

    public int getMaxRssiIndex(ArrayList<RfidScanData> data){
        if (data.size() > 0) {
            double max_rssi = Integer.MIN_VALUE;
            int max_rssi_index = 0;
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).rssi > max_rssi) {
                    max_rssi = data.get(i).rssi;
                    max_rssi_index = i;
                }
            }
            return max_rssi_index;
        }else{
            return -1;
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
        String logText = "";
        long now = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String formatTime = dateFormat.format(now);
//        setTimeText(formatTime);
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

            case 1://짐이 있는 상황
                if ((now - mCurrentLoadIn) > 3000                                                   //짐이 실리고 3초 후
                        && (now - mCurrentRack.time) < 3000                                         //스캔된 RFID가 3초내에 있을 때
                        && mCurrentRack.rack.equals(mOnRackEpc)                                     //최근 10초동안 선반에 있다고 판단되는 RFID가 최근 스캔된 가장쎈 RFID와 일치
                        && mCurrentLoadIn != 0 ) {                                                  //짐을 이미 실었다고 판단하는 경우 : 이 로직을 타지 않음
                    this.mCurrentLoadIn = 0;
                    //선반에 있는 짐을 실은 상황
                    /**
                     * 짐의 이름 : cargoName
                     * 현재 위치 : mCurrentRack.rack
                     * 현재 높이 : mCurrentRack.floor
                     * 현재 짐의 부피 : mCurrentCargoVolume
                     */
                    logText = "[" + formatTime + "]" + "EVENT:"+AppConfig.GET_IN + ", RACK_ID:" + mCurrentRack.rack + ", CARGO_ID:" + mCurrentLoadCargo + ", Height:" + mCurrentRack.floor + ", Volume:" + mLoadInCargoVolume + "\n";
                    viewShortToast(logText);
                    FileUtil.writeDataToFile(mLogFile,logText);
                    int[] defaultMatrix ={0,0,0,0,0,0,0,0,0,0};

                    //백엔드 연동 추가
                    platformActionInfoMessage(new ActionInfoReqData(new ActionInfo(AppConfig.WORK_LOCATION_ID
                            , AppConfig.GET_IN
                            , AppConfig.VEHICLE_ID
                            , mCurrentRack.rack
                            , String.valueOf(mCurrentRack.floor)
                            , mCurrentLoadCargo
                            , "0"
                            , String.valueOf(AppConfig.MATRIX_X)
                            , String.valueOf(AppConfig.MATRIX_Y)
                            , defaultMatrix)));
                    setCargoAddress(mCurrentRack.rack);
                } else if (mCurrentLoadIn != 0 && (now - mCurrentLoadIn) > 3000) {//선반이 아닌 곳의 짐을 실음
                    int[] defaultMatrix ={0,0,0,0,0,0,0,0,0,0};
                    this.mCurrentLoadIn = 0;
                    setCargoAddress("field");
                    logText ="[" + formatTime + "]" +  "EVENT:"+AppConfig.GET_IN + ", RACK_ID:" + "Field" + ", CARGO_ID:" + mCurrentLoadCargo + ", Height:" + mCurrentRack.floor + ", Volume:" + mLoadInCargoVolume+ "\n";
                    viewShortToast(logText);
                    FileUtil.writeDataToFile(mLogFile,logText);

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
                if ((now - mCurrentRack.time) < 5000
                        && mCurrentRack.rack.equals(mOnRackEpc)) {
                    //내린 선반 입력''''
                    logText = "[" + formatTime + "]" + "EVENT:"+AppConfig.GET_OUT + ", RACK_ID:" + mCurrentRack.rack + ", CARGO_ID:" + mCurrentLoadCargo + ", Height:" + mCurrentRack.floor + ", Volume:" + mLoadInCargoVolume+ "\n";
                    FileUtil.writeDataToFile(mLogFile,logText);
                    viewShortToast(logText);
                    //백엔드 연동 추가
                    platformActionInfoMessage(new ActionInfoReqData(new ActionInfo(AppConfig.WORK_LOCATION_ID
                            , AppConfig.GET_OUT
                            , AppConfig.VEHICLE_ID
                            , mCurrentRack.rack
                            , String.valueOf(mCurrentRack.floor)
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
                    logText = "[" + formatTime + "]" + "EVENT:"+AppConfig.GET_OUT + ", RACK_ID:" + "Field"+ ", CARGO_ID:" + mCurrentLoadCargo + ", Height:" + mCurrentRack.floor + ", Volume:" + mLoadInCargoVolume+ "\n";
                    viewShortToast(logText);
                    FileUtil.writeDataToFile(mLogFile,logText);

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

                }
            });
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

    public RackData getCurrentRack() {
        return mCurrentRack;
    }

//    public void setCurrentPosition(PositionData mCurrentPosition) {
//        this.mCurrentPosition = mCurrentPosition;
//    }

    //현재 RFID 스캔 위치
    public void setCurrentRack(String rck){
        this.mCurrentRack.setRack(rck);
        this.mCurrentRack.setTime(System.currentTimeMillis());
    }

    //현재 거리센서 높이
    public void setCurrentFloor(int f){
        this.mCurrentRack.setFloor(f);
    }


    //짐 위치 텍스트뷰
    public void setCargoAddress(String s){
        runOnUiThread(new Runnable() {
            public void run() {
                mCargoAddress.setText(s);
            }
        });
    }


    //시간 텍스트뷰
    public void setTimeText(String s){
        runOnUiThread(new Runnable() {
            public void run() {
                mTimeText.setText(s);
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
//                            connBLE();
                            Log.d("AliveResdata", res.toString());
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
        mService.ACTION_INFO_MESSAGE(data).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Response<ResponseData>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }
                    @Override
                    public void onSuccess(Response<ResponseData>response) {
                        ResponseData res = response.body();
                        if(res != null) {
                            Log.d("ActionInfoResdata", res.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });
    }


    /**
     * 백엔드 연동 테스트
     * Location 패킷 전송
     * @param data
     */
    public void platformLocationMessage(LocationInfoReqData data){
        mService.LOCATION_INFO_MESSAGE(data).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Response<ResponseData>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }
                    @Override
                    public void onSuccess(Response<ResponseData>response) {
                        ResponseData res = response.body();
                        if(res != null) {
                            Log.d("LocationInfoResdata", res.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
    }



    public void testpp(View v ){
       mApulseRFIDInstance.startScan();
    }


    //플랫폼 alive 신호 전송
    public void platformSendAliveMessage(){
        mAlive = true;
        if (mAliveThread != null){
            mAliveThread.interrupt();
            mAliveThread = null;
        }
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

    //플랫폼 Location 정보 전송
    public void platformSendLocationMessage(){
        mLocation = true;
        if (mLocationThread != null){
            mLocationThread.interrupt();
            mLocationThread = null;
        }
        mLocationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mLocation) {
                    platformLocationMessage(new LocationInfoReqData(new LocationInfo(AppConfig.WORK_LOCATION_ID,AppConfig.VEHICLE_ID,LocationEPC)));
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }

        });
        mLocationThread.start();
    }



    //플랫폼 alive 신호 전송
    public void platformSendErrorMessage(String errorCode){
        platformAliveMessage(new AliveReqData(new Alive(AppConfig.WORK_LOCATION_ID,AppConfig.VEHICLE_ID,errorCode)));
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
                mAlive = false;
                mLocation = false;
                if(mCodeScanner != null) {
                    mCodeScanner.releaseResources();
                }

                mTerabeeSensorInstance.closeInstance();
                mApulseRFIDInstance.closeInstance();

                stopService(new Intent(MainActivity.this, BluetoothService.class));
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

        mApulseRFIDInstance.stopScan();

    }

    @Override
    protected void onPause() {
        if(mCodeScanner != null) {
            mCodeScanner.releaseResources();
        }

        mApulseRFIDInstance.stopScan();


        super.onPause();
    }


    @Override
    protected void onDestroy() {
        mAlive = false;
        mLocation = false;
        if(mCodeScanner != null) {
            mCodeScanner.releaseResources();
        }
        mTerabeeSensorInstance.closeInstance();
        mApulseRFIDInstance.closeInstance();

        stopService(new Intent(MainActivity.this, BluetoothService.class));
        super.onDestroy();
    }


    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e("refreshDeviceCache", "An exception occurred while refreshing device");
        }
        return false;
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






