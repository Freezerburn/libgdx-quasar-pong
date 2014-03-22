package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/23/14
 * Time: 7:49 AM
 */
public class Paddle extends BasicActor<Event, Void> {
    public static final float SPEED = 250.0f;
    public static final float WIDTH = 100;
    public static final float HEIGHT = 2;

    boolean going = true;
    Rectangle rect;
    Runnable render;
    Vector2 velocity;
    Pong.ShapeRunnableRemover remover;
    Fiber tick;

    public Paddle(Rectangle rect) {
        super("Paddle", Pong.MAILBOX_CONFIG);

//        Pong.collisionSynchPoint.register();

        this.rect = rect;
        this.velocity = new Vector2();

        render = () -> {
            Pong.shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
        };
        remover = Pong.postShapeRunnable(render, ShapeRenderer.ShapeType.Filled);

        Pong.tickSynchPoint.register();
        Pong.collisionSynchPoint.register();
        tick = new Fiber(Pong.scheduler, () -> {
            while(going) {
                System.out.println("Paddle arriving and awaiting tick.");
                Pong.tickSynchPoint.arriveAndAwaitAdvance();
                if(going) {
                    tick(Gdx.graphics.getDeltaTime());
                }
                System.out.println("Paddle arriving and awaiting collision.");
                Pong.collisionSynchPoint.arriveAndAwaitAdvance();
            }
            Pong.tickSynchPoint.arriveAndDeregister();
            Pong.collisionSynchPoint.arriveAndDeregister();
        }).start();
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        try {
            while(going) {
                final Event e = receive();
                switch (e.type) {
                    case INPUT:
                        final Event.InputEvent input = (Event.InputEvent) e;
                        handleInput(input.keycode, input.pushed);
                        break;
                    case REQUEST_RECT:
                        System.out.println(ref() + " got REQUEST_RECT event. Giving back rect: " + rect);
                        RequestReplyHelper.reply(e, rect);
                        break;
                    case COLLISIONS:
                        final Event.CollisionsEvent collisionsEvent = (Event.CollisionsEvent) e;
                        System.out.println("Paddle handling collision.");
                        handleCollisions(collisionsEvent.between, collisionsEvent.deltas);
                        System.out.println("Paddle arriving and deregistering render.");
                        Pong.renderSynchPoint.arriveAndDeregister();
                        break;
                    case KILL:
                        going = false;
                        break;
                    default:
                        System.out.println("Unhandled Event: " + e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        remover.remove();
        return null;
    }

    void tick(float s) throws SuspendExecution, InterruptedException {
        try {
            rect.x += velocity.x * s;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void handleCollisions(Array<ActorRef<Event>> colliders, Array<Vector2> deltas)
            throws SuspendExecution, InterruptedException {
        for(int i = 0; i < colliders.size; i++) {
            ActorRef<Event> ref = colliders.get(i);
            String name = ref.getName();
            if(name.startsWith("Wall")) {
                Vector2 delta = deltas.get(i);
                rect.x += delta.x;
            }
        }
    }

    void handleInput(int keycode, boolean pushed) {
        if(keycode == Input.Keys.LEFT) {
            velocity.x += pushed ? -SPEED : SPEED;
        }
        else if(keycode == Input.Keys.RIGHT) {
            velocity.x += pushed ? SPEED : -SPEED;
        }
    }
}
