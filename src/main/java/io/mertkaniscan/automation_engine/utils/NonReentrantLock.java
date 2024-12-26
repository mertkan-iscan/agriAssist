package io.mertkaniscan.automation_engine.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public class NonReentrantLock {
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private Thread owningThread = null;

    public void lock() throws InterruptedException {
        Thread currentThread = Thread.currentThread();
        while (!locked.compareAndSet(false, true)) {
            if (owningThread == currentThread) {
                throw new IllegalStateException("Thread attempted to re-enter lock.");
            }
            Thread.sleep(10); // Avoid busy-wait
        }
        owningThread = currentThread;
    }

    public void unlock() {
        if (Thread.currentThread() != owningThread) {
            throw new IllegalMonitorStateException("Unlock attempted by non-owning thread.");
        }
        owningThread = null;
        locked.set(false);
    }

    public boolean isLocked() {
        return locked.get();
    }
}
