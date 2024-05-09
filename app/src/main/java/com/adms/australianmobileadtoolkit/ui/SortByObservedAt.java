package com.adms.australianmobileadtoolkit.ui;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;

public class SortByObservedAt implements Comparator<JSONObject> {
   @Override
   public int compare(JSONObject lhs, JSONObject rhs) {
      try {
         return Long.compare(rhs.getLong("observed_at"), lhs.getLong("observed_at"));
      } catch (JSONException e) {
         e.printStackTrace();
      }
      return 0;

   }
}
