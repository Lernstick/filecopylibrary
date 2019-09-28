package ch.fhnw.filecopier;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;

public class ChangeLabel extends JLabel {

    private static final Logger LOGGER =
            Logger.getLogger(ChangeLabel.class.getName());
    private long previousDataVolume;
    private int previousFractionDigits;
    private boolean fresh = true;

    public void setDataVolume(long dataVolume) {
        LOGGER.log(Level.INFO, "previousDataVolume: {0}, dataVolume: {1}",
                new Object[]{previousDataVolume, dataVolume});
        if ((previousDataVolume == dataVolume) && !fresh) {
            LOGGER.info("previousDataVolume == dataVolume");
            return;
        }
        fresh = false;

        String newText = FileCopierPanel.getDataVolumeString(dataVolume, 0);

        // compare the order of magnitudes
        double newOrder = Math.floor(Math.log10(dataVolume));
        double previousOrder = Math.floor(Math.log10(previousDataVolume));
        if (newOrder == previousOrder) {
            LOGGER.log(Level.INFO,
                    "order of magnitude of both values: {0}", newOrder);
            // compare integer values
            int previousInt = getSubKiloInt(previousDataVolume);
            int newInt = getSubKiloInt(dataVolume);
            if (previousInt == newInt) {
                LOGGER.log(Level.INFO,
                        "inter of both values: {0}", newInt);
                // both values have the same order of magnitude and the same
                // integer value -> use as much fraction digits as necessary
                // to create a visible change
                String previousText = getText();
                LOGGER.log(Level.INFO, "previous text: \"{0}\"", previousText);
                for (int i = previousFractionDigits; i < 4; i++) {
                    newText = FileCopierPanel.getDataVolumeString(
                            dataVolume, i);
                    LOGGER.log(Level.INFO,
                            "new text with {0} fraction digits: \"{1}\"",
                            new Object[]{i, newText});
                    if (previousText.equals(newText)) {
                        LOGGER.info("both texts were equal");
                    } else {
                        LOGGER.info("there was a visible change");
                        previousFractionDigits = i;
                        break;
                    }
                }
            } else {
                previousFractionDigits = 0;
            }
        } else {
            LOGGER.log(Level.INFO,
                    "order of magnitude differs, "
                    + "previousOrder == {0}, newOrder == {1} -> no digits",
                    new Object[]{previousOrder, newOrder});
            previousFractionDigits = 0;
        }

        setText(newText);
        previousDataVolume = dataVolume;
    }

    private static int getSubKiloInt(long bytes) {
        if (bytes >= FileCopierPanel.KILO) {
            float kbytes = (float) bytes / FileCopierPanel.KILO;
            if (kbytes >= FileCopierPanel.KILO) {
                float mbytes = (float) bytes / FileCopierPanel.MEGA;
                if (mbytes >= FileCopierPanel.KILO) {
                    float gbytes = (float) bytes / FileCopierPanel.GIGA;
                    if (gbytes >= FileCopierPanel.KILO) {
                        float tbytes = (float) bytes / FileCopierPanel.TERA;
                        return (int) tbytes;
                    }
                    return (int) gbytes;
                }
                return (int) mbytes;
            }
            return (int) kbytes;
        }
        return (int) bytes;
    }
}
