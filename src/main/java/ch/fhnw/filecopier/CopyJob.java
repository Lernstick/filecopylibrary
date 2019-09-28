/*
 * CopyJob.java
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

import java.util.List;

/**
 * A class that contains information about files to copy
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class CopyJob {

    private final Source[] sources;
    private final String[] destinations;
    private List<DirectoryInfo> directoryInfos;

    /**
     * A class representing a copy job.
     * @param destinations the destinations
     * @param sources the sources
     */
    public CopyJob(Source[] sources, String[] destinations) {
        this.sources = sources;
        this.destinations = destinations;
    }

    /**
     * returns the sources
     * @return the sources
     */
    public Source[] getSources() {
        return sources;
    }

    /**
     * returns the destinations
     * @return the destinations
     */
    public String[] getDestinations() {
        return destinations;
    }

    /**
     * sets the directory infos
     * @param directoryInfos the directory infos
     */
    public void setDirectoryInfos(List<DirectoryInfo> directoryInfos) {
        this.directoryInfos = directoryInfos;
    }

    /**
     * returns the directory infos 
     * @return the directory infos
     */
    public List<DirectoryInfo> getDirectoryInfos() {
        return directoryInfos;
    }
}
