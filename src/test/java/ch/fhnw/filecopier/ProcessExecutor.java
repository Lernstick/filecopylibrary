/*
 * ProcessExecutor.java
 *
 * Created on 31. August 2003, 14:32
 */
package ch.fhnw.filecopier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that provides an easy interface for executing processes
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class ProcessExecutor {

    private final static Logger logger =
            Logger.getLogger(ProcessExecutor.class.getName());
    private List<String> stdOut;
    private List<String> stdErr;
    private List<String> stdAll;

    public String createScript(String script) throws IOException {
        logger.info("script:\n" + script);
        File tmpFile = File.createTempFile("knx2usb", null);
        FileWriter fileWriter = new FileWriter(tmpFile);
        fileWriter.write(script);
        fileWriter.close();
        String scriptPath = tmpFile.getPath();
        executeProcess("chmod", "+x", scriptPath);
        return scriptPath;
    }

    /**
     * executes the given command
     * @param commandArray the command and parameters
     * @return the exit value of the command
     */
    public int executeProcess(String... commandArray) {
        if (logger.isLoggable(Level.FINE)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("executing \"");
            for (int i = 0; i < commandArray.length; i++) {
                stringBuilder.append(commandArray[i]);
                if (i != commandArray.length - 1) {
                    stringBuilder.append(" ");
                }
            }
            stringBuilder.append("\"");
            logger.fine(stringBuilder.toString());
        }
        stdOut = new ArrayList<String>();
        stdErr = new ArrayList<String>();
        stdAll = new ArrayList<String>();
        ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
        try {
            Process process = processBuilder.start();
            StreamReader stdoutReader = new StreamReader(
                    process.getInputStream(), "OUTPUT", stdOut, stdAll);
            StreamReader stderrReader = new StreamReader(
                    process.getErrorStream(), "ERROR", stdErr, stdAll);
            stdoutReader.start();
            stderrReader.start();
            int exitValue = process.waitFor();
            // wait for readers to finish...
            stdoutReader.join();
            stderrReader.join();
            return exitValue;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getOutput() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String string : stdAll) {
            stringBuilder.append(string);
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    /**
     * returns the output
     * @return the output
     */
    public List<String> getStdOut() {
        return stdOut;
    }

    /**
     * returns the output
     * @return the output
     */
    public List<String> getStdErr() {
        return stdErr;
    }

    private class StreamReader extends Thread {

        private final InputStream inputStream;
        private final String type;
        private final List<String> output;
        private final List<String> all;

        public StreamReader(InputStream inputStream, String type,
                List<String> output, List<String> all) {
            super("ProcessExecutor.StreamReader");
            this.inputStream = inputStream;
            this.type = type;
            this.output = output;
            this.all = all;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(isReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                    String allLine = type + ">" + line;
                    all.add(allLine);
                    logger.info(allLine);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
