package com.jzby.vmwork;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jzby.util.PreferenceUtil;
import com.jzby.util.TenantProcess;

public class ChangePwdActivity extends Activity implements View.OnClickListener,
        View.OnFocusChangeListener,View.OnHoverListener,View.OnTouchListener,TenantProcess.HttpProcessInterface
{
    final static int FLAG_GET_USER_ID_SUCCESS=10000;
    final static int FLAG_GET_USER_ID_FAILED=10001;
    final static int FLAG_CHANGE_PWD_SUCCESS=10002;
    final static int FLAG_CHANGE_PWD_FAILED=10003;

    private TextView tvModifyPwd,tvReturn;

    private EditText etUserName,etOldPwd,etNewPwd,etNewConfirmPwd;

    private int inputType,inputPwdType;

    private int inputLength,inputPwdLength,inputNewPwdLength,inputNewConfirmPwdLength;

    String mUserName,mOldPwd,mNewPwd;

    TenantProcess mTenantProcess;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_change_pwd);

        tvModifyPwd=(TextView) findViewById(R.id.btn_pwd_forget);
        tvReturn=(TextView) findViewById(R.id.btn_pwd_return);

        etUserName=(EditText) findViewById(R.id.etUsername);
        etOldPwd=(EditText) findViewById(R.id.etOldPwd);
        etNewPwd=(EditText) findViewById(R.id.etNewPwd);
        etNewConfirmPwd=(EditText) findViewById(R.id.etNewConfirmPwd);

        inputType=etUserName.getInputType();
        inputPwdType=etOldPwd.getInputType();

        etUserName.setOnTouchListener(this);
        etOldPwd.setOnTouchListener(this);
        etNewPwd.setOnTouchListener(this);
        etNewConfirmPwd.setOnTouchListener(this);

        tvModifyPwd.setOnClickListener(this);
        tvReturn.setOnClickListener(this);

        tvModifyPwd.setOnFocusChangeListener(this);
        tvReturn.setOnFocusChangeListener(this);

        tvModifyPwd.setOnHoverListener(this);
        tvReturn.setOnHoverListener(this);

        String username = PreferenceUtil.getString(this, PreferenceUtil.USERNAME_KEY) + "";
        if(!TextUtils.isEmpty(username))
        {
            etUserName.setText(username);
            mUserName=username;
        }

        mTenantProcess=TenantProcess.getInstance();
        mTenantProcess.setHttpProcessListener(this);
    }


    Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message)
        {
            switch (message.what)
            {
                case FLAG_GET_USER_ID_SUCCESS:
                    mTenantProcess.changeUserPassword(mOldPwd,mNewPwd);
                    break;

                case FLAG_GET_USER_ID_FAILED:
                    showText(getString(R.string.tips_pwd_uid_error));
                    break;

                case FLAG_CHANGE_PWD_SUCCESS:
                    showText(getString(R.string.tips_pwd_success));
                    break;

                case FLAG_CHANGE_PWD_FAILED:
                    showText(getString(R.string.tips_pwd_failed));
                    break;
            }
            return false;
        }
    });

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        switch (view.getId())
        {
            case R.id.etUsername:
                etUserName.setInputType(InputType.TYPE_NULL);
                etUserName.onTouchEvent(motionEvent);
                etUserName.setInputType(inputType);
                inputLength = etUserName.getText().toString().length();
                etUserName.setSelection(inputLength);
                break;
            case R.id.etOldPwd:
                etOldPwd.setInputType(InputType.TYPE_NULL);
                etOldPwd.onTouchEvent(motionEvent);
                etOldPwd.setInputType(inputPwdType);
                inputPwdLength = etOldPwd.getText().toString().length();
                etOldPwd.setSelection(inputPwdLength);
                break;
            case R.id.etNewPwd:
                etNewPwd.setInputType(InputType.TYPE_NULL);
                etNewPwd.onTouchEvent(motionEvent);
                etNewPwd.setInputType(inputPwdType);
                inputNewPwdLength = etNewPwd.getText().toString().length();
                etNewPwd.setSelection(inputNewPwdLength);
                break;

            case R.id.etNewConfirmPwd:
                etNewConfirmPwd.setInputType(InputType.TYPE_NULL);
                etNewConfirmPwd.onTouchEvent(motionEvent);
                etNewConfirmPwd.setInputType(inputPwdType);
                inputNewConfirmPwdLength = etNewConfirmPwd.getText().toString().length();
                etNewConfirmPwd.setSelection(inputNewConfirmPwdLength);
                break;
        }
        return true;
    }

    @Override
    public boolean onHover(View view, MotionEvent motionEvent)
    {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_HOVER_ENTER:
                ((TextView)view).setTextColor(Color.WHITE);
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
                ((TextView)view).setTextColor(Color.BLACK);
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onFocusChange(View view, boolean b)
    {
        if(b)
        {
            ((TextView)view).setTextColor(Color.WHITE);
        }
        else
        {
            ((TextView)view).setTextColor(Color.BLACK);
        }
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_pwd_forget:

                mUserName=etUserName.getText()+"";
                mUserName=mUserName.trim();
                mOldPwd=etOldPwd.getText()+"";
                mOldPwd=mOldPwd.trim();
                mNewPwd=etNewPwd.getText()+"";
                mNewPwd=mNewPwd.trim();
                String confirmPwd=etNewConfirmPwd.getText()+"";
                confirmPwd=confirmPwd.trim();

                if(TextUtils.isEmpty(mUserName))
                {
                    showText(getString(R.string.tips_pwd_username));
                    return;
                }

                if(TextUtils.isEmpty(mOldPwd))
                {
                    showText(getString(R.string.tips_pwd_old));
                    return;
                }

                if(TextUtils.isEmpty(mNewPwd))
                {
                    showText(getString(R.string.tips_pwd_new));
                    return;
                }

                if(TextUtils.isEmpty(confirmPwd))
                {
                    showText(getString(R.string.tips_pwd_confirm));
                    return;
                }

                if(!confirmPwd.equals(mNewPwd))
                {
                    showText(getString(R.string.tips_pwd_not_match));
                    return;
                }
                mTenantProcess.TenantProcess_GetInfolist(mUserName,mOldPwd);

                break;

            case R.id.btn_pwd_return:

                this.finish();

                break;
        }
    }

    @Override
    public void onHttpSuccess(String url)
    {
        if(url.contains(TenantProcess.URL_GET_USERID))
        {
            mHandler.sendEmptyMessage(FLAG_GET_USER_ID_SUCCESS);
        }
        else if(url.contains(TenantProcess.URL_ACTION_CHANGE_PWD))
        {
            mHandler.sendEmptyMessage(FLAG_CHANGE_PWD_SUCCESS);
        }
        else
        {

        }
    }

    @Override
    public void onHttpFailed(String url, int httpCode)
    {
        if(url.contains(TenantProcess.URL_GET_USERID))
        {
            mHandler.sendEmptyMessage(FLAG_GET_USER_ID_FAILED);
        }
        else if(url.contains(TenantProcess.URL_ACTION_CHANGE_PWD))
        {
            mHandler.sendEmptyMessage(FLAG_CHANGE_PWD_FAILED);
        }
        else
        {

        }
    }

    private void showText(String text)
    {
        Toast.makeText(this,text,Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        mTenantProcess.setHttpProcessListener(null);
        TenantProcess.clearNativeCache();
        super.onDestroy();
    }
}