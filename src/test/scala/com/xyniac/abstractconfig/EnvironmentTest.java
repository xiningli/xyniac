package com.xyniac.abstractconfig;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.xyniac.environment.Environment;
import com.xyniac.environment.Environment$;
public class EnvironmentTest {
    @BeforeClass
    public void setup()
    {
        System.setProperty("iaas", "aws");
        System.setProperty("env", "dev");
        System.setProperty("region", "US_WEST_2");

    }

    @Test
    public void test(){
        Environment$ env = Environment$.MODULE$;
        System.out.println(env);
        Assert.assertEquals(env.region(), "US_WEST_2");
        Assert.assertEquals(env.env(), "dev");
        Assert.assertEquals(env.iaas(), "aws");
    }
}
