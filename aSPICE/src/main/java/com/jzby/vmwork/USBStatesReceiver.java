package com.jzby.vmwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class USBStatesReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                Log.d("LSLONG","get usb device connect");
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                Log.d("LSLONG","get usb device disconnect");
                break;
            case Intent.ACTION_MEDIA_MOUNTED:
                break;
            case Intent.ACTION_MEDIA_UNMOUNTED:
                break;
            default:
                break;
        }
    }

}