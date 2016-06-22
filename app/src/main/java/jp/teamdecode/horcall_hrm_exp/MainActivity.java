package jp.teamdecode.horcall_hrm_exp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_BT = 2;

    private BluetoothAdapter mBluetoothAdapter;

    public final static UUID UUID_HEART_RATE_SERVICE = UUID
            .fromString(SampleGattAttributes.HEART_RATE_SERVICE);

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID
            .fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);


    private String mDeviceAddress;

    private ServiceConnection mBtServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
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
    private BluetoothLeService mBluetoothLeService;


    Button mStartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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
                            REQUEST_BT);
                } else {
                    btActions();
                }
            }
        });

    }

    private void bindHrmService(String deviceAddress) {
        mDeviceAddress = deviceAddress;
        bindService(new Intent(this, BluetoothLeService.class), mBtServiceConnection, BIND_AUTO_CREATE);
    }

    private void btActions() {

        // Initializes Bluetooth adapter.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        //displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(MainActivity.this, "Turn on bluetooth!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()");
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

//        initBtDeviceListener();

        List<BluetoothDevice> gattServerConnectedDevices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

        BluetoothDevice hrmDevice = getHrmDevice(gattServerConnectedDevices);
        if (hrmDevice != null) {
            Log.d(TAG, "btActions: GATT HRM device already connected");
            bindHrmService(hrmDevice.getAddress());
        } else {
            Log.d(TAG, "btActions: GATT HRM device not connected");
        }

        gattServerConnectedDevices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);

        hrmDevice = getHrmDevice(gattServerConnectedDevices);
        if (hrmDevice != null) {
            Log.d(TAG, "btActions: GATT_SERVER HRM device already connected");
            bindHrmService(hrmDevice.getAddress());
        } else {
            Log.d(TAG, "btActions: GATT_SERVER HRM device not connected");
        }


    }

    private void initBtDeviceListener() {
        // register for new GATT_SERVER connections
        BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                Log.d(TAG, "onServiceConnected: profile = " + profile);
                if (profile == BluetoothProfile.GATT) {
                    boolean deviceConnected = false;
                    BluetoothGattServer btGattServer = (BluetoothGattServer) proxy;

                    BluetoothDevice hrmDevice = getHrmDevice(btGattServer.getConnectedDevices());
                    if (hrmDevice != null) {
                        deviceConnected = true;
                        Log.d(TAG, "onServiceConnected: new HRM device connected");
                        bindHrmService(hrmDevice.getAddress());
                    }

                    if (!deviceConnected) {
                        Toast.makeText(MainActivity.this, "IT IS NOT HRM CONNECTED", Toast.LENGTH_SHORT).show();
                    }
//                    mBluetoothAdapter.closeProfileProxy(BluetoothProfile.GATT_SERVER, btGattServer);
                }
            }

            public void onServiceDisconnected(int profile) {

            }
        };

        if (mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.GATT)) {
            Log.d(TAG, "btActions: getProfileProxy = true");
        } else {
            Log.d(TAG, "btActions: getProfileProxy = false");
        }
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
                Log.d(TAG, "We have HRM device! Name/address" + device.getName() + "/" + device.getAddress() + "Uuid: " + currentUuid.toString());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BT) {
            if (grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                btActions();
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

    private class HrmServiceCallbackListener implements BluetoothLeService.CallbackListener {

        @Override
        public void onGattConnected() {
            Log.d(TAG, "onGattConnected: ");
        }

        @Override
        public void onGattDisconnected() {
            Log.d(TAG, "onGattDisconnected: ");
        }

        @Override
        public void onGattServicesDiscovered() {
            Log.d(TAG, "onGattServicesDiscovered: ");
        }

        @Override
        public void onNewPulseValueReceived(String payload) {
            Log.d(TAG, "onNewPulseValueReceived: pules = " + payload);
        }

        @Override
        public void onNewDataReceived(String payload) {
            Log.d(TAG, "onNewDataReceived: " + payload);
        }
    }
}
