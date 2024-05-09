package com.adms.australianmobileadtoolkit;

import android.util.Log;

public class Logger {

   public static final String ANSI_RESET = "\u001B[0m";
   public static final String ANSI_BLACK = "\u001B[30m";
   public static final String ANSI_RED = "\u001B[31m";
   public static final String ANSI_GREEN = "\u001B[32m";
   public static final String ANSI_YELLOW = "\u001B[33m";
   public static final String ANSI_BLUE = "\u001B[34m";
   public static final String ANSI_PURPLE = "\u001B[35m";
   public static final String ANSI_CYAN = "\u001B[36m";
   public static final String ANSI_WHITE = "\u001B[37m";

   final public static String
         I = "INFO",
         E = "ERROR",
         W = "WARNING",
         H = "HIGHLIGHT";
   final public static int
         LOG_OUTPUT_MOBILE = 0, LOG_OUTPUT_MACHINE = 1;
   public static String DEFAULT_LOG_TYPE = I;
   public static String DEFAULT_LOG_IN = "Undefined";
   public static int DEFAULT_LOG_OUTPUT = LOG_OUTPUT_MOBILE;
   public static String DEFAULT_LOG_MESSAGE = "Hello World!";
   public static boolean DEFAULT_LOG_RECORD = true;
   public static void log(Arguments args) {
      int logOutput = (int) args.get("output", DEFAULT_LOG_OUTPUT);
      String logIn = (String) args.get("in", DEFAULT_LOG_IN);
      String logType = (String) args.get("type", DEFAULT_LOG_TYPE);
      String logMessage = (String) args.get("message", DEFAULT_LOG_MESSAGE);
      boolean logRecord = (boolean) args.get("record", DEFAULT_LOG_RECORD);
      switch (logOutput) {
         case LOG_OUTPUT_MOBILE :
            switch (logType) {
               case (I) : Log.i(logIn, logMessage); break;
               case (W) : Log.w(logIn, logMessage); break;
               case (E) : Log.e(logIn, logMessage); break;
               case (H) : Log.v(logIn, logMessage); break;
            }
            break;
         case LOG_OUTPUT_MACHINE :
            switch (logType) {
               case (I) : System.out.println(ANSI_BLUE + logMessage + ANSI_RESET); break;
               case (W) : System.out.println(ANSI_PURPLE + logMessage + ANSI_RESET); break;
               case (E) : System.out.println(ANSI_RED + logMessage + ANSI_RESET); break;
               case (H) : System.out.println(ANSI_YELLOW + logMessage + ANSI_RESET); break;
            }
            break;
      }
      if (logRecord) {
         // TODO - make it write to file somewhere
      }
   }

}
