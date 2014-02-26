package com.unlockeddoors;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.unlockeddoors.pong.Pong;

public class Main {
    public static void main(String[] args) {
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "jpngame";
        cfg.useGL20 = true;
        cfg.vSyncEnabled = true;
        cfg.width = 640;
        cfg.height = 480;

//		new LwjglApplication(new JpnGame(), cfg);
        new LwjglApplication(new Pong(), cfg);
    }
}

