package com.adms.australianmobileadtoolkit.utils;

import java.io.File;
import java.io.FileNotFoundException;

public class Guards {
    public static void ensureFileExists(File file) throws FileNotFoundException {
        if (!file.exists()) throw new FileNotFoundException("File does not exist at " + file.getAbsolutePath());
    }
}
