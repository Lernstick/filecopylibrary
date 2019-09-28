/*
 * OverwriteSingleFileAsSecondJobTest.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Some tests for the file copier
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class OverwriteSingleFileAsSecondJobTest {

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
     * test, if we correctly overwrite a single file if it is the second job
     * @throws Exception if an exception occurs
     */
    @Test
    public void testOverwriteSingleFileAsSecondJob() throws Exception {

        File singleFile = null;
        File existingDestinationFile1 = null;
        File expected = null;

        try {

            // create a single source file with some content
            String content = "We can overwrite single files.";
            singleFile = new File(sourceDir, "singleFile");
            try {
                if (!singleFile.createNewFile()) {
                    fail("could not create test file " + singleFile);
                }
                FileWriter fileWriter = new FileWriter(singleFile);
                fileWriter.write(content);
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException ex) {
                System.out.println("Could not create " + singleFile);
                throw ex;
            }

            // create first destination file
            existingDestinationFile1 =
                    new File(destinationDir, "existingFile1");
            try {
                if (!existingDestinationFile1.createNewFile()) {
                    fail("could not create test file " + existingDestinationFile1);
                }
                FileWriter fileWriter = new FileWriter(existingDestinationFile1);
                fileWriter.write("something completely different");
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException ex) {
                System.out.println("Could not create " + existingDestinationFile1);
                throw ex;
            }

            // create second destination file
            File existingDestinationFile2 =
                    new File(destinationDir, "existingFile2");
            try {
                if (!existingDestinationFile2.createNewFile()) {
                    fail("could not create test file " + existingDestinationFile2);
                }
                FileWriter fileWriter = new FileWriter(existingDestinationFile2);
                fileWriter.write("and even more different...");
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException ex) {
                System.out.println("Could not create " + existingDestinationFile2);
                throw ex;
            }

            // try copying the test files
            CopyJob copyJob1 = new CopyJob(
                    new Source[]{new Source(singleFile.getPath())},
                    new String[]{existingDestinationFile1.getPath()});
            CopyJob copyJob2 = new CopyJob(
                    new Source[]{new Source(singleFile.getPath())},
                    new String[]{existingDestinationFile2.getPath()});
            fileCopier.copy(copyJob1, copyJob2);

            // check
            expected = new File(destinationDir, "existingFile2");
            FileReader fileReader = new FileReader(expected);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            bufferedReader.close();
            fileReader.close();

            assertTrue("destination was not created", expected.exists());
            assertTrue("destination is no file", expected.isFile());
            assertTrue("the file was not copied", content.equals(line));

        } finally {

            // cleanup
            if ((singleFile != null) &&
                    singleFile.exists() && !singleFile.delete()) {
                fail("could not delete source file " + singleFile);
            }

            if (!sourceDir.delete()) {
                fail("could not delete source dir " + sourceDir);
            }

            if ((existingDestinationFile1 != null) &&
                    existingDestinationFile1.exists() &&
                    !existingDestinationFile1.delete()) {
                fail("could not delete destination file " +
                        existingDestinationFile1);
            }

            if ((expected != null) &&
                    expected.exists() && !expected.delete()) {
                fail("could not delete destination file " + expected);
            }

            if (!destinationDir.delete()) {
                fail("could not delete destination dir " + destinationDir);
            }
        }
    }
}
