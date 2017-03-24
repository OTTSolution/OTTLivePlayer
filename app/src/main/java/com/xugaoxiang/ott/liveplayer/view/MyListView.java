package com.xugaoxiang.ott.liveplayer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.ListView;

/**
 * Created by user on 2016/11/29.
 */
public class MyListView extends ListView{

    private boolean isLongPress;
    private int keyFlag = 1;

    public MyListView(Context context) {
        super(context);
    }

    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                keyFlag++;
                if (keyFlag % 2 == 0){
                    return super.dispatchKeyEvent(event);
                }else {
                    return false;
                }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        keyFlag = 1;
        return super.onKeyUp(keyCode, event);
    }
}