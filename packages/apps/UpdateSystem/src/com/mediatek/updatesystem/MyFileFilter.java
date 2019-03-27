package com.mediatek.updatesystem;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
        if (!pathname.isFile()) return false;
        String filename = pathname.getName().toLowerCase();
        if (filename.indexOf(".zip") != -1 && filename.startsWith("update")) {
            if (filename.length() > 10) {
                String number = filename.substring(6, filename.length() - 4);
                if (isNumeric(number)) {
                    return true;
                } else {
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    public boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public boolean CheckMemberId(String str) {
        Pattern pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

}
