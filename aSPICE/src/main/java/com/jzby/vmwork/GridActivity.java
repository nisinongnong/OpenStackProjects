package com.jzby.vmwork;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jzby.util.TenantProcess;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.jzby.util.TenantProcess.remoteServerList;

/**
 * Created by T530 on 2017/5/18.
 */

public class GridActivity extends Activity implements ServerAdapter.ServerAdapterInterface,
        TenantProcess.HttpProcessInterface,View.OnClickListener
{
    final static String TAG=GridActivity.class.getSimpleName();

    ListView listView;

    //ProgressBar pbServer;

    //TextView tvPbValue;

    List<ConnectionBean> mServers;

    ServerAdapter adapter;

    boolean refreshDataFlag;
    int tryCount,detailsTryCount;

    String server_ca_path="";
    String server_ca_password="";
    String server_ca_subject="";

    //int progress=10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gird_view);
        Log.i(TAG,"========onCreate()==========");
        if(TextUtils.isEmpty(TenantProcess.userId) || TextUtils.isEmpty(TenantProcess.TOKEN_VALUE))
        {
            Log.i(TAG,"===========has not authorized yet!==============");
            Intent intent=new Intent(this,LoginActivity.class);
            startActivity(intent);
            this.finish();
        }
        refreshDataFlag=true;
        tenantProcess=TenantProcess.getInstance();
        tenantProcess.setHttpProcessListener(this);

        //tenantProcess.getProjectId();

        listView=(ListView)findViewById(R.id.lv_server_list);
       /* pbServer=(ProgressBar)findViewById(R.id.pb_server);
        tvPbValue=(TextView)findViewById(R.id.tv_pb_value);*/

        if(TenantProcess.remoteServerList!=null && TenantProcess.remoteServerList.size()>0)
        {
            mServers=new ArrayList<>();
            mServers.addAll(TenantProcess.remoteServerList);
            adapter=new ServerAdapter(GridActivity.this, mServers);
            adapter.setAdapterListener(GridActivity.this);
            listView.setAdapter(adapter);
            listView.setItemsCanFocus(true);
        }
        else
        {
            showLoadingDialog();
        }

        try
        {
            ApplicationInfo applicationInfo=getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            server_ca_password=applicationInfo.metaData.getString("server_ca_password");
            server_ca_path=applicationInfo.metaData.getString("server_ca_path");
            server_ca_subject=applicationInfo.metaData.getString("server_ca_subject");
            Log.i(TAG,"====="+server_ca_password+"====="+server_ca_path+"======="+server_ca_subject);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        checkList();
    }

    private void checkList() {
        Thread checkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int index = 0;
                int count = 0;
                if(TenantProcess.remoteServerList!=null){
                    for(int i = 0;i<TenantProcess.remoteServerList.size();i++)
                    {
                        if(Constants.SERVER_STATUS_ACTIVE.equals(TenantProcess.remoteServerList.get(i).getSshPrivKey())){
                            index = i;
                            count++;
                        }
                    }
                    if(count==1){
                        Message message = new Message();
                        message.what = 10006;
                        message.arg1 = index;
                        mHandler.sendMessage(message);
                    }
                }
            }
        });
        checkThread.start();
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        Log.i(TAG,"========onResume()=========="+refreshDataFlag);
        if(!refreshDataFlag)
        {
            tryCount=1;
            //showLoadingDialog();
            //tenantProcess.refreshServerList2();
        }
        refreshDataFlag=false;

        if(adapter!=null)
        {
            adapter.setClickFlag(false);
        }
    }

    Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg)
        {
            Log.i(TAG,"=========handleMessage()=========="+msg.what);
            switch (msg.what)
            {
                case 10001:
                    if(loadingDialog!=null && loadingDialog.isShowing())
                    {
                        loadingDialog.dismiss();
                    }
                    break;
                case 10000:
                    if(loadingDialog!=null && loadingDialog.isShowing())
                    {
                        loadingDialog.dismiss();
                    }

                    mServers=new ArrayList<>();
                    mServers.addAll(TenantProcess.remoteServerList);

                    if(adapter==null)
                    {
                        adapter=new ServerAdapter(GridActivity.this, mServers);
                        adapter.setAdapterListener(GridActivity.this);
                        listView.setAdapter(adapter);
                        listView.setItemsCanFocus(true);
                        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                            {
                                Log.i(TAG,"=======onItemSelected()========"+position);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });

                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                            {
                                Log.i(TAG,"=======onItemClick()========"+position);
                            }
                        });
                    }
                    else
                    {
                        adapter.notifyDataChanged(mServers);
                    }
                    break;
                case 10002:
                    tenantProcess.refreshServerStatus(selectedPosition,portFlag);
                    break;
                case 10003:
                    if(tryCount>=3)
                    {
                        if(loadingDialog!=null && loadingDialog.isShowing())
                        {
                            loadingDialog.dismiss();
                        }
                        showText("gain server list failed,please login again!");
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run()
                            {
                                Intent intent=new Intent(GridActivity.this,LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        },5000);
                        break;
                    }
                    tryCount++;
                    tenantProcess.refreshServerList();
                    break;
                case 10005:
