/*
 * FileCopierPanel.java
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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.TimeZone;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A JPanel that can show the progress of file copy operations.
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class FileCopierPanel extends JPanel implements PropertyChangeListener {

    // some constants
    public static final int KILO = 1024;
    public static final int MEGA = KILO * 1024;
    public static final int GIGA = MEGA * 1024;
    public static final long TERA = GIGA * 1024;
    public static final int HOUR = 3600000;
    public final static NumberFormat numberFormat = NumberFormat.getInstance();

    private static final ResourceBundle STRINGS
            = ResourceBundle.getBundle("ch/fhnw/filecopier/Strings");
    // the sum of the size of all files to copy
    private long byteCount;
    // the number of bytes already copied
    private long bytesCopied;
    private FileCopier fileCopier;
    private final DateFormat timeFormat;
    private final DateFormat dateFormat;
    private final DateFormat minuteFormat;
    // remaining time calcuation
    private long startTime;
    private final Timer updateTimer = new Timer(
            1000, (ActionEvent e) -> update()
    );

    /**
     * Creates new form FileCopierPanel
     */
    public FileCopierPanel() {
        timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        minuteFormat = new SimpleDateFormat("mm:ss");

        initComponents();

        passedTimeLabel.setText(" ");
        estimatedDurationLabel.setText(" ");
        estimatedRemainingTimeLabel.setText(" ");
        transferredDataVolumeLabel.setText(" ");
        dataVolumeLabel.setText(" ");
        missingDataVolumeLabel.setText(" ");
        progressBar.setIndeterminate(false);
        progressBar.setString(null);
        startTimeLabel.setText(" ");
        estimatedStopTimeLabel.setText(" ");

    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (topPanel != null) {
            topPanel.setBackground(bg);
        }
        if (leftPanel != null) {
            leftPanel.setBackground(bg);
        }
        if (centerPanel != null) {
            centerPanel.setBackground(bg);
        }
        if (rightPanel != null) {
            rightPanel.setBackground(bg);
        }
        if (bottomPanel != null) {
            bottomPanel.setBackground(bg);
        }
    }

    /**
     * Sets the fileCopier
     *
     * @param fileCopier the fileCopier to monitor
     */
    public void setFileCopier(FileCopier fileCopier) {
        if (this.fileCopier != null) {
            this.fileCopier.removePropertyChangeListener(
                    FileCopier.STATE_PROPERTY, this);
//            this.fileCopier.removePropertyChangeListener(
//                    FileCopier.BYTE_COUNTER_PROPERTY, this);
        }
        this.fileCopier = fileCopier;
        fileCopier.addPropertyChangeListener(FileCopier.STATE_PROPERTY, this);
//        fileCopier.addPropertyChangeListener(
//                FileCopier.BYTE_COUNTER_PROPERTY, this);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String propertyName = evt.getPropertyName();
                if (FileCopier.STATE_PROPERTY.equals(propertyName)) {
                    FileCopier.State newState
                            = (FileCopier.State) evt.getNewValue();

                    switch (newState) {
                        case START:
                            passedTimeLabel.setText(" ");
                            estimatedDurationLabel.setText(" ");
                            Font defaultLabelFont = (new JLabel()).getFont();
                            Font nonBoldFont = defaultLabelFont.deriveFont(
                                    defaultLabelFont.getStyle()
                                    & ~java.awt.Font.BOLD);
                            estimatedDurationLabel.setFont(nonBoldFont);
                            estimatedDurationLabel.setForeground(Color.GRAY);
                            estimatedRemainingTimeLabel.setText(" ");
                            transferredDataVolumeLabel.setText(" ");
                            dataVolumeLabel.setText(" ");
                            missingDataVolumeLabel.setText(" ");
                            progressBar.setIndeterminate(false);
                            progressBar.setString("0%");
                            startTimeLabel.setText(" ");
                            estimatedStopTimeLabel.setText(" ");
                            estimatedStopTimeLabel.setFont(nonBoldFont);
                            estimatedStopTimeLabel.setForeground(Color.GRAY);
                            break;

                        case CHECKING_SOURCE:
                            progressBar.setIndeterminate(true);
                            progressBar.setString(STRINGS.getString(
                                    "Checking_Source_Directory"));
                            break;

                        case COPYING:
                            startTime = System.currentTimeMillis();
                            startTimeLabel.setText(
                                    dateFormat.format(new Date()));
                            updateTimer.setInitialDelay(0);
                            updateTimer.start();
                            byteCount = fileCopier.getByteCount();
                            dataVolumeLabel.setText(
                                    getDataVolumeString(byteCount, 1));
                            progressBar.setIndeterminate(false);
                            bytesCopied = 0;
                            updateProgressBar();
                            break;

                        case END:
                            update();
                            updateTimer.stop();
                            passedTimeLabel.setText(" ");
                            transferredDataVolumeLabel.setText(" ");
                            estimatedRemainingTimeLabel.setText(" ");
                            missingDataVolumeLabel.setText(" ");
                            defaultLabelFont = (new JLabel()).getFont();
                            estimatedDurationLabel.setFont(defaultLabelFont);
                            estimatedDurationLabel.setForeground(Color.BLACK);
                            estimatedStopTimeLabel.setFont(defaultLabelFont);
                            progressBar.setString(STRINGS.getString("Done"));
                            estimatedStopTimeLabel.setForeground(Color.BLACK);
                    }

                } else if (FileCopier.BYTE_COUNTER_PROPERTY.equals(
                        propertyName)) {
                    bytesCopied = (Long) evt.getNewValue();
                    updateProgressBar();
                }
            }
        });
    }

    /**
     * returns a human readably data volume string
     *
     * @param bytes the bytes of the data volume
     * @param fractionDigits the number of fraction digits
     * @return a human readably data volume string
     */
    public static String getDataVolumeString(long bytes, int fractionDigits) {
        if (bytes >= KILO) {
            numberFormat.setMaximumFractionDigits(fractionDigits);
            float kbytes = (float) bytes / KILO;
            if (kbytes >= KILO) {
                float mbytes = (float) bytes / MEGA;
                if (mbytes >= KILO) {
                    float gbytes = (float) bytes / GIGA;
                    if (gbytes >= KILO) {
                        float tbytes = (float) bytes / TERA;
                        return numberFormat.format(tbytes) + " TB";
                    }
                    return numberFormat.format(gbytes) + " GB";
                }
                return numberFormat.format(mbytes) + " MB";
            }
            return numberFormat.format(kbytes) + " KB";
        }
        return numberFormat.format(bytes) + " Byte";
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        statusLabel = new javax.swing.JLabel();
        topPanel = new javax.swing.JPanel();
        leftPanel = new javax.swing.JPanel();
        passedTimeLabel = new javax.swing.JLabel();
        transferredDataVolumeLabel = new ch.fhnw.filecopier.ChangeLabel();
        centerPanel = new javax.swing.JPanel();
        estimatedDurationLabel = new javax.swing.JLabel();
        dataVolumeLabel = new javax.swing.JLabel();
        rightPanel = new javax.swing.JPanel();
        estimatedRemainingTimeLabel = new javax.swing.JLabel();
        missingDataVolumeLabel = new ch.fhnw.filecopier.ChangeLabel();
        progressBar = new javax.swing.JProgressBar();
        bottomPanel = new javax.swing.JPanel();
        startTimeLabel = new javax.swing.JLabel();
        estimatedStopTimeLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/fhnw/filecopier/Strings"); // NOI18N
        statusLabel.setText(bundle.getString("FileCopierPanel.statusLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(statusLabel, gridBagConstraints);

        topPanel.setLayout(new java.awt.GridLayout(1, 0));

        leftPanel.setLayout(new java.awt.GridBagLayout());

        passedTimeLabel.setText("0:00"); // NOI18N
        passedTimeLabel.setToolTipText(bundle.getString("FileCopierPanel.passedTimeLabel.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftPanel.add(passedTimeLabel, gridBagConstraints);

        transferredDataVolumeLabel.setText("0 Byte"); // NOI18N
        transferredDataVolumeLabel.setToolTipText(bundle.getString("FileCopierPanel.transferredDataVolumeLabel.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        leftPanel.add(transferredDataVolumeLabel, gridBagConstraints);

        topPanel.add(leftPanel);

        centerPanel.setLayout(new java.awt.GridBagLayout());

        estimatedDurationLabel.setFont(estimatedDurationLabel.getFont().deriveFont(estimatedDurationLabel.getFont().getStyle() & ~java.awt.Font.BOLD));
        estimatedDurationLabel.setForeground(java.awt.Color.gray);
        estimatedDurationLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        estimatedDurationLabel.setText("0:00"); // NOI18N
        estimatedDurationLabel.setToolTipText(bundle.getString("FileCopierPanel.estimatedDurationLabel.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        centerPanel.add(estimatedDurationLabel, gridBagConstraints);

        dataVolumeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        dataVolumeLabel.setText("0 Byte"); // NOI18N
        dataVolumeLabel.setToolTipText(bundle.getString("FileCopierPanel.dataVolumeLabel.toolTipText")); // NOI18N
        centerPanel.add(dataVolumeLabel, new java.awt.GridBagConstraints());

        topPanel.add(centerPanel);

        rightPanel.setLayout(new java.awt.GridBagLayout());

        estimatedRemainingTimeLabel.setFont(estimatedRemainingTimeLabel.getFont().deriveFont(estimatedRemainingTimeLabel.getFont().getStyle() & ~java.awt.Font.BOLD));
        estimatedRemainingTimeLabel.setForeground(java.awt.Color.gray);
        estimatedRemainingTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        estimatedRemainingTimeLabel.setText("0:00"); // NOI18N
        estimatedRemainingTimeLabel.setToolTipText(bundle.getString("FileCopierPanel.estimatedRemainingTimeLabel.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        rightPanel.add(estimatedRemainingTimeLabel, gridBagConstraints);

        missingDataVolumeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        missingDataVolumeLabel.setText("0 Byte"); // NOI18N
        missingDataVolumeLabel.setToolTipText(bundle.getString("FileCopierPanel.missingDataVolumeLabel.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        rightPanel.add(missingDataVolumeLabel, gridBagConstraints);

        topPanel.add(rightPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
        add(topPanel, gridBagConstraints);

        progressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
        add(progressBar, gridBagConstraints);

        bottomPanel.setLayout(new java.awt.GridBagLayout());

        startTimeLabel.setText("0:00"); // NOI18N
        startTimeLabel.setToolTipText(bundle.getString("FileCopierPanel.startTimeLabel.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        bottomPanel.add(startTimeLabel, gridBagConstraints);

        estimatedStopTimeLabel.setFont(estimatedStopTimeLabel.getFont().deriveFont(estimatedStopTimeLabel.getFont().getStyle() & ~java.awt.Font.BOLD));
        estimatedStopTimeLabel.setForeground(java.awt.Color.gray);
        estimatedStopTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        estimatedStopTimeLabel.setText("0:00"); // NOI18N
        estimatedStopTimeLabel.setToolTipText(bundle.getString("FileCopierPanel.estimatedStopTimeLabel.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        bottomPanel.add(estimatedStopTimeLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
        add(bottomPanel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JPanel centerPanel;
    private javax.swing.JLabel dataVolumeLabel;
    private javax.swing.JLabel estimatedDurationLabel;
    private javax.swing.JLabel estimatedRemainingTimeLabel;
    private javax.swing.JLabel estimatedStopTimeLabel;
    private javax.swing.JPanel leftPanel;
    private ch.fhnw.filecopier.ChangeLabel missingDataVolumeLabel;
    private javax.swing.JLabel passedTimeLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JLabel startTimeLabel;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JPanel topPanel;
    private ch.fhnw.filecopier.ChangeLabel transferredDataVolumeLabel;
    // End of variables declaration//GEN-END:variables

    private void update() {
        bytesCopied = fileCopier.getCopiedBytes();
        if (bytesCopied == 0) {
            return;
        }

        CurrentlyProcessedFile currentlyProcessedFile
                = fileCopier.getCurrentlyProcessedFile();
        String status = STRINGS.getString(
                currentlyProcessedFile.STATE
                == CurrentlyProcessedFile.State.COPYING
                        ? "Copying" : "Checking");
        statusLabel.setText(MessageFormat.format(
                status, currentlyProcessedFile.NAME));

        long currentTime = System.currentTimeMillis();
        long timeSpent = currentTime - startTime;
        passedTimeLabel.setText(getTimeString(timeSpent));

        transferredDataVolumeLabel.setDataVolume(bytesCopied);

        long estimate = (timeSpent * byteCount) / bytesCopied;
        estimatedDurationLabel.setText(getTimeString(estimate));

        long remaining = estimate - timeSpent;
        estimatedRemainingTimeLabel.setText(getTimeString(remaining));

        missingDataVolumeLabel.setDataVolume(byteCount - bytesCopied);

        updateProgressBar();

        estimatedStopTimeLabel.setText(
                dateFormat.format(currentTime + remaining));
    }

    private String getTimeString(long time) {
        if (time < HOUR) {
            return minuteFormat.format(time);
        } else {
            return timeFormat.format(time);
        }
    }

    // must be called from the Swing Event Thread!
    private void updateProgressBar() {
        if (byteCount == 0) {
            // there are only empty files and directories...
            return;
        }
        int progress = (int) ((100 * bytesCopied) / byteCount);
        long currentTime = System.currentTimeMillis();
        long timeSpent = (currentTime - startTime) / 1000;
        int speed = 0;
        if (timeSpent != 0) {
            speed = (int) (bytesCopied / timeSpent);
        }
        String currentBandwidth = getDataVolumeString(speed, 1) + "/s";
        progressBar.setValue(progress);
        progressBar.setString(progress + "% (" + currentBandwidth + ')');
    }
}
