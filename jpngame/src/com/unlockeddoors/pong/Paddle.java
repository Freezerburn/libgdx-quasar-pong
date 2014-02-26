package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.DelayedVal;
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

    boolean going = true;
    Rectangle rect;
    Val<Rectangle> collisionRect;
    Runnable render;
    Vector2 velocity;
    Pong.ShapeRunnableRemover remover;
    long lastTick;

    public Paddle(Rectangle rect) {
        super("Paddle", Pong.MAILBOX_CONFIG);
        this.rect = rect;
        this.velocity = new Vector2();
        this.collisionRect = new Val<Rectangle>();

        render = () -> {
            Pong.shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
        };
        remover = Pong.postShapeRunnable(render, ShapeRenderer.ShapeType.Filled);
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        while(going) {
            System.out.println(this.getQueueLength());
            final Event e = receive();
            long time = System.nanoTime();
            System.out.println("Time since last event: " + (time - lastTick) / 1000000.0f);
            lastTick = time;
            System.out.println(e.type);
            switch (e.type) {
                case TICK:
                    final Event.TickEvent tick = (Event.TickEvent) e;
                    tick(tick.getS());
                    break;
                case INPUT:
                    final Event.InputEvent input = (Event.InputEvent) e;
                    handleInput(input.keycode, input.pushed);
                    break;
                case REQUEST_RECT:
                    RequestReplyHelper.reply(e, collisionRect);
                    break;
                case COLLISIONS:
                    final Event.CollisionsEvent collisionsEvent = (Event.CollisionsEvent) e;
                    handleCollisions(collisionsEvent.between, collisionsEvent.deltas);
                    break;
                case KILL:
                    going = false;
                    break;
                default:
                    System.out.println("Unhandled Event: " + e);
            }
        }
        remover.remove();
        return null;
    }

    private void tick(float s) {
        try {
            rect.x += velocity.x * s;
            collisionRect.set(new Rectangle(rect));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCollisions(Array<ActorRef<Event>> colliders, Array<Rectangle> deltas)
            throws SuspendExecution, InterruptedException {
        System.out.println(colliders);
        System.out.println(deltas);
        for(int i = 0; i < colliders.size; i++) {
            ActorRef<Event> ref = colliders.get(i);
            Object name = RequestReplyHelper.call(ref, new Event(Event.Type.REQUEST_NAME));
            if(name.equals("Wall")) {
                Rectangle delta = deltas.get(i);
                rect.x += delta.width;
                collisionRect.set(new Rectangle(rect));
            }
        }
    }

    private void handleInput(int keycode, boolean pushed) {
        if(keycode == Input.Keys.LEFT) {
            velocity.x += pushed ? -SPEED : SPEED;
        }
        else if(keycode == Input.Keys.RIGHT) {
            velocity.x += pushed ? SPEED : -SPEED;
        }
    }
}
