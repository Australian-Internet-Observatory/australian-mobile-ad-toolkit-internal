package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.interpreter.Platform.createDirectory;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class checkPoint {

    public String id;
    public JSONXObject container;
    public JSONXObject flats;
    public JSONXObject types;
    public JSONXObject preContainer;
    HashMap<String, Boolean> initialized;
    public File outputDirectory;
    private boolean malformationFlag = false;

    Map<Class, String> primitiveClassMappings = Map.of(
            String.class,"STRING",
            Integer.class,"INTEGER",
            Boolean.class,"BOOLEAN",
            Double.class,"DOUBLE"
    );

    private static JSONObject readFromFile(File inputFile) {
        try {
            Gson gson = new Gson();
            Reader reader = new FileReader(inputFile.getAbsolutePath());
            return gson.fromJson(reader, JSONObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object assess(String k) {
        if (!malformationFlag) {
            try {
                Object v = flats.get(k);
                String thisType = (String) types.get(k);
                if (thisType == null) {
                    System.out.println(k);
                }
                if (thisType.equals("STRING")) {
                    initialized.put(k, true);
                    return (String) v;
                } else if (thisType.equals("INTEGER")) {
                    initialized.put(k, true);
                    return (Integer) v;
                } else if (thisType.equals("DOUBLE")) {
                    initialized.put(k, true);
                    if (v.getClass().equals(String.class)) {
                        return Double.parseDouble((String) v);
                    } else if (v.getClass().equals(Integer.class)) {
                        return ((Integer) v).doubleValue();
                    } else if (v.getClass().equals(Double.class)) {
                        return (Double) v;
                    }
                } else if (thisType.equals("BOOLEAN")) {
                    initialized.put(k, true);
                    return (Boolean) v;
                } else if (thisType.equals("NULL")) {
                    initialized.put(k, true);
                    return null;
                } else {
                    if (thisType.equals("LIST")) {
                        Gson gson = new Gson();
                        List<String> keysToCheck = (List<String>) gson.fromJson((String) v, List.class);
                        if (keysToCheck.stream().allMatch(initialized::get)) {
                            List<Object> tentativeList = new ArrayList<>();
                            for (String thisListKey : keysToCheck) {
                                tentativeList.add(this.assess(thisListKey));
                            }
                            initialized.put(k, true);
                            return tentativeList;
                        }
                    } else if (thisType.equals("HASHMAP")) {
                        Gson gson = new Gson();
                        HashMap<String, String> keysToCheck = (HashMap<String, String>) gson.fromJson((String) v, HashMap.class);
                        if (keysToCheck.values().stream().allMatch(initialized::get)) {
                            try {
                                HashMap<Integer, Object> tentativeHashMap = new HashMap<>();
                                for (String thisHashMapKey : keysToCheck.keySet()) {
                                    String thisHashMapKeyValue = keysToCheck.get(thisHashMapKey);
                                    tentativeHashMap.put(Integer.parseInt(thisHashMapKey), this.assess(thisHashMapKeyValue));
                                }
                                initialized.put(k, true);
                                return tentativeHashMap;
                            } catch (Exception ignored) {
                                HashMap<String, Object> tentativeHashMap = new HashMap<>();
                                for (String thisHashMapKey : keysToCheck.keySet()) {
                                    String thisHashMapKeyValue = keysToCheck.get(thisHashMapKey);
                                    tentativeHashMap.put(thisHashMapKey, this.assess(thisHashMapKeyValue));
                                }
                                initialized.put(k, true);
                                return tentativeHashMap;
                            }
                        }

                    } else if (thisType.equals("JSONOBJECT")) {
                        Gson gson = new Gson();
                        HashMap<String, String> keysToCheck = (HashMap<String, String>) gson.fromJson((String) v, HashMap.class);
                        if (keysToCheck.values().stream().allMatch(initialized::get)) {
                            JSONObject tentativeJSONObject = new JSONObject();
                            for (String thisHashMapKey : keysToCheck.keySet()) {
                                String thisHashMapKeyValue = keysToCheck.get(thisHashMapKey);
                                try {
                                    tentativeJSONObject.put(thisHashMapKey, this.assess(thisHashMapKeyValue));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            initialized.put(k, true);
                            return tentativeJSONObject;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Reinitiating as unstable data - 1");
                // Any error during assessment indicates a malformation in the data structure
                types = new JSONXObject();
                flats = new JSONXObject();
                container = new JSONXObject();
                malformationFlag = true;
            }
        }
        return null;
    }

    public checkPoint(String argId, File argOutputDirectory) {
        setTargetDirectory(argOutputDirectory);
        id = argId;
        types = new JSONXObject();
        flats = new JSONXObject();
        container = new JSONXObject();
        File flatsFile = new File(outputDirectory, id+".flats.json");
        File typesFile = new File(outputDirectory, id+".types.json");
        if (flatsFile.exists()) {
            flats = new JSONXObject(readFromFile(flatsFile));
            types = new JSONXObject(readFromFile(typesFile));

            Gson gson = new Gson();
            List<String> containerKeys = (List<String>) gson.fromJson((String)
                    (new JSONXObject(readFromFile(new File(outputDirectory, id+".keys.json")))).get("data"), List.class);

            // Initialise the initialized HashMap
            Boolean wellFormed = true;// TODO - we are exploring an error where the keys within the 'types' do not match those of the flats
            initialized = new HashMap<>();
            for (String thisKey : flats.keys()) {
                initialized.put(thisKey, false);
                if (!types.has(thisKey)) {
                    wellFormed = false;
                }
            }

            if (wellFormed) {
                preContainer = new JSONXObject();
                while (initialized.containsValue(false) && (!malformationFlag)) {
                    for (String thisKey : initialized.keySet()) {
                        if (!initialized.get(thisKey)) {
                            preContainer.set(thisKey, this.assess(thisKey));
                        }
                    }
                }
                // at the end of the process, hand over the precontainer values to the container itself
                //containerKeys
                for (String k : containerKeys) {
                    container.set(k, preContainer.get(k));
                }
            } else {
                //preContainer = new JSONXObject();
                //initialized = new HashMap<>();
                System.out.println("Reinitiating as unstable data - 2");

            }

            // The flats and types will not be accessed again until it is time to write the file
            // and so they are wiped to avoid unnecessarily conflating the object with random UUIDs
            flats = new JSONXObject();
            types = new JSONXObject();
        }
    }

    public void setTargetDirectory(File argOutputDirectory) {
        createDirectory(argOutputDirectory, false);
        outputDirectory = argOutputDirectory;
    }

    /*
     *
     * should put the data into a temporary container
     *
     * */
    public void set(String k, Object v) {
        container.set(k, v);
    }

    /*
     *
     * convert the container to a flat object
     *
     * */
    public void containerToFlat() {
        for (String k : container.keys()) {
            this.flatApply(k, container.get(k));
        }
    }

    public void flatApply(String k, Object v) {
        if (v == null) {
            types.set(k, "NULL");
            flats.set(k, "NULL");
        } else {
            Class<?> aClass = v.getClass();

            if (primitiveClassMappings.containsKey(aClass)) {
                types.set(k, primitiveClassMappings.get(aClass));
                flats.set(k, v);
            } else if ((aClass.equals(ArrayList.class)) || (String.valueOf(v.getClass()).equals("class java.util.Arrays$ArrayList"))) {
                types.set(k, "LIST");
                List<String> objectList = new ArrayList<>();
                for (Object x : ((List<Object>) v)) {
                    String thisUUID = UUID.randomUUID().toString().substring(0,8);
                    objectList.add(thisUUID);
                    this.flatApply(thisUUID, x);
                }
                flats.set(k, objectList);
            } else if (aClass.equals(HashMap.class)) {
                types.set(k, "HASHMAP");
                try {
                    HashMap<Integer, String> objectHashMap = new HashMap<>();
                    HashMap<Integer, Object> valueAsHashMap = (HashMap<Integer, Object>) v;
                    for (Integer thisHashMapKey : valueAsHashMap.keySet()) {
                        String thisUUID = UUID.randomUUID().toString().substring(0,8);
                        objectHashMap.put(thisHashMapKey, thisUUID);
                        this.flatApply(thisUUID, valueAsHashMap.get(thisHashMapKey));
                    }
                    flats.set(k, objectHashMap);
                } catch (Exception e) {
                    HashMap<String, String> objectHashMap = new HashMap<>();
                    HashMap<String, Object> valueAsHashMap = (HashMap<String, Object>) v;
                    for (String thisHashMapKey : valueAsHashMap.keySet()) {
                        String thisUUID = UUID.randomUUID().toString().substring(0,8);
                        objectHashMap.put(thisHashMapKey, thisUUID);
                        this.flatApply(thisUUID, valueAsHashMap.get(thisHashMapKey));
                    }
                    flats.set(k, objectHashMap);
                }
            } else if (aClass.equals(JSONObject.class)) {
                types.set(k, "JSONOBJECT");
                HashMap<String, String> objectHashMap = new HashMap<>();
                List<String> thisKeys = new ArrayList<>();
                ((JSONObject) v).keys().forEachRemaining(thisKeys::add);

                for (String thisJSONKey : thisKeys) {
                    String thisUUID = UUID.randomUUID().toString().substring(0,8);
                    objectHashMap.put(thisJSONKey, thisUUID);
                    try {
                        this.flatApply(thisUUID, ((JSONObject) v).get(thisJSONKey));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                flats.set(k, objectHashMap);
            } else {
                Log.i("serialObject", "Dealing with unknown object for reference: "+k);
                Log.i("serialObject", String.valueOf(v.getClass()));
            }
        }
    }


    /*
     *
     * at this stage, the flat and types objects are instantiated
     *
     * we then formalize them into an object
     *
     * */
    private void writeToFile(File outputFile, JSONXObject thisJson, String thisCase) {

        // this needs to be replaced by a recursive process that goes over all the values and steadily populates the object
        try {
            try (Writer writer = new FileWriter((outputFile).getAbsolutePath())) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JSONXObject adjustedObject = new JSONXObject();
                for (String thisKey : thisJson.keys()) {
                    if (thisCase.equals("flats")) {
                        switch ((String) types.get(thisKey)) {
                            case "STRING" : adjustedObject.set(thisKey, thisJson.get(thisKey)); break;
                            case "INTEGER" : adjustedObject.set(thisKey, thisJson.get(thisKey)); break;
                            case "BOOLEAN" : adjustedObject.set(thisKey, thisJson.get(thisKey)); break;
                            case "DOUBLE" : adjustedObject.set(thisKey, thisJson.get(thisKey)); break;
                            default : adjustedObject.set(thisKey, gson.toJson(thisJson.get(thisKey))); break;
                        }
                    } else {
                        adjustedObject.set(thisKey, thisJson.get(thisKey));
                    }
                }
                gson.toJson(adjustedObject.internalJSONObject, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // A state 'write' failure is easily recovered from...
        }
    }


    public void save() {
        containerToFlat();

        try {
            try (Writer writer = new FileWriter((new File(outputDirectory, id + ".keys.json")).getAbsolutePath())) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JSONObject output = new JSONObject();
                output.put("data",container.keys());
                gson.toJson(output, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // A state 'write' failure is easily recovered from...
        }

        writeToFile((new File(outputDirectory, id+".flats.json")), flats, "flats");
        writeToFile((new File(outputDirectory, id+".types.json")), types, "types");
    }

    public void delete() {
        Log.i("checkPoint","SERIAL OBJECT" + (new File(outputDirectory, id+".flats.json")).getAbsolutePath());
        try { (new File(outputDirectory, id+".flats.json")).delete(); } catch (Exception e) {
            e.printStackTrace();
        }
        try { (new File(outputDirectory, id+".types.json")).delete(); } catch (Exception e) {
            e.printStackTrace();
        }
        try { (new File(outputDirectory, id+".keys.json")).delete(); } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
