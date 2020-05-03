package com.xyniac.hotdeployment;

import org.testng.Assert;
import org.testng.annotations.Test;


public class HotDeploymentTest {
    public static String testString = "testString";
    @Test
    public void TestAttributeReset(){
        HotDeployment$ hotDeployment = HotDeployment$.MODULE$;


        Object voidReturn = hotDeployment.runScalaCommand(
                "val task:Runnable = ()=>println(\"2\")\nnew Thread(task).start()"
        );
        System.out.println(voidReturn.getClass());
        hotDeployment.runScalaCommand(
                "com.xyniac.hotdeployment.HotDeploymentTest.testString = \"modified\""
        );
        Assert.assertEquals(testString, "modified");
    }

    @Test
    public void TestClassDefinition() throws ClassNotFoundException {
        HotDeployment$ hotDeployment = HotDeployment$.MODULE$;
        hotDeployment.runScalaCommand(
                "case class Person (name: String, age: Int)"
        );

        Class<?> cl = Class.forName("Person");

        System.out.println(cl);
    }
}
