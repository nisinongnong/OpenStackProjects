/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

//
// CanvasView is the Activity for showing VNC Desktop.
//
package com.jzby.vmwork;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.jzby.util.TenantProcess;
import com.jzby.util.UsbStoreInfo;
import com.jzby.vmwork.dialogs.EnterTextDialog;
import com.jzby.vmwork.dialogs.MetaKeyDialog;
import com.jzby.vmwork.input.AbstractInputHandler;
import com.jzby.vmwork.input.Panner;
import com.jzby.vmwork.input.SimulatedTouchpadInputHandler;
import com.jzby.vmwork.input.SingleHandedInputHandler;
import com.jzby.vmwork.input.TouchMouseDragPanInputHandler;
import com.jzby.vmwork.input.TouchMouseSwipePanInputHandler;

import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

@SuppressWarnings("deprecation")
public class RemoteCanvasActivity extends Activity implements OnKeyListener,OnClickListener {
    private final static String TAG = "RemoteCanvasActivity";
    private final static String LSLTAG = "LSLONG";
    //protected Context context;
    //yxlei 输入方式控制
    AbstractInputHandler inputHandler;
    LinearLayout llContent;
    DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private RemoteCanvas canvas;

    private Database database;

   // private static native int SpiceUsbRedir(int productid, int vendorid);

    //private MenuItem[] inputModeMenuItems;
    //    private MenuItem[] scalingModeMenuItems;
    private AbstractInputHandler inputModeHandlers[];
    private ConnectionBean connection;
    /*    private static final int inputModeIds[] = { R.id.itemInputFitToScreen,
            R.id.itemInputTouchpad,
            R.id.itemInputMouse, R.id.itemInputPan,
            R.id.itemInputTouchPanTrackballMouse,
            R.id.itemInputDPadPanTouchMouse, R.id.itemInputTouchPanZoomMouse };
     */
    private static final int inputModeIds[] = {R.id.itemInputTouchpad,
            R.id.itemInputTouchPanZoomMouse,
            R.id.itemInputDragPanZoomMouse,
            R.id.itemInputSingleHanded};

    Panner panner;
    //    SSHConnection sshConnection;
    //Handler handler;

    //    RelativeLayout layoutKeys;
    //ImageButton keyStow;
    private ArrayAdapter settingAdapter;
    //    ImageButton keyCtrl;
    boolean keyCtrlToggled;
    //    ImageButton keySuper;
    boolean keySuperToggled;
    //    ImageButton keyAlt;
    boolean keyAltToggled;

