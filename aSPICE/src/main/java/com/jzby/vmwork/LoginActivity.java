package com.jzby.vmwork;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jzby.util.HttpHelper;
import com.jzby.util.PreferenceUtil;
import com.jzby.util.TenantProcess;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;


public class LoginActivity extends Activity implements View.OnClickListener, TenantProcess.HttpProcessInterface {
    final String TAG = LoginActivity.class.getSimpleName();
    final static String CLOUD_DESKTOP_SETTINGS = "desktop_settings.json";
    TenantProcess tenantProcess;
    TextView tvLogin;
    TextView tvChangePwd;
    EditText etUsername;
    EditText etPwd;
    CheckBox cbKeepPwd;
    ImageView ivKeyboard;
    boolean keyboardFlag;
    boolean keepPwd = true;
    int tryCount = 0;
    int inputType;
    int inputPwdType;
    int inputLength;
    int inputPwdLength;
    private int str_version;
    private String str_appname;
    private String str_downurl;
    private String str_downcontent;
    /* 更新进度条 */
    private ProgressBar mProgress;
    private Dialog mDownloadDialog;
    /* 下载保存路径 */
    private String mSavePath;
    /* 记录进度条数量 */
    private int progress;
    /* 是否取消更新 */
    private boolean cancelUpdate = false;

    public static String username;
    String pwd;

    String server_ca_path="";
    String server_ca_password="";
    String server_ca_subject="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_login);
        //版本检查
        //Thread.setDefaultUncaughtExceptionHandler();
        checkUpdate();

