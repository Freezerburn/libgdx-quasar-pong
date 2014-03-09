package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.StrandLocalRandom;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Created by freezerburn on 3/4/14.
 */
public class Enemy extends Paddle {
    // Time (in seconds) it takes to move from the last offset to the new offset.
    public static final float TIME_TO_OFFSET = 2.5f;

    ActorRef<Event> ball;
    Rectangle leftWall, rightWall;
    Vector2 hitOffset;
    Vector2 nextOffset;
    long timeToStopMoving;
    float ballWidth;

    public Enemy(Rectangle rect) {
        super(rect);
        hitOffset = new Vector2();
        nextOffset = new Vector2();

        new Fiber(Pong.scheduler, () -> {
            while(null == ball) {
                ball = Registry.get("Ball");
            }
            Array<ActorRef<Event>> walls = new Array<>(2);
            while(walls.size != 2) {
                for(int i = 0; i < Pong.actors.size; i++) {
                    ActorRef<Event> ref = Pong.actors.get(i);
                    if(ref.getName().startsWith("Wall")) {
                        if(!walls.contains(ref, true)) {
                            walls.add(ref);
                        }
                    }
                }
            }
            Rectangle wall1 = (Rectangle) RequestReplyHelper.call(walls.get(0), new Event(Event.Type.REQUEST_RECT));
            Rectangle wall2 = (Rectangle) RequestReplyHelper.call(walls.get(1), new Event(Event.Type.REQUEST_RECT));
            if(wall1.x < wall2.x) {
                leftWall = wall1;
                rightWall = wall2;
            }
            else {
                leftWall = wall2;
                rightWall = wall1;
            }
        }).start();
    }

    @Override
    void tick(float s) throws SuspendExecution, InterruptedException {
        if(null != ball) {
            Rectangle ballRect = (Rectangle) RequestReplyHelper.call(ball, new Event(Event.Type.REQUEST_RECT));
            if(ballWidth == 0) {
                ballWidth = ballRect.width;
            }
            rect.x = ballRect.x - rect.width / 2.0f + ballRect.width / 2.0f;
            rect.x += hitOffset.x;

            if(System.nanoTime() < timeToStopMoving) {
                hitOffset.x += nextOffset.x * (s / TIME_TO_OFFSET);
            }
        }

        if(leftWall != null && rightWall != null) {
            if(rect.x < leftWall.x + leftWall.width) {
                rect.x = leftWall.x + leftWall.width;
            }
            else if(rect.x + rect.width > rightWall.x) {
                rect.x = rightWall.x - rect.width;
            }
        }
    }

    @Override
    void handleCollisions(Array<ActorRef<Event>> between, Array<Vector2> deltas) {
        System.out.println("Between enemy: " + between);
        for(int i = 0; i < between.size; i++) {
            if(between.get(i).getName().equals("Ball") && timeToStopMoving < System.nanoTime()) {
                // Choose a number within the width of the paddle, with a little taken away to avoid
                // hitting the very edges.
                nextOffset.x = (float) StrandLocalRandom.current().nextGaussian() * (rect.width / 2.0f - ballWidth);
                nextOffset.x *= StrandLocalRandom.current().nextGaussian() < 0.5 ? -1 : 1;
                timeToStopMoving = System.nanoTime() + Math.round(TIME_TO_OFFSET * 1000000000);
            }
        }
    }
}
