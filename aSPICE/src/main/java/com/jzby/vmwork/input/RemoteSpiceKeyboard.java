/**
 * Copyright (C) 2013- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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


package com.jzby.vmwork.input;

import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.jzby.vmwork.Constants;
import com.jzby.vmwork.MetaKeyBean;
import com.jzby.vmwork.RemoteCanvas;
import com.jzby.vmwork.SpiceCommunicator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class RemoteSpiceKeyboard extends RemoteKeyboard {
    private final static String TAG = "RemoteSpiceKeyboard";
    private HashMap<Integer, Integer[]> table;
    final static int SCANCODE_SHIFT_MASK = 0x10000;
    final static int SCANCODE_ALTGR_MASK = 0x20000;
    final static int SCANCODE_CIRCUMFLEX_MASK = 0x40000;
    final static int SCANCODE_DIAERESIS_MASK = 0x80000;
    final static int UNICODE_MASK = 0x100000;
    final static int UNICODE_META_MASK = KeyEvent.META_CTRL_MASK | KeyEvent.META_META_MASK | KeyEvent.META_CAPS_LOCK_ON;
    private int KEY_ALT_ON = 0;
    private int KEY_SHIFT_ON = 0;
    private int KEY_CTRL_ON = 0;
    public RemoteSpiceKeyboard(Resources resources, SpiceCommunicator r, RemoteCanvas v, Handler h, String layoutMapFile) throws IOException {
        super(r, v, h);
        context = v.getContext();
        this.table = loadKeyMap(resources, "layouts/" + layoutMapFile);
        this.KEY_ALT_ON = 0;
        this.KEY_SHIFT_ON = 0;
        this.KEY_SHIFT_ON = 0;
    }

    private HashMap<Integer, Integer[]> loadKeyMap(Resources r, String file) throws IOException {
        InputStream is;
        try {
            is = r.getAssets().open(file);
        } catch (IOException e) {
            // If layout map file was not found, load the default one.
            is = r.getAssets().open("layouts/" + Constants.DEFAULT_LAYOUT_MAP);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line = in.readLine();
        HashMap<Integer, Integer[]> table = new HashMap<Integer, Integer[]>(500);
        while (line != null) {
            //android.util.Log.i (TAG, "Layout " + file + " " + line);
            String[] tokens = line.split(" ");
            Integer[] scanCodes = new Integer[tokens.length - 1];
            for (int i = 1; i < tokens.length; i++) {
                scanCodes[i - 1] = Integer.parseInt(tokens[i]);
            }
            table.put(Integer.parseInt(tokens[0]), scanCodes);
            line = in.readLine();
        }
        return table;
    }

    /**
     * Sets the hardwareMetaState based on certain keys and scancodes being detected.
     * @param keyCode
     * @param event
     * @param down
     */
    private void setHardwareMetaState(int keyCode, KeyEvent event, boolean down) {
        // Detect whether this event is coming from a default hardware keyboard.
        boolean defaultHardwareKbd = (event.getDeviceId() == 0);

        int metaMask = 0;
        //Log.e(TAG,"[jcktest] setHardwareMetaState----defaultHardwareKbd:"+defaultHardwareKbd);
        //Log.e(TAG,"[jcktest] setHardwareMetaState----getScanCode:"+event.getScanCode());
        switch (event.getScanCode()) 
	    {
            case SCAN_LEFTCTRL: // 29
            case SCAN_RIGHTCTRL: // 97
                metaMask |= CTRL_MASK;
                break;
        }
        //Log.e(TAG,"[jcktest] setHardwareMetaState----keyCode:"+keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                metaMask |= CTRL_MASK;
                break;
            case KeyEvent.KEYCODE_ALT_LEFT:
                // Leaving KeyEvent.KEYCODE_ALT_LEFT for symbol input on hardware keyboards.
                if (!defaultHardwareKbd) {
                    metaMask |= ALT_MASK;
                }
                break;
            case KeyEvent.KEYCODE_ALT_RIGHT:
                metaMask |= RALT_MASK;
                break;
        }
        Log.e(TAG,"[jcktest] setHardwareMetaState----down:"+down+"metaMask:"+metaMask);
        if (!down) {
            hardwareMetaState &= ~metaMask;
        } else {
            hardwareMetaState |= metaMask;
        }
        Log.e(TAG,"[jcktest] setHardwareMetaState----hardwareMetaState:"+hardwareMetaState);
    }

    /**
     * Converts event meta state to our meta state.
     * @param event
     * @return
     */
    protected int convertEventMetaState(KeyEvent event, int eventMetaState) {
        int metaState = 0;
        int altMask = KeyEvent.META_ALT_RIGHT_ON;
        // Detect whether this event is coming from a default hardware keyboard.
        // We have to leave KeyEvent.KEYCODE_ALT_LEFT for symbol input on a default hardware keyboard.
        boolean defaultHardwareKbd = (event.getScanCode() != 0 && event.getDeviceId() == 0);
        Log.e(TAG,"[jcktest] keyevent-----defaultHardwareKbd:"+defaultHardwareKbd+"       bb:"+bb);
        if (!bb && !defaultHardwareKbd)
        {
            altMask = KeyEvent.META_ALT_MASK;
        }
        Log.e(TAG,"[jcktest] keyevent-----eventMetaState:"+eventMetaState);
        // Add shift, ctrl, alt, and super to metaState if necessary.
        if ((eventMetaState & 0x000000c1 /*KeyEvent.META_SHIFT_MASK*/) != 0) {
            metaState |= SHIFT_MASK;
            Log.e(TAG,"[jcktest] keyevent-----SHIFT_MASK");
        }
        if ((eventMetaState & 0x00007000 /*KeyEvent.META_CTRL_MASK*/) != 0) {
            metaState |= CTRL_MASK;
            Log.e(TAG,"[jcktest] keyevent-----CTRL_MASK");
        }
        if ((eventMetaState & altMask) != 0) {
            metaState |= ALT_MASK;
            Log.e(TAG,"[jcktest] keyevent-----ALT_MASK");
        }
        if ((eventMetaState & 0x00070000 /*KeyEvent.META_META_MASK*/) != 0) {
            metaState |= SUPER_MASK;
            Log.e(TAG,"[jcktest] keyevent-----SUPER_MASK");
        }
        Log.e(TAG,"[jcktest] convertEventMetaState-----metaState:"+metaState);
        return metaState;
    }

    public boolean processLocalKeyEvent(int keyCode, KeyEvent event, int additionalMetaState) {
        return keyEvent(keyCode, event, additionalMetaState);
    }

    //yxlei TODO
    public boolean keyEvent(int keyCode, KeyEvent event, int additionalMetaState)
    {
        Log.e(TAG, "[jcktest99999999]"+event.toString());

        int action = event.getAction();
        boolean down = (action == KeyEvent.ACTION_DOWN);
        // Combine current event meta state with any meta state passed in.
        int metaState = additionalMetaState | convertEventMetaState(event, event.getMetaState());
         /* TODO: Consider whether this is a good idea. At least some scan codes between
           my bluetooth keyboard and what the VM expects do not match. For example, d-pad does not send arrows.
        // If the event has a scan code, just send that along!
        }*/
        //jck add

        Log.e(TAG,"[jcktest1] keyevent--------------keyCode:"+keyCode+"getKeyCode:"+event.getKeyCode());
        Log.e(TAG,"[jcktest1] keyevent--------------down:"+down);
        Log.e(TAG,"[jcktest1] keyevent--------------metaState:"+metaState);
        Log.e(TAG,"[jcktest1] keyevent---KEY_ALT_ON:"+KEY_ALT_ON+"KEY_SHIFT_ON:"+KEY_SHIFT_ON+"KEY_CTRL_ON:"+KEY_CTRL_ON);
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                     if(down)
                    {
                        if(KEY_ALT_ON == 1)
                        {
                            Log.d(TAG,"[jcktest1] keyevent---KEYCODE_ALT_* is on for return\r\n");
                            return true;
                        }
                        KEY_ALT_ON = 1;
                    }
                    else
                    {
                        KEY_ALT_ON = 0;
                    }
                break;
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    if(down)
                    {
                        if(1 == KEY_SHIFT_ON)
                        {
                            Log.d(TAG,"[jcktest1] keyevent---KEYCODE_SHIFT_* is on for return\r\n");
                            return true;
                        }
                        KEY_SHIFT_ON = 1;
                    }
                    else
                    {
                        KEY_SHIFT_ON = 0;
                    }
                    break;
                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    if(down)
                    {
                        if(1 == KEY_CTRL_ON)
                        {
                            Log.d(TAG,"[jcktest1] keyevent---KEY_CTRL_* is on for return\r\n");
                            return true;
                        }
                        KEY_CTRL_ON = 1;
                    }
                    else
                    {
                        KEY_CTRL_ON = 0;
                    }
                    break;
                default:
                    break;
            }
        // Set the hardware meta state from any special keys pressed.
        setHardwareMetaState(keyCode, event, down);
        // Ignore menu key and handle other hardware buttons here.
        //if (keyCode == KeyEvent.KEYCODE_MENU ||
        if( canvas.getPointer().handleHardwareButtons(keyCode,
         event,metaState | onScreenMetaState | hardwareMetaState))
        {
            Log.e(TAG,"[jcktest1] 333333keyevent--------------handlerhandwarebuttons !!!!");
            return true;
        }
        if(event.getScanCode()!= 0 )
        {
            Log.e(TAG,"[jcktest1] 1111111keyevent--------------getScanCode:"+ event.getScanCode()+"event.getKeyCode():"+event.getKeyCode());
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_NUM_LOCK:
                {

                    if(((down == true)&&(0 == (event.getMetaState()& KeyEvent.META_NUM_LOCK_ON)))||
                    ((down == false)&&(0 != (event.getMetaState()& KeyEvent.META_NUM_LOCK_ON))))
                    {
                        writeKeyEvent(false,event.getKeyCode()+0x1000,0,down,false);
                    }
                    else
                    {
                        writeKeyEvent(false,event.getKeyCode(),0,down,false);
                    }
                }
                break;
                case KeyEvent.KEYCODE_CAPS_LOCK:
                {
                    if(((down == true)&&(0 == (event.getMetaState()& KeyEvent.META_CAPS_LOCK_ON)))||
                            ((down == false)&&(0 != (event.getMetaState()& KeyEvent.META_CAPS_LOCK_ON))))
                    {
                        writeKeyEvent(false,event.getKeyCode()+0x1000,0,down,false);
                    }
                    else
                    {
                        writeKeyEvent(false,event.getKeyCode(),0,down,false);
                    }
                }
                break;
                case KeyEvent.KEYCODE_SCROLL_LOCK:
                {
                    if(((down == true)&&(0 == (event.getMetaState()& KeyEvent.META_SCROLL_LOCK_ON)))||
                            ((down == false)&&(0 != (event.getMetaState()& KeyEvent.META_SCROLL_LOCK_ON))))
                    {
                        writeKeyEvent(false,event.getKeyCode()+0x1000,0,down,false);
                    }
                    else
                    {
                        writeKeyEvent(false,event.getKeyCode(),0,down,false);
                    }

                }
                break;
                default:
                    writeKeyEvent(false,event.getKeyCode(),event.getMetaState(),down,false);
                    break;
            }
            Log.e(TAG,"[jcktest1] keyevent--------------send success!!!!!!!!!");
            return true;
        }
          Log.e(TAG,"[jcktest1] 222keyEvent------------------------end\r\n");
         return true;
    }

    private void writeKeyEvent(boolean isUnicode, int code, int metaState, boolean down, boolean sendUpEvents)
    {
        if (down)
        {
            lastDownMetaState = metaState;
        }
        else
        {
            lastDownMetaState = 0;
        }

        if (isUnicode)
        {
            code |= UNICODE_MASK;
        }
        Log.e(TAG, "[jcktest]   Trying to convert keycode or masked unicode: " + code);
        Integer[] scanCode = null;
        try {
            scanCode = table.get(code);

            for (int i = 0; i < scanCode.length; i++) {
                int scode = scanCode[i];
                int meta = metaState;
                Log.e(TAG, "[jcktest]	Got back possibly masked scanCode: " + scode);

                Log.e(TAG, "[jcktest]	Will send scanCode: " + scode + " with meta: " + meta);
                rfb.writeKeyEvent(scode, meta, down);
                Log.e(TAG, "[jcktest]	Will send --------------end sendUpEvents:"+sendUpEvents);
                if (sendUpEvents) {
                    rfb.writeKeyEvent(scode, meta, false);
                    Log.i(TAG, "[jcktest]	UNsetting lastDownMetaState--------------");
                    lastDownMetaState = 0;
                }
            }
        } catch (NullPointerException e) {
        }
    }

    public void sendMetaKey(MetaKeyBean meta) {
        RemotePointer pointer = canvas.getPointer();
        int x = pointer.getX();
        int y = pointer.getY();

        if (meta.isMouseClick()) {
            //android.util.Log.e("RemoteRdpKeyboard", "is a mouse click");
            int button = meta.getMouseButtons();
            switch (button) {
                case RemoteVncPointer.MOUSE_BUTTON_LEFT:
                    pointer.processPointerEvent(x, y, MotionEvent.ACTION_DOWN, meta.getMetaFlags() | onScreenMetaState | hardwareMetaState,
                            true, false, false, false, 0);
                    break;
                case RemoteVncPointer.MOUSE_BUTTON_RIGHT:
                    pointer.processPointerEvent(x, y, MotionEvent.ACTION_DOWN, meta.getMetaFlags() | onScreenMetaState | hardwareMetaState,
                            true, true, false, false, 0);
                    break;
                case RemoteVncPointer.MOUSE_BUTTON_MIDDLE:
                    pointer.processPointerEvent(x, y, MotionEvent.ACTION_DOWN, meta.getMetaFlags() | onScreenMetaState | hardwareMetaState,
                            true, false, true, false, 0);
                    break;
                case RemoteVncPointer.MOUSE_BUTTON_SCROLL_UP:
                    pointer.processPointerEvent(x, y, MotionEvent.ACTION_MOVE, meta.getMetaFlags() | onScreenMetaState | hardwareMetaState,
                            true, false, false, true, 0);
                    break;
                case RemoteVncPointer.MOUSE_BUTTON_SCROLL_DOWN:
                    pointer.processPointerEvent(x, y, MotionEvent.ACTION_MOVE, meta.getMetaFlags() | onScreenMetaState | hardwareMetaState,
                            true, false, false, true, 1);
                    break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            pointer.processPointerEvent(x, y, MotionEvent.ACTION_UP, meta.getMetaFlags() | onScreenMetaState | hardwareMetaState,
                    false, false, false, false, 0);

            //rfb.writePointerEvent(x, y, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState, button);
            //rfb.writePointerEvent(x, y, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState, 0);
        } else if (meta.equals(MetaKeyBean.keyCtrlAltDel)) {
            rfb.writeKeyEvent(RemoteKeyboard.SCAN_DELETE, RemoteKeyboard.CTRL_MASK | RemoteKeyboard.ALT_MASK, true);
            rfb.writeKeyEvent(RemoteKeyboard.SCAN_DELETE, RemoteKeyboard.CTRL_MASK | RemoteKeyboard.ALT_MASK, false);
        } else {
            sendKeySym(meta.getKeySym(), meta.getMetaFlags());
        }
    }
}