        if (!TextUtils.isEmpty(TenantProcess.userId) && !TextUtils.isEmpty(TenantProcess.TOKEN_VALUE)) {
            Log.i(TAG,"=====already login=====");

            Intent intent = new Intent(this, RemoteCanvasActivity.class);
            startActivity(intent);

            this.finish();
        }
        tvLogin = (TextView) findViewById(R.id.btn_login);
        tvChangePwd = (TextView) findViewById(R.id.btn_pwd_forget);
        etUsername = (EditText) findViewById(R.id.etUsername);
        etPwd = (EditText) findViewById(R.id.etPassword);
        cbKeepPwd=(CheckBox) findViewById(R.id.cbKeepPwd);
        ivKeyboard = (ImageView) findViewById(R.id.iv_keyboard);
        ivKeyboard.setOnClickListener(this);
        inputType = etUsername.getInputType();
        inputPwdType = etPwd.getInputType();
        etUsername.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                etUsername.setInputType(InputType.TYPE_NULL);
                etUsername.onTouchEvent(event);
                etUsername.setInputType(inputType);
                inputLength = etUsername.getText().toString().length();
                etUsername.setSelection(inputLength);
                return true;
            }
        });

        etPwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                etPwd.setInputType(InputType.TYPE_NULL);
                etPwd.onTouchEvent(event);
                etPwd.setInputType(inputPwdType);
                inputPwdLength = etPwd.getText().toString().length();
                etPwd.setSelection(inputPwdLength);
                return true;
            }
        });

        String username = PreferenceUtil.getString(this, PreferenceUtil.USERNAME_KEY) + "";
        String pwd = PreferenceUtil.getString(this, PreferenceUtil.PASSWORD_KEY) + "";
        Log.i(TAG, "=====PreferenceUtil=====" + username + "=========" + pwd);
        if (!TextUtils.isEmpty(username)) {
            etUsername.setText(username);
        }

        if(!TextUtils.isEmpty(pwd))
        {
            etPwd.setText(pwd);
            cbKeepPwd.setChecked(true);
            keepPwd=true;
        }
        else
        {
            cbKeepPwd.setChecked(false);
            keepPwd=false;
        }

        tvLogin.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //tvLogin.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_btn_select));
                    tvLogin.setTextColor(Color.WHITE);
                } else {
                    //tvLogin.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_btn_unselect));
                    tvLogin.setTextColor(Color.BLACK);
                }
            }
        });

        tvChangePwd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b)
            {
                if (b) {
                    tvChangePwd.setTextColor(Color.WHITE);
                } else {
                    tvChangePwd.setTextColor(Color.BLACK);
                }
            }
        });

        tvLogin.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        tvLogin.setTextColor(Color.WHITE);
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        tvLogin.setTextColor(Color.BLACK);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });


        tvChangePwd.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View view, MotionEvent motionEvent)
            {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        tvChangePwd.setTextColor(Color.WHITE);
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        tvChangePwd.setTextColor(Color.BLACK);
                        break;
                    default:
                        break;
                }

                return false;
            }
        });

        findViewById(R.id.btn_login).setOnClickListener(this);
        findViewById(R.id.btn_pwd_forget).setOnClickListener(this);
        findViewById(R.id.cbKeepPwd).setOnClickListener(this);

        etUsername.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (!TextUtils.isEmpty(etUsername.getText() + "")) {
                        etUsername.setSelection(etUsername.getText().length());
                    }
                }
            }
        });

        etPwd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (!TextUtils.isEmpty(etPwd.getText() + "")) {
                        etPwd.setSelection(etPwd.getText().length());
                    }
                }
            }
        });
    }


    private void checkUpdate(){
        Thread checkThread = new Thread(new Runnable(){
            @Override
            public void run() {
                //add by lslong 2018/08/26
                if (isUpdate())
                {
                    // 显示提示对话框
                    mHandler.sendEmptyMessage(30002);
                } else
                {
                    mHandler.sendEmptyMessage(30003);
                }
            }
        });
        checkThread.start();
        try {
            checkThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示软件更新对话框
     */
    private final static String LSLTAG="LSLONG";
    private void showNoticeDialog()
    {
        // 构造对话框
      //  Log.d(LSLTAG,"show 1");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("应用更新");
        if(str_downcontent !=null) {
            builder.setMessage(str_downcontent);
        }else{
            builder.setMessage("有新的更新，需要立即更新吗");
        }
       // Log.d(LSLTAG,"show 2");
        // 更新
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                // 显示下载对话框
                showDownloadDialog();
            }
        });
        // 稍后更新
       // Log.d(LSLTAG,"show 3");
        builder.setNegativeButton("稍后更新", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        try {
           // Log.d(LSLTAG,"show 4");
            Dialog noticeDialog = builder.create();
            //Log.d(LSLTAG,"show 5");
            noticeDialog.show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 显示软件下载对话框
     */
    private void showDownloadDialog()
    {
        // 构造软件下载对话框
        Log.d(LSLTAG,"show 5");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("软件下载");
        // 给下载对话框增加进度条
        final LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.soft_update,null);
        mProgress = (ProgressBar) v.findViewById(R.id.update_progress);
        builder.setView(v);
        // 取消更新
        builder.setNegativeButton("取消更新", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                // 设置取消状态
                cancelUpdate = true;
            }
        });
        try {
            mDownloadDialog = builder.create();
            mDownloadDialog.show();
            Log.d(LSLTAG, "show 6");
            // 现在文件
        }catch (Exception e){
            Log.e(LSLTAG, "show error!!!");
            e.printStackTrace();
        }
        downloadApk();
    }

    /**
     * 下载apk文件
     */
    private void downloadApk()
    {
        // 启动新线程下载软件
        Thread downloadApkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    // 判断SD卡是否存在，并且是否具有读写权限
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                    {
                        // 获得存储卡的路径
                        String sdpath = Environment.getExternalStorageDirectory() + "/";
                        mSavePath = sdpath + "download";
                        Log.d(LSLTAG,"mSavePath = "+mSavePath);
                        URL url= new URL(str_downurl);
                        // 创建连接
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.connect();
                        // 获取文件大小
                        int length = conn.getContentLength();
                        // 创建输入流
                        InputStream is = conn.getInputStream();

                        File file = new File(mSavePath);
                        // 判断文件目录是否存在
                        if (!file.exists())
                        {
                            file.mkdir();
                        }
                        File apkFile = new File(mSavePath, str_appname);
                        FileOutputStream fos = new FileOutputStream(apkFile);
                        int count = 0;
                        // 缓存
                        byte buf[] = new byte[1024];
                        // 写入到文件中
                        do
                        {
                            int numread = is.read(buf);
                            count += numread;
                            // 计算进度条位置
                            progress = (int) (((float) count / length) * 100);
                            // 更新进度
                            mHandler.sendEmptyMessage(30000);
                            if (numread <= 0)
                            {
                                // 下载完成
                                mHandler.sendEmptyMessage(30001);
                                break;
                            }
                            // 写入文件
                            fos.write(buf, 0, numread);
                        } while (!cancelUpdate);// 点击取消就停止下载.
                        fos.close();
                        is.close();
                    }
                } catch (MalformedURLException e)
                {
                    e.printStackTrace();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                // 取消下载对话框显示
                mDownloadDialog.dismiss();
            }
        });
        downloadApkThread.start();
    }

    private boolean isUpdate(){
        // 获取当前软件版本
        int versionCode = getVersionCode(LoginActivity.this);
        Log.d("LSLONG","the versionCode is "+versionCode);
        //通过服务器获得版本号
        try {
            String str = HttpHelper.sendGetRequest();
            if(str!=null) {
                JSONObject json1 = new JSONObject(str);
                str_version = json1.getInt("version");
                str_appname = json1.getString("name");
                str_downurl = json1.getString("download");
                str_downcontent = json1.getString("content");
            }
            Log.d("LSLONG","the old versionCode is "+versionCode+" the new versionCode is "+str_version);
            if (str_version > versionCode) {
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取软件版本号
     *
     * @param context
     * @return
     */
    private int getVersionCode(Context context)
    {
        int versionCode = 0;
        try
        {
            // 获取软件版本号，对应AndroidManifest.xml下android:versionCode
            PackageManager packageManager = context.getPackageManager();
            versionCode = packageManager.getPackageInfo("com.jzby.vmwork",0).versionCode;
            //versionCode = context.getPackageManager().getPackageInfo("com.jzby.vmwork",  0).versionCode;
        } catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return versionCode;
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            Log.i(TAG,"=====handleMessage====="+msg.what);

            switch (msg.what) {

                case 10005:
                    //获取虚拟机列表和虚拟机状态成功
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    //当前用户的虚拟机只有一台并且是激活状态的时候直接连接虚拟机
                    ConnectionBean bean=TenantProcess.remoteServerList.get(msg.arg1);
                    String filename = getFilesDir()+server_ca_path;
                    bean.setPassword(server_ca_password);
                    bean.setCaCertPath(filename);
                    bean.setCertSubject(server_ca_subject);

                    TenantProcess.CONNECTION_TYPE=1;

                    Intent intent = new Intent(LoginActivity.this,RemoteCanvasActivity.class);
                    intent.putExtra(Constants.CONNECTION, bean.Gen_getValues());
                    startActivity(intent);
                    finish();
                    break;

                case 10000:
                    //登录云桌面成功
                    if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(pwd)) {
                        PreferenceUtil.setString(LoginActivity.this, PreferenceUtil.USERNAME_KEY, username);
                        if (keepPwd) {
                            PreferenceUtil.setString(LoginActivity.this, PreferenceUtil.PASSWORD_KEY, pwd);
                        } else {
                            PreferenceUtil.setString(LoginActivity.this, PreferenceUtil.PASSWORD_KEY, "");
                        }
                    }

                    //登录成功以后 获取用户的Project列表
                    tenantProcess.getProjectId();

                    try
                    {
                        //获取CA相关的设置信息
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

                    break;
                case 10001:
                    //登录云桌面失败 重试
                    if (tryCount >= 3) {
                        showText("username or password incorrect,please check it and try again later!");
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }
                        break;
                    }
                    tryCount++;

                    username = etUsername.getText() + "";
                    pwd = etPwd.getText() + "";
                    Log.i(TAG, "========" + username + "=========" + pwd);
                    TenantProcess.clearNativeCache();
                    if (tenantProcess == null) {
                        tenantProcess = TenantProcess.getInstance();
                    }
                    tenantProcess.setHttpProcessListener(LoginActivity.this);
                    tenantProcess.TenantProcess_GetInfolist(username, pwd);
                    break;

                case 10003:
                    //登录云桌面成功 但是获取虚拟机列表或者状态的时候失败
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    TenantProcess.CONNECTION_TYPE=-1;
                    intent = new Intent(LoginActivity.this, GridActivity.class);
                    startActivity(intent);
                    finish();
                    break;

                case 10002:
                    showText("can not connected to server,please check network and try again later!");
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    break;

                case 20000:
                    Log.i(TAG, "======settingsJson=====" + cloudSettingsJson);
                    try {
                        if (!TextUtils.isEmpty(cloudSettingsJson)) {
                            JSONObject object = new JSONObject(cloudSettingsJson);
                            String address = object.optString("server").trim();
                            if(!TenantProcess.SERVER.equalsIgnoreCase(address+":"))
                            {
                                TenantProcess.SERVER = (address + ":");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case 30000:
                    mProgress.setProgress(progress);
                    break;

                case 30001:
                    installApk();
                    break;
                case 30002:
                    showNoticeDialog();
                    break;
                case 30003:
                    showText("无需更新");
                break;
                default:
                    break;
            }
            return false;
        }
    });

    private String cloudSettingsJson = "";

    private void readCloudDesktopSettingsFromJsonFile() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
                    Log.i(TAG, "======path========" + dir + File.separator + CLOUD_DESKTOP_SETTINGS);
                    File file = new File(dir + File.separator + CLOUD_DESKTOP_SETTINGS);
                    if (file.exists()) {
                        try {
                            FileInputStream inputStream = new FileInputStream(file);
                            byte[] bytes = new byte[1024 * 3];
                            int length = -1;
                            while ((length = inputStream.read(bytes)) != -1) {
                                cloudSettingsJson += new String(bytes, 0, length);
                            }
                            Log.i(TAG, "====readContent=====" + cloudSettingsJson);
                            inputStream.close();

                            mHandler.sendEmptyMessage(20000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        readCloudDesktopSettingsFromJsonFile();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.i(TAG, "===dispatchKeyEvent()=====" + event.getKeyCode());
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            Log.i(TAG, "===dispatchKeyEvent()==onKeyDown=====" + event.getKeyCode());
            Log.i(TAG, "===getMetaState=====" + event.getMetaState());
            switch (event.getKeyCode()) {

                case KeyEvent.KEYCODE_NUMPAD_ENTER:

                    //KeyEvent.META_NUM_LOCK_ON
                    if((event.getMetaState() & 0x00200000)==0)
                    {
                        Log.i(TAG,"====Num pad off====");
                        return true;
                    }
                case KeyEvent.KEYCODE_ENTER:
                    username = etUsername.getText() + "";
                    pwd = etPwd.getText() + "";
                    tvLogin.requestFocus();
                    if (TextUtils.isEmpty(username) || TextUtils.isEmpty(pwd)) {
                        showText("username or pwd is empty!");
                        break;
                    }

                    if(!checkNetwork())
                    {
                        showText("no available network,please check it and try again later!");
                        break;
                    }

                    tryCount = 0;
                    mHandler.sendEmptyMessage(10001);
                    showLoadingDialog();

                    return true;
                default:
                    break;
            }
        }


        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pwd_forget:

                Intent intent=new Intent(this,ChangePwdActivity.class);
                startActivity(intent);

                break;

            case R.id.btn_login:

                username = etUsername.getText() + "";
                pwd = etPwd.getText() + "";
                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(pwd)) {
                    showText("username or pwd is empty!");
                    break;
                }

                if(!checkNetwork())
                {
                    showText("no available network,please check it try again later!");
                    break;
                }

                tryCount = 0;
                mHandler.sendEmptyMessage(10001);
                showLoadingDialog();
                break;
            case R.id.cbKeepPwd:

                keepPwd = !keepPwd;

                break;
            case R.id.iv_keyboard:
                if (keyboardFlag) {
                    etUsername.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            etUsername.setInputType(InputType.TYPE_NULL);
                            etUsername.onTouchEvent(event);
                            etUsername.setInputType(inputType);
                            inputLength = etUsername.getText().toString().length();
                            etUsername.setSelection(inputLength);
                            return true;
                        }
                    });
                    etPwd.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            etPwd.setInputType(InputType.TYPE_NULL);
                            etPwd.onTouchEvent(event);
                            etPwd.setInputType(inputPwdType);
                            inputPwdLength = etPwd.getText().toString().length();
                            etPwd.setSelection(inputPwdLength);
                            return true;
                        }
                    });

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                        ivKeyboard.setImageDrawable(getResources().getDrawable(R.drawable.keyboard_close));
                    } else {

                    }
                } else {
                    etUsername.setOnTouchListener(null);
                    etPwd.setOnTouchListener(null);
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                        ivKeyboard.setImageDrawable(getResources().getDrawable(R.drawable.keyboard_open));
                    } else {

                    }
                }
                etUsername.requestFocus();
                keyboardFlag = !keyboardFlag;
                break;
            default:
                break;
        }
    }

    AlertDialog loadingDialog;

    private void showLoadingDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);

        View root = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null);
        loadingDialog = mBuilder.create();
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setCancelable(true);
        loadingDialog.show();
        loadingDialog.setContentView(root);
    }

    private boolean checkNetwork()
    {
        ConnectivityManager connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            Log.i(TAG, "===Network===Connected====");
            return true;
        } else {
            Log.i(TAG, "===Network===DisConnected====");
        }
        return false;
    }


    public void openKeybord(EditText mEditText, Context mContext) {
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditText, InputMethodManager.RESULT_SHOWN);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }


    public void closeKeybord(EditText mEditText, Context mContext) {
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        //tenantProcess.setHttpProcessListener(null);
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onHttpFailed(String url, int httpCode) {
        Log.i(TAG, "======onHttpFailed()=======" + url);
        if (url.contains(TenantProcess.URL_GET_USERID))
        {
            if(httpCode==404)
            {
                mHandler.sendEmptyMessage(10002);
            }
            else
            {
                mHandler.sendEmptyMessageDelayed(10001, 5000);
            }
        }
        else
        {
            mHandler.sendEmptyMessage(10003);
        }
    }

    @Override
    public void onHttpSuccess(String url) {
        int count = 0;
        int index = 0;
        Log.i(TAG, "======onHttpSuccess()=======" + url);
        if (url.contains(TenantProcess.URL_GET_USERID)) {
            if(TextUtils.isEmpty(TenantProcess.projectId))
            {
                mHandler.sendEmptyMessage(10000);
            }
            else
            {
                Log.i(TAG,"====get server token again========");
            }
        }
        else if(url.contains(TenantProcess.URL_GET_SERVER_ADDRESS_A) && url.contains(TenantProcess.URL_GET_SERVER_ADDRESS_B))
        {
            //mHandler.sendEmptyMessage(10005);
        }
        else if(url.contains(TenantProcess.URL_GET_SERVER_LIST_A) && url.contains(TenantProcess.URL_GET_SERVER_LIST_B))
        {
            if(TenantProcess.remoteServerList!=null && TenantProcess.remoteServerList.size()>1)
            {
                Log.i(TAG,"======remoteServerList======"+TenantProcess.remoteServerList.size());
                mHandler.sendEmptyMessage(10003);
            }
        }
        else if(url.contains(TenantProcess.URL_GET_PROJECTID))
        {
            if(TenantProcess.remoteServerList!=null)
            {
                mHandler.sendEmptyMessage(10003);
            }
        }
        else
        {

        }
    }

    /**
     * 安装APK文件
     */
    private void installApk()
    {
        File apkfile = new File(mSavePath, str_appname);
        if (!apkfile.exists())
        {
            return;
        }
        // 通过Intent安装APK文件
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
        this.startActivity(i);
    }


    private void showText(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
