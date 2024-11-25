package com.adms.australianmobileadtoolkit;

import static java.util.Collections.frequency;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class Common {

   public static boolean withinTestEnvironment() {
      boolean result;
      try {
         Class.forName("com.adms.australianmobileadtoolkit.testsMachine");
         result = true;
      } catch (final Exception e) {
         result = false;
      }
      return result;
   }


   /*
   *
   * This function converts a list of strings into a File path object
   *
   * */
   public static File filePath(List<String> path) {
      File output = null;
      for (String s : path) {
         output = (path.indexOf(s) == 0) ? new File(s) : (new File(output, s));
      }
      return output;
   }

   /*
   *
   * This function retrieves the files within a directory
   *
   * */
   public static List<String> getFilesInDirectory(File thisDirectory) {
      return Arrays.stream(Objects.requireNonNull(thisDirectory.listFiles()))
                     .filter(File::isFile).map(File::getName).collect(Collectors.toList());
   }

   /*
   *
   * This function makes a new directory (if it does not exist)
   *
   * */
   public static void makeDirectory(File thisDirectory) {
      if (!thisDirectory.exists()){ thisDirectory.mkdirs(); }
   }


   /*
   *
   * This function determines whether a value is within another value
   *
   * */
   public static boolean in(Object z, Object y) {
      if (z instanceof HashMap) {
         return (((HashMap<?, ?>) z).containsKey(y));
      } else if (z instanceof List) {
         List<?> zFormalised = ((List<?>) z);
         for (int i = 0; i < zFormalised.size(); i ++) {
            if (zFormalised.get(i).equals(y)) {
               return true;
            }
         }
      }
      return false;
   }

   /*
    *
    * This function converts a list of integers into a HashMap, with values
    * that correspond to frequencies of distinct values from the deriving list
    *
    * */

   public static HashMap<Integer, Integer> weightedHashMap(List<Integer> input) {
      HashMap<Integer, Integer> output = new HashMap<>();
      for (Integer z : input) {
         // If the entry is not already in the HashMap, add it
         if (!in(output, z)) {
            output.put(z, 0);
         }
         // Add one frequency point
         output.put(z, Objects.requireNonNull(output.get(z))+1);
      }
      return output;
   }
   public static HashMap<Integer, Double> weightedHashMap(List<Integer> input, List<List<Integer>> inputIndices, HashMap<Integer, Integer> propensity) {
      HashMap<Integer, Double> output = new HashMap<>();
      for (int i = 0; i < input.size(); i ++) {

         Integer z = input.get(i);

         List<Integer> thisInputListIndex = inputIndices.get(i);

         int divisorA = propensity.get(thisInputListIndex.get(0));
         int divisorB = propensity.get(thisInputListIndex.get(1));

         double thisPropensity = (1 / (double) divisorA / (double) divisorB);

         // If the entry is not already in the HashMap, add it
         if (!in(output, z)) {
            output.put(z, 0.0);
         }

         // Add one frequency point
         output.put(z, Objects.requireNonNull(output.get(z))+thisPropensity);
      }
      return output;
   }

   /*
   *
   * This function captures optional double errors, returning zero in the error case
   *
   * */
   public static Double optionalGetDouble(Object x) {
      if (x instanceof OptionalDouble) {
         OptionalDouble y = (OptionalDouble) x;
         if (y.isPresent()) {
            return y.getAsDouble();
         }
      }
      return 0.0;
   }

   /*
   *
   * This function returns the average of a list of doubles
   *
   * */
   public static Double average(List<Double> z) {
      return optionalGetDouble(z.stream().mapToDouble(Double::doubleValue).average());
   }

   /*
    *
    * This function snaps together values numerically by distance-based
    * 'likeness' into a dictionary keyed by averages of said values
    *
    * */
   public static double DEFAULT_BIN_AS_AVERAGES_LIKENESS = 2.0; // TODO - settings
   public static HashMap<Double, List<Double>> binAsAverages(Arguments args) {
      double likeness = (Double) args.get("likeness", DEFAULT_BIN_AS_AVERAGES_LIKENESS);
      List<Double> input = (List<Double>) args.get("input", new ArrayList<Double>());

      List<List<Double>> intermediate = new ArrayList<>();
      // For each entry in the input list...
      for (Double x : input) {
         // Find all candidate lists, where the average of the given list is within
         // 'likeness' distance of the entry
         List<List<Double>> candidates = intermediate.stream().filter(y ->
                                          Math.abs(x - average(y)) < likeness).collect(Collectors.toList());
         // If there are no candidate lists, create one and insert the entry
         if (candidates.isEmpty()) {
            intermediate.add(new ArrayList<>(Collections.singletonList(x)));
         } else {
            // Or else add the value to the first candidate that it is 'like'
            // TODO - should we consider instances where a value is 'like' multiple candidate lists
            candidates.get(0).add(x);
         }
      }

      // Convert the lists into a HashMap, keyed by the respective averages
      HashMap<Double, List<Double>> output = new HashMap<>();
      for (List<Double> x : intermediate) {
         output.put(Math.floor(average(x)), x);
      }

      return output;
   }


   /*
   *
   * This function returns all unordered combinations that can be yielded from a list of integers
   *
   * */
   public static List<List<Integer>> combinationPairs(List<Integer> list) {
      List<List<Integer>> thisCombinations = new ArrayList<>();
      for (int i = 0; i < list.size(); i++)
         for (int j = i + 1; j < list.size(); j++) thisCombinations.add(Arrays.asList(i, j));
      return thisCombinations;
   }

   /*
   *
   * This function determines the frequency of an element within an integer array
   *
   * */
   public static int frequencyInArray(int[][] z, int y) {
      return (int) Arrays.stream(z).map(x -> frequency(Collections.singletonList(x), y))
            .collect(Collectors.toList()).stream().mapToDouble(x->x).sum();
   }

   /*
   *
   * This function retrieves the nth index of a boolean value within a supplied boolean list
   *
   * */
   public static int BooleanIndexOfN(List<Boolean> values, Boolean x, int n) {
      // Convert the list into a string to take advantage of the in-built string functions
      // that assist this process
      String str = values.stream().map(y -> (y) ? "a" : "b").collect(Collectors.joining(""));
      String substr = (x) ? "a" : "b";
      int pos = str.indexOf(substr);
      while (--n > 0 && pos != -1)
         pos = str.indexOf(substr, pos + 1);
      return pos;
   }

}
