package net.mazatlab;

import java.net.URL;

/** A student in the class: a display name and the photo it comes from. */
record Student(String name, URL image) {

    /** Builds the display name from a photo's file name: "maria_lopez.png" becomes "maria lopez". */
    static String nameFromFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        return base.replace('_', ' ').trim();
    }
}