    boolean hardKeyboardExtended;
    boolean extraKeysHidden = false;
    int prevBottomOffset = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Enables sticky immersive mode if supported.
     */
    private void enableImmersive() {
        if (Utils.querySharedPreferenceBoolean(this, Constants.disableImmersiveTag))
            return;

        if (Constants.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            canvas.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersive();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (TextUtils.isEmpty(TenantProcess.userId) || TextUtils.isEmpty(TenantProcess.TOKEN_VALUE)) {
            Log.i(TAG, "===========has not authorized yet!==============");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            this.finish();
        }
        //获取屏幕分辨率
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        System.out.println("====屏幕密度===" + mDisplayMetrics.density);
        //这里是干啥的？？？
        Utils.showMenu(this);

        initialize();
        if (connection != null && connection.isReadyForConnection()) {
            continueConnecting();
        }
    }

    @Override
    public void onBackPressed() {
         canvas.closeConnection();
         super.onBackPressed();
    }

    void initialize() {
        //获取当前系统的版本号>9表示当前版本在android 2.3以上
        if (android.os.Build.VERSION.SDK_INT >= 9) {
            android.os.StrictMode.ThreadPolicy policy = new android.os.StrictMode.ThreadPolicy.Builder().permitAll().build();
            android.os.StrictMode.setThreadPolicy(policy);
        }
        //设置当前窗口无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置当前窗口保持全屏状态
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //屏幕保持常亮
        if (Utils.querySharedPreferenceBoolean(this, Constants.keepScreenOnTag))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //设置屏幕不响应重力感应器，既横竖屏和本应用无关
        if (Utils.querySharedPreferenceBoolean(this, Constants.forceLandscapeTag))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        //获取数据库
        database = ((App) getApplication()).getDatabase();

        Intent i = getIntent();
        connection = null;

        Uri data = i.getData();
        //从登陆界面获取准确的登陆响应信息
        boolean isSupportedScheme = false;
        if (data != null) {
            String s = data.getScheme();
            //Log.d("LSLONG","init data is:"+s);
            isSupportedScheme = s.equals("rdp") || s.equals("spice") || s.equals("vnc");
            //Log.d("LSLONG","init data is:"+s+"isSupportedScheme:"+isSupportedScheme);
        }

        if (isSupportedScheme || !Utils.isNullOrEmptry(i.getType())) {
            //Log.d("LSLONG","my test +++++++++++++++++++++++++");
            //获取验证消息是否完成
            if (isMasterPasswordEnabled()) {
                Utils.showFatalErrorMessage(this, getResources().getString(R.string.master_password_error_intents_not_supported));
                return;
            }
            //从数据库中读取连接信息
            connection = ConnectionBean.createLoadFromUri(data, this);

            String host = data.getHost();
            if (!host.startsWith(Constants.CONNECTION)) {
                connection.parseFromUri(data, this);
            }

            if (connection.isSaved()) {
                connection.saveAndWriteRecent(false, database);
            }
            // we need to save the connection to display the loading screen, so otherwise we should exit
            if (!connection.isReadyForConnection()) {
                if (!connection.isSaved()) {
                    Log.i(TAG, "Exiting - Insufficent information to connect and connection was not saved.");
                    Toast.makeText(this, getString(R.string.error_uri_noinfo_nosave), Toast.LENGTH_LONG).show();
                }
                finish();
                return;
            }
        } else {

            //这里啥都没干
            //这个地方没调用
            connection = new ConnectionBean();
            Bundle extras = i.getExtras();

            if (extras != null) {
                Log.d("LSLONG","extras++++++++++++++++++++++++++");
                connection.Gen_populate((ContentValues) extras.getParcelable(Constants.CONNECTION));
            }

            //检查是不是ipv6地址
            // Parse a HOST:PORT entry but only if not ipv6 address
            String host = connection.getAddress();
            Log.d("LSLONG","host============:"+host);

            if (!Utils.isValidIpv6Address(host) && host.indexOf(':') > -1) {
                String p = host.substring(host.indexOf(':') + 1);
                try {
                    int parsedPort = Integer.parseInt(p);
                    connection.setPort(parsedPort);
                    connection.setAddress(host.substring(0, host.indexOf(':')));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (connection.getPort() == 0) {
                connection.setPort(Constants.DEFAULT_VNC_PORT);
            }


            if (connection.getSshPort() == 0) {
                connection.setSshPort(Constants.DEFAULT_SSH_PORT);
            }

        }
    }

    void continueConnecting() {
        // TODO: Implement left-icon
        setContentView(R.layout.canvas);
        canvas = (RemoteCanvas) findViewById(R.id.vnc_canvas);

        llContent = (LinearLayout) findViewById(R.id.ll_content);
        findViewById(R.id.tv_back).setOnClickListener(this);
        findViewById(R.id.tv_home).setOnClickListener(this);
        findViewById(R.id.tv_disconnect).setOnClickListener(this);
        findViewById(R.id.usb_list).setOnClickListener(this);
        findViewById(R.id.video_play).setOnClickListener(this);

        canvas.initializeCanvas(connection, database, new Runnable() {
            public void run() {
            try {
                setModes();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            }
        });

        canvas.setOnKeyListener(this);
        canvas.setFocusableInTouchMode(true);
        canvas.setDrawingCacheEnabled(false);

        // This code detects when the soft keyboard is up and sets an appropriate visibleHeight in remoteCanvas.
        // When the keyboard is gone, it resets visibleHeight and pans zero distance to prevent us from being
        // below the desktop image (if we scrolled all the way down when the keyboard was up).
        // TODO: Move this into a separate thread, and post the visibility changes to the handler.
        //       to avoid occupying the UI thread with this.
        final View rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                //创建一个矩形
                Rect r = new Rect();
                //用来获取当前窗口可视区域大小的
                rootView.getWindowVisibleDisplayFrame(r);

                // To avoid setting the visible height to a wrong value after an screen unlock event
                // (when r.bottom holds the width of the screen rather than the height due to a rotation)
                // we make sure r.top is zero (i.e. there is no notification bar and we are in full-screen mode)
                // It's a bit of a hack.
                if (r.top == 0) {
                    if (canvas.bitmapData != null) {
                        canvas.setVisibleHeight(r.bottom);
                        canvas.pan(0, 0);
                    }
                }

                // Enable/show the zoomer if the keyboard is gone, and disable/hide otherwise.
                // We detect the keyboard if more than 19% of the screen is covered.
                int offset = 0;
                int rootViewHeight = rootView.getHeight();
                if (r.bottom > rootViewHeight * 0.81) {
                    offset = rootViewHeight - r.bottom;
                    Log.d("LSLONG","offset -------------------:"+offset+" rootViewHeight:"+rootViewHeight);
                    // Soft Kbd gone, shift the meta keys and arrows down.
//                    if (layoutKeys != null) {
//                        layoutKeys.offsetTopAndBottom(offset);
//                        keyStow.offsetTopAndBottom(offset);
//                        if (prevBottomOffset != offset) {
//                            setExtraKeysVisibility(View.GONE, false);
//                            canvas.invalidate();
////                            zoomer.enable();
//                        }
//                    }
                } else {
                    offset = r.bottom - rootViewHeight;
                    Log.d("LSLONG","offset ++++++++++++++++++++:"+offset);
                    //  Soft Kbd up, shift the meta keys and arrows up.
//                    if (layoutKeys != null) {
//                        layoutKeys.offsetTopAndBottom(offset);
//                        keyStow.offsetTopAndBottom(offset);
//                        if (prevBottomOffset != offset) {
//                            setExtraKeysVisibility(View.VISIBLE, true);
//                            canvas.invalidate();
////                            zoomer.hide();
////                            zoomer.disable();
//                        }
//                    }
                }
                prevBottomOffset = offset;
                enableImmersive();
            }
        });


        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        if (Utils.querySharedPreferenceBoolean(this, Constants.leftHandedModeTag)) {
            params.gravity = Gravity.CENTER | Gravity.LEFT;
        } else {
            params.gravity = Gravity.CENTER | Gravity.RIGHT;
        }
//        zoomer.setLayoutParams(params);

        panner = new Panner(this, canvas.handler);

        inputHandler = getInputHandlerById(R.id.itemInputTouchPanZoomMouse);
    }


    /**
     * Sets the visibility of the extra keys appropriately.
     */
    private void setExtraKeysVisibility(int visibility, boolean forceVisible) {
        Configuration config = getResources().getConfiguration();

        boolean makeVisible = forceVisible;
        if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
            makeVisible = true;

        if (!extraKeysHidden && makeVisible &&
                connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON) {

            return;
        }
    }

    /*
     * TODO: REMOVE THIS AS SOON AS POSSIBLE.
     * onPause: This is an ugly hack for the Playbook, because the Playbook hides the keyboard upon unlock.
     * This causes the visible height to remain less, as if the soft keyboard is still up. This hack must go
     * away as soon as the Playbook doesn't need it anymore.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "======onPause()======");

        //xpz
        /*try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(canvas.getWindowToken(), 0);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }*/
    }


