/*
 * FileCopierTest.java
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Some tests for the file copier
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class CopySingleFileWithCheckTest {

    private final File tmpDir = new File(System.getProperty("java.io.tmpdir")
            + File.separatorChar + "filecopiertest");
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
     * test, if we correctly copy a single file to a target directory
     *
     * @throws Exception if an exception occurs
     */
    @Test
    public void testCopyingSingleFile() throws Exception {
        // should work identically in both the recursive and non-recursive case
        testSingleFile(true/*recursive*/);
        setUp();
        testSingleFile(false/*recursive*/);
    }

    private void testSingleFile(boolean recursive)
            throws IOException, NoSuchAlgorithmException {

        File singleFile = null;
        File expected = null;
        try {
            // create a single file
            singleFile = new File(sourceDir, "singleFile");
            try {
                if (!singleFile.createNewFile()) {
                    fail("could not create test file " + singleFile);
                }
                try (FileWriter writer = new FileWriter(singleFile)) {
                    writer.write("test");
                }
            } catch (IOException ex) {
                System.out.println("Could not create " + singleFile);
                throw ex;
            }

            // try copying the test file
            Source[] sources = new Source[]{
                new Source(singleFile.getParent(), singleFile.getName(), recursive)
            };
            String[] destinations = new String[]{
                destinationDir.getPath()
            };
            CopyJob copyJob = new CopyJob(sources, destinations);
            fileCopier.copy(true, copyJob);

            // check
            expected = new File(destinationDir, singleFile.getName());
            assertTrue("destination was not created", expected.exists());
            assertTrue("destination is no file", expected.isFile());

        } catch (Exception e) {
            e.printStackTrace();
            
        } finally {
            // cleanup
            Files.deleteIfExists(singleFile.toPath());
            Files.deleteIfExists(sourceDir.toPath());
            Files.deleteIfExists(expected.toPath());
            Files.deleteIfExists(destinationDir.toPath());
        }
    }
}
