package com.github.ewoowe.backupper;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * run a task but maximum two tasks,
 * first one is running, second one is waiting util first one done
 *
 * @author wangcheng
 * @since 2023/7/17
 */
public class Backupper {

    private final Runnable RUNNER;

    public Backupper(Runnable runnable) {
        RUNNER = runnable;
    }

    private volatile int state = 0;
    private static final int NO_RUNNING = 0;
    private static final int RUNNING = 1;
    private static final int RUNNING_BACKUP = 2;

    private static final Unsafe unsafe;
    private static final long stateOffSet;

    static {
        try {
            unsafe = reflectGetUnsafe();
            stateOffSet = unsafe.objectFieldOffset(Backupper.class.getDeclaredField("state"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public static Unsafe reflectGetUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (Unsafe) theUnsafe.get(null);
    }

    public void doRun() {
        while (true) {
            if (state == NO_RUNNING) {
                if (unsafe.compareAndSwapInt(this, stateOffSet, NO_RUNNING, RUNNING)) {
                    RUNNER.run();
                    while (state == RUNNING_BACKUP) {
                        unsafe.getAndSetInt(this, stateOffSet, RUNNING);
                        RUNNER.run();
                    }
                    if (unsafe.compareAndSwapInt(this, stateOffSet, RUNNING, NO_RUNNING))
                        break;
                    // cas fail，state must be RUNNING_BACKUP now，set state to NO_RUNNING to continue while
                    unsafe.getAndSetInt(this, stateOffSet, NO_RUNNING);
                }
            } else if (state == RUNNING) {
                if (unsafe.compareAndSwapInt(this, stateOffSet, RUNNING, RUNNING_BACKUP))
                    break;
            } else if (state == RUNNING_BACKUP) {
                break;
            }
        }
    }

}