/**
 * Copyright (c) 2013 Iordan Iordanov
 * Copyright (c) 2011 Michael A. MacDonald
 */
package com.jzby.android.bc;

import android.os.Environment;

import com.jzby.vmwork.MainConfiguration;

import java.io.File;

/**
 * @author Michael A. MacDonald
 */
public class BCStorageContext7 implements IBCStorageContext {

    /* (non-Javadoc)
     * @see com.jzby.android.bc.IBCStorageContext#getExternalStorageDir(android.content.Context, java.lang.String)
     */
    @Override
    public File getExternalStorageDir(MainConfiguration context, String type) {
        File f = Environment.getExternalStorageDirectory();
        f = new File(f, "Android/data/com.jzby.bVNC/files");
        if (type != null)
            f = new File(f, type);
        f.mkdirs();
        return f;
    }

}
