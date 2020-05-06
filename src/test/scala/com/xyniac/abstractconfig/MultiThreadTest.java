package com.xyniac.abstractconfig;

import org.testng.annotations.Test;

public class MultiThreadTest {
    public synchronized void testMethod(){
        try {
            Thread.sleep(1000000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testMultiThread() {
        int counter = 0;
        try {

            for (int i = 0; i <1000000; i++) {
                counter = i;
                new Thread(()->testMethod()).start();
            }
        } catch (Exception|Error e) {
            System.out.println(counter);
        }
        System.out.println(counter);


    }
}
