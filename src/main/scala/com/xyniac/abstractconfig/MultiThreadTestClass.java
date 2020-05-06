package com.xyniac.abstractconfig;

public class MultiThreadTestClass {
    public void lockBreaker() throws InterruptedException {
        AbstractConfig.lock().readLock().lock();
        Thread.sleep(1000);
        AbstractConfig.lock().readLock().unlock();
    }
    public void testMethod(){
        int counter = 0;
        for (int i = 0; i <1000000; i++) {
            counter = i;
            new Thread(()-> {
                try {
                    lockBreaker();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }).start();
        }
        System.out.println(counter);
    }
}
