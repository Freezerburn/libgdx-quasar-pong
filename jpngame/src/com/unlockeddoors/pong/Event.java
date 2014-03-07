package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.behaviors.RequestMessage;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/23/14
 * Time: 9:49 AM
 */
public class Event extends RequestMessage<Object> {
    public static enum Type {
        TICK,
        INPUT,
        REQUEST_RECT,
        SEND_RECT,
        COLLISIONS,
        KILL
    }

    public static class TickEvent extends Event {
        protected float s;

        public TickEvent(float s) {
            super(Type.TICK);
            this.s = s;
        }

        public float getS() {
            return s;
        }
    }

    public static class InputEvent extends Event {
        public final int keycode;
        public final boolean pushed;

        public InputEvent(int keycode, boolean pushed) {
            super(Type.INPUT);
            this.keycode = keycode;
            this.pushed = pushed;
        }
    }

    public static class CollisionsEvent extends Event {
        public final Array<ActorRef<Event>> between;
        public final Array<Vector2> deltas;

        public CollisionsEvent(Array<ActorRef<Event>> between, Array<Vector2> deltas) {
            super(Type.COLLISIONS);
            this.between = between;
            this.deltas = deltas;
        }
    }

    public static class SendRect extends Event {
        public final Rectangle rect;
        public final String name;

        public SendRect(Rectangle rect, String name) {
            super(Type.SEND_RECT);
            this.rect = rect;
            this.name = name;
        }
    }

    public final Type type;

    public Event(Type type) {
        this.type = type;
    }
}
