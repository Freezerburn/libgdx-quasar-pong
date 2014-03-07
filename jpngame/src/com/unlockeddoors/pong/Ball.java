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
        Registry.set("Ball", ref());
        while(going) {
            final Event e = receive();
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
                case REQUEST_NAME:
                    System.out.println("Ball responding to name request.");
                    RequestReplyHelper.reply(e, "Ball");
                    System.out.println("Done replying.");
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
        for(int i = 0; i < between.size; i++) {
            final ActorRef<Event> a = between.get(i);
            final Object name = RequestReplyHelper.call(a, new Event(Event.Type.REQUEST_NAME));
            if(name.equals("Paddle")) {
                velocity.y = -velocity.y;
                rect.y += deltas.get(i).y;
            }
            else if(name.equals("Wall")) {
                velocity.x = -velocity.x;
            }
        }
    }
}
