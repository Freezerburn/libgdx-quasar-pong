package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.SuspendExecution;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/23/14
 * Time: 10:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class Ball extends BasicActor<Event, Void> {
    static final float MAX_PERCENT = 0.6f;
    static final float ADD_VEL_PER_HIT = 11.0f;
    public static final float SIZE = 19;

    Rectangle rect;
    Vector2 velocity;
    boolean going = true;

    public Ball(Rectangle rect, Vector2 velocity) {
        super("Ball", Pong.MAILBOX_CONFIG);

        this.rect = rect;
        this.velocity = velocity;

        Pong.collisionPhaser.register();
        Pong.postShapeRunnable(
                () -> Pong.shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height),
                ShapeRenderer.ShapeType.Filled
        );
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        System.out.println("Setting Ball to " + ref() + " in registry.");
        Registry.set("Ball", ref());
        System.out.println("Ball now looping until killed.");
        while(going) {
            System.out.println("Ball waiting for an event.");
            Event e;
            try {
                e = receive();
                System.out.println("Ball got an event.");
            } catch (Exception e1) {
                System.out.println("Ball: Exception while getting an event.");
                e = new Event(Event.Type.KILL);
                System.out.println("Message: " + e1.getMessage());
                System.out.println("Cause: " + e1.getCause());
                e1.printStackTrace();
            }
            System.out.println("Ball got an event: " + e);
            switch (e.type) {
                case TICK:
                    Event.TickEvent te = (Event.TickEvent)e;
                    doTick(te.getS());
                    Pong.collisionPhaser.arrive();
                    break;
                case COLLISIONS:
                    Event.CollisionsEvent ce = (Event.CollisionsEvent)e;
                    doCollision(ce.between, ce.deltas);
                    Pong.collisionEndPhaser.arriveAndDeregister();
                    break;
                case REQUEST_RECT:
                    RequestReplyHelper.reply(e, rect);
                    break;
                case KILL:
                    going = false;
                    Pong.collisionPhaser.arriveAndDeregister();
                    break;
            }
        }
        return null;
    }

    void doTick(float s) {
        rect.x += velocity.x * s;
        rect.y += velocity.y * s;

        if(rect.y < 0) {
            velocity.y = -velocity.y;
        }
        else if(rect.y + rect.height > Pong.camera.viewportHeight) {
            velocity.y = -velocity.y;
        }
    }

    void doCollision(Array<ActorRef<Event>> between, Array<Vector2> deltas) throws SuspendExecution, InterruptedException {
//        System.out.println("Between ball: " + between);
        for(int i = 0; i < between.size; i++) {
            final ActorRef<Event> a = between.get(i);
            String name = a.getName();
            if(name.equals("Paddle")) {
                Rectangle rect = (Rectangle) RequestReplyHelper.call(a, new Event(Event.Type.REQUEST_RECT));
                float offset = this.rect.getCenter(new Vector2()).x - rect.getCenter(new Vector2()).x;
                float sign = offset < 0 ? -1 : 1;
                float offsetPercent = Math.min(Math.abs(offset / (rect.width / 2.0f)), MAX_PERCENT) * sign;
                float totalVelocity = Math.abs(velocity.x) + Math.abs(velocity.y) + ADD_VEL_PER_HIT;
                velocity.x = totalVelocity * offsetPercent;
                velocity.y = (velocity.y < 0 ? 1 : -1) * (totalVelocity - Math.abs(velocity.x));

//                velocity.y = -velocity.y;
                this.rect.y += deltas.get(i).y;
            }
            else if(name.startsWith("Wall")) {
                velocity.x = -velocity.x;
            }
        }
    }
}
