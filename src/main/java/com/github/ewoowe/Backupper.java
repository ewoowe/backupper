package com.github.ewoowe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author wangcheng@ictnj.ac.cn
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
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    static {
        try {
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
            System.out.println(Thread.currentThread().getName() + " enter 0");
            if (state == NO_RUNNING) {
                if (unsafe.compareAndSwapInt(this, stateOffSet, NO_RUNNING, RUNNING)) {
                    System.out.println(Thread.currentThread().getName() + " enter 1");
                    RUNNER.run();
                    while (state == RUNNING_BACKUP) {
                        System.out.println(Thread.currentThread().getName() + " enter 2");
                        unsafe.getAndSetInt(this, stateOffSet, RUNNING);
                        RUNNER.run();
                    }
                    System.out.println(Thread.currentThread().getName() + " enter 3");
                    if (unsafe.compareAndSwapInt(this, stateOffSet, RUNNING, NO_RUNNING)) {
                        System.out.println(Thread.currentThread().getName() + " enter 4");
                        break;
                    }
                    System.out.println(Thread.currentThread().getName() + " enter 5");
                    // cas fail，state must be RUNNING_BACKUP now，set state to NO_RUNNING to continue while
                    unsafe.getAndSetInt(this, stateOffSet, NO_RUNNING);
                }
                System.out.println(Thread.currentThread().getName() + " enter 6");
            } else if (state == RUNNING) {
                System.out.println(Thread.currentThread().getName() + " enter 7");
                if (unsafe.compareAndSwapInt(this, stateOffSet, RUNNING, RUNNING_BACKUP)) {
                    System.out.println(Thread.currentThread().getName() + " enter 8");
                    break;
                }
                System.out.println(Thread.currentThread().getName() + " enter 9");
            } else if (state == RUNNING_BACKUP) {
                System.out.println(Thread.currentThread().getName() + " enter 10");
                break;
            }
        }
    }

}