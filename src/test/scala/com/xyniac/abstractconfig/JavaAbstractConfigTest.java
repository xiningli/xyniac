package com.xyniac.abstractconfig;

import com.google.gson.JsonObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.Option;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Created by Xining Li on May/10/2020
 */

public class JavaAbstractConfigTest {

    @BeforeClass
    public void setup() {
        System.setProperty("iaas", "aws");
        System.setProperty("environment", "development");
        System.setProperty("region", "EU_NORTH_1");
    }


    @Test
    public void test() {
        TestAbstractConfig$ config = TestAbstractConfig$.MODULE$;
        Assert.assertEquals(config.getName(), "Mike");
    }
    @Test
    public void testTrivial() {
        final int nTest = 1000000;
        TestAbstractConfig$ config = TestAbstractConfig$.MODULE$;
        ExecutorService executor = Executors.newFixedThreadPool(100);
        long startTime = System.currentTimeMillis();
        for (int i=0; i<nTest; i++) {
            Callable<String> callable = ()-> config.getTrival();
            FutureTask<String> futureTask = new FutureTask<>(callable);
            executor.submit(futureTask);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("trivial:" + (endTime - startTime));
    }

    @Test
    public void testNull() {
        TestAbstractConfig$ config = TestAbstractConfig$.MODULE$;
        Assert.assertEquals(config.getProperty("Mike2", String.class, Option.empty()), null);
    }

    @Test
    public void testErrorConversion() {
        TestAbstractConfig$ config = TestAbstractConfig$.MODULE$;
        Assert.assertEquals(config.getProperty("Mike2", String.class, Option.empty()), null);
    }

    @Test
    public void testReset(){
        TestAbstractConfig$ config = TestAbstractConfig$.MODULE$;
        config.setProperty("name", "Jessica");
        Assert.assertEquals(config.getName(), "Jessica");

    }

    @Test
    public void testReadWriteLock() {
        final int nTest = 1000000;
        TestAbstractConfig$ config = TestAbstractConfig$.MODULE$;
        ExecutorService executor = Executors.newFixedThreadPool(100,
                new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = Executors.defaultThreadFactory().newThread(r);
                        t.setDaemon(true);
                        return t;
                    }
                });
        AtomicLong configChangeTime = new AtomicLong(-1);

        final CopyOnWriteArraySet<String> beforeChangeProperty = new CopyOnWriteArraySet<>();
        final CopyOnWriteArraySet<String> afterChangeProperty = new CopyOnWriteArraySet<>();
        executor.submit(()-> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            configChangeTime.set(System.currentTimeMillis());
            config.setProperty("name", "Jessica");
        });

        for (int i=0; i<nTest; i++) {
            Runnable setUpdater = ()-> {

                if (configChangeTime.get() == -1L) {
                    beforeChangeProperty.add(config.getName());
                } else {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    afterChangeProperty.add(config.getName());
                }
            };

            executor.submit(setUpdater);

        }



        executor.shutdown();
        Assert.assertTrue(beforeChangeProperty.size()<=1);
        Assert.assertTrue(afterChangeProperty.size()<=1);

        Assert.assertEquals(afterChangeProperty.stream().findFirst().get(), "Jessica");
    }

    @Test
    public void perfTest() {
        final int nTest = 1000000;
        TestAbstractConfig$ config = TestAbstractConfig$.MODULE$;
        ExecutorService executor = Executors.newFixedThreadPool(100);
        long startTime = System.currentTimeMillis();
        for (int i=0; i<nTest; i++) {
            Callable<String> callable = ()-> config.getName();
            FutureTask<String> futureTask = new FutureTask<>(callable);
            executor.submit(futureTask);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("perf: " + (endTime - startTime));
    }

    @Test
    public void testViewAllConfig() {
        AbstractDynamicConfig$ abstractconfig = AbstractDynamicConfig$.MODULE$;
        JsonObject all = abstractconfig.checkAllConfig();
        System.out.println("testViewAllConfig:" + all);
    }
}
