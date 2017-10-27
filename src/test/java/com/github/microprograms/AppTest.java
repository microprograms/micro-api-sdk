package com.github.microprograms;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AppTest {
    @BeforeClass
    public void beforeClass() {
        System.out.println("this is before class");
    }

    @Test
    public void test() {
        System.out.println("this is TestNG test case");
    }

    @AfterClass
    public void afterClass() {
        System.out.println("this is after class");
    }
}
