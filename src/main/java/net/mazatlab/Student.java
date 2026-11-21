package net.mazatlab;

import java.net.URL;

/** A student in the class: a display name and the photo it comes from. */
record Student(String name, URL image) {

    /**
     * Builds the display name from a photo's file name: "maria_lopez.png" becomes "maria lopez".
     * Falls back to the whole file name if nothing would be left (e.g. "_.png").
     */
    static String nameFromFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String name = base.replace('_', ' ').trim();
        return name.isEmpty() ? fileName : name;
    }
}
