/*
 * Dir2DirTest.java
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
public class Dir2DirTest {

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
     * test, if we correctly handle the situation of copying a full directory
     * to an existing directory
     * @throws IOException if an I/O exception occurs
     */
    @Test
    public void testFullDir2Dir() throws IOException {

        File testFile = null;
        File expectedDir = null;
        File expectedFile = null;

        try {

            // create a test file in the source directory
            testFile = new File(sourceDir, "testFile");
            try {
                if (!testFile.createNewFile()) {
                    fail("could not create test file " + testFile);
                }
            } catch (IOException ex) {
                System.out.println("Could not create " + testFile);
                throw ex;
            }

            // try copying the source directory (recursive)
            CopyJob copyJob = new CopyJob(
                    new Source[]{new Source(sourceDir.getPath())},
                    new String[]{destinationDir.getPath()});
            fileCopier.copy(copyJob);

            // check
            expectedDir = new File(destinationDir, sourceDir.getName());
            expectedFile = new File(expectedDir, testFile.getName());
            assertTrue("destination directory was not created",
                    expectedDir.exists());
            assertTrue("destination directory is no directory",
                    expectedDir.isDirectory());
            assertTrue("destination file was not created",
                    expectedFile.exists());
            assertTrue("destination file is no file",
                    expectedFile.isFile());

        } finally {
            if ((testFile != null) && testFile.exists() && !testFile.delete()) {
                fail("could not delete test file " + testFile);
            }
            if (!sourceDir.delete()) {
                fail("could not delete source dir " + sourceDir);
            }
            if ((expectedFile != null) &&
                    expectedFile.exists() && !expectedFile.delete()) {
                fail("could not delete destination file " + expectedFile);
            }
            if ((expectedDir != null) &&
                    expectedDir.exists() && !expectedDir.delete()) {
                fail("could not delete destination dir " + expectedDir);
            }
            if (!destinationDir.delete()) {
                fail("could not delete destination dir " + destinationDir);
            }
        }
    }
}
