/**
 * Copyright (C) 2009 Michael A. MacDonald
 */
package com.jzby.android.bc;

import android.app.ActivityManager;

/**
 * @author Michael A. MacDonald
 */
public class BCActivityManagerV5 implements IBCActivityManager {

    /* (non-Javadoc)
     * @see com.jzby.android.bc.IBCActivityManager#getMemoryClass(android.app.ActivityManager)
     */
    @Override
    public int getMemoryClass(ActivityManager am) {
        return am.getMemoryClass();
    }

}
