package ch.fhnw.filecopier;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChangeLabelTest {

    @Test
    public void testSimpleDataVolume() {
        System.out.println("testSimpleDataVolume");
        long dataVolume = 0L;
        ChangeLabel changeLabel = new ChangeLabel();
        changeLabel.setDataVolume(dataVolume);
        assertEquals(FileCopierPanel.getDataVolumeString(0, 0),
                changeLabel.getText());
    }

    @Test
    public void testSimpleDataVolumeChange() {
        System.out.println("testSimpleDataVolumeChange");
        ChangeLabel changeLabel = new ChangeLabel();
        changeLabel.setDataVolume(FileCopierPanel.KILO);
        changeLabel.setDataVolume(2 * FileCopierPanel.KILO);
        assertEquals(FileCopierPanel.getDataVolumeString(
                2 * FileCopierPanel.KILO, 0), changeLabel.getText());
    }

    @Test
    public void testMagnitudeDataVolumeChange() {
        System.out.println("testMagnitudeDataVolumeChange");
        ChangeLabel changeLabel = new ChangeLabel();
        changeLabel.setDataVolume(FileCopierPanel.KILO);
        changeLabel.setDataVolume(FileCopierPanel.MEGA);
        assertEquals(FileCopierPanel.getDataVolumeString(
                FileCopierPanel.MEGA, 0), changeLabel.getText());
    }

    @Test
    public void testFractionChange() {
        System.out.println("testFractionChange");
        ChangeLabel changeLabel = new ChangeLabel();
        changeLabel.setDataVolume(FileCopierPanel.KILO);

        // one fraction digit
        long newVolume = (long) (1.1 * FileCopierPanel.KILO);
        changeLabel.setDataVolume(newVolume);
        assertEquals(FileCopierPanel.getDataVolumeString(newVolume, 1),
                changeLabel.getText());

        // two fraction digits
        newVolume = (long) (1.11 * FileCopierPanel.KILO);
        changeLabel.setDataVolume(newVolume);
        assertEquals(FileCopierPanel.getDataVolumeString(newVolume, 2),
                changeLabel.getText());

        // three fraction digits
        newVolume = (long) (1.111 * FileCopierPanel.KILO);
        changeLabel.setDataVolume(newVolume);
        assertEquals(FileCopierPanel.getDataVolumeString(newVolume, 3),
                changeLabel.getText());

        // reset
        newVolume = (long) (2 * FileCopierPanel.KILO);
        changeLabel.setDataVolume(newVolume);
        assertEquals(FileCopierPanel.getDataVolumeString(newVolume, 0),
                changeLabel.getText());

        // only integer change
        changeLabel.setDataVolume(0);
        newVolume = (long) (800.11 * FileCopierPanel.MEGA);
        changeLabel.setDataVolume(newVolume);
        newVolume = (long) (800.111 * FileCopierPanel.MEGA);
        changeLabel.setDataVolume(newVolume);
        newVolume = (long) (810.111 * FileCopierPanel.MEGA);
        changeLabel.setDataVolume(newVolume);
        assertEquals(FileCopierPanel.getDataVolumeString(newVolume, 0),
                changeLabel.getText());

        // test no change at all
        changeLabel.setDataVolume(0);
        newVolume = (long) (1.11 * FileCopierPanel.MEGA);
        changeLabel.setDataVolume(newVolume);
        changeLabel.setDataVolume(newVolume);
        assertEquals(FileCopierPanel.getDataVolumeString(newVolume, 0),
                changeLabel.getText());
    }
}