package com.adms.australianmobileadtoolkit.interpreter.platform;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToPictogram;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToStencil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.adms.australianmobileadtoolkit.R;

import java.util.HashMap;

public class Facebook {

   private static String TAG = "Facebook";


   /*
   *
   * This function instantiates the HashMap that contains the references for all pictograms and
   * stencils used in the Facebook ad interpreter software logic
   *
   * */
   public static HashMap<String, Integer> DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_INDEX =
         new HashMap<String, Integer>() {{
            // Stencils
            put("facebookDarkHomeActive", R.drawable.facebook_dark_home_active);
            put("facebookDarkHomeInactive", R.drawable.facebook_dark_home_inactive);
            put("facebookDarkWatchActive", R.drawable.facebook_dark_watch_active);
            put("facebookDarkWatchInactive", R.drawable.facebook_dark_watch_inactive);
            put("facebookLightHomeActive", R.drawable.facebook_light_home_active);
            put("facebookLightHomeInactive", R.drawable.facebook_light_home_inactive);
            put("facebookLightWatchActive", R.drawable.facebook_light_watch_active);
            put("facebookLightWatchInactive", R.drawable.facebook_light_watch_inactive);
            put("facebookLightSponsored", R.drawable.facebook_light_sponsored);
            put("facebookDarkSponsored", R.drawable.facebook_dark_sponsored);
            put("facebookLightSponsoredAlt", R.drawable.facebook_light_sponsored_alt);
            put("facebookDarkSponsoredAlt", R.drawable.facebook_dark_sponsored_alt);
            // Pictograms
            put("facebookReactLike", R.drawable.facebook_react_like);
            put("facebookReactLove", R.drawable.facebook_react_love);
            put("facebookReactCare", R.drawable.facebook_react_care);
            put("facebookReactLaugh", R.drawable.facebook_react_laugh);
            put("facebookReactWow", R.drawable.facebook_react_wow);
            put("facebookReactSad", R.drawable.facebook_react_sad);
            put("facebookReactHate", R.drawable.facebook_react_hate);
            put("facebookReactMask", R.drawable.facebook_react_mask);
         }};
   // By default, the exclusion is applied to the Facebook navbar buttons, where it overtakes the part
   // of each icon that occasionally changes to display notifications
   public static int DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION_RADIUS = 32;
   public static HashMap<String, Integer>
         DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION =
                        new HashMap<String, Integer>() {{
                           put("x",DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION_RADIUS);
                           put("y",0);
                           put("h",DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION_RADIUS);
                           put("w",DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION_RADIUS);
                        }};
   public static int DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_RADIUS = 64;
   public static HashMap <String,Integer>
         DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE =
                              new HashMap<String, Integer>() {{
                                 put("s", DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_RADIUS);
                              }};
   public static int DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_WIDTH = 128;
   public static int DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_HEIGHT = 64;
   public static HashMap <String,Integer>
         DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_SPONSORED =
                              new HashMap<String, Integer>() {{
                                 put("w", DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_WIDTH);
                                 put("h", DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_HEIGHT);
                              }};

   @SuppressLint("UseCompatLoadingForDrawables")
   public static HashMap<String, Object> retrieveReferenceStencilsPictograms(Context context) {
      HashMap<String, Bitmap> pictogramReferenceHashMap = new HashMap<>();
      // Apply the drawable references to a hashmap that loads in the corresponding resources
      for (HashMap.Entry<String, Integer> e
            : DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_INDEX.entrySet()) {
         pictogramReferenceHashMap.put(e.getKey(),
               ((BitmapDrawable)context.getResources().getDrawable(e.getValue())).getBitmap());
      }
      // Assemble the references
      HashMap<String, Object> reference = new HashMap<>();
      for (String key : pictogramReferenceHashMap.keySet()) {
         Bitmap thisPictogram = pictogramReferenceHashMap.get(key);
         HashMap <String,Integer> exclusion = DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION;
         HashMap <String,Integer> size = DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE;
         // Sizing specific to Facebook 'sponsored' text case...
         if (key.contains("facebook") && key.contains("Sponsored")) {
            exclusion = null;
            size = DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_SPONSORED;
         }
         // Sizing specific to Facebook 'react' button case...
         if (key.contains("facebookReact")) {
            reference.put(key,imageToPictogram(Args(A("bitmap", thisPictogram))));
         } else {
            reference.put(key,imageToStencil(Args(
                                             A("bitmap", thisPictogram),
                                             A("size", size),
                                             A("exclusion", exclusion))));
         }
      }
      return reference;
   }

   // TODO - checkpoint here


}
