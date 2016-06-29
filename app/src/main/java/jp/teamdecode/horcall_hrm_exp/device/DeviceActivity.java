package jp.teamdecode.horcall_hrm_exp.device;

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
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import jp.teamdecode.horcall_hrm_exp.MyBluetoothLeService;
import jp.teamdecode.horcall_hrm_exp.R;
import jp.teamdecode.horcall_hrm_exp.SampleGattAttributes;
import jp.teamdecode.horcall_hrm_exp.device.adapter.LogRecyclerAdapter;
import jp.teamdecode.horcall_hrm_exp.scanning.model.PolarHrmModel;

public class DeviceActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    public static final String CALLING_EXTRA_HRM = "hrm";

    public static Intent getCallingIntent(Context context, PolarHrmModel hrm) {
        Intent intent = new Intent(context, DeviceActivity.class);
        intent.putExtra(CALLING_EXTRA_HRM, hrm);
        return intent;
    }

    private final int REQUEST_BT = 3;
    private final int REQUEST_WRITE_EXTERNAL_STORAGE = 4;

    private BluetoothAdapter mBluetoothAdapter;

    private PolarHrmModel mHrm;

    private ServiceConnection mBtServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            MyBluetoothLeService.LocalBinder binder = (MyBluetoothLeService.LocalBinder) service;
            mBluetoothLeService = binder.getService();
            mBluetoothLeService.connect(mHrm.getDevice().getAddress());
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
    LogRecyclerAdapter mRecyclerAdapter;
    RecyclerView mRecyclerView;
    Button mLogCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mHrm = (PolarHrmModel) getIntent().getSerializableExtra(CALLING_EXTRA_HRM);

        mStartButton = (Button) findViewById(R.id.start);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bindHrmService(mHrm);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.logs);
        mLogCopy = (Button) findViewById(R.id.clip_log);
        mLogCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(DeviceActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Check Permissions Now
                    ActivityCompat.requestPermissions(DeviceActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    shareLogfile();
                }
            }
        });

        initRecyclerVew();
    }

    private void initRecyclerVew() {
        mRecyclerAdapter = new LogRecyclerAdapter();
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


    private void bindHrmService(PolarHrmModel hrm) {
        mRecyclerAdapter.addLog("bindHrmService: deviceAddress: " + hrm.getDevice().getAddress());
        bindService(new Intent(this, MyBluetoothLeService.class), mBtServiceConnection, BIND_AUTO_CREATE);
    }


    private void connectBtnAction() {
        // Initializes Bluetooth adapter.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        //displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(DeviceActivity.this, "Turn on bluetooth!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()");
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BT) {
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
