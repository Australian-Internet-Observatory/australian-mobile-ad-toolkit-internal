package com.adms.australianmobileadtoolkit;

import android.graphics.Bitmap;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class Arguments {

   private JSONObject args;

   public Arguments() {
      args = new JSONObject();
   }

   public Arguments put(String key, Object value) {
      try {
         args.put(key, value);
      } catch (JSONException e) {
         throw new RuntimeException(e);
      }
      return this;
   }

   public Object get(String X, Object defaultX) {
      try {
         return (args.has(X)) ? args.get(X) : defaultX;
      } catch (JSONException e) {
         return defaultX;
      }
   }

   public boolean has(String X) {
      return args.has(X);
   }

   public HashMap<String, Integer> getHashMap(String X, Object defaultX) {
      return  (HashMap<String, Integer>) get(X, defaultX);
   }

   public HashMap<String, Object> getHashMapStringObject(String X, Object defaultX) {
      return  (HashMap<String, Object>) get(X, defaultX);
   }

   public List<Bitmap> getListBitmap(String X, Object defaultX) {
      return  (List<Bitmap>) get(X, defaultX);
   }

   public List<Boolean> getListBoolean(String X, Object defaultX) {
      return (List<Boolean>) get(X, defaultX);
   }

   @SafeVarargs
   public static Arguments Args(Pair<String, Object>... arguments) {
      Arguments thisArguments = new Arguments();
      for (Pair<String, Object> argument : arguments) {
         thisArguments.put( argument.first, argument.second);
      }
      return thisArguments;
   }

   public static Pair<String, Object> A(String key, Object value) {
      return new Pair<>(key, value);
   }
}
