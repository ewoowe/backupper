package com.github.ewoowe;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wangcheng@ictnj.ac.cn
 * @since 2023/7/17
 */
public class Test {

    private static final AtomicInteger flag = new AtomicInteger(0);

    public static void main(String[] args) {
        Backupper backUpper = new Backupper(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "=" + flag.addAndGet(1));
        });
        for (int i = 0; i < 1000; i++) {
            new Thread(backUpper::doRun).start();
        }
    }
}