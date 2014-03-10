package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.strands.concurrent.ReentrantLock;

import java.util.HashMap;

/**
 * Created by freezerburn on 3/4/14.
 */
public class Registry {
    private static HashMap<String, ActorRef<Event>> registered = new HashMap<>();
    private static ReentrantLock lock = new ReentrantLock();

    public static ActorRef<Event> get(String name) {
        lock.lock();
        if(registered.containsKey(name)) {
            ActorRef<Event> ret = registered.get(name);
            lock.unlock();
            return ret;
        }
        lock.unlock();
        return null;
    }

    public static void set(String name, ActorRef<Event> actor) {
        lock.lock();
        registered.put(name, actor);
        lock.unlock();
    }
}
