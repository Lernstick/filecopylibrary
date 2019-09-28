/*
 * CopySymlinkTest.java
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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Some tests for the file copier
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class CopySymlinkTest {

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
     * test, if we correctly copy symlinks
     * @throws Exception if an exception occurs
     */
    @Test
    public void testCopySymlink() throws Exception {
        File normalFile = null;
        File symlinkFile = null;
        File expectedFile = null;
        File expectedSymlink = null;

        try {
            // create a source file with some content
            String content = "Please link here.";
            normalFile = new File(sourceDir, "normalFile");
            try {
                if (!normalFile.createNewFile()) {
                    fail("could not create test file " + normalFile);
                }
                try (FileWriter fileWriter = new FileWriter(normalFile)) {
                    fileWriter.write(content);
                    fileWriter.flush();
                }
            } catch (IOException ex) {
                System.out.println("Could not create " + normalFile);
                throw ex;
            }

            // create symlink to file
            String symlinkFileName = "symlink";
            symlinkFile = new File(normalFile.getParentFile().getPath() +
                    File.separator + symlinkFileName);
            ProcessExecutor executor = new ProcessExecutor();
            int exitValue = executor.executeProcess(
                    "ln", "-s", normalFile.getName(), symlinkFile.getPath());
            assertEquals("could not create symlink \"" + symlinkFile.getPath() +
                    '\"', 0, exitValue);

            // copy file and symlink
            CopyJob copyJob = new CopyJob(
                    new Source[]{new Source(sourceDir.getPath(), ".*")},
                    new String[]{destinationDir.getPath()});
            fileCopier.copy(copyJob);

            // check
            expectedFile = new File(destinationDir, normalFile.getName());
            expectedSymlink = new File(destinationDir, symlinkFileName);
            assertTrue("destination file was not created",
                    expectedFile.exists());
            assertTrue("destination file is no file",
                    expectedFile.isFile());
            assertTrue("destination symlink was not created",
                    expectedSymlink.exists());
//            assertFalse("destination symlink is no symlink",
//                    expectedSymlink.isFile());

        } finally {
            if ((normalFile != null) && !normalFile.delete()) {
                fail("could not delete source file " + normalFile);
            }
            if ((symlinkFile != null) && !symlinkFile.delete()) {
                fail("could not delete source symlink " + symlinkFile);
            }
            if (!sourceDir.delete()) {
                fail("could not delete source dir " + sourceDir);
            }
            if ((expectedFile != null) && !expectedFile.delete()) {
                fail("could not delete destination file " + expectedFile);
            }
            if ((expectedSymlink != null) && !expectedSymlink.delete()) {
                fail("could not delete destination symlink " + expectedSymlink);
            }
            if (!destinationDir.delete()) {
                fail("could not delete destination dir " + destinationDir);
            }

        }
    }
}
