package com.jzby.vmwork;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

/**
 * Created by gordan on 2017/9/27.
 */

public class ServerAdapter extends BaseAdapter {
    final static String TAG = ServerAdapter.class.getSimpleName();

    ServerAdapterInterface listener;
    Context context;
    List<ConnectionBean> servers;

    boolean clickFlag;

    public ServerAdapter(Context context, List<ConnectionBean> servers) {
        this.context = context;
        this.servers = servers;

    }

    public void setAdapterListener(ServerAdapterInterface listener) {
        this.listener = listener;
    }

    public void notifyDataChanged(List<ConnectionBean> data) {
        this.servers = data;
        this.notifyDataSetChanged();
        clickFlag=false;
    }

    public void setClickFlag(boolean flag)
    {
        clickFlag=flag;
    }

    @Override
    public int getCount() {
        return this.servers.size();
    }

    @Override
    public Object getItem(int position) {
        return servers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ConnectionBean bean = servers.get(position);
        ViewHolder holder = null;
        String status = bean.getSshPrivKey();
        Log.i(TAG, "======getView()=======" + status);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_listview, null);
            holder = new ViewHolder();
            holder.server_name = (TextView) convertView.findViewById(R.id.tv_server_name);
            holder.server_ip = (TextView) convertView.findViewById(R.id.tv_server_ip);
            holder.server_status = (TextView) convertView.findViewById(R.id.tv_server_status);

            holder.action_start = (TextView) convertView.findViewById(R.id.tv_server_start);
            holder.action_reset=(TextView) convertView.findViewById(R.id.tv_server_reset);
            holder.action_restart = (TextView) convertView.findViewById(R.id.tv_server_restart);
            holder.action_shutdown = (TextView) convertView.findViewById(R.id.tv_server_shutdown);
            holder.action_connect = (TextView) convertView.findViewById(R.id.tv_server_connect);
            holder.progressBar=(ProgressBar)convertView.findViewById(R.id.pb_server);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.progressBar.setVisibility(View.INVISIBLE);
        final ProgressBar pb=holder.progressBar;

        if (!TextUtils.isEmpty(bean.getNickname())) {
            holder.server_name.setText(bean.getNickname());
        } else {
            holder.server_name.setText("Server_JZ");
        }

        int port=0;
        if(bean.getTlsPort()>0)
        {
            port=bean.getTlsPort();
        }
        else
        {
            port=bean.getPort();
        }

        holder.server_ip.setText(bean.getAddress() + ":" + port);

        if (Constants.SERVER_STATUS_ACTIVE.equals(status) || Constants.SERVER_STATUS_REBOOT.equals(status)) {
            holder.server_status.setText(context.getString(R.string.server_status_1));
            holder.action_start.setVisibility(View.GONE);
            holder.action_shutdown.setVisibility(View.VISIBLE);
            addTextViewStyle(holder.action_shutdown);
            holder.action_shutdown.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.i(TAG,"=======action_shutdown==========");
                    if(clickFlag)
                    {
                        Log.i(TAG,"======please wait======="+clickFlag);
                        return;
                    }
                    pb.setVisibility(View.VISIBLE);
                    if (listener != null) {
                        listener.execRemoteServer(position, "shutdown");
                    }
                    clickFlag=true;
                }
            });

            holder.action_reset.setVisibility(View.VISIBLE);
            addTextViewStyle(holder.action_reset);
            holder.action_reset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG,"=========action_reset========");
                    if(clickFlag)
                    {
                        Log.i(TAG,"======please wait======="+clickFlag);
                        return;
                    }
                    pb.setVisibility(View.VISIBLE);
                    if (listener != null) {
                        listener.execRemoteServer(position, "reset");
                    }
                    clickFlag=true;
                }
            });

            holder.action_restart.setVisibility(View.VISIBLE);
            addTextViewStyle(holder.action_restart);
            holder.action_restart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG,"=========action_restart========");
                    if(clickFlag)
                    {
                        Log.i(TAG,"======please wait======="+clickFlag);
                        return;
                    }
                    pb.setVisibility(View.VISIBLE);
                    if (listener != null) {
                        listener.execRemoteServer(position, "restart");
                    }
                    clickFlag=true;
                }
            });



            holder.action_connect.setVisibility(View.VISIBLE);
            addTextViewStyle(holder.action_connect);
            holder.action_connect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG,"=======action_connect==========");
                    if(clickFlag)
                    {
                        Log.i(TAG,"======please wait======="+clickFlag);
                        return;
                    }


                    if (listener != null) {
                        listener.execRemoteServer(position, "connect");
                    }
                    clickFlag=true;
                }
            });

        } else if (Constants.SERVER_STATUS_SHUTOFF.equals(status)) {
            holder.server_status.setText(context.getString(R.string.server_status_2));
            holder.action_shutdown.setVisibility(View.GONE);
            holder.action_reset.setVisibility(View.GONE);
            holder.action_restart.setVisibility(View.GONE);
            holder.action_connect.setVisibility(View.GONE);
            holder.action_start.setVisibility(View.VISIBLE);
            addTextViewStyle(holder.action_start);
            holder.action_start.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.i(TAG,"========action_start=========");
                    if(clickFlag)
                    {
                        Log.i(TAG,"======please wait======="+clickFlag);
                        return;
                    }
                    pb.setVisibility(View.VISIBLE);
                    if (listener != null) {
                        listener.execRemoteServer(position, "start");
                    }
                    clickFlag=true;
                }
            });
        } else if (Constants.SERVER_STATUS_BUILD.equals(status)) {
            holder.server_status.setText(context.getString(R.string.server_status_4));
            holder.action_reset.setVisibility(View.GONE);
            holder.action_start.setVisibility(View.GONE);
            holder.action_shutdown.setVisibility(View.GONE);
            holder.action_restart.setVisibility(View.GONE);
            holder.action_connect.setVisibility(View.GONE);
        } else {
            holder.server_status.setText(context.getString(R.string.server_status_3));
            holder.action_reset.setVisibility(View.GONE);
            holder.action_start.setVisibility(View.GONE);
            holder.action_shutdown.setVisibility(View.GONE);
            holder.action_restart.setVisibility(View.GONE);
            holder.action_connect.setVisibility(View.GONE);
        }
        return convertView;
    }




    private void addTextViewStyle(final TextView textView) {
        textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    /*if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
                    {
                        textView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.dialog_btn_select));
                    }*/
                    textView.setTextColor(Color.WHITE);
                } else {
                    textView.setTextColor(Color.BLACK);
                }
            }
        });

        textView.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        textView.setTextColor(Color.WHITE);
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        textView.setTextColor(Color.BLACK);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    public interface ServerAdapterInterface {
         void execRemoteServer(int position, String action);
    }


    class ViewHolder {
        TextView server_name;
        TextView server_ip;
        TextView server_status;

        TextView action_start;
        TextView action_reset;
        TextView action_restart;
        TextView action_shutdown;
        TextView action_connect;

        ProgressBar progressBar;
    }
}
