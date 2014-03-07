package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Created by freezerburn on 3/4/14.
 */
public class Enemy extends Paddle {
    ActorRef<Event> ball;

    public Enemy(Rectangle rect) {
        super(rect);

        new Fiber(Pong.scheduler, () -> {
            while(null == ball) {
                ball = Registry.get("Ball");
            }
        }).start();
    }

    @Override
    void tick(float s) throws SuspendExecution, InterruptedException {
        if(null != ball) {
            Rectangle ballRect = (Rectangle) RequestReplyHelper.call(ball, new Event(Event.Type.REQUEST_RECT));
            rect.x = ballRect.x + ballRect.width / 2.0f;
        }
    }

    @Override
    void handleCollisions(Array<ActorRef<Event>> between, Array<Vector2> deltas) {
    }
}
