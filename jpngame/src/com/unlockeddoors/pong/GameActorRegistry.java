package com.unlockeddoors.pong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.GlobalRegistry;
import co.paralleluniverse.fibers.SuspendExecution;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/24/14
 * Time: 8:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class GameActorRegistry implements GlobalRegistry {
    private static HashMap<String, ActorRef<?>> registry = new HashMap<>();

    @Override
    public Object register(ActorRef<?> actor) throws SuspendExecution {
        return registry.put(actor.getName(), actor);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void unregister(ActorRef<?> actor) throws SuspendExecution {
        if(registry.containsKey(actor.getName())) {
            registry.remove(actor.getName());
        }
    }

    @Override
    public <Message> ActorRef<Message> getActor(String name) throws SuspendExecution {
        if(registry.containsKey(name)) {
            return (ActorRef<Message>) registry.get(name);
        }
        return null;
    }
}
