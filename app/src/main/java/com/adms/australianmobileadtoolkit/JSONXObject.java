package com.adms.australianmobileadtoolkit;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class JSONXObject {

    public JSONObject internalJSONObject;

    public JSONXObject() {
        internalJSONObject = new JSONObject();
    }


    public JSONXObject(JSONXObject input) {
        if (input != null) {
            try {
                internalJSONObject = new JSONObject(input.toString());
            } catch (Exception e) { }
        } else {
            internalJSONObject = new JSONObject();
        }
    }

    public JSONXObject(JSONObject input) {
        if (input != null) {
            try {
                internalJSONObject = new JSONObject(input.toString());
            } catch (Exception e) { }
        } else {
            internalJSONObject = new JSONObject();
        }
    }

    @NonNull
    public String toString() {
        return internalJSONObject.toString();
    }

    public Object get(String key) {
        if (internalJSONObject.has(key)) {
            try {
                return internalJSONObject.get(key);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public Boolean has(String key) {
        return internalJSONObject.has(key);
    }

    public Object get(String ...keys) {
        JSONXObject tentativeOutcome = new JSONXObject(internalJSONObject);
        for (int i = 0; i < keys.length; i ++) {
            String thisKey = keys[i];
            if (tentativeOutcome.has(thisKey)) {
                try {
                    try {
                        tentativeOutcome = ((JSONXObject) tentativeOutcome.get(thisKey));
                    } catch (Exception ignored) {
                        tentativeOutcome = (new JSONXObject((JSONObject) tentativeOutcome.get(thisKey)));
                    }
                    if (i == (keys.length - 1)) {
                        return tentativeOutcome;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    public JSONXObject set(String key, Object value) {
        try {
            internalJSONObject.put(key, value);
            return this;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> keys() {
        try {
            List<String> thisKeys = new ArrayList<>();
            internalJSONObject.keys().forEachRemaining(thisKeys::add);
            return thisKeys;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
