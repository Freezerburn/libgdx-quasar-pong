package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.concurrent.ReentrantLock;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by freezerburn on 3/4/14.
 */
public class Registry {
    private static ConcurrentHashMap<String, ActorRef<Event>> registered = new ConcurrentHashMap<>();

    @Suspendable
    public static ActorRef<Event> get(String name) {
        return registered.getOrDefault(name, null);
    }

    @Suspendable
    public static void set(String name, ActorRef<Event> actor) {
        registered.put(name, actor);
    }
}
