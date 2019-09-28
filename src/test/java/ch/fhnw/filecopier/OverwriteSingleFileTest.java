/*
 * OverwriteSingleFileTest.java
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
public class OverwriteSingleFileTest {

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
     * test, if we correctly overwrite a single file
     * @throws Exception if an exception occurs
     */
    @Test
    public void testOverwriteSingleFile() throws Exception {

        File singleFile = null;
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

            // create a single destination file
            File existingDestinationFile =
                    new File(destinationDir, "existingFile");
            try {
                if (!existingDestinationFile.createNewFile()) {
                    fail("could not create test file " +
                            existingDestinationFile);
                }
                FileWriter fileWriter = new FileWriter(existingDestinationFile);
                fileWriter.write("something completely different");
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException ex) {
                System.out.println("Could not create " +
                        existingDestinationFile);
                throw ex;
            }

            // try copying the test file
            CopyJob copyJob = new CopyJob(
                    new Source[]{new Source(singleFile.getPath())},
                    new String[]{existingDestinationFile.getPath()});
            fileCopier.copy(copyJob);

            // check
            expected = new File(destinationDir, "existingFile");
            FileReader fileReader = new FileReader(expected);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            bufferedReader.close();
            fileReader.close();
            assertTrue("destination was not created", expected.exists());
            assertTrue("destination is no file", expected.isFile());
            assertTrue("the file was not copied", content.equals(line));

        } finally {
            if ((singleFile != null) &&
                    singleFile.exists() && !singleFile.delete()) {
                fail("could not delete source file " + singleFile);
            }
            if (!sourceDir.delete()) {
                fail("could not delete source dir " + sourceDir);
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
