/*
 * Dir2FileTest.java
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
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Some tests for the file copier
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class Dir2FileTest {

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
     * test, if we correctly handle the situation of copying an empty directory
     * to an existing file
     * (do nothing, print warning)
     * @throws IOException if an I/O exception occurs
     */
    @Test
    public void testDir2File() throws IOException {


        File destinationFile = null;
        try {
            destinationFile = new File(destinationDir, "destinationFile");
            if (!destinationFile.createNewFile()) {
                fail("could not create test file " + destinationFile);
            }
            try {
                // try copying the test file
                CopyJob copyJob = new CopyJob(
                        new Source[]{new Source(sourceDir.getPath())},
                        new String[]{destinationFile.getPath()});
                fileCopier.copy(copyJob);
                fail("this test must throw an exception");
            } catch (IOException ex) {
                ex.printStackTrace();
                // yes, we want an exception here!
            }

            // check
            destinationFile = new File(destinationDir, "destinationFile");
            assertTrue("destination was not created", destinationFile.exists());
            assertTrue("destination is no file", destinationFile.isFile());

        } finally {
            if (!sourceDir.delete()) {
                fail("could not delete source dir " + sourceDir);
            }
            if ((destinationFile != null) &&
                    destinationFile.exists() && !destinationFile.delete()) {
                fail("could not delete destination file " + destinationFile);
            }
            if (!destinationDir.delete()) {
                fail("could not delete destination dir " + destinationDir);
            }
        }
    }
}
