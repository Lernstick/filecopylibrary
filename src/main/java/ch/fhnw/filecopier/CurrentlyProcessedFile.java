/*
 * FileCopier.java
 *
 * Created on Sat, 02 Nov 2019 14:49:46 +0100
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

/**
 * Contains information about the currently processed file.
 *
 * @author Ronny Standtke <ronny.standtke@fhnw.ch>
 */
public class CurrentlyProcessedFile {

    /**
     * the state of the currently processed file
     */
    public enum State {

        /**
         * the copy operation is still running
         */
        COPYING,

        /**
         * the copy of the file is checked
         */
        CHECKING
    }

    /**
     * the name of the currently processed file
     */
    public final String NAME;

    /**
     * the state of the currently processed file
     */
    public State STATE;

    /**
     * Creates a new CurrentlyProcessedFile
     * @param name the name of the currently processed file
     */
    public CurrentlyProcessedFile(String name) {
        NAME = name;
        STATE = State.COPYING;
    }

    /**
     * sets the state State.CHECKING
     */
    public void setChecking() {
        STATE = State.CHECKING;
    }
}
