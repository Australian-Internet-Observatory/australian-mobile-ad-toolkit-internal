package com.adms.australianmobileadtoolkit.interpreter;

import com.adms.australianmobileadtoolkit.JSONXObject;

import java.util.Comparator;

public class SortByXLastFrame implements Comparator<JSONXObject>
{
    public int compare(JSONXObject a, JSONXObject b)
    {
        try {
            return (((Integer) a.get("lastFrame")) - ((Integer) b.get("lastFrame")));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
