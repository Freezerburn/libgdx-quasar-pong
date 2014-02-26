package com.unlockeddoors.jpngame.ents;

import co.paralleluniverse.actors.ActorRegistry;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import com.unlockeddoors.jpngame.JpnGame;
import com.unlockeddoors.jpngame.input.KeyEvent;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/22/14
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestActor extends BasicActor<Object, Void> {
    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        register("TestActor");
        Channel<KeyEvent> c = JpnGame.keyTopic.subscribe(Channels.newChannel(0));
        while(true) {
            Object foo = receive();
            System.out.println("Actor recieved message: " + foo);
        }
//        return null;
    }
}