    /*
     * TODO: REMOVE THIS AS SOON AS POSSIBLE.
     * onResume: This is an ugly hack for the Playbook which hides the keyboard upon unlock. This causes the visible
     * height to remain less, as if the soft keyboard is still up. This hack must go away as soon
     * as the Playbook doesn't need it anymore.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume called.");
        try {
            canvas.postInvalidateDelayed(600);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (canvas.bitmapData != null) {
            canvas.bitmapData.showCursorFlag = false;
        }

    }

    private boolean isAltDown = false;
    private boolean isCtrlDown = false;
    //private boolean isKeyCtrlAndCtrlToggled = false;

    //handle mouse icon visiable or invisiable
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int keyAction = event.getAction();
        Log.i(TAG, "=====xpz========dispatchKeyEvent()===" + keyCode + "===" + keyAction);

        if (keyAction == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
                Log.i(TAG, "======alt down====");
                isAltDown = true;
            } else if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT) {
                Log.i(TAG, "======ctrl down====");
                isCtrlDown = true;
            }

            Log.i(TAG, isAltDown + "=========ACTION_DOWN=========" + isCtrlDown);
            if (isCtrlDown && isAltDown) {

                if (canvas.bitmapData != null) {
                    if (canvas.bitmapData.showCursorFlag) {
                        canvas.bitmapData.showCursorFlag = false;
                        //show android mouse
                        showOrHiddenAndroidMouse(1);
                        Log.i(TAG, "======show android mouse======");
                    } else {
                        canvas.bitmapData.showCursorFlag = true;
                        //hidden android mouse
                        showOrHiddenAndroidMouse(0);
                        Log.i(TAG, "======hidden android mouse======");
                    }
                }
            }
        } else if (keyAction == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
                isAltDown = false;
            } else if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT) {
                isCtrlDown = false;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    /**
     * Set modes on start to match what is specified in the ConnectionBean;
     * color mode (already done) scaling, input mode
     */
    void setModes() {
        AbstractInputHandler handler = getInputHandlerByName(connection.getInputMode());
//        AbstractScaling.getByScaleType(connection.getScaleMode()).setScaleTypeForActivity(this);
        this.inputHandler = handler;
        //showPanningState(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case R.layout.entertext:
                return new EnterTextDialog(this);
            case R.id.itemHelpInputMode:
                return createHelpDialog();
        }

