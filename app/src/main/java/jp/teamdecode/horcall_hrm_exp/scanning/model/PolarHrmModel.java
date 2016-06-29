package jp.teamdecode.horcall_hrm_exp.scanning.model;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

/**
 * Created by Akadem on 29.06.2016.
 */
public class PolarHrmModel implements Serializable {
    private BluetoothDevice mDevice;
    private boolean isBounded;

    public PolarHrmModel(BluetoothDevice device) {
        mDevice = device;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void setDevice(BluetoothDevice device) {
        mDevice = device;
    }

    public boolean isBounded() {
        return isBounded;
    }

    public PolarHrmModel setBounded(boolean bounded) {
        isBounded = bounded;
        return this;
    }
}
