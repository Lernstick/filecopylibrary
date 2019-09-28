/*
 * MultipleDestinationsTest.java
 *
 * Created on 22. April 2008, 14:21
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Some tests for the file copier
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class MultipleDestinationsTest {

    private final File tmpDir = new File(System.getProperty("java.io.tmpdir") +
            File.separatorChar + "filecopiertest");
    private FileCopier fileCopier;
    private File sourceDir;
    private File destinationDir;

    /**
     * sets up some things before a test runs
     */
    @Before
    public void setUp() {
        // create a copier instance
        fileCopier = new FileCopier();

        // create all test directories
        sourceDir = new File(tmpDir, "testSourceDir");
        if (!sourceDir.exists() && !sourceDir.mkdirs()) {
            fail("could not create source dir " + sourceDir);
        }
        destinationDir = new File(tmpDir, "testDestinationDir");
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            fail("could not create destination dir " + destinationDir);
        }
    }

    /**
     * test, if we correctly handle empty copy jobs
     * @throws Exception if an exception occurs
     */
    @Test
    public void testMultipleDestinations() throws Exception {

        // log everything to console
        Logger logger = Logger.getLogger(FileCopier.class.getName());
        logger.setLevel(Level.ALL);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        logger.addHandler(consoleHandler);
        consoleHandler.setLevel(Level.ALL);

        File sourceFile = null;
        File expected1 = null;
        File expected2 = null;
        File destinationDir2 = null;
        try {

            // create a large (3 MiB) random source file
            int testSize = 3 * 1024 * 1024;
            byte[] data = new byte[testSize];
            Random random = new Random();
            random.nextBytes(data);
            sourceFile = new File(sourceDir, "sourceFile");
            FileOutputStream fileOutputStream =
                    new FileOutputStream(sourceFile);
            fileOutputStream.write(data);
            fileOutputStream.close();

            // create a second destination directory
            destinationDir2 = new File(tmpDir, "testDestinationDir2");
            if (!destinationDir2.exists() && !destinationDir2.mkdir()) {
                fail("could not create destination dir " + destinationDir2);
            }

            // copy source file in both destination directories
            CopyJob copyJob = new CopyJob(
                    new Source[]{new Source(sourceFile.getPath())},
                    new String[]{
                        destinationDir.getPath(),
                        destinationDir2.getPath()
                    });
            fileCopier.copy(copyJob);

            // check both destination files
            expected1 = new File(destinationDir, "sourceFile");
            boolean exists1 = expected1.exists();
            boolean isFile1 = expected1.isFile();
            byte[] expectedData1 = readData(expected1, testSize);
            boolean contentMatch1 = Arrays.equals(data, expectedData1);
            expected2 = new File(destinationDir2, "sourceFile");
            boolean exists2 = expected2.exists();
            boolean isFile2 = expected2.isFile();
            byte[] expectedData2 = readData(expected2, testSize);
            boolean contentMatch2 = Arrays.equals(data, expectedData2);

            // final check
            assertTrue("first destination file was not created", exists1);
            assertTrue("first destination file is no file", isFile1);
            assertTrue("first destination file content does not match " +
                    "source file", contentMatch1);
            assertTrue("second destination file was not created", exists2);
            assertTrue("second destination file is no file", isFile2);
            assertTrue("second destination file content does not match " +
                    "source file", contentMatch2);

        } finally {

            if ((sourceFile != null) &&
                    sourceFile.exists() && !sourceFile.delete()) {
                fail("could not delete source file " + sourceFile);
            }
            if (!sourceDir.delete()) {
                fail("could not delete source dir " + sourceDir);
            }
            if ((expected1 != null) &&
                    expected1.exists() && !expected1.delete()) {
                fail("could not delete destination file " + expected1);
            }
            if (!destinationDir.delete()) {
                fail("could not delete destination dir " + destinationDir);
            }
            if ((expected2 != null) &&
                    expected2.exists() && !expected2.delete()) {
                fail("could not delete destination file " + expected2);
            }
            if ((destinationDir2 != null) &&
                    destinationDir2.exists() && !destinationDir2.delete()) {
                fail("could not delete destination dir " + destinationDir2);
            }
        }
    }

    private byte[] readData(File file, int size) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream =
                new BufferedInputStream(fileInputStream);
        byte[] data = new byte[size];
        bufferedInputStream.read(data);
        bufferedInputStream.close();
        fileInputStream.close();
        return data;
    }
}
