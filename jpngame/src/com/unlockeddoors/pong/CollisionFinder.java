package com.unlockeddoors.pong;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.DelayedVal;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        try {
            while(going) {
                final Event e = receive();
                switch (e.type) {
                    case TICK:
                        for(int i = 0; i < Pong.actors.size; i++) {
                            ActorRef<Event> ref = Pong.actors.get(i);
                            if(ref == ref()) {
                                continue;
                            }

                            final int stored = i;
                            new Fiber(Pong.scheduler, () -> {
                                try {
                                    Val<Rectangle> val = (Val<Rectangle>)RequestReplyHelper.call(ref, new Event(Event.Type.REQUEST_RECT));
                                    final Rectangle[] rect = new Rectangle[1];
                                    val.use((r) -> rect[0] = r);
                                    Array<ActorRef<Event>> colliders = new Array<>(4);
                                    Array<Rectangle> deltas = new Array<>(4);
                                    Rectangle delta = new Rectangle();
                                    for(int j = 0; j < Pong.actors.size; j++) {
                                        if(j == stored || Pong.actors.get(j) == ref()) {
                                            continue;
                                        }
                                        Val<Rectangle> otherVal = (Val<Rectangle>)RequestReplyHelper.call(Pong.actors.get(j), new Event(Event.Type.REQUEST_RECT));
                                        final Rectangle[] otherRect = new Rectangle[1];
                                        otherVal.use((r) -> otherRect[0] = r);
                                        if(Intersector.intersectRectangles(rect[0], otherRect[0], delta)) {
                                            colliders.add(Pong.actors.get(j));
                                            deltas.add(new Rectangle(delta));
                                        }
                                    }
                                    if(colliders.size > 0) {
                                        ref.send(new Event.CollisionsEvent(colliders, deltas));
                                    }
                                }
                                catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }).start();
                        }
                        break;
                    case REQUEST_NAME:
                        RequestReplyHelper.reply(e, "CollisionFinder");
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
}
