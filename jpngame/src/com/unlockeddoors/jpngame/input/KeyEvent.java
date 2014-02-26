package com.unlockeddoors.jpngame.input;

import com.badlogic.gdx.utils.Array;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/22/14
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class KeyEvent {
    private static Array<KeyEvent> pool = new Array<KeyEvent>(10);

    public int keyCode;
    public boolean pushed;

    private KeyEvent() {
    }

    public static KeyEvent get(int keyCode, boolean pushed) {
       if(pool.size > 0) {
           return pool.pop();
       }
       else {
           KeyEvent ret = new KeyEvent();
           ret.keyCode = keyCode;
           ret.pushed = pushed;
           return ret;
       }
    }

    public static void recycle(KeyEvent event) {
        pool.add(event);
    }
}
