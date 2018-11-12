package com.jzby.util;

import android.content.Context;
import android.preference.PreferenceManager;

public class PreferenceUtil
{
	public static final String USERNAME_KEY="jz_name";
	public static final String PASSWORD_KEY="jz_pwd";

	public static void setString(Context context, String key,String value)
	{
		PreferenceManager.getDefaultSharedPreferences(context).edit()
		.putString(key, value).commit();
	}	
	
	public static String getString(Context context, String key)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key,"");
	}
	
	
	public static void setInt(Context context, String key, int value) {
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putInt(key, value).commit();
	}



}
