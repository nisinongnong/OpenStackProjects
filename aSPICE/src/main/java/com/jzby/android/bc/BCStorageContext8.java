/**
 * Copyright (c) 2013 Iordan Iordanov
 * Copyright (c) 2010 Michael A. MacDonald
 */
package com.jzby.android.bc;

import android.content.Context;

import com.jzby.vmwork.MainConfiguration;

import java.io.File;

/**
 * @author Michael A. MacDonald
 */
class BCStorageContext8 implements IBCStorageContext {

    /* (non-Javadoc)
     * @see com.jzby.android.bc.IBCStorageContext#getExternalStorageDir(android.content.Context, java.lang.String)
     */
    @Override
    public File getExternalStorageDir(MainConfiguration context, String type) {
        return ((Context) context).getExternalFilesDir(type);
    }

}