//                    progress+=10;
//                    pbServer.setProgress(progress);
//                    tvPbValue.setText(progress+"%");
//                    mHandler.sendEmptyMessageDelayed(10005,1000);
                    break;
                case 10006:
                    execRemoteServer(msg.arg1,"connect");
                    break;
                default:break;
            }

            return false;
        }
    });

    AlertDialog loadingDialog;

    private void showLoadingDialog()
    {
        AlertDialog.Builder mBuilder=new AlertDialog.Builder(this);

        View root= LayoutInflater.from(this).inflate(R.layout.loading_dialog,null);
        loadingDialog=mBuilder.create();
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setCancelable(true);
        if(this.isFinishing())
        {
            Log.i(TAG,"========Activity is finishing and return=========");
            return;
        }
        loadingDialog.show();
        loadingDialog.setContentView(root);
    }

    AlertDialog confirmDialog;
    private void showConfirmDialog()
    {
        AlertDialog.Builder mBuilder=new AlertDialog.Builder(this,R.style.dialog_style_2);
        View root= LayoutInflater.from(this).inflate(R.layout.tips_dialog,null);

        final TextView tvConfirm=(TextView)root.findViewById(R.id.tv_dialog_ok);
        final TextView tvCancel=(TextView)root.findViewById(R.id.tv_dialog_cancel);
        tvConfirm.setOnClickListener(this);
        tvConfirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if(hasFocus)
                {
                    tvConfirm.setTextColor(Color.WHITE);
                }
                else
                {
                    tvConfirm.setTextColor(Color.BLACK);
                }

            }
        });
        tvConfirm.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        tvConfirm.setTextColor(Color.WHITE);
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        tvConfirm.setTextColor(Color.BLACK);
                        break;
                    default:break;
                }
                return false;
            }
        });
        tvCancel.setOnClickListener(this);
        tvCancel.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if(hasFocus)
                {
                    tvCancel.setTextColor(Color.WHITE);
                }
                else
                {
                    tvCancel.setTextColor(Color.BLACK);
                }

            }
        });
        tvCancel.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        tvCancel.setTextColor(Color.WHITE);
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        tvCancel.setTextColor(Color.BLACK);
                        break;
                    default:break;
                }
                return false;
            }
        });
        confirmDialog=mBuilder.create();
        confirmDialog.setCanceledOnTouchOutside(false);
        confirmDialog.setCancelable(true);
        confirmDialog.show();
        confirmDialog.setContentView(root);
        tvCancel.requestFocus();
    }


    private void showText(String text)
    {
        Toast.makeText(this,text,Toast.LENGTH_SHORT).show();
    }

    private void startRemoveServer(int position)
    {
        ConnectionBean bean= remoteServerList.get(position);
        String filename = getFilesDir()+server_ca_path;
        bean.setPassword(server_ca_password);
        bean.setCaCertPath(filename);
        bean.setCertSubject(server_ca_subject);
        /*// Write out CA to file if it doesn't exist.
        String caCertData = bean.getCaCert();
        try {
            // If a cert has been set, write out a unique file containing the cert and save the path to that file to give to libspice.
            String filename = getFilesDir() + "/ca" + Integer.toString(bean.getCaCert().hashCode()) + ".pem";
            bean.setCaCertPath(filename);
            File file = new File(filename);
            if (!file.exists() && !caCertData.equals("")) {
                Log.e(TAG, filename);
                PrintWriter fout = new PrintWriter(filename);
                fout.println(bean.getCaCert().toString());
                fout.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/

        Intent intent = new Intent(this, RemoteCanvasActivity.class);
        intent.putExtra(Constants.CONNECTION, bean.Gen_getValues());
        startActivity(intent);
    }


    @Override
    public void onBackPressed()
    {
        Log.i(TAG,"======onBackPressed()=======");
        showConfirmDialog();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.i(TAG,"======onPause()=======");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"======onStop()=======");
    }

    @Override
    protected void onDestroy()
    {
        updateAndroidCursor();
        mHandler.removeCallbacksAndMessages(null);
        tenantProcess.setHttpProcessListener(null);
        TenantProcess.clearNativeCache();
        Process.killProcess(Process.myPid());
        super.onDestroy();
    }

    TenantProcess tenantProcess;

    boolean portFlag;

    int selectedPosition;

    @Override
    public void execRemoteServer(int position, String action)
    {
        selectedPosition=position;
        Log.i(TAG,"=======execRemoteServer()========"+position+"========"+action);
        if("connect".equals(action))
        {
            startRemoveServer(position);
        }
        else if("start".equals(action))
        {
            portFlag=true;
            tenantProcess.startRemoteServer(selectedPosition);
        }
        else if("restart".equals(action))
        {
            portFlag=true;
            tenantProcess.restartRemoteServer(selectedPosition,2);
        }
        else if("shutdown".equals(action))
        {
            portFlag=false;
            tenantProcess.shutdownRemoteServer(selectedPosition);
        }
        else if("reset".equals(action))
        {
            portFlag=false;
            tenantProcess.resetRemoteServer(selectedPosition);
        }
        else
        {

        }
    }

    @Override
    public void onHttpSuccess(String url)
    {
        Log.i(TAG,"======onHttpSuccess()========="+url);
        if(url.contains(TenantProcess.URL_ACTION_SERVER_START))
        {
            detailsTryCount=1;
            mHandler.sendEmptyMessageDelayed(10002,3000);
        }
        else if(url.contains(TenantProcess.URL_GET_SERVER_ADDRESS_A))
        {
            mHandler.sendEmptyMessage(10000);
        }
        else if(url.contains(TenantProcess.URL_GET_SERVER_ADDRESS_B))
        {

        }
        else if(url.contains(TenantProcess.URL_GET_SERVER_LIST_B))
        {
            mHandler.sendEmptyMessage(10000);
        }
        else if(url.contains(TenantProcess.URL_ACTION_RESET))
        {
            //mHandler.sendEmptyMessage(10000);
            mHandler.sendEmptyMessageDelayed(10000,3000);
        }
        else
        {

        }
    }

    @Override
    public void onHttpFailed(String url, int httpCode)
    {
        Log.i(TAG,"======onHttpFailed()========="+url);
        if(url.contains(TenantProcess.URL_ACTION_SERVER_START))
        {
            //mHandler.sendEmptyMessage(10001);
            mHandler.sendEmptyMessageDelayed(10002,3000);
        }
        else if(url.contains(TenantProcess.URL_GET_SERVER_ADDRESS_A))
        {
            if(url.contains(TenantProcess.URL_GET_SERVER_ADDRESS_B))
            {
                detailsTryCount++;
                if(detailsTryCount<=3)
                {
                    Log.i(TAG,"=====gain port failed will query again=====");
                    mHandler.sendEmptyMessageDelayed(10002,1500);
                }
                else
                {
                    Log.i(TAG,"=====gain port failed bigger than 3 and return=====");
                    mHandler.sendEmptyMessage(10001);
                }
            }
            else
            {
                mHandler.sendEmptyMessage(10001);
            }
        }
        else if(url.contains(TenantProcess.URL_GET_SERVER_LIST_B))
        {
            mHandler.sendEmptyMessageDelayed(10003,5000);
        }
        else if(url.contains(TenantProcess.URL_ACTION_RESET))
        {
            mHandler.sendEmptyMessage(10000);
        }
        else
        {

        }
    }

    private void updateAndroidCursor()
    {
        try
        {
            Bitmap bitmap= BitmapFactory.decodeResource(getResources(),R.drawable.pointer_arrow);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] bytes=baos.toByteArray();
            baos.close();

            Intent intent=new Intent("KeyAndMouseSetting.ACTION_SET_MOUSE_ICON");
            intent.putExtra("hotspotX",0f);
            intent.putExtra("hotspotY",0f);
            Bundle data=new Bundle();
            data.putByteArray("bitmap",bytes);
            intent.putExtras(data);
            sendBroadcast(intent);

            bitmap.recycle();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.tv_dialog_ok:
                if(confirmDialog!=null && confirmDialog.isShowing())
                {
                    confirmDialog.dismiss();
                }
                this.finish();
                break;
            case R.id.tv_dialog_cancel:
                if(confirmDialog!=null && confirmDialog.isShowing())
                {
                    confirmDialog.dismiss();
                }
                break;
            default:break;
        }
    }

    //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            getWindow().getDecorView().setPointerIcon(PointerIcon.getSystemIcon(this, PointerIcon.TYPE_NULL));
//        }


}
