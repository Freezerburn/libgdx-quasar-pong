package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;

import java.util.HashMap;

/**
 * Created by freezerburn on 3/4/14.
 */
public class Registry {
    private static HashMap<String, ActorRef<Event>> registered = new HashMap<>();

    public static ActorRef<Event> get(String name) {
        if(registered.containsKey(name)) {
            return registered.get(name);
        }
        return null;
    }

    public static void set(String name, ActorRef<Event> actor) {
        registered.put(name, actor);
    }
}
