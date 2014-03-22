package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.concurrent.Phaser;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Pool;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/23/14
 * Time: 7:46 AM
 */
public class Pong implements ApplicationListener, InputProcessor {
    public static final MailboxConfig MAILBOX_CONFIG = new MailboxConfig(20, Channels.OverflowPolicy.THROW);

    public static SpriteBatch batch;
    public static ShapeRenderer shapeRenderer;
    public static OrthographicCamera camera;

    public static BitmapFont font12;
    public static BitmapFont font18;
    public static BitmapFont font24;
    public static BitmapFont font32;
    public static BitmapFont font36;
    public static BitmapFont font48;

    public static Array<ShapeRunnableContainer> postedShapeRunnables = new Array<>(100);
    public static Array<Runnable> postedSpriteRunnables = new Array<>(100);
    public static Pool<ShapeRunnableContainer> shapeRunnableContainerPool = new Pool<ShapeRunnableContainer>() {
        @Override
        protected ShapeRunnableContainer newObject() {
            return new ShapeRunnableContainer();
        }
    };

    public static Array<ActorRef<Event>> actors = new Array<>(10);
    public static final FiberForkJoinScheduler scheduler = new FiberForkJoinScheduler("Background Fibers", 4);

    public static final Phaser tickSynchPoint = new Phaser();
    public static final Phaser collisionSynchPoint = new Phaser();
    public static final Phaser renderSynchPoint = new Phaser();

    static class ShapeRunnableContainer implements Pool.Poolable {
        Runnable r;
        ShapeRenderer.ShapeType type;

        public ShapeRunnableContainer() {
        }

        public void init(Runnable r, ShapeRenderer.ShapeType type) {
            this.r = r;
            this.type = type;
        }

        @Override
        public void reset() {
            this.r = null;
            this.type = null;
        }
    }

    static class ShapeRunnableRemover {
        protected ShapeRunnableContainer container;

        ShapeRunnableRemover(ShapeRunnableContainer container) {
            this.container = container;
        }

        public void remove() {
            postedShapeRunnables.removeValue(container, true);
            shapeRunnableContainerPool.free(container);
        }
    }

    public static ShapeRunnableRemover postShapeRunnable(Runnable r, ShapeRenderer.ShapeType type) {
        ShapeRunnableContainer container = shapeRunnableContainerPool.obtain();
        container.init(r, type);
        postedShapeRunnables.add(container);
        return new ShapeRunnableRemover(container);
    }

    public static void postSpriteRunnable(Runnable r) {
        postedSpriteRunnables.add(r);
    }

    @Override
    public void create() {
        try {
            Gdx.input.setInputProcessor(this);

            font12 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-12.fnt"));
            font18 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-18.fnt"));
            font24 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-24.fnt"));
            font32 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-32.fnt"));
            font36 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-36.fnt"));
            font48 = new BitmapFont(Gdx.files.internal("font/Hiragino Sans GB W3-48.fnt"));

            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();

            camera = new OrthographicCamera(w, h);
            camera.setToOrtho(false);
            batch = new SpriteBatch();
            shapeRenderer = new ShapeRenderer();

            final float wallWidth = 15;
            final float wallBufferToEdge = 30;

            final float paddleBufferToEdge = 20;
            final float paddleWidth = 100;
            final float paddleHeight = 20;
            final float paddleX = w / 2.0f - paddleWidth / 2.0f;
            final float paddleY = paddleHeight + paddleBufferToEdge;

            tickSynchPoint.register();
            renderSynchPoint.register();

            actors.add(new Paddle(new Rectangle(paddleX, paddleY, Paddle.WIDTH, Paddle.HEIGHT)).spawn(scheduler));
            actors.add(new Wall(new Rectangle(wallBufferToEdge, 0, wallWidth, h)).spawn(scheduler));
            actors.add(new Wall(new Rectangle(w - wallBufferToEdge - wallWidth, 0, wallWidth, h)).spawn(scheduler));
            actors.add(new Ball(new Rectangle(w / 2.0f, h / 2.0f, Ball.SIZE, Ball.SIZE), new Vector2(100, 100)).spawn(scheduler));
            actors.add(new Enemy(new Rectangle(paddleX, h - paddleBufferToEdge, Paddle.WIDTH, Paddle.HEIGHT)).spawn(scheduler));
            actors.add(new CollisionFinder().spawn(scheduler));
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
    public void resize(int width, int height) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void render() {
        try {
            Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            System.out.println("Arriving at tickSynchPoint.");
            tickSynchPoint.arrive();
            System.out.println("Arriving and awaiting renderSynchPoint.");
            renderSynchPoint.arriveAndAwaitAdvance();
            System.out.println("About to start rendering.");

            postedShapeRunnables.sort((one, two) -> one.type.compareTo(two.type));
            shapeRenderer.setProjectionMatrix(camera.combined);

            // Draw filled shapes.
            Iterable<ShapeRunnableContainer> itFilled = postedShapeRunnables.select(
                    (container) -> null != container && container.type.equals(ShapeRenderer.ShapeType.Filled)
            );
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            itFilled.forEach((container) -> container.r.run());
            shapeRenderer.end();

            // Draw line shapes.
            Iterable<ShapeRunnableContainer> itLine = postedShapeRunnables.select(
                    (container) -> null != container && container.type.equals(ShapeRenderer.ShapeType.Line)
            );
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            itLine.forEach((container) -> container.r.run());
            shapeRenderer.end();

            // Draw point shapes.
            Iterable<ShapeRunnableContainer> itPoint = postedShapeRunnables.select(
                    (container) -> null != container && container.type.equals(ShapeRenderer.ShapeType.Point)
            );
            shapeRenderer.begin(ShapeRenderer.ShapeType.Point);
            itPoint.forEach((container) -> container.r.run());
            shapeRenderer.end();

            // Draw sprites.
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            postedSpriteRunnables.forEach(Runnable::run);
            batch.end();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void resume() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void dispose() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean keyDown(int keycode) {
        try {
            if(keycode == Input.Keys.Q) {
                Gdx.app.exit();
            }
            else {
                new Fiber(scheduler, () -> {
                    final Event e = new Event.InputEvent(keycode, true);
                    for(int i = 0; i < actors.size; i++) {
                        actors.get(i).send(e);
                    }
                }).start();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean keyUp(int keycode) {
        try {
            new Fiber(scheduler, () -> {
                final Event e = new Event.InputEvent(keycode, false);
                for(int i = 0; i < actors.size; i++) {
                    actors.get(i).send(e);
                }
            }).start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
