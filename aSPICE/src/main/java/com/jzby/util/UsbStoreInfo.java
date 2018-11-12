package com.jzby.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.jzby.vmwork.LoginActivity;
import com.jzby.vmwork.RemoteCanvasActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class UsbStoreInfo extends BroadcastReceiver {

    public static native int SpiceUsbRedir(int productid, int vendorid);
    public static native int SpiceUsbDisRedir(int productid, int vendorid);
    public static native String SpiceGetUsbDeviceEvent(String var);
    private static final int port = 8091;
    private static final String TAG = "LSLONG";
    public static Boolean success = false;
    List<JZInputDevice> inputDevices = new ArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        switch (action) {
            case Intent.ACTION_MEDIA_MOUNTED: {
                Log.d(TAG,"FIND A NEW USB DEVICE");
                break;
            }
            case Intent.ACTION_MEDIA_UNMOUNTED: {
                Log.d(TAG,"THE USB UNMOUNT AND REMOVE");
                break;
            }
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                Log.d(TAG,"HARD DEVICE ATTACH!!!");
                getUSBListInfo(context);
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                Log.d(TAG,"HARD DEVICE DETACHED!!!");
                mvUSBDevice(context);
                break;
            default:
                break;
        }
    }

    //发送数据
    private void httpDataSend(String usbinform,String devClass,String devicename,int stat) {
        String status = null;
        String data = null;
        //获取用户名
        String username = LoginActivity.username;

        if(username!=null){
            //获取ip地址
            String ipaddress = getLocalIpAddress();
            //获取port
            //拼接字符串
            try {

                if(stat==1) {
                    status="add";
                }
                else{
                    status="remove";
                }
                data = String.format("{\"user\":\"%s\",\"value\":\"%s\",\"ip\":\"%s\"," +
                                "\"port\":%d,\"type\":\"%s\",\"status\":\"%s\",\"name\":\"%s\"}",
                        username, usbinform, ipaddress, port, devClass,status,devicename);
                JSONObject json = new JSONObject(data);
                Log.d(TAG,"the data :"+json);
                HttpHelper.sendPostRequest(json.toString());
            }catch (JSONException e){
                e.printStackTrace();
            }
        }else{
            Log.w(TAG,"请先登陆再使用USB功能");
        }
    }

    //获取device ip
    private static String getLocalIpAddress(){

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address))
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (SocketException ex){
            ex.printStackTrace();
        }
        return null;
    }

    private void AddInputDevices(JZInputDevice device_info)
    {
        int ix =0;
        if(inputDevices != null)
        {
            int size=inputDevices.size();
            if( size>0)
            {
                for(ix = 0;ix < size;ix++)
                {
                    if((device_info.getProductId() == inputDevices.get(ix).getProductId())&&
                            (inputDevices.get(ix).getVendorId() == device_info.getVendorId()))
                    {
                        Log.i(TAG, "id:"+device_info.getId());
                        inputDevices.get(ix).setId(device_info.getId());
                        return;
                    }
                }
            }
            inputDevices.add(device_info);
        }
    }

    private synchronized boolean checkDeviceByProductId(int vendorid) {
        if (inputDevices.size() == 0) {
            Log.e(TAG,"this list not data");
            return false;
        }

        for (int i = 0; i < inputDevices.size(); i++) {
            if (inputDevices.get(i).getVendorId() == vendorid)
            {
                Log.i(TAG, "this device haved informed:" + vendorid);
                return true;
            }
        }
        return false;
    }

    private void getUSBListInfo(Context context){
        int status = 1;
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
        Iterator<UsbDevice> iterator = devices.values().iterator();
        while (iterator.hasNext()) {
            UsbDevice device = iterator.next();
            if (device != null && device.getDeviceId() > 0) {
                int vendorid= device.getVendorId();
                //Log.i(TAG, "devicename:" + device.getDeviceName());
                //检查当前的设备是否已经报备
                if (!checkDeviceByProductId(vendorid)){
                    String devicename = device.getDeviceName();
                    JZInputDevice myDevice = new JZInputDevice();
                    myDevice.setName(devicename);
                    myDevice.setRedirectFlag(false);
                    myDevice.setProductId(device.getProductId());
                    myDevice.setId(device.getDeviceId());
                    myDevice.setVendorId(device.getVendorId());
                    AddInputDevices(myDevice);

                    int deviceClass = device.getDeviceClass();

                    String usbinform = vendorid+"";
                    String devClass = deviceClass+"";

                    //Log.i(TAG, "deviceclass:" + devClass);
                   // httpDataSend(usbinform,devClass,devicename,status);
                }
            }
        }
    }

    public static boolean sendUSBRepirRequest(final String varid, final String flag){
                final String status = flag;
                String username = LoginActivity.username;
                String ipaddress = getLocalIpAddress();
                String data = String.format("{\"user\":\"%s\",\"value\":\"%s\",\"ip\":\"%s\"," +
                                "\"port\":%d,\"status\":\"%s\"}",
                        username, varid, ipaddress, port, status);
                try {
                    JSONObject json = new JSONObject(data);
                    Log.d(TAG, "the data :" + json);
                    String res = HttpHelper.sendPostRequest(json.toString());

                    if (res.equals("true")) {
                        Log.d(TAG,"请求返回的结果为："+res);
                        if (status.equals("add")) {
                            Log.d(TAG, "startUsbRepir:"+Integer.parseInt(varid));
                            startUsbRepir(123, Integer.parseInt(varid));
                        } else if(status.equals("remove")) {
                            Log.d(TAG, "endUsbRepir:" + Integer.parseInt(varid));
                            endUsbRepir(123, Integer.parseInt(varid));
                        }
                        success = true;
                        return true;
                    } else {
                        Log.e(TAG, "send 无响应");
                        success = false;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return false;
    }

    private void mvUSBDevice(Context context){
        int n = 0;
        int nowdevice[] = new int[10];
        boolean isExist = false;
        if (inputDevices.size() == 0) {
            Log.e(TAG,"this list not data");
            return;
        }

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
        Iterator<UsbDevice> iterator = devices.values().iterator();
        while (iterator.hasNext()) {
            UsbDevice device = iterator.next();
            int vendorid = device.getVendorId();
            nowdevice[n]=vendorid;
            n++;
        }

        //通过vendorid检索出哪一个被拔掉了
        for (int i = 0; i < inputDevices.size(); i++) {
            int id = inputDevices.get(i).getVendorId();
            for(int j=0;j<=n;j++){
                if(id == nowdevice[j]){
                   // Log.d(TAG,"the id : "+id);
                    isExist=true;
                    break;
                }else {
                    isExist = false;
                }
            }
            if(!isExist) {
                String usbinform = inputDevices.get(i).getVendorId() + "";
                String devClass = inputDevices.get(i).getId() + "";
                String devicename = inputDevices.get(i).getName();
                //httpDataSend(usbinform, devClass, devicename,0);

                SharedPreferences userSettings = context.getSharedPreferences("setting", 0);
                SharedPreferences.Editor editor = userSettings.edit();
                editor.putBoolean(usbinform,false);
                Log.d(TAG,"the usbinform:"+usbinform);
                editor.commit();

                inputDevices.remove(i);
            }
        }

    }

    public static void startUsbRepir(final int v, final int p){
        Thread mythread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"-----start test-----:"+p+"------:"+v);
                SpiceUsbRedir(p,v);
                Log.d(TAG,"-----start test-----:"+p+"------:"+v);
            }
        });
        mythread.start();
    }

    public static void endUsbRepir(final int v, final int p){
        Thread mythread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"-----end test-----:"+p+"------:"+v);
                SpiceUsbDisRedir(p,v);
                Log.d(TAG,"-----end test-----:"+p+"------:"+v);
            }
        });
        mythread.start();
    }

    //根据vendor id判断设备是否存在




    /*
    private void getUSBHostName(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumeList", null);
            getVolumeList.setAccessible(true);

            Class<?> classStorageVolume = Class.forName("android.os.storage.StorageVolume");
            Method getPath = classStorageVolume.getDeclaredMethod("getPath", null);
            Method getLabel = classStorageVolume.getDeclaredMethod("getUserLabel", null);
            Object[] volumes = (Object[]) getVolumeList.invoke(storageManager, null);
            Log.i(TAG, "detectUSBName:" + volumes);
            if (volumes != null) {
                for (int i = 0; i < volumes.length; i++) {
                    String path = (String) getPath.invoke(volumes[i], null);
                    String label = (String) getLabel.invoke(volumes[i], null);
                    Log.i(TAG, "detectUSBName Path:" + path + " Label:" + label);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/
}
