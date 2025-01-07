package io.mertkaniscan.automation_engine.components;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class DeviceLockManager {
    private final ConcurrentHashMap<Integer, Lock> lockRegistry = new ConcurrentHashMap<>();

    /**
     * Get the lock for a specific device. Creates a new lock if it doesn't exist.
     *
     * @param deviceId the ID of the device
     * @return the lock associated with the device
     */
    public Lock getLockForDevice(int deviceId) {
        return lockRegistry.computeIfAbsent(deviceId, id -> new ReentrantLock());
    }

    /**
     * Remove the lock for a specific device after use (optional cleanup).
     *
     * @param deviceId the ID of the device
     */
    public void removeLockForDevice(int deviceId) {
        lockRegistry.remove(deviceId);
    }
}