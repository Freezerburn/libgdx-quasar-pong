package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/23/14
 * Time: 4:28 PM
 */
public class CollisionFinder extends BasicActor<Event, Void> {
    static class PooledRect implements Pool.Poolable {
        Rectangle rect = new Rectangle();

        public void init(Rectangle r) {
            rect.set(r);
        }

        @Override
        public void reset() {
            rect.set(0, 0, 0, 0);
        }
    }

    public static final int NUMBER_CHILD_ACTORS = 4;
    static final Pool<PooledRect> rectPool = new Pool<PooledRect>() {
        @Override
        protected PooledRect newObject() {
            return new PooledRect();
        }
    };
    static final Array<PooledRect> collisionSnapshot = new Array<>(100);

    public boolean going = true;
    HashMap<String, Rectangle> positionMap = new HashMap<>();
    ReentrantLock lock = new ReentrantLock();

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

                        // Reclaim all pooled rectangles and clear out the snapshot.
                        for(int i = 0; i < collisionSnapshot.size; i++) {
                            rectPool.freeAll(collisionSnapshot);
                        }
                        collisionSnapshot.clear();

                        // Get all rectangles to create a snapshot of the current collision environment.
                        final Event requestRect = new Event(Event.Type.REQUEST_RECT);
                        for(int i = 0; i < Pong.actors.size; i++) {
                            ActorRef<Event> ref = Pong.actors.get(i);
                            // Ignore ourselves.
                            if(ref == ref()) {
                                continue;
                            }
                            Rectangle rect = (Rectangle) RequestReplyHelper.call(ref, requestRect);
                            PooledRect pooled = rectPool.obtain();
                            pooled.init(rect);
                            collisionSnapshot.add(pooled);
                        }

                        // Find collisions for all actors in the current snapshot.
                        for(int i = 0; i < Pong.actors.size; i++) {
                            ActorRef<Event> ref = Pong.actors.get(i);
                            if(ref == ref()) {
                                continue;
                            }

                            final int stored = i;
                            Pong.collisionEndPhaser.register();
                            new Fiber(Pong.scheduler, () -> {
                                try {
                                    Rectangle rect = collisionSnapshot.get(stored).rect;
                                    Array<ActorRef<Event>> colliders = new Array<>(4);
                                    Array<Vector2> deltas = new Array<>(4);
                                    Vector2 delta = new Vector2();
                                    for(int j = 0; j < Pong.actors.size; j++) {
                                        if(j == stored || Pong.actors.get(j) == ref()) {
                                            continue;
                                        }
                                        Rectangle otherRect = collisionSnapshot.get(j).rect;
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
