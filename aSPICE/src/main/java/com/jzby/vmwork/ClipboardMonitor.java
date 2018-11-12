/**
 * Copyright (C) 2012 Iordan Iordanov
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

import android.content.ClipboardManager;
import android.content.Context;

import com.jzby.vmwork.RemoteCanvas;

import java.util.TimerTask;

/*
 * This is a TimerTask which checks the clipboard for changes, and if
 * a change is detected, sends the new contents to the VNC server.
 */

public class ClipboardMonitor extends TimerTask {
    private String TAG = "ClipboardMonitor";
    private Context context;
    ClipboardManager clipboard;
    private String knownClipboardContents;
    RemoteCanvas remoteCanvas;

    public ClipboardMonitor(Context c, RemoteCanvas vc) {
        context = c;
        remoteCanvas = vc;
        clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        knownClipboardContents = new String("");
    }

    /*
     * Grab the current clipboard contents.
     */
    private String getClipboardContents() {

//        if (clipboard != null && clipboard.getText() != null)
//            return clipboard.getText().toString();


        if (clipboard != null && clipboard.hasPrimaryClip())
            return clipboard.getPrimaryClip().getItemAt(0).getText().toString();
        else
            return null;
    }

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        String currentClipboardContents = getClipboardContents();
        //Log.d(TAG, "Current clipboard contents: " + currentClipboardContents);
        //Log.d(TAG, "Previously known clipboard contents: " + knownClipboardContents);
        if (!remoteCanvas.serverJustCutText && currentClipboardContents != null &&
                !currentClipboardContents.equals(knownClipboardContents)) {
            if (remoteCanvas.spicecomm != null && remoteCanvas.spicecomm.isInNormalProtocol()) {
                remoteCanvas.spicecomm.writeClientCutText(currentClipboardContents);
                knownClipboardContents = new String(currentClipboardContents);
                //Log.d(TAG, "Wrote: " + knownClipboardContents + " to remote clipboard.");
            }
        } else if (remoteCanvas.serverJustCutText && currentClipboardContents != null) {
            knownClipboardContents = new String(currentClipboardContents);
            remoteCanvas.serverJustCutText = false;
            //Log.d(TAG, "Set knownClipboardContents to equal what server just sent over.");
        }
    }
}
