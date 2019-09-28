/*
 * DirectoryInfo.java
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

import java.io.File;
import java.util.List;

/**
 * Some informations about a directory.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class DirectoryInfo {

    private final File baseDirectory;
    private final List<File> files;
    private final long byteCount;

    /**
     * creates a new DirectoryInfo
     * @param baseDirectory the base directory
     * @param files the list of all files and subdirectories (relative paths)
     * @param byteCount the sum of all file sizes
     * (recursively)
     */
    public DirectoryInfo(File baseDirectory, List<File> files, long byteCount) {
        this.baseDirectory = baseDirectory;
        this.files = files;
        this.byteCount = byteCount;
    }

    /**
     * returns the base directory
     * @return the base directory
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * returns the list of all files and directories
     * @return the list of all files and directories
     */
    public List<File> getFiles() {
        return files;
    }

    /**
     * returns the sum of all file sizes
     * @return the sum of all file sizes
     */
    public long getByteCount() {
        return byteCount;
    }
}
