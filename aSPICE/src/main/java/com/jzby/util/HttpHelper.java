package com.jzby.util;

import android.text.TextUtils;
import android.util.Log;

import com.jzby.vmwork.RemoteCanvasActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.jar.JarException;

import static java.lang.Thread.sleep;

public class HttpHelper {
    private static final int REQUEST_TIMEOUT = 5 * 1000;
    private static final int READ_TIMEOUT = 30 * 1000;
    private static final String USB_URL = "http://10.7.14.40:8080/hello_world";
    private static final String UPDATE_URL = "http://10.7.14.40:80/appversion";
    private static final int PORT = 8091;

    public static String sendGetRequest() {
        String result;
        String msg = null;
        result = doGet(UPDATE_URL,msg);
        return result;
    }

    public static String sendPostRequest(String msg) {
        String result;
        result = doGet(USB_URL,msg);
        return result;
    }

    public static String doGet(String address,String msg) {
        try {
            //1,找水源--创建URL
            URL url = new URL(address);//放网站
            //2,开水闸--openConnection
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            //3，设置连接超时
            httpURLConnection.setConnectTimeout(REQUEST_TIMEOUT);
            //4，设置读取超时
            httpURLConnection.setReadTimeout(READ_TIMEOUT);
            if(msg!=null){
                //5，设置为post请求
                httpURLConnection.setRequestMethod("POST");
            }else
            {
                //5，设置为get请求
                httpURLConnection.setRequestMethod("GET");
            }

            if (msg != null && !TextUtils.isEmpty(msg)) {
                byte[] writebytes = msg.getBytes();
                // 设置文件长度
                httpURLConnection.setRequestProperty("Content-Length", String.valueOf(writebytes.length));
                OutputStream outwritestream = httpURLConnection.getOutputStream();
                outwritestream.write(msg.getBytes());
                outwritestream.flush();
                outwritestream.close();
            }
            //6，进行连接
           // httpURLConnection.connect();
            //7，获取结果
            int code = httpURLConnection.getResponseCode();
            Log.d("LSLONG", "the code is " + code);

            if (code == 200) {
                //5，建管道--InputStream
                InputStream inputStream = httpURLConnection.getInputStream();
                Log.i("LSLONG", "message length:" + httpURLConnection.getContentLength());
                //6，建蓄水池蓄水-InputStreamReader
                InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                //7，水桶盛水--BufferedReader
                BufferedReader bufferedReader = new BufferedReader(reader);

                StringBuffer buffer = new StringBuffer();
                String temp = null;

                while ((temp = bufferedReader.readLine()) != null) {
                    //取水--如果不为空就一直取
                    buffer.append(temp);
                }
                bufferedReader.close();//记得关闭
                reader.close();
                inputStream.close();
                String res = buffer.toString();
                Log.d("LSLONG", res);//打印结果
                return res;
            } else {
                Log.e("LSLONG", "the code is " + code + " . so close session!!!!");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void datacreate(Socket socket){
        BufferedReader br = null;
        BufferedWriter bw = null;
        String line=null;
        int vendorid = 0;
        int productid = 0;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            while ((line = br.readLine()) != null) {
                line = line.trim();
                Log.d("LSLONG","the data:" + line);
                //根据数据下发调用
                if (line!=null) {
                    vendorid = getVendorId(line);
                    if (vendorid!=0){
                        UsbStoreInfo.SpiceUsbRedir(productid,vendorid);
                    }
                }

                String result = "200 OK";
                bw.write(result+"\n");
                bw.newLine();
                bw.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static int getVendorId(String line) {
        int vendorId = 0;
        try {
            JSONObject myJson = new JSONObject(line);
            vendorId = myJson.getInt("value");
        }catch (JSONException e){
            e.printStackTrace();
        }
        return vendorId;
    }

    public static void startServer(){
        Thread usbServer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                {
                    /*
                    try {
                        sleep(2);
                        Log.d("LSLONG","this is server test!!!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    ServerSocket ss=null;
                    try {
                        ss = new ServerSocket(PORT);
                        while (true) {
                            Socket s = ss.accept();
                            s.setKeepAlive(true);
                            datacreate(s);
                        }
                    } catch (Exception e) {
                        if(ss != null) {
                            try {
                                ss.close();
                            } catch (IOException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        usbServer.start();
    }

}