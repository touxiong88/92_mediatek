
package com.mediatek.factorymode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShellExe {
    public static String ERROR = "ERROR";

    private static StringBuilder sb = new StringBuilder("");

    public static String getOutput() {
        return sb.toString();
    }

    public static int execCommand(String[] command) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec(command);
        InputStream inputstream = proc.getInputStream();
        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

        sb.delete(0, sb.length());
        try {
            if (proc.waitFor() != 0) {
                sb.append(ERROR);
                return -1;
            } else {
                String line;
                line = bufferedreader.readLine();
                if (line != null) {
                    sb.append(line);
                } else {
                    return 0;
                }
                while (true) {
                    line = bufferedreader.readLine();
                    if (line == null) {
                        break;
                    } else {
                        sb.append('\n');
                        sb.append(line);
                    }
                }
                return 0;
            }
        } catch (InterruptedException e) {
            sb.append(ERROR);
            return -1;
        }
    }
}