        // Default to meta key dialog
        return new MetaKeyDialog(this);
    }

    /**
     * Creates the help dialog for this activity.
     */
    private Dialog createHelpDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this)
                .setMessage(R.string.input_mode_help_text)
                .setPositiveButton(R.string.close,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // We don't have to do anything.
                            }
                        });
        Dialog d = adb.setView(new ListView(this)).create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        d.show();
        d.getWindow().setAttributes(lp);
        return d;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (dialog instanceof ConnectionSettable)
            ((ConnectionSettable) dialog).setConnection(connection);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "======onConfigurationChanged()======");
        try {
            setExtraKeysVisibility(View.GONE, false);


        } catch (NullPointerException e) {
            e.printStackTrace();

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "======onStart()======");
        try {
            canvas.postInvalidateDelayed(800);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "======onStop()======");
        updateAndroidCursor();
        showOrHiddenAndroidMouse(1);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "======onRestart()======");
        try {
            canvas.postInvalidateDelayed(1000);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.e(TAG, "onCreateOptionsMenu");
       /* try {
            getMenuInflater().inflate(R.menu.canvasactivitymenu, menu);

            Menu inputMenu = menu.findItem(R.id.itemInputMode).getSubMenu();
            inputModeMenuItems = new MenuItem[inputModeIds.length];
            for (int i = 0; i < inputModeIds.length; i++) {
                inputModeMenuItems[i] = inputMenu.findItem(inputModeIds[i]);
            }
            updateInputMenu();

        } catch (NullPointerException e) {
        }*/
        return true;
    }

    /**
     * If id represents an input handler, return that; otherwise return null
     *
     * @param id
     * @return
     */
    AbstractInputHandler getInputHandlerById(int id) {
        boolean isRdp = getPackageName().contains("RDP");

        if (inputModeHandlers == null) {
            inputModeHandlers = new AbstractInputHandler[inputModeIds.length];
        }
        for (int i = 0; i < inputModeIds.length; ++i) {
            if (inputModeIds[i] == id) {
                if (inputModeHandlers[i] == null) {
                    switch (id) {

                        case R.id.itemInputTouchPanZoomMouse:
                            inputModeHandlers[i] = new TouchMouseSwipePanInputHandler(this, canvas, isRdp);
                            break;
                        case R.id.itemInputDragPanZoomMouse:
                            inputModeHandlers[i] = new TouchMouseDragPanInputHandler(this, canvas, isRdp);
                            break;
                        case R.id.itemInputTouchpad:
                            inputModeHandlers[i] = new SimulatedTouchpadInputHandler(this, canvas, isRdp);
                            break;
                        case R.id.itemInputSingleHanded:
                            inputModeHandlers[i] = new SingleHandedInputHandler(this, canvas, isRdp);
                            break;

                    }
                }
                return inputModeHandlers[i];
            }
        }
        return null;
    }

    void clearInputHandlers() {
        if (inputModeHandlers == null)
            return;

        for (int i = 0; i < inputModeIds.length; ++i) {
            inputModeHandlers[i] = null;
        }
        inputModeHandlers = null;
    }

    AbstractInputHandler getInputHandlerByName(String name) {
        AbstractInputHandler result = null;
        for (int id : inputModeIds) {
            AbstractInputHandler handler = getInputHandlerById(id);
            if (handler.getName().equals(name)) {
                result = handler;
                break;
            }
        }
        if (result == null) {
            result = getInputHandlerById(R.id.itemInputTouchPanZoomMouse);
        }
        return result;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        canvas.getKeyboard().setAfterMenu(true);
        switch (item.getItemId()) {
            case R.id.itemInfo:
                canvas.showConnectionInfo();
                return true;
            case R.id.itemSpecialKeys:
                showDialog(R.layout.metakey);
                return true;

            case R.id.itemCenterMouse:
                canvas.getPointer().warpMouse(canvas.absoluteXPosition + canvas.getVisibleWidth() / 2,
                        canvas.absoluteYPosition + canvas.getVisibleHeight() / 2);
                return true;
            case R.id.itemDisconnect:
                canvas.closeConnection();
                finish();
                return true;
            case R.id.itemEnterText:
                showDialog(R.layout.entertext);
                return true;
            case R.id.itemCtrlAltDel:
                canvas.getKeyboard().sendMetaKey(MetaKeyBean.keyCtrlAltDel);
                return true;


            case R.id.itemSendKeyAgain:
                sendSpecialKeyAgain();
                return true;

            case R.id.itemHelpInputMode:
                showDialog(R.id.itemHelpInputMode);
                return true;
            default:
                AbstractInputHandler input = getInputHandlerById(item.getItemId());
                if (input != null) {
                    inputHandler = input;
                    connection.setInputMode(input.getName());
                    if (input.getName().equals(SimulatedTouchpadInputHandler.TOUCHPAD_MODE)) {
                        connection.setFollowMouse(true);
                        connection.setFollowPan(true);
                    } else {
                        connection.setFollowMouse(false);
                        connection.setFollowPan(false);
                    }
                    item.setChecked(true);
                    //showPanningState(true);
                    connection.save(database.getWritableDatabase());
                    database.close();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private MetaKeyBean lastSentKey;

    private void sendSpecialKeyAgain() {
        if (lastSentKey == null
                || lastSentKey.get_Id() != connection.getLastMetaKeyId()) {
            ArrayList<MetaKeyBean> keys = new ArrayList<MetaKeyBean>();
            Cursor c = database.getReadableDatabase().rawQuery(
                    MessageFormat.format("SELECT * FROM {0} WHERE {1} = {2}",
                            MetaKeyBean.GEN_TABLE_NAME,
                            MetaKeyBean.GEN_FIELD__ID, connection
                                    .getLastMetaKeyId()),
                    MetaKeyDialog.EMPTY_ARGS);
            MetaKeyBean.Gen_populateFromCursor(c, keys, MetaKeyBean.NEW);
            c.close();
            if (keys.size() > 0) {
                lastSentKey = keys.get(0);
            } else {
                lastSentKey = null;
            }
        }
        if (lastSentKey != null)
            canvas.getKeyboard().sendMetaKey(lastSentKey);

    }

    @Override
    protected void onDestroy() {
        //showOrHiddenAndroidMouse(1);
        super.onDestroy();
        Log.i(TAG, "======onDestroy()======");
        if (canvas != null) {
            canvas.closeConnection();
        }

        if (database != null) {
            database.close();
        }

        canvas = null;
        connection = null;
        database = null;
        panner = null;
        clearInputHandlers();
        inputHandler = null;
        System.gc();
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "====CONNECTION_TYPE=====" + TenantProcess.CONNECTION_TYPE);
        if (TenantProcess.CONNECTION_TYPE == 1) {
            canvas.closeConnection();
            TenantProcess.clearNativeCache();
            finish();
            Process.killProcess(Process.myPid());
            return;
        }

        switch (v.getId()) {
            case R.id.tv_back:
                onBackPressed();
                break;
            case R.id.tv_home:
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);

                break;
            case R.id.tv_disconnect:
                canvas.closeConnection();
                finish();
                break;
            //弹出usb repir的选项卡
            case R.id.usb_list:
                try {
                    showUsbListDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.video_play:
                startPlayVideo();
                break;
            default:
                break;
        }
    }

    /*start play video*/
    private void startPlayVideo(){
        //canvas.closeConnection();
        Log.d(LSLTAG,"start play video ...");
        ComponentName componetName = new ComponentName(
                //app2的包名
                "com.jzby.lslnong.myvideodemo",
                // 你要启动的界面
                "com.jzby.lslnong.myvideodemo.VideoPlayerActivity");
        Intent intent = new Intent();
        intent.setComponent(componetName);
        this.startActivity(intent);
        //this.finish();
    }


    /*add by lslong 20180809-11:40*/

    private void showUsbListDialog(){
        String USBDILOG = "请插入USB！！！";
        ArrayList<String> usbDevice = new ArrayList<>();
        ArrayList<String> usbVentor = new ArrayList<>();
        ArrayList<String> usbProduct = new ArrayList<>();
        getUsbList(usbDevice,usbVentor,usbProduct);

        //创建弹窗的builder
        final AlertDialog.Builder builder = new AlertDialog.Builder(this).
                setTitle("Usb Device List").setIcon(android.R.drawable.ic_dialog_info);
        if (usbDevice.size() < 1) {
            builder.setMessage(USBDILOG);
            builder.setNegativeButton("确认", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        } else {
            final SharedPreferences userSettings = getSharedPreferences("setting", 0);
            int size = usbDevice.size();
            String[] dList = usbDevice.toArray(new String[size]);
            final boolean[] isCheckeds = new boolean[size];
            final String[] vList = usbVentor.toArray(new String[size]);

            for(int n = 0;n<vList.length;n++) {
                isCheckeds[n] =  userSettings.getBoolean(vList[n],false);
            }
            //设置弹窗为复选框
            builder.setMultiChoiceItems(dList, isCheckeds, new DialogInterface.OnMultiChoiceClickListener() {
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                String var = vList[which];
                //SharedPreferences.Editor editor = userSettings.edit();
                //editor.putBoolean(var,isChecked);
                Log.d(LSLTAG,"var:"+var+"isChecked:"+isChecked);

                Boolean s;
                if(var!=null&&!var.equals("")) {
                    if (isChecked) {
                        s = UsbStoreInfo.sendUSBRepirRequest(var,"add");

                    } else {
                        s = UsbStoreInfo.sendUSBRepirRequest(var,"remove");
                    }
                    if(s){
                        SharedPreferences.Editor editor = userSettings.edit();
                        editor.putBoolean(var,isChecked);
                        editor.commit();
                    }
                }
                }
            });

            builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt)
                {
                    if(!UsbStoreInfo.success) {
                        Toast.makeText(getApplicationContext(), "请向管理员申请权限", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        builder.show();
    }


    @Override
    public boolean onKey(View v, int keyCode, KeyEvent evt) {
        Log.i(TAG, "=====xpz========onKey()===" + keyCode + "=======" + evt);
        int keyAction = evt.getAction();
        boolean consumed = false;

        try {
            if (evt.getAction() == KeyEvent.ACTION_DOWN || evt.getAction() == KeyEvent.ACTION_MULTIPLE) {
                consumed = inputHandler.onKeyDown(keyCode, evt);
            } else if (evt.getAction() == KeyEvent.ACTION_UP) {
                consumed = inputHandler.onKeyUp(keyCode, evt);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return consumed;
    }


    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onTrackballEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTrackballEvent(MotionEvent event) {

        Log.i(TAG, "=====onTrackballEvent()======" + event.getX() + "======" + event.getY());
        try {
            // If we are using the Dpad as arrow keys, don't send the event to the inputHandler.
//            if (connection.getUseDpadAsArrows())
//                return false;
            return inputHandler.onTrackballEvent(event);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return super.onTrackballEvent(event);
    }

    // Send touch events or mouse events like button clicks to be handled.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i(TAG, "=====onTouchEvent()======" + event.getX() + "======" + event.getY());
        Log.i(TAG, "[jcktest]====xpz=====onTouchEvent=======" + System.currentTimeMillis());
        try {
            return inputHandler.onTouchEvent(event);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return super.onTouchEvent(event);
    }

    // Send e.g. mouse events like hover and scroll to be handled.
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // Ignore TOOL_TYPE_FINGER events that come from the touchscreen with HOVER type action
        // which cause pointer jumping trouble in simulated touchpad for some devices.
        //Log.i(TAG,"=====onGenericMotionEvent()======"+event.getX()+"======"+event.getY());
        checkPointInMenuArea(event.getX(), event.getY());
        int a = event.getAction();
        if (!((a == MotionEvent.ACTION_HOVER_ENTER ||
                a == MotionEvent.ACTION_HOVER_EXIT ||
                a == MotionEvent.ACTION_HOVER_MOVE) &&
                event.getSource() == InputDevice.SOURCE_TOUCHSCREEN &&
                event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
        )) {
            try {
                return inputHandler.onTouchEvent(event);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void showOrHiddenAndroidMouse(int flag) {
        Intent intent = new Intent("KeyAndMouseSetting.ACTION_SET_MOUSE_VISIBLE");
        intent.putExtra("SHOW_HIDE_FLAG", flag);
        sendBroadcast(intent);
    }

    private void updateAndroidCursor() {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pointer_arrow);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] bytes = baos.toByteArray();
            baos.close();

            Intent intent = new Intent("KeyAndMouseSetting.ACTION_SET_MOUSE_ICON");
            intent.putExtra("hotspotX", 0f);
            intent.putExtra("hotspotY", 0f);
            Bundle data = new Bundle();
            data.putByteArray("bitmap", bytes);
            intent.putExtras(data);
            sendBroadcast(intent);

            bitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPointInMenuArea(float x, float y) {
        double left = (mDisplayMetrics.widthPixels - (mDisplayMetrics.density * 240 + 0.5)) / 2;
        double right = left + (mDisplayMetrics.density * 240 + 0.5);
        double top = 0;
        //double bottom=top+(mDisplayMetrics.density*5+0.5);
        double bottom = 5;
        Log.i(TAG, "======" + left + "=====" + right + "======" + top + "=====" + bottom);
        String tag = llContent.getTag().toString();
        float curTranslationX = llContent.getTranslationY();
        ObjectAnimator animator = null;
        if (x >= left && x <= right && y >= top && y <= bottom) {
            if ("1".equals(tag)) {
                animator = ObjectAnimator.ofFloat(llContent, "translationY",
                        curTranslationX, 30 * mDisplayMetrics.density + 0.5f, 30 * mDisplayMetrics.density + 0.5f);
                animator.setDuration(800);
                animator.start();
                llContent.setTag("-1");
            } else {

            }


        } else {
            if ("-1".equals(tag)) {
                animator = ObjectAnimator.ofFloat(llContent, "translationY", curTranslationX, 0f, 0f);
                animator.setDuration(800);
                animator.start();
                llContent.setTag("1");
            }
        }
    }

    public void stopPanner() {
        panner.stop();
    }


    public ConnectionBean getConnection() {
        return connection;
    }

    // Returns whether we are using D-pad/Trackball to send arrow key events.
    public boolean getUseDpadAsArrows() {
        return false;
    }

    // Returns whether the D-pad should be rotated to accommodate BT keyboards paired with phones.
    public boolean getRotateDpad() {
        return false;
    }

    public float getSensitivity() {
        // TODO: Make this a slider config option.
        return 2.0f;
    }

    public boolean getAccelerationEnabled() {
        // TODO: Make this a config option.
        return true;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public RemoteCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(RemoteCanvas vncCanvas) {
        this.canvas = vncCanvas;
    }

    public Panner getPanner() {
        return panner;
    }

    public void setPanner(Panner panner) {
        this.panner = panner;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    private boolean isMasterPasswordEnabled() {
        SharedPreferences sp = getSharedPreferences(Constants.generalSettingsTag, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.masterPasswordEnabledTag, false);
    }


    private int getUsbList( ArrayList<String> usbDevice, ArrayList<String> usbvendor, ArrayList<String> usbproduct){
        int res = 0;
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
        Iterator<UsbDevice> iterator = devices.values().iterator();
        while (iterator.hasNext()) {
            UsbDevice device = iterator.next();
            if (device != null && device.getDeviceId() > 0) {
                int vendorid = device.getVendorId();
                int productid = device.getProductId();
                String devicename = device.getDeviceName();
                int devicetype = device.getDeviceClass();
                Log.d(LSLTAG,"vid : "+vendorid+" pid : "+productid+" dname : "+devicename
                +" dtype : "+devicetype);
                usbDevice.add(devicename);
                usbvendor.add(Integer.toString(vendorid));
                usbproduct.add(Integer.toString(productid));
                res++;
            }
        }
        return res;
    }
}

