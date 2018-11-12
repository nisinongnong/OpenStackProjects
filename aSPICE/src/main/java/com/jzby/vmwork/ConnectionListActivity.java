/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2009-2010 Michael A. MacDonald
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

package com.jzby.vmwork;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.jzby.vmwork.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael A. MacDonald
 */
public class ConnectionListActivity extends ListActivity
{
    final static String TAG=ConnectionListActivity.class.getSimpleName();

    Database database;

    ConnecttionListAdapter mAdapter=null;

    List<ConnectionBean> mConnections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new Database(this);

        if (isMasterPasswordEnabled()) {
            Utils.showFatalErrorMessage(this, getResources().getString(R.string.master_password_error_shortcuts_not_supported));
            return;
        }
        queryDB();
    }


    Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 10000:

                    Log.i(TAG,"====xpz====update Ui============");

                    if(mConnections!=null && mConnections.size()>0)
                    {
                        mAdapter=new ConnecttionListAdapter(ConnectionListActivity.this,mConnections,R.layout.connection_list);

                        setListAdapter(mAdapter);
                    }
                    break;
                default:break;
            }

            return false;
        }
    });


    private void queryDB()
    {

        new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    Cursor mCursor = database.getReadableDatabase().query(ConnectionBean.GEN_TABLE_NAME, new String[]{
                                    ConnectionBean.GEN_FIELD__ID,
                                    ConnectionBean.GEN_FIELD_NICKNAME,
                                    ConnectionBean.GEN_FIELD_USERNAME,
                                    ConnectionBean.GEN_FIELD_ADDRESS,
                                    ConnectionBean.GEN_FIELD_PORT,
                                    ConnectionBean.GEN_FIELD_REPEATERID},
                            ConnectionBean.GEN_FIELD_KEEPPASSWORD + " <> 0", null, null, null, ConnectionBean.GEN_FIELD_NICKNAME);

                    while (mCursor!=null && mCursor.moveToNext())
                    {
                        ConnectionBean bean=new ConnectionBean();
                        String name=mCursor.getString(mCursor.getColumnIndex(ConnectionBean.GEN_FIELD_NICKNAME));
                        String address=mCursor.getString(mCursor.getColumnIndex(ConnectionBean.GEN_FIELD_ADDRESS));
                        int port=mCursor.getInt(mCursor.getColumnIndex(ConnectionBean.GEN_FIELD_PORT));
                        String seprator=mCursor.getString(mCursor.getColumnIndex(ConnectionBean.GEN_FIELD_REPEATERID));
                        bean.setNickname(name);
                        bean.setAddress(address);
                        bean.setPort(port);
                        bean.setRepeaterId(seprator);

                        Log.i(TAG,"====="+name+"======"+address+"===="+port+"===="+seprator);

                        if(mConnections==null)
                        {
                            mConnections=new ArrayList<ConnectionBean>();
                        }
                        mConnections.add(bean);
                    }

                    if(mCursor!=null)
                    {
                        mCursor.close();
                    }

                    if(mConnections!=null && mConnections.size()>0)
                    {
                        mHandler.sendEmptyMessage(10000);
                    }

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();


    }


    /* (non-Javadoc)
             * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
             */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ConnectionBean connection = new ConnectionBean();
        if (connection.Gen_read(database.getReadableDatabase(), id)) {
            // create shortcut if requested
            ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.icon);

            Intent intent = new Intent();

            Intent launchIntent = new Intent(this, RemoteCanvasActivity.class);
            Uri.Builder builder = new Uri.Builder();
            builder.authority(Constants.CONNECTION + ":" + connection.get_Id());
            builder.scheme("vnc");
            launchIntent.setData(builder.build());

            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, connection.getNickname());
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

            setResult(RESULT_OK, intent);
        } else
            setResult(RESULT_CANCELED);

        finish();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }

    private boolean isMasterPasswordEnabled() {
        SharedPreferences sp = getSharedPreferences(Constants.generalSettingsTag, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.masterPasswordEnabledTag, false);
    }


    class ConnecttionListAdapter extends BaseAdapter
    {
        Context mContext;
        List<ConnectionBean> beanList;
        int resId;
        public ConnecttionListAdapter(Context mContext,List<ConnectionBean> beanList,int resId)
        {
            this.mContext=mContext;
            this.beanList=beanList;
            this.resId=resId;
        }

        @Override
        public int getCount() {
            return beanList.size();
        }

        @Override
        public Object getItem(int position) {
            return beanList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ConnecttionViewHolder holder=null;

            ConnectionBean bean=beanList.get(position);

            if(convertView==null)
            {
                convertView= LayoutInflater.from(this.mContext).inflate(this.resId,null);
                holder=new ConnecttionViewHolder();
                holder.tvName=(TextView) convertView.findViewById(R.id.list_text_nickname);
                holder.tvAddress=(TextView) convertView.findViewById(R.id.list_text_address);
                holder.tvPort=(TextView) convertView.findViewById(R.id.list_text_port);
                holder.tvSeprate=(TextView) convertView.findViewById(R.id.list_text_separator);

                convertView.setTag(holder);
            }
            else
            {
                holder=(ConnecttionViewHolder)convertView.getTag();
            }
            holder.tvName.setText(bean.getNickname());
            holder.tvAddress.setText(bean.getAddress());
            holder.tvPort.setText(bean.getPort()+"");
            holder.tvSeprate.setText(bean.getRepeaterId());
            return convertView;
        }
    }

    class ConnecttionViewHolder
    {
        TextView tvName;
        TextView tvAddress;
        TextView tvPort;
        TextView tvSeprate;
    }


}
