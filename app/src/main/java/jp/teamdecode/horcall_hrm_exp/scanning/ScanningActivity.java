package jp.teamdecode.horcall_hrm_exp.scanning;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jp.teamdecode.horcall_hrm_exp.R;
import jp.teamdecode.horcall_hrm_exp.SampleGattAttributes;
import jp.teamdecode.horcall_hrm_exp.device.DeviceActivity;
import jp.teamdecode.horcall_hrm_exp.scanning.adapter.HrmRecyclerAdapter;
import jp.teamdecode.horcall_hrm_exp.scanning.model.PolarHrmModel;

/**
 * Created by Akadem on 29.06.2016.
 */
public class ScanningActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning;
    private Handler mHandler;
    private Set<BluetoothDevice> mPairedDevices = new HashSet<>();

    private final int REQUEST_LE_SCAN_BT = 2;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    public final static UUID UUID_HEART_RATE_SERVICE = UUID
            .fromString(SampleGattAttributes.HEART_RATE_SERVICE);

    public final static UUID[] HRM_UUIDS = {UUID_HEART_RATE_SERVICE};

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d(TAG, "onLeScan: HRM founded: Name: " + device.getName() + "; Address: " + device.getAddress());
                    mRecyclerAdapter.addHrm(new PolarHrmModel(device).setBounded(isBoundedDevice(device)));
                }
            };

    Button mStopScanning;
    RecyclerView mRecyclerView;
    HrmRecyclerAdapter mRecyclerAdapter;

    MenuItem mScanProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);

        mHandler = new Handler();

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mStopScanning = (Button) findViewById(R.id.stop);
        mStopScanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        });

        if (ActivityCompat.checkSelfPermission(ScanningActivity.this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(ScanningActivity.this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(ScanningActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(ScanningActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                ) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(ScanningActivity.this,
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_LE_SCAN_BT);
        } else {
            scanBtActions();
        }

        initRecyclerView();

    }

    private void initRecyclerView() {
        mRecyclerAdapter = new HrmRecyclerAdapter();
        mRecyclerAdapter.setClickListener(new HrmRecyclerAdapter.HrmClickListener() {
            @Override
            public void onHrmClick(PolarHrmModel hrm) {
                startActivity(DeviceActivity.getCallingIntent(ScanningActivity.this, hrm));
            }
        });
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private void scanBtActions() {

        // Initializes Bluetooth adapter.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mPairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d(TAG, "mPairedDevices count: " + mPairedDevices.size());

        for (BluetoothDevice device : mPairedDevices) {
            Log.d(TAG, "mPairedDevices: Name: " + device.getName() + "; address: " + device.getAddress() + "; Bond state: " + device.getBondState());
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        //displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(ScanningActivity.this, "Turn on bluetooth!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()");
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            scanLeDevice(true);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    invalidateOptionsMenu();
//                }
//            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(HRM_UUIDS, mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private boolean isBoundedDevice(BluetoothDevice device) {
        for (BluetoothDevice bounded : mPairedDevices) {
            if (bounded.getAddress().equals(device.getAddress())) return true;
        }
        return false;
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
        if (requestCode == REQUEST_LE_SCAN_BT) {
            if (grantResults.length == 4
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED
                    && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                scanBtActions();
            } else {
                // Permission was denied or request was cancelled
                Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show();
            }
        }
    }

}
