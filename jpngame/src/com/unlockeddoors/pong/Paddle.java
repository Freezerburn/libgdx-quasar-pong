package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.SuspendExecution;
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
    Runnable render;
    Vector2 velocity;
    Pong.ShapeRunnableRemover remover;

    public Paddle(Rectangle rect) {
        super("Paddle", Pong.MAILBOX_CONFIG);

        Pong.collisionPhaser.register();

        this.rect = rect;
        this.velocity = new Vector2();

        render = () -> {
            Pong.shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
        };
        remover = Pong.postShapeRunnable(render, ShapeRenderer.ShapeType.Filled);
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        while(going) {
            final Event e = receive();
            switch (e.type) {
                case TICK:
                    final Event.TickEvent tick = (Event.TickEvent) e;
                    tick(tick.getS());
                    Pong.collisionPhaser.arrive();
                    break;
                case INPUT:
                    final Event.InputEvent input = (Event.InputEvent) e;
                    handleInput(input.keycode, input.pushed);
                    break;
                case REQUEST_RECT:
                    RequestReplyHelper.reply(e, rect);
                    break;
                case REQUEST_NAME:
                    RequestReplyHelper.reply(e, "Paddle");
                    break;
                case COLLISIONS:
                    System.out.println("Found collision event.");
                    final Event.CollisionsEvent collisionsEvent = (Event.CollisionsEvent) e;
                    System.out.println("Handling collision.");
                    handleCollisions(collisionsEvent.between, collisionsEvent.deltas);
                    System.out.println("Arriving and deregistering.");
                    Pong.collisionEndPhaser.arriveAndDeregister();
                    System.out.println("Breaking.");
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

    void tick(float s) throws SuspendExecution, InterruptedException {
        try {
            rect.x += velocity.x * s;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void handleCollisions(Array<ActorRef<Event>> colliders, Array<Vector2> deltas)
            throws SuspendExecution, InterruptedException {
//        System.out.println(colliders);
//        System.out.println(deltas);
        for(int i = 0; i < colliders.size; i++) {
            System.out.println("Getting actor ref " + i);
            ActorRef<Event> ref = colliders.get(i);
            System.out.println("Requesting name for actor ref " + ref);
            Object name = RequestReplyHelper.call(ref, new Event(Event.Type.REQUEST_NAME));
            System.out.println("Got name: " + name);
            if(name.equals("Wall")) {
                System.out.println("Wall, so offsetting.");
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
