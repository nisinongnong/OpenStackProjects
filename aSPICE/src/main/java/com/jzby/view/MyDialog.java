package com.jzby.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.jzby.vmwork.R;

/**
 * Created by T530 on 2017/5/24.
 */

public class MyDialog extends Dialog {
    private ImageView mIcon;
    private TextView mTitle;
    private TextView mMessage;
    private Button mButton;
    private Context mContext;

    public MyDialog(Context context) {
        super(context, R.style.dialog_style);
        this.mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_dialog);
        mIcon = (ImageView) findViewById(R.id.icon);
        mTitle = (TextView) findViewById(R.id.alertTitle);
        mMessage = (TextView) findViewById(R.id.message);
        mButton = (Button) findViewById(R.id.button1);

    }

    public void setIcon(int id) {
        this.mIcon.setImageResource(id);
    }

    public void setTitle(String title) {
        this.mTitle.setText(title);
    }

    public void setMessage(String message) {
        this.mMessage.setText(message);
    }

    public void setButton(String text, final View.OnClickListener ackHandler) {
        if (text == null || ackHandler == null) {
            mButton.setVisibility(View.GONE);
            return;
        }
        mButton.setText(text);
        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ackHandler.onClick(v);
                dismiss();
            }
        });
    }

}
