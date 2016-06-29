package jp.teamdecode.horcall_hrm_exp.scanning.model;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Akadem on 29.06.2016.
 */
public class PolarHrmModel implements Parcelable {
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
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PolarHrmModel that = (PolarHrmModel) o;

        return mDevice.equals(that.mDevice);
    }

    @Override
    public int hashCode() {
        return mDevice.hashCode();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mDevice, flags);
        dest.writeByte(this.isBounded ? (byte) 1 : (byte) 0);
    }

    protected PolarHrmModel(Parcel in) {
        this.mDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        this.isBounded = in.readByte() != 0;
    }

    public static final Creator<PolarHrmModel> CREATOR = new Creator<PolarHrmModel>() {
        @Override
        public PolarHrmModel createFromParcel(Parcel source) {
            return new PolarHrmModel(source);
        }

        @Override
        public PolarHrmModel[] newArray(int size) {
            return new PolarHrmModel[size];
        }
    };
}
