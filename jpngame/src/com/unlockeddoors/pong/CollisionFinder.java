package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/23/14
 * Time: 4:28 PM
 */
public class CollisionFinder extends BasicActor<Event, Void> {
    public static final int NUMBER_CHILD_ACTORS = 4;

    public boolean going = true;
    HashMap<String, Rectangle> positionMap = new HashMap<>();

    public CollisionFinder() {
        super("CollisionFinder", Pong.MAILBOX_CONFIG);
        Pong.collisionPhaser.register();
        Pong.collisionEndPhaser.register();
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        try {
            while(going) {
                final Event e = receive();
                switch (e.type) {
                    case TICK:
                        Pong.collisionPhaser.arriveAndAwaitAdvance();
                        for(int i = 0; i < Pong.actors.size; i++) {
                            ActorRef<Event> ref = Pong.actors.get(i);
                            if(ref == ref()) {
                                continue;
                            }

                            final int stored = i;
                            Pong.collisionEndPhaser.register();
                            new Fiber(Pong.scheduler, () -> {
                                try {
                                    Rectangle rect = (Rectangle)RequestReplyHelper.call(ref, new Event(Event.Type.REQUEST_RECT));
                                    Array<ActorRef<Event>> colliders = new Array<>(4);
                                    Array<Vector2> deltas = new Array<>(4);
                                    Vector2 delta = new Vector2();
                                    for(int j = 0; j < Pong.actors.size; j++) {
                                        if(j == stored || Pong.actors.get(j) == ref()) {
                                            continue;
                                        }
                                        Rectangle otherRect = (Rectangle)RequestReplyHelper.call(Pong.actors.get(j), new Event(Event.Type.REQUEST_RECT));
                                        if(aabbCollision(rect, otherRect, delta)) {
                                            colliders.add(Pong.actors.get(j));
                                            deltas.add(new Vector2(delta));
                                        }
                                    }
                                    if(colliders.size > 0) {
                                        ref.send(new Event.CollisionsEvent(colliders, deltas));
                                    }
                                    else {
                                        Pong.collisionEndPhaser.arriveAndDeregister();
                                    }
                                }
                                catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }).start();
                        }
                        Pong.collisionEndPhaser.arrive();
                        break;
                    case KILL:
                        going = false;
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    bool AABBCollision(Collidable *body1, Collidable *body2, Vector2 &normal) {
//        // The normal vector has to be reset every time this function is called.
//        // At this point we do not know which of its components will change to push the object being collided with.
//        // The other component is always zero.
//        normal.x = 0;
//        normal.y = 0;
//
//        Vector2 distance, absDistance;
//
//        // xMag and yMag represent the magnitudes of the x and y components of the normal vector.
//        float xMag, yMag;
//
//        // Calculate the difference in position of the two rectangles.
//        distance = body2->position - body1->position;
//
//        // xAdd is the combined half-widths of the two rectangles.
//        // yAdd is the combined half-heights of the two rectangles.
//        float xAdd = ((body1->size.x * body1->scale.x) + (body2->size.x * body2->scale.x)) / 2.0f;
//        float yAdd = ((body1->size.y * body1->scale.y) + (body2->size.y * body2->scale.y)) / 2.0f;
//
//        //printf("body1.size %f, %f\n", body1->size.x, body1->size.y);
////   printf("body2.size %f, %f\n", body2->size.x, body2->size.y);
//
//        // Calculate absDistance, according to distance.
//        // This will actually be used to determine whether or not the two rectangles are colliding.
//        (distance.x < 0) ? absDistance.x = distance.x * -1 : absDistance.x = distance.x;
//        (distance.y < 0) ? absDistance.y = distance.y * -1 : absDistance.y = distance.y;
//
//        // The two rectangles are not colliding if both of the following statements evaluate to false:
//        // 1. The distance between their x position is less than their combined half-widths.
//        // 2. The distance between their y position is less than their combined half-heights.
//        // So return false as soon as we know there's no collision!
//        if(!((absDistance.x < xAdd) && (absDistance.y < yAdd))) return false;
//
//        // Otherwise, there is a collision:
//
//        // The magnitude of the normal vector is determined by the overlap in the rectangles.
//        xMag = xAdd - absDistance.x;
//        yMag = yAdd - absDistance.y;
//
//        // Only adjust the normal vector in the direction of the least significant overlap.
//        if(xMag < yMag)
//            normal.x = (distance.x > 0) ? xMag : -xMag;
//        else
//            normal.y = (distance.y > 0) ? yMag : -yMag;
//
//        return true;
//    }

    private boolean aabbCollision(Rectangle r1, Rectangle r2, Vector2 normal) {
        normal.x = 0;
        normal.y = 0;

        Vector2 distance, absDistance = new Vector2();
        float xMag, yMag;

        Vector2 r1Pos = r1.getCenter(new Vector2());
        Vector2 r2Pos = r2.getCenter(new Vector2());
        distance = r2Pos.sub(r1Pos);

        float xAdd = (r1.width + r2.width) / 2.0f;
        float yAdd = (r1.height + r2.height) / 2.0f;

        absDistance.x = (distance.x < 0) ? distance.x * -1 : distance.x;
        absDistance.y = (distance.y < 0) ? distance.y * -1 : distance.y;

        if(!((absDistance.x < xAdd) && (absDistance.y < yAdd))) {
            return false;
        }

        xMag = xAdd - absDistance.x;
        yMag = yAdd - absDistance.y;

        if(xMag < yMag) {
            normal.x = (distance.x > 0) ? -xMag : xMag;
        }
        else {
            normal.y = (distance.y > 0) ? -yMag : yMag;
        }

        return true;
    }
}
