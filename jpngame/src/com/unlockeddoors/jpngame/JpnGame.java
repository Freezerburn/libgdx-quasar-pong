package com.unlockeddoors.jpngame;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.Topic;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.unlockeddoors.jpngame.ents.TestActor;
import com.unlockeddoors.jpngame.input.KeyEvent;

public class JpnGame implements ApplicationListener, InputProcessor {
	private OrthographicCamera camera;
	private SpriteBatch batch;
	private Texture texture;
	private Sprite sprite;
    private Matrix4 restore = new Matrix4();

    public static BitmapFont font12;
    public static BitmapFont font18;
    public static BitmapFont font24;
    public static BitmapFont font32;
    public static BitmapFont font36;
    public static BitmapFont font48;

    public static Topic<KeyEvent> keyTopic = new Topic<KeyEvent>();

	@Override
	public void create() {
        try {
            Gdx.input.setInputProcessor(this);

            JpnGame.font12 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-12.fnt"));
            JpnGame.font18 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-18.fnt"));
            JpnGame.font24 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-24.fnt"));
            JpnGame.font32 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-32.fnt"));
            JpnGame.font36 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-36.fnt"));
            JpnGame.font48 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-48.fnt"));

            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();

            camera = new OrthographicCamera(1, h/w);
            batch = new SpriteBatch();

            texture = new Texture(Gdx.files.internal("data/libgdx.png"));
            texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

            TextureRegion region = new TextureRegion(texture, 0, 0, 512, 275);

            sprite = new Sprite(region);
            sprite.setSize(0.9f, 0.9f * sprite.getHeight() / sprite.getWidth());
            sprite.setOrigin(sprite.getWidth()/2, sprite.getHeight()/2);
            sprite.setPosition(-sprite.getWidth()/2, -sprite.getHeight()/2);
        }
        catch (GdxRuntimeException e) {
            e.getCause().printStackTrace();
            e.printStackTrace();
            Gdx.app.exit();
        }
        catch (Exception e) {
            e.printStackTrace();
            Gdx.app.exit();
        }
	}

	@Override
	public void dispose() {
		batch.dispose();
        texture.dispose();
	}

	@Override
	public void render() {		
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

        // Make sure we save off the current state of the projection matrix...
        restore.set(batch.getProjectionMatrix());
		batch.setProjectionMatrix(camera.combined);
		batch.begin();

        sprite.draw(batch);

		batch.end();

        // ...so that we can restore it here, before we draw fonts. Otherwise
        // the projection matrix will break how the fonts expect to be drawn.
        batch.setProjectionMatrix(restore);
        batch.begin();

        font32.setColor(Color.BLACK);
        font32.draw(batch, "Test foobar かきくけこ", 150, 150);

        batch.end();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

    @Override
    public boolean keyDown(int keycode) {
        if(keycode == Input.Keys.Q || keycode == Input.Keys.ESCAPE) {
            Gdx.app.exit();
        }
        else {
            new Fiber((SuspendableRunnable) () -> {
                KeyEvent event = KeyEvent.get(keycode, false);
                keyTopic.send(event);
            }).start();
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean keyTyped(char character) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean scrolled(int amount) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
