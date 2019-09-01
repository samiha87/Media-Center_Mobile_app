package com.example.media_center;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.media_center.ui.main.SectionsPagerAdapter;
import com.example.media_center.ui.main.PlaceholderFragment;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.example.media_center.BluetoothLeService.ACTION_GATT_CONNECTED;
import static com.example.media_center.BluetoothLeService.ACTION_GATT_DISCONNECTED;

public class MainActivity extends AppCompatActivity implements PlaceholderFragment.OnListFragmentInteractionListener {
    Timer timer;
    TimerTask timerTask;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private static final String TAG = "MainActivity";
    private BluetoothDevice ourDevice = null;
    private BluetoothGatt mGatt = null;
    private BluetoothGattService mCommunicationService = null;
    private BluetoothGattCharacteristic mCharacteristic = null;
    private Context mContext = this;
    private BluetoothLeService mBluetoothLeService;
    // Code to manage Service lifecycle.
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private SectionsPagerAdapter mSectionsPagerAdapter = null;

    private int projState = 0;
    private int audioState = 0;
    private int lightsState = 0;

    final Handler handler = new Handler();

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
               // invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        // Start scanning for ble device
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
           // finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        resetBle();
        scanLeDevice(true);
        startTimer();
    }

    private void resetBle() {
        if(mBluetoothLeService != null) mBluetoothLeService.disconnect();
        if(mGatt != null) {
            mGatt.disconnect();
            //mGatt.close();
        }
        if(ourDevice != null) {
            if(ourDevice.getBondState() != BluetoothDevice.BOND_NONE) {
                Log.d(TAG, "resetBle: BLE still bonded");
                mGatt = ourDevice.connectGatt(mContext, false, mGattCallback);
                mDeviceAddress = ourDevice.getAddress();
                if(mBluetoothLeService == null) {
                    resetBle();
                    scanLeDevice(true);
                } else {
                    mBluetoothLeService.connect(mDeviceAddress);
                }
                return;
            }
        }
        ourDevice = null;
        mGatt = null;
        mCharacteristic = null;
        mCommunicationService = null;
        mConnected = false;
    }
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if(ourDevice != null) {
                if(ourDevice.getBondState() != BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "resetBle: BLE still bonded");
                    mGatt = ourDevice.connectGatt(mContext, false, mGattCallback);
                    mDeviceAddress = ourDevice.getAddress();
                    if(mBluetoothLeService == null) {
                        resetBle();
                        scanLeDevice(true);
                    } else {
                        mBluetoothLeService.connect(mDeviceAddress);
                    }
                    return;
                }
            }
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    //invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 3000, 3000);
    }
    public void stoptimertask(View v) {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        //get the current timeStamp
                       // check all bluetooth variables
                        if(mGatt == null) {
                            resetBle();
                            scanLeDevice(true);
                        }

                        if(mCharacteristic == null) {
                            resetBle();
                            scanLeDevice(true);
                        }
                    }
                });
            }
        };
    }

    private static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if  (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                Log.d(TAG, "onConnectionStateChange: connected");
                // Discover services
                if (mGatt== null) {
                    mGatt = gatt;
                    Log.d(TAG, "onConnectionStateChange: mGatt iss null");
                }

                mGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.d(TAG, "onConnectionStateChange: Disconnected");
                resetBle();
                scanLeDevice(true);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicCanged: " + characteristic.getValue());
            // Data comes as in decimals
            final byte[] data = characteristic.getValue();

            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append((char)byteChar);

                Log.d(TAG, "onCharacteristicChanged: " + stringBuilder.toString());
                mSectionsPagerAdapter.mMainFragment.bleData(stringBuilder.toString());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead: " + characteristic.getValue());
            } else if (status == BluetoothGatt.GATT_FAILURE){
                Log.d(TAG, "onCharacteristicRead: Failed to read");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered: Gatt success");
                // return messaging service
                // Loop through services until transmission service is found
                for (BluetoothGattService serv: gatt.getServices()) {
                    Log.d(TAG, "onServicesDiscovered: Services " + serv.getUuid());
                }
                mCommunicationService = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
                for (BluetoothGattCharacteristic characteristic: mCommunicationService.getCharacteristics()) {
                    Log.d(TAG, "onServicesDiscovered: Discovering characteristic" + characteristic.getUuid());
                    for (BluetoothGattDescriptor descriptor: characteristic.getDescriptors()) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mGatt.writeDescriptor(descriptor);
                    }
                    mCharacteristic = characteristic;
                    mGatt.setCharacteristicNotification(characteristic, true);
                }

            } else {
                Log.d(TAG, "onServicesDiscovered: Received" + status);
            }
        }
    };

    void sendBLE(String toSend) {
        if(mBluetoothLeService == null) {
            if(mGatt != null) mGatt.close();
            Log.d(TAG, "sendBLE: mBluetoothLeService is null");
            resetBle();
            scanLeDevice(true);
            return;
        }
        if(mCharacteristic == null) {
            if(mGatt != null) mGatt.close();
            mBluetoothLeService.close();
            Log.d(TAG, "sendBLE: mCharacteristic is null");
            resetBle();
            scanLeDevice(true);
            return;
        }
        Log.d(TAG, "Send BLE");
        final byte[] tx = toSend.getBytes();
        mCharacteristic.setValue(tx);

        mBluetoothLeService.writeCharacteristic(mCharacteristic);
        mBluetoothLeService.setCharacteristicNotification(mCharacteristic, true);
    }

    @Override
    public void onButtonPressed(String msg) {
        Log.d(TAG, "onButtonPressed: " + msg);
        sendBLE(msg);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(device != null) {
                        Log.d(TAG, "Device: " + device.getName());
                        if(ourDevice == null) {
                            if(device.getName() != null) {
                                if (device.getName().contains("Totem_Blue")) {
                                    if(device.getAddress() != null) {
                                        ourDevice = device;
                                        scanLeDevice(false);
                                        // We found the device so we can stop scanning
                                        ourDevice.createBond();
                                        Log.d(TAG, "run: Connecting device");
                                        mGatt = device.connectGatt(mContext, false, mGattCallback);
                                        mDeviceAddress = device.getAddress();
                                        if(mBluetoothLeService == null) {
                                            resetBle();
                                            scanLeDevice(true);
                                        } else {
                                            mBluetoothLeService.connect(mDeviceAddress);
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    };
}