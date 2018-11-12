package com.jzby.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jzby.vmwork.R;

/**
 * Created by T530 on 2017/5/24.
 */

public class MyProgressDialog extends Dialog {

    private Context mContext;
    private ProgressBar pb;
    private TextView mTitle;
    private TextView mMessage;

    public MyProgressDialog(Context context) {
        super(context, R.style.dialog_style);
        this.mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress_dialog);
        pb = (ProgressBar) findViewById(R.id.progress);
        mTitle = (TextView) findViewById(R.id.alertTitle);
        mMessage = (TextView) findViewById(R.id.message);
    }

    public void setTitle(CharSequence title){
        this.mTitle.setText(title);
    }

    public void setMessage( CharSequence message){
        this.mMessage.setText(message);
    }

    public static MyProgressDialog show(Context context, CharSequence title,
                                        CharSequence message,
                                        boolean cancelable, OnCancelListener cancelListener){
        MyProgressDialog dialog = new MyProgressDialog(context);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();
        dialog.setTitle(title);
        return dialog;
    }

}
