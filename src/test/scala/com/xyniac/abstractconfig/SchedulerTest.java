package com.xyniac.abstractconfig;

//import org.slf4j.Logger;
//import org.apache.log4j.PropertyConfigurator;
import org.testng.Assert;
import org.slf4j.LoggerFactory;

public class SchedulerTest {
    public static void main(String[] args) throws InterruptedException {
//        PropertyConfigurator.configure("log4j.xml");


        System.setProperty("iaas", "aws");
        System.setProperty("env", "dev");
        System.setProperty("region", "EU_NORTH_1");
        TestAbstractConfig$ config = TestAbstractConfig$.MODULE$;
        Assert.assertEquals(config.getName(), "Mike");
        Thread.sleep(100000);
    }
}
