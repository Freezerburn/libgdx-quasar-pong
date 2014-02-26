package com.unlockeddoors.jpngame.ents;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/17/14
 * Time: 6:58 PM
 */
public class BattleCardTarget implements Disposable {
    private Sprite background;

    public BattleCardTarget(Definition def) {
        Pixmap pix = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
        pix.setColor(Color.WHITE);
        pix.fillRectangle(0, 0, 100, 100);
        Texture tex = new Texture(pix);
        pix.dispose();
        TextureRegion region = new TextureRegion(tex);
        background = new Sprite(region);
    }

    @Override
    public void dispose() {
    }

    public static class Definition {
        public Definition(String jpnText, String romaji) {
            this.jpnText = jpnText;
            this.romaji = romaji;
        }

        public String jpnText;
        public String romaji;
    }
}
