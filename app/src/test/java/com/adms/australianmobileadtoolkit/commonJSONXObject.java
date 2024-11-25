package com.adms.australianmobileadtoolkit;

import org.junit.Test;


public class commonJSONXObject {

    @Test
    public void testJSONXObject() {
        JSONXObject testObjectA = new JSONXObject();
        testObjectA.set("foo", "bar");
        System.out.println(testObjectA.get("foo"));
        System.out.println(testObjectA.get("bar"));

        JSONXObject testObjectB = new JSONXObject(testObjectA);
        System.out.println(testObjectB.get("foo"));
        System.out.println(testObjectB.get("bar"));
    }

}
