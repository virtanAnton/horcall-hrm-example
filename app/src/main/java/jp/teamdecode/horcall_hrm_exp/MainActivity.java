package jp.teamdecode.horcall_hrm_exp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_SCAN_BT = 2;
    private final int REQUEST_BT = 3;
    private final int REQUEST_WRITE_EXTERNAL_STORAGE = 4;

    private BluetoothAdapter mBluetoothAdapter;

    public final static UUID UUID_HEART_RATE_SERVICE = UUID
            .fromString(SampleGattAttributes.HEART_RATE_SERVICE);

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID
            .fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning;
    private Handler mHandler;
    private Set<BluetoothDevice> mPairedDevices = new HashSet<>();

    private String mDeviceAddress;

    private ServiceConnection mBtServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            MyBluetoothLeService.LocalBinder binder = (MyBluetoothLeService.LocalBinder) service;
            mBluetoothLeService = binder.getService();
            mBluetoothLeService.connect(mDeviceAddress);
            mBluetoothLeService.setCallbackListener(new HrmServiceCallbackListener());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    private boolean mBound = false;
    private BluetoothManager mBluetoothManager;
    private MyBluetoothLeService mBluetoothLeService;


    Button mStartButton;
    Button mTryButton;
    LogAdapter mRecyclerAdapter;
    RecyclerView mRecyclerView;
    Button mLogCopy;

    MenuItem mScanProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        mStartButton = (Button) findViewById(R.id.start);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH)
                        != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_ADMIN)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Check Permissions Now
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                            REQUEST_SCAN_BT);
                } else {
                    scanBtActions();
                }
            }
        });

        mTryButton = (Button) findViewById(R.id.connect);
        mTryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Check Permissions Now
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                            REQUEST_SCAN_BT);
                } else {

                    connectBtnAction();
                }
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.logs);
        mLogCopy = (Button) findViewById(R.id.clip_log);
        mLogCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Check Permissions Now
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_SCAN_BT);
                } else {
                    shareLogfile();
                }
            }
        });

        initRecyclerVew();
    }

    private void initRecyclerVew() {
        mRecyclerAdapter = new LogAdapter();
        mRecyclerView.setAdapter(mRecyclerAdapter);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        manager.setReverseLayout(true);
        mRecyclerView.setLayoutManager(manager);
    }

    private void shareLogfile() {
        Uri uri = null;
        try {
            File filename = new File(Environment.getExternalStorageDirectory() + "/logfile.log");
            filename.createNewFile();
            String cmd = "logcat -d -f " + filename.getAbsolutePath();
            Runtime.getRuntime().exec(cmd);
            uri = Uri.fromFile(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (uri != null) {
            shareFile(uri);
        } else {
            Toast.makeText(this, "Log file not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(Uri uri) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, "Send"));
    }


    private void bindHrmService(String deviceAddress) {
        mRecyclerAdapter.addLog("bindHrmService: deviceAddress: " + deviceAddress);
        mDeviceAddress = deviceAddress;
        bindService(new Intent(this, MyBluetoothLeService.class), mBtServiceConnection, BIND_AUTO_CREATE);
    }

    private void scanBtActions() {

        // Initializes Bluetooth adapter.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mPairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d(TAG, "mPairedDevices count: " + mPairedDevices.size());
        mRecyclerAdapter.addLog("mPairedDevices count: " + mPairedDevices.size());

        for (BluetoothDevice device : mPairedDevices) {
            Log.d(TAG, "mPairedDevices: Name: " + device.getName() + "; address: " + device.getAddress() + "; Bond state: " + device.getBondState());
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        //displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(MainActivity.this, "Turn on bluetooth!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()");
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            scanLeDevice(true);
        }
    }

    private void connectBtnAction() {
        // Initializes Bluetooth adapter.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mPairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d(TAG, "mPairedDevices count: " + mPairedDevices.size());
        mRecyclerAdapter.addLog("mPairedDevices count: " + mPairedDevices.size());


        // Ensures Bluetooth is available on the device and it is enabled. If not,
        //displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(MainActivity.this, "Turn on bluetooth!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()");
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            for (BluetoothDevice device : mPairedDevices) {
                Log.d(TAG, "mPairedDevices: Name: " + device.getName() + "; address: " + device.getAddress() + "; Bond state: " + device.getBondState());
                mRecyclerAdapter.addLog("mPairedDevices: Name: " + device.getName() + "; address: " + device.getAddress() + "; Bond state: " + device.getBondState());
                if (hasValidUuid(device)) {
                    bindHrmService(device.getAddress());
                }
            }

        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecyclerAdapter.addLog("scanLeDevice: stop");
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mRecyclerAdapter.addLog("scanLeDevice: start");
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mRecyclerAdapter.addLog("scanLeDevice: stop");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SupportMenuInflater inflater = new SupportMenuInflater(this);
        inflater.inflate(R.menu.main_activity_menu, menu);
        mScanProgress = menu.findItem(R.id.scanning);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mScanning) {
            mScanProgress.setVisible(true);
        } else {
            mScanProgress.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Nullable
    private BluetoothDevice getHrmDevice(List<BluetoothDevice> devices) {
        BluetoothDevice resultDevice = null;
        if (devices.size() != 0) {
            for (BluetoothDevice device : devices) {
                if (hasValidUuid(device)) {
                    resultDevice = device;
                }
            }
        }
        return resultDevice;
    }

    private boolean hasValidUuid(BluetoothDevice device) {
        Log.d(TAG, "hasValidUuid: device.getName(): " + device.getName());
        ParcelUuid[] deviceUuids = device.getUuids();
        for (ParcelUuid deviceUuid : deviceUuids) {
            UUID currentUuid = deviceUuid.getUuid();
            Log.d(TAG, "Device: Name/address" + device.getName() + "/" + device.getAddress() + "Uuid: " + currentUuid.toString());
            if (currentUuid.equals(UUID_HEART_RATE_SERVICE)) {
                mRecyclerAdapter.addLog("We have HRM device! Name/address" + device.getName() + "/" + device.getAddress() + "Uuid: " + currentUuid.toString());
                Log.d(TAG, "We have HRM device! Name/address" + device.getName() + "/" + device.getAddress() + "Uuid: " + currentUuid.toString());
                return true;
            }
        }
        return false;
    }

    private boolean isBoundedDevice(BluetoothDevice device) {
        for (BluetoothDevice bounded : mPairedDevices) {
            if (bounded.getAddress().equals(device.getAddress())) return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_SCAN_BT) {
            if (grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                scanBtActions();
            } else {
                // Permission was denied or request was cancelled
                Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_BT) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectBtnAction();
            } else {
                // Permission was denied or request was cancelled
                Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shareLogfile();
            } else {
                // Permission was denied or request was cancelled
                Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBound) {
            unbindService(mBtServiceConnection);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mRecyclerAdapter.addLog("onLeScan: device scanned. Name: " + device.getName());
                    Log.d(TAG, "onLeScan: device scanned. Name: " + device.getName());
                    if (isBoundedDevice(device) && hasValidUuid(device)) {
                        mRecyclerAdapter.addLog("onLeScan: HRM founded: Name: " + device.getName() + "; Address: " + device.getAddress());
                        Log.d(TAG, "onLeScan: HRM founded: Name: " + device.getName() + "; Address: " + device.getAddress());
                        scanLeDevice(false);
                        bindHrmService(device.getAddress());
                    }

                }
            };

    private class HrmServiceCallbackListener implements MyBluetoothLeService.CallbackListener {

        @Override
        public void onGattConnected() {
            mRecyclerAdapter.addLog("onGattConnected: ");
            Log.d(TAG, "onGattConnected: ");
        }

        @Override
        public void onGattDisconnected() {
            mRecyclerAdapter.addLog("onGattDisconnected: ");
            Log.d(TAG, "onGattDisconnected: ");
        }

        @Override
        public void onGattServicesDiscovered() {
            mRecyclerAdapter.addLog("onGattServicesDiscovered: ");
            Log.d(TAG, "onGattServicesDiscovered: ");
        }

        @Override
        public void onNewPulseValueReceived(String payload) {
            mRecyclerAdapter.addLog("onNewPulseValueReceived: pules" + payload);
            Log.d(TAG, "onNewPulseValueReceived: pules = " + payload);
        }

        @Override
        public void onNewDataReceived(String payload) {
            mRecyclerAdapter.addLog("onNewDataReceived: " + payload);
            Log.d(TAG, "onNewDataReceived: " + payload);
        }
    }
}
