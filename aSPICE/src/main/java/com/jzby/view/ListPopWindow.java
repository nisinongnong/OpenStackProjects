package com.jzby.view;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
//import android.support.v4.content.res.ResourcesCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.jzby.vmwork.R;

/**
 * Created by T530 on 2017/5/19.
 */

public class ListPopWindow extends PopupWindow {
    View contentView;
    ListView lv;
    Context mContext;
    BaseAdapter mAdapter;

    public ListPopWindow(Context context) {
        this.mContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        contentView = inflater.inflate(R.layout.pop_window, null);
        this.setContentView(contentView);
        // 设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        // 设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        // 设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        setTouchInterceptor(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
            }
        });

        //setBackgroundDrawable(context.getResources().getDrawable(R.drawable.menu_bg));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            //setBackgroundDrawable(context.getDrawable(R.drawable.menu_bg));
            setBackgroundDrawable(context.getResources().getDrawable(R.drawable.menu_bg));
        }
        else
        {
            //setBackgroundDrawable(ResourcesCompat.getDrawable(context.getResources(),R.drawable.menu_bg,null));
        }
        lv = (ListView) contentView.findViewById(R.id.pop_listview);
        // 刷新状态
        this.update();

    }


    public void setAdapter(BaseAdapter adapter) {
        this.mAdapter = adapter;
        this.lv.setAdapter(mAdapter);
        this.mAdapter.notifyDataSetChanged();
    }

    public void setOnItemClickListener(@Nullable AdapterView.OnItemClickListener listener) {
        this.lv.setOnItemClickListener(listener);
    }


    public void show(Activity activity) {
        if (isShowing()) {
            return;
        } else {
            if (activity != null) {
                View v = activity.getWindow().getDecorView().findViewById(android.R.id.content);
                if (v != null) {
                    showAtLocation(v, Gravity.CENTER, 0, 0);
                }
            }
        }
    }

    public void show(View view) {
        if (isShowing()) {
          this.dismiss();
        } else {
            if (view != null) {
                showAsDropDown(view);
            }
        }
    }

}
