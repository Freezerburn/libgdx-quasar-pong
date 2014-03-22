package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.concurrent.TimeUnit;

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
    Fiber tick;

    public Ball(Rectangle rect, Vector2 velocity) {
        super("Ball", Pong.MAILBOX_CONFIG);

        this.rect = rect;
        this.velocity = velocity;

//        Pong.collisionSynchPoint.register();
        Pong.postShapeRunnable(
                () -> Pong.shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height),
                ShapeRenderer.ShapeType.Filled
        );

        Pong.tickSynchPoint.register();
        Pong.collisionSynchPoint.register();
        tick = new Fiber(Pong.scheduler, () -> {
            while(going) {
                System.out.println("Ball arriving and awaiting tick.");
                Pong.tickSynchPoint.arriveAndAwaitAdvance();
                doTick(Gdx.graphics.getDeltaTime());
                System.out.println("Ball arriving and awaiting collision.");
                Pong.collisionSynchPoint.arriveAndAwaitAdvance();
            }
            Pong.tickSynchPoint.arriveAndDeregister();
            Pong.collisionSynchPoint.arriveAndDeregister();
        }).start();
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        Registry.set("Ball", ref());
        while(going) {
            final Event e = receive();
            if(null != e) {
                switch (e.type) {
                    case COLLISIONS:
                        Event.CollisionsEvent ce = (Event.CollisionsEvent)e;
                        System.out.println("Ball handling collision.");
                        doCollision(ce.between, ce.deltas);
                        System.out.println("Ball arriving and deregistering render.");
                        Pong.renderSynchPoint.arriveAndDeregister();
                        break;
                    case REQUEST_RECT:
                        RequestReplyHelper.reply(e, rect);
                        break;
                    case KILL:
                        going = false;
                        break;
                }
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
