package com.unlockeddoors.pong;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.ConditionSynchronizer;
import co.paralleluniverse.strands.SettableFuture;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import com.unlockeddoors.SuspendableAction1;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/24/14
 * Time: 8:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class Val<Type> {
    private Type val;
    private ReentrantLock lock;
    private SimpleConditionSynchronizer sync;

    public Val() {
        lock = new ReentrantLock();
        sync = new SimpleConditionSynchronizer(this);
    }

    public void set(Type val) {
        lock.lock();
        try {
            this.val = val;
        }
        finally {
            lock.unlock();
        }
    }

    public void use(SuspendableAction1<Type> c) throws SuspendExecution, InterruptedException {
        if(null == val) {
            Object token = sync.register();
            try {
                for(int i = 0; null == val; i++) {
                    sync.await(i);
                }
            }
            finally {
                sync.unregister(token);
            }
        }

        lock.lock();
        try {
            c.call(val);
        }
        finally {
            lock.unlock();
        }
    }
}
