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
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests that copying files from the root directory works as expected
 * @author Ronny.Standtke <Ronny.Standtke@gmx.net>
 */
public class RootTest {

    private final File tmpDir = new File(System.getProperty("java.io.tmpdir") +
            File.separatorChar + "filecopiertest");
    private FileCopier fileCopier;
    private File destinationDir;

    /**
     * sets up some things before a test runs
     */
    @Before
    public void setUp() {
        // create a copier instance
        fileCopier = new FileCopier();

        // create destination directory
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
    public void testRootDir() throws IOException {

        // choose source file based on operating system
        String baseDirectory = null;
        String searchPattern = null;
        String osName = System.getProperty("os.name");
        if (osName.equals("Linux")) {
            baseDirectory = "/etc/";
            searchPattern = "passwd";
        } else if (osName.startsWith("Windows")) {
            baseDirectory = "C:\\";
            searchPattern = "boot.ini";
        } else {
            fail("test does not support " + osName);
        }

        // try copying the source file
        CopyJob copyJob = new CopyJob(
                new Source[]{new Source(baseDirectory, searchPattern, false)},
                new String[]{destinationDir.getPath()});

        File expectedFile = null;
        try {
            fileCopier.copy(copyJob);

            // check
            expectedFile = new File(destinationDir, searchPattern);
            assertTrue("destination file was not created", expectedFile.exists());
            assertTrue("destination file is no file", expectedFile.isFile());

        } finally {
            // cleanup
            if ((expectedFile != null) && expectedFile.exists()) {
                  Files.delete(expectedFile.toPath());
            }
            if (!destinationDir.delete()) {
                fail("could not delete destination dir " + destinationDir);
            }
        }
    }
}
