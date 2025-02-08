package com.adms.australianmobileadtoolkit;


import static com.adms.australianmobileadtoolkit.Common.exceptionWrite;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.createDirectory;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.filePath;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.logger;

import static java.util.Arrays.asList;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})

public class testSerialization {

    public static final File targetDirectory = (new File(String.valueOf(filePath(asList(((new File(".")).getAbsolutePath()),
            "..", "..", "serializationTests")))));

    /*
    *
    * data we want to persist
    *
    *   Integer
    *
    *   String
    *
    *   List<T,T>
    *
    *   HashMap<T,T>
    *
    *   JSONObject
    *
    *   JSONXObject
    *
    * */
    // read and write

    @Test
    public void testWriteSerial() {

        serialXObject.setTargetDirectory(targetDirectory);

    /*
        serialXObject x = new serialXObject("x");
        x.set("A","1");
        x.set("B",2);
        List<Integer> c = new ArrayList<>();
        c.add(1);
        x.set("C",c);
        List<List<Integer>> d = new ArrayList<>();
        d.add(c);
        x.set("D",d);
        HashMap<Integer, Integer> e = new HashMap<>();
        e.put(1,1);
        x.set("E",e);
        System.out.println(e.get(1));
        JSONObject f = new JSONObject();
        try { f.put("wut", e); } catch (Exception ex) {}
        x.set("F",f);
        x.save();
        serialXObject y = new serialXObject("x");
        y.save();*/


/*
        serialObject x = new serialObject("test");
        x.set("a", new JSONObject());
        System.out.println(x.flats);
        System.out.println(x.flats.get("a").getClass());
        System.out.println(x.types);
        x.save();
        System.out.println();
        serialObject y = new serialObject("test");
        System.out.println(y.flats);
        System.out.println(y.flats.get("a").getClass());
        System.out.println(y.types);
        y.save();
        System.out.println();
        serialObject z = new serialObject("test");
        System.out.println(z.flats);
        System.out.println(z.flats.get("a").getClass());
        System.out.println(z.types);
        z.save();*/



        /*serialXObject y = new serialXObject("frameSampleMetadata");
        y.save();*/

        serialXObject frameSnippetIDsByOffsetChainOBJ = new serialXObject("frameSnippetIDsByOffsetChain");
        HashMap<Integer, HashMap<Integer, JSONObject>> frameSnippetIDsByOffsetChain = new HashMap<>();
        try {
            frameSnippetIDsByOffsetChain = (HashMap<Integer, HashMap<Integer, JSONObject>>) ((JSONObject) frameSnippetIDsByOffsetChainOBJ.container.get("DATA")).get("frameSnippetIDsByOffsetChains");
        } catch (Exception e) {
        }

        // For each frame snippet...
        Integer nAds = 0;
        for (Integer offsetChainID : frameSnippetIDsByOffsetChain.keySet()) {
            for (Integer frameSnippetID : frameSnippetIDsByOffsetChain.get(offsetChainID).keySet()) {
                JSONObject frameSnippet = frameSnippetIDsByOffsetChain.get(offsetChainID).get(frameSnippetID);
                // Determine whether the frame snippet is an ad (or not)
                Boolean determinedAsFacebookAd = false;
                List<String> generatedCroppingFiles = new ArrayList<>();
                System.out.println(frameSnippet);
                System.out.println(new JSONXObject(frameSnippet).keys());
                try {
                    determinedAsFacebookAd = (Boolean) frameSnippet.get("determinedAsFacebookAd");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // brute force hashmap differences
}
