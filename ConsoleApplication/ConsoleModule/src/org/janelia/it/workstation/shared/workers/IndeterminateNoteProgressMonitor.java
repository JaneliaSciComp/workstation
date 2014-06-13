package org.janelia.it.workstation.shared.workers;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JProgressBar;

/**
 * A quick fix for supporting notes in the alignment board.
 */
public class IndeterminateNoteProgressMonitor extends IndeterminateProgressMonitor {

    public IndeterminateNoteProgressMonitor(Component parentComponent, Object message, String note) {
        super(parentComponent, message, note);
        this.noteLabel = new JLabel();
        if ( printableNote() ) {
            noteLabel.setText( note );
        }
    }

    @Override
    public void setProgress(int nv) {
        if (nv >= max) {
            close();
        }
        else {
            myBar = new JProgressBar();
            myBar.setIndeterminate(true);
            if (note != null) noteLabel.setText(note);
            pane = new ProgressOptionPane(new Object[] {message,
                                                        printableNote() ? noteLabel : null,
                                                        myBar});
            dialog = pane.createDialog(parentComponent, "Processing");
            dialog.show();
        }
    }

    private boolean printableNote() {
        return printable( note );
    }

    private static boolean printable( String st ) {
        return st != null || st.trim().length() > 0;
    }

}
