/*
 * FileCopier.java
 *
 * Created on 19.09.2008, 15:37:49
 *
 * This file is part of the Java File Copy Library.
 *
 * The Java File Copy Libraryis free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * The Java File Copy Libraryis distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.fhnw.filecopier;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A class for copying files and directories. It can be used headless. This
 * class is NOT threadsafe!
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class FileCopier {

    /**
     * the string used for the file property
     */
    public final static String FILE_PROPERTY = "file";
    /**
     * the string used for the byte counter property
     */
    public final static String BYTE_COUNTER_PROPERTY = "byte_counter";
    /**
     * the string used for the state property
     */
    public final static String STATE_PROPERTY = "state";

    /**
     * the state of the FileCopier
     */
    public enum State {

        /**
         * the initial state
         */
        START,
        /**
         * the FileCopier is checking the source directory (determining the
         * number of files and directories and the file size sum)
         */
        CHECKING_SOURCE,
        /**
         * the FileCopier is copying files and directories
         */
        COPYING,
        /**
         * the FileCopier finished copying files and directories
         */
        END
    }
    private State state = State.START;
    private final static Logger LOGGER
            = Logger.getLogger(FileCopier.class.getName());
    // the copy intervall we want to get in ms
    private static final int WANTED_TIME = 1000;
    private final PropertyChangeSupport propertyChangeSupport
            = new PropertyChangeSupport(this);
    private long byteCount;
    private long oldCopiedBytes;
    private long copiedBytes;
    private final static NumberFormat NUMBER_FORMAT
            = NumberFormat.getInstance();
    private long position;
    private long sourceLength;
    private long slice = 1048576; // 1 MiB
    private long transferVolume;
    private long sliceStartTime;

    private final ExecutorService executorService;
    private final ExecutorCompletionService<Void> completionService;
    private final BarrierAction barrierAction;
    private CyclicBarrier barrier;

    private final static String DIGEST_ALGORITHM = "MD5";
    private HashMap<String, byte[]> digestCache;

    /**
     * creates a new FileCopier
     *
     * @param digestCache a cache for digests when checking copies
     */
    public FileCopier(HashMap<String, byte[]> digestCache) {
        this.digestCache = digestCache;
        this.executorService = Executors.newCachedThreadPool();
        this.completionService
                = new ExecutorCompletionService<>(executorService);
        this.barrierAction = new BarrierAction();
    }

    /**
     * creates a new FileCopier
     */
    public FileCopier() {
        this(null);
    }

    /**
     * Add a listener for property changes.
     *
     * @param property the property that changes
     * @param listener the listener for the change
     */
    public void addPropertyChangeListener(
            String property, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(
                property, listener);
    }

    /**
     * Remove a listener for property changes.
     *
     * @param property the property that changes
     * @param listener the listener for property changes
     */
    public void removePropertyChangeListener(
            String property, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(
                property, listener);
    }

    /**
     * returns the byte count of all source files
     *
     * @return the byte count of all source files
     */
    public long getByteCount() {
        return byteCount;
    }

    /**
     * returns the sum of all bytes copied so far
     *
     * @return the sum of all bytes copied so far
     */
    public long getCopiedBytes() {
        return copiedBytes;
    }

    /**
     * resets the copier so that another copy operation can be started
     */
    public void reset() {
        State previousState = state;
        state = State.START;
        propertyChangeSupport.firePropertyChange(
                STATE_PROPERTY, previousState, state);
    }

    /**
     * copies source files to a given destination
     *
     * @param copyJobs all copyjobs to execute
     * @throws java.io.IOException if an I/O exception occurs
     * @throws java.security.NoSuchAlgorithmException if the file checking
     * algorithm is not found
     */
    public void copy(CopyJob... copyJobs)
            throws IOException, NoSuchAlgorithmException {

        copy(false, copyJobs);
    }

    /**
     * copies source files to a given destination
     *
     * @param checkCopies if the copies should be checked for errors
     * @param copyJobs all copyjobs to execute
     * @throws java.io.IOException if an I/O exception occurs
     * @throws java.security.NoSuchAlgorithmException if the file checking
     * algorithm is not found
     */
    public void copy(boolean checkCopies, CopyJob... copyJobs)
            throws IOException, NoSuchAlgorithmException {

        byteCount = 0;
        copiedBytes = 0;

        // feed our property change listeners
        State previousState = state;
        state = State.CHECKING_SOURCE;
        propertyChangeSupport.firePropertyChange(
                STATE_PROPERTY, previousState, state);

        // scan all sources of all copyJobs and store the directoryInfos
        int fileCount = 0;
        for (CopyJob copyJob : copyJobs) {
            if (copyJob == null) {
                continue;
            }
            Source[] sources = copyJob.getSources();
            List<DirectoryInfo> directoryInfos = new ArrayList<>();
            for (Source source : sources) {
                File baseDirectory = source.getBaseDirectory();
                int baseDirectoryPathLength;
                String baseDirectoryPath = baseDirectory.getPath();
                if (baseDirectoryPath.endsWith(File.separator)) {
                    // baseDirectory is a file system root, e.g.
                    // "/" or "C:\"
                    baseDirectoryPathLength = baseDirectoryPath.length();
                } else {
                    // baseDirectory is a normal directory, e.g.
                    // "/etc" or "C:\test"
                    baseDirectoryPathLength = baseDirectoryPath.length() + 1;
                }
                DirectoryInfo tmpInfo = expand(baseDirectoryPathLength,
                        baseDirectory, source.getPattern(),
                        source.isRecursive());
                if (tmpInfo != null) {
                    directoryInfos.add(tmpInfo);
                    byteCount += tmpInfo.getByteCount();
                    fileCount += tmpInfo.getFiles().size();
                }
            }
            copyJob.setDirectoryInfos(directoryInfos);
            if (LOGGER.isLoggable(Level.INFO)) {
                StringBuilder stringBuilder
                        = new StringBuilder("source files:\n");
                for (DirectoryInfo directoryInfo : directoryInfos) {
                    stringBuilder.append("source files in base directory ");
                    stringBuilder.append(directoryInfo.getBaseDirectory());
                    stringBuilder.append(":\n");
                    for (File sourceFile : directoryInfo.getFiles()) {
                        stringBuilder.append(sourceFile.isFile() ? "f " : "d ");
                        stringBuilder.append(sourceFile.getPath());
                        stringBuilder.append('\n');
                    }
                }
                LOGGER.info(stringBuilder.toString());
            }
        }

        if (fileCount == 0) {
            LOGGER.info("there are no files to copy");
            return;
        }

        // do all known sanity checks
        for (CopyJob copyJob : copyJobs) {
            // skip empty jobs
            if (copyJob == null) {
                continue;
            }
            // get number of source files in this job
            List<DirectoryInfo> directoryInfos = copyJob.getDirectoryInfos();
            int sourceCount = 0;
            for (DirectoryInfo directoryInfo : directoryInfos) {
                sourceCount += directoryInfo.getFiles().size();
            }
            // skip empty jobs
            if (sourceCount == 0) {
                continue;
            }

            String[] destinations = copyJob.getDestinations();
            for (String destination : destinations) {
                File destinationFile = new File(destination);
                if (destinationFile.isFile()) {
                    if (sourceCount == 1) {
                        File sourceFile
                                = directoryInfos.get(0).getFiles().get(0);
                        if (sourceFile.isDirectory()) {
                            throw new IOException("can not overwrite file \""
                                    + destinationFile + "\" with directory \""
                                    + sourceFile + "\"");
                        }
                    } else {
                        StringBuilder errorMessage = new StringBuilder(
                                "can not copy several files to another file\n"
                                + " sources:");
                        for (DirectoryInfo directoryInfo : directoryInfos) {
                            List<File> files = directoryInfo.getFiles();
                            for (File file : files) {
                                errorMessage.append("  ");
                                errorMessage.append(file.getPath());
                            }
                        }
                        errorMessage.append(" destination: ");
                        errorMessage.append(destinationFile.getPath());
                        throw new IOException(errorMessage.toString());
                    }
                }
            }
        }

        // feed our property change listeners
        previousState = state;
        state = State.COPYING;
        propertyChangeSupport.firePropertyChange(
                STATE_PROPERTY, previousState, state);

        // execute all copy jobs
        for (CopyJob copyJob : copyJobs) {
            // skip empty copy jobs
            if (copyJob == null) {
                continue;
            }

            for (DirectoryInfo directoryInfo : copyJob.getDirectoryInfos()) {
                for (File sourceFile : directoryInfo.getFiles()) {
                    File[] destinationFiles = getDestinationFiles(
                            directoryInfo.getBaseDirectory(),
                            sourceFile, copyJob.getDestinations());
                    if (sourceFile.isDirectory()) {
                        // make target directories (sequentially)
                        for (File destinationFile : destinationFiles) {
                            if (destinationFile.exists()) {
                                if (destinationFile.isDirectory()) {
                                    LOGGER.log(Level.INFO,
                                            "Directory \"{0}\" already exists",
                                            destinationFile);
                                } else {
                                    throw new IOException("can not overwrite "
                                            + "file \"" + destinationFile
                                            + "\" with directory \""
                                            + sourceFile + "\"");
                                }
                            } else {
                                LOGGER.log(Level.INFO,
                                        "Creating directory \"{0}\"",
                                        destinationFile);
                                if (!destinationFile.mkdirs()) {
                                    throw new IOException(
                                            "Could not create directory \""
                                            + destinationFile + "\"");
                                }
                            }
                        }
                    } else {
                        // create target files in parrallel
                        copyFile(checkCopies, sourceFile, destinationFiles);
                    }
                }
            }
        }

        if (oldCopiedBytes != copiedBytes) {
            // need to fire one last time...
            // (last slice was not fully used)
            propertyChangeSupport.firePropertyChange(
                    BYTE_COUNTER_PROPERTY, oldCopiedBytes, copiedBytes);
        }
        previousState = state;
        state = State.END;
        propertyChangeSupport.firePropertyChange(
                STATE_PROPERTY, previousState, state);
    }

    private File[] getDestinationFiles(
            File baseDirectory, File sourceFile, String[] destinations) {
        int destinationCount = destinations.length;
        File[] destinationFiles = new File[destinationCount];
        for (int i = 0; i < destinationCount; i++) {
            File destinationFile = new File(destinations[i]);
            if (destinationFile.isDirectory()) {
                // remap target
                int baseLength = baseDirectory.getPath().length();
                String filePath = sourceFile.getPath();
                String destinationPath = filePath.substring(baseLength);
                destinationFiles[i]
                        = new File(destinationFile, destinationPath);
            } else {
                destinationFiles[i] = destinationFile;
            }
        }
        return destinationFiles;
    }

    private DirectoryInfo expand(int baseDirectoryPathLength,
            File currentDirectory, Pattern pattern, boolean recursive) {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO,
                    "\n\tcurrent directory: \"{0}\"\n\tpattern: \"{1}\"",
                    new Object[]{currentDirectory, pattern});
        }

        // feed the listeners
        propertyChangeSupport.firePropertyChange(
                FILE_PROPERTY, null, currentDirectory);

        if (!currentDirectory.exists()) {
            LOGGER.log(Level.WARNING, "{0} does not exist", currentDirectory);
            return null;
        }

        if (!currentDirectory.isDirectory()) {
            LOGGER.log(Level.WARNING, "{0} is no directory", currentDirectory);
            return null;
        }

        if (!currentDirectory.canRead()) {
            LOGGER.log(Level.WARNING, "can not read {0}", currentDirectory);
            return null;
        }

        if (pattern == null) {
            throw new IllegalArgumentException("pattern must not be null");
        }

        LOGGER.log(Level.FINE, "recursing directory {0}", currentDirectory);
        long tmpByteCount = 0;
        List<File> files = new ArrayList<>();
        for (File subFile : currentDirectory.listFiles()) {

            // check if subfile matches
            String relativePath
                    = subFile.getPath().substring(baseDirectoryPathLength);
            if (pattern.matcher(relativePath).matches()) {
                LOGGER.log(Level.FINE, "{0} matches", subFile);
                if (subFile.isDirectory()) {
                    // copy directories itself only when using recursive mode
                    if (recursive) {
                        files.add(subFile);
                    }
                } else {
                    files.add(subFile);
                    tmpByteCount += subFile.length();
                }
            } else {
                LOGGER.log(Level.FINE, "{0} does not match", subFile);
            }

            // recurse directories
            if (subFile.isDirectory()) {
                if (recursive) {
                    DirectoryInfo tmpInfo = expand(baseDirectoryPathLength,
                            subFile, pattern, recursive);
                    if (tmpInfo != null) {
                        files.addAll(tmpInfo.getFiles());
                        tmpByteCount += tmpInfo.getByteCount();
                    }
                }
            }
        }
        return new DirectoryInfo(currentDirectory, files, tmpByteCount);
    }

    private void copyFile(boolean checkCopies,
            File source, File... destinations)
            throws IOException, NoSuchAlgorithmException {

        // some initial logging
        if (LOGGER.isLoggable(Level.INFO)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Copying file\n");
            stringBuilder.append(source.toString());
            stringBuilder.append("\nto the following destinations:\n");
            for (int i = 0, length = destinations.length; i < length; i++) {
                stringBuilder.append(destinations[i].getPath());
                if (i != length - 1) {
                    stringBuilder.append('\n');
                }
            }
            LOGGER.info(stringBuilder.toString());
        }

        // ensure that the directory path exist before starting the transfer
        for (File destination : destinations) {
            if (!destination.exists()) {
                destination.getParentFile().mkdirs();
                // !!! DON'T create the file itself here !!!
                // We tried both variants below and they are both (equally)
                // HORRIBLY slow:
                // destination.createNewFile();
                // Files.createFile(destination.toPath());
            }
        }

        int destinationCount = destinations.length;

        // quick return when source is an empty file
        sourceLength = source.length();
        if (sourceLength == 0) {
            for (int i = 0; i < destinationCount; i++) {
                // create empty destination files
                new FileOutputStream(destinations[i]);
            }
            return;
        }

        MessageDigest messageDigest = null;
        if (checkCopies && (digestCache == null
                || !digestCache.containsKey(source.getPath()))) {
            messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        }

        // create a Transferrer thread for every destination
        final Transferrer[] transferrers = new Transferrer[destinationCount];
        for (int i = 0; i < destinationCount; i++) {
            transferrers[i] = new Transferrer(
                    new FileInputStream(source).getChannel(),
                    new FileOutputStream(destinations[i]).getChannel(),
                    messageDigest);
        }

        barrier = new CyclicBarrier(destinationCount, barrierAction);

        // start the transfer process
        position = 0;
        transferVolume = Math.min(slice, sourceLength);
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST,
                    "starting with slice = {0} byte, transferVolume = {1} byte",
                    new Object[]{
                        NUMBER_FORMAT.format(slice),
                        NUMBER_FORMAT.format(transferVolume)
                    });
        }
        sliceStartTime = System.currentTimeMillis();

        for (Transferrer transferrer : transferrers) {
            completionService.submit(transferrer, null);
        }

        // wait until all transferrers completed their execution
        for (int i = 0; i < destinationCount; i++) {
            try {
                completionService.take();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        if (checkCopies) {

            byte[] expectedDigest;
            if (messageDigest == null) {
                LOGGER.log(Level.FINE,
                        "taking {0} from digest cache", source.getPath());
                expectedDigest = digestCache.get(source.getPath());
            } else {
                expectedDigest = messageDigest.digest();
                if (digestCache != null) {
                    LOGGER.log(Level.FINE,
                            "adding {0} to digest cache", source.getPath());
                    digestCache.put(source.getPath(), expectedDigest);
                }
            }
            for (File destination : destinations) {
                checkCopy(expectedDigest, destination);
            }
        }
    }

    private void checkCopy(byte[] exptectedDigest, File copy)
            throws IOException, NoSuchAlgorithmException {

        LOGGER.log(Level.INFO, "getting checksum of {0}", copy);
                
        MessageDigest messageDigest
                = MessageDigest.getInstance(DIGEST_ALGORITHM);
        try (InputStream inputStream = new FileInputStream(copy)) {
            byte[] buffer = new byte[1024 * 1024];
            for (int i = inputStream.read(buffer); i >= 0;) {
                messageDigest.update(buffer, 0, i);
                i = inputStream.read(buffer);
            }
        }

        byte[] digest = messageDigest.digest();
        if (Arrays.equals(exptectedDigest, digest)) {
            LOGGER.log(Level.INFO, "{0} has correct checksum {1}",
                    new Object[]{copy, getHexString(digest)});
        } else {
            throw new IOException(copy
                    + " was not correctly copied (expected checksum: "
                    + getHexString(exptectedDigest)
                    + ", actual checksum: " + getHexString(digest));
        }
    }

    private String getHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder(32);
        for (byte b : bytes) {
            stringBuilder.append(String.format("%02x", b));
        }
        return stringBuilder.toString();
    }

    private class BarrierAction implements Runnable {

        @Override
        public void run() {
            // inform property listeners about copied data volume
            position += transferVolume;
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST,
                        "new position: {0}", NUMBER_FORMAT.format(position));
            }
            copiedBytes += transferVolume;
            propertyChangeSupport.firePropertyChange(
                    BYTE_COUNTER_PROPERTY, oldCopiedBytes, copiedBytes);
            oldCopiedBytes = copiedBytes;

            // update slice/transferVolume
            long stop = System.currentTimeMillis();
            long time = stop - sliceStartTime;
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "time = {0} ms",
                        NUMBER_FORMAT.format(time));
            }
            if (time == 0) {
                // if time was "0" we can not update the slice/transferVolume
                // values because we then would divide by zero...
            } else {
                // bandwidth = transferVolume / time
                // newSlice = bandwith * WANTED_TIME
                long newSlice = (transferVolume * WANTED_TIME) / time;
                // just using newSlice here leads to overmodulation
                // doubling or halving is the slower (and probably better)
                // approach
                long doubleSlice = 2 * slice;
                long halfSlice = slice / 2;
                if (newSlice > doubleSlice) {
                    slice = doubleSlice;
                } else if ((newSlice < halfSlice) && (halfSlice > 0)) {
                    slice = halfSlice;
                }
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "slice = {0} byte",
                            NUMBER_FORMAT.format(slice));
                }

                transferVolume = Math.min(slice, sourceLength - position);
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "transferVolume = {0} byte",
                            NUMBER_FORMAT.format(transferVolume));
                }
            }
            sliceStartTime = System.currentTimeMillis();
        }
    }

    private class Transferrer extends Thread {

        private final FileChannel sourceChannel;
        private final FileChannel destinationChannel;
        private final MessageDigest messageDigest;

        public Transferrer(FileChannel sourceChannel,
                FileChannel destinationChannel, MessageDigest messageDigest)
                throws NoSuchAlgorithmException {

            this.sourceChannel = sourceChannel;
            this.destinationChannel = destinationChannel;
            this.messageDigest = messageDigest;
        }

        @Override
        public void run() {

            try {

                // loop over the whole file
                ByteBuffer byteBuffer = null;
                long myPosition = 0;
                while (myPosition < sourceLength) {

                    // transfer the currently planned volume
                    long transferred = 0;
                    while (transferred < transferVolume) {

                        long count = transferVolume - transferred;
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST, "already transferred = "
                                    + "{0} byte, to be transferred = {1} byte",
                                    new Object[]{
                                        NUMBER_FORMAT.format(transferred),
                                        NUMBER_FORMAT.format(count)
                                    });
                        }

                        long tmpTransferred = 0;

                        if (messageDigest == null) {
                            // no digest needed, direct transfer possible
                            tmpTransferred = sourceChannel.transferTo(
                                    myPosition, count, destinationChannel);

                        } else {
                            // digest needed, no direct transfer possible
                            if (byteBuffer == null
                                    || byteBuffer.capacity() < count) {
                                byteBuffer = ByteBuffer.allocate((int) count);
                            } else {
                                byteBuffer.clear();
                            }

                            // read from source
                            tmpTransferred = sourceChannel.read(byteBuffer);
                            byteBuffer.flip();

                            // digest read bytes
                            int limit = byteBuffer.limit();
                            messageDigest.update(byteBuffer);

                            // reset byteBuffer
                            byteBuffer.position(0);
                            byteBuffer.limit(limit);

                            // write to destination
                            while (byteBuffer.hasRemaining()) {
                                destinationChannel.write(byteBuffer);
                            }

                        }

                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST, "{0} byte transferred",
                                    NUMBER_FORMAT.format(tmpTransferred));
                        }

                        myPosition += tmpTransferred;
                        transferred += tmpTransferred;
                    }

                    // wait for all other Transferrers to finish their slice
                    barrier.await();
                }

            } catch (IOException | InterruptedException
                    | BrokenBarrierException ex) {
                LOGGER.log(Level.SEVERE, "could not transfer data", ex);

            } finally {
                // To be able to continue with the next file as soon as possible
                // we close the channels via the executorService.
                executorService.execute(new Closer(sourceChannel));
                executorService.execute(new Closer(destinationChannel));
            }
        }
    }

    private class Closer extends Thread {

        private final Closeable closeable;

        public Closer(Closeable closeable) {
            this.closeable = closeable;
        }

        @Override
        public void run() {
            try {
                closeable.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "could not close " + closeable, ex);
            }
        }
    }
}
