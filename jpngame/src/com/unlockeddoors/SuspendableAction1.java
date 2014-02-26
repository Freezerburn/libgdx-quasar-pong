package com.unlockeddoors;

import co.paralleluniverse.fibers.SuspendExecution;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/24/14
 * Time: 9:03 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SuspendableAction1<V> {
    void call(V v1) throws SuspendExecution, InterruptedException;
}
