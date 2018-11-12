package com.jzby.vmwork.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.widget.ListView;

import com.jzby.vmwork.R;

/**
 * Created by gordan on 2017/8/8.
 */

public class DialogUtil {

    public static Dialog showHelpDialog(Context mContext)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(mContext).setMessage(
                R.string.spice_main_screen_help_text).setPositiveButton(
                R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // We don't have to do anything.
                    }
                });
        Dialog d = adb.setView(new ListView(mContext)).create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        d.show();
        d.getWindow().setAttributes(lp);

        return d;
    }

}
