/*
 * Source.java
 *
 * Created on 06.12.2009, 10:47:31
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
import java.util.regex.Pattern;

/**
 * A source definition for copy operations
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class Source {

    private final File baseDirectory;
    private final Pattern pattern;
    private final boolean recursive;

    /**
     * Creates a new recursive Source with a wildcard search pattern depending
     * on the provided file type.<br>
     * When <tt>fileName</tt> is a directory, the base directory is set to
     * <tt>fileName</tt> and the pattern is set to <tt>".*"</tt>.<br>
     * Otherwise the base directory is set to the parent file and the search
     * pattern is set to the file name.
     * @param fileName the name of the source file
     */
    public Source(String fileName) {
        File sourceFile = new File(fileName);
        if (sourceFile.isDirectory()) {
            File parent = sourceFile.getParentFile();
            if (parent == null) {
                // baseFile is the file system root
                baseDirectory = sourceFile;
                pattern = Pattern.compile(".*");
                recursive = true;
            } else {
                baseDirectory = parent;
                pattern = Pattern.compile(sourceFile.getName() + ".*");
                recursive = true;
            }
        } else {
            baseDirectory = sourceFile.getParentFile();
            pattern = Pattern.compile(sourceFile.getName());
            recursive = true;
        }
    }

    /**
     * creates a new recursive Source
     * @param baseDirectory the base directory to search through
     * @param pattern the search pattern to use
     */
    public Source(String baseDirectory, String pattern) {
        this(baseDirectory, pattern, true);
    }

    /**
     * creates a new Source
     * @param baseDirectory the base directory to search through
     * @param pattern the search pattern to use
     * @param recursive if <tt>true</tt>, the source is evaluated recursively,
     * if <tt>false</tt> not
     */
    public Source(String baseDirectory, String pattern, boolean recursive) {
        this.baseDirectory = new File(baseDirectory);
        this.pattern = Pattern.compile(pattern);
        this.recursive = recursive;
    }

    /**
     * returns the base directory
     * @return the base directory
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * returns the search pattern
     * @return the search pattern
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * returns <tt>true</tt> if the source must be evaluated recursively,
     * <tt>false</tt> otherwise
     * @return <tt>true</tt> if the source must be evaluated recursively,
     * <tt>false</tt> otherwise
     */
    public boolean isRecursive() {
        return recursive;
    }
}
