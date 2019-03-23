/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.movie;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * part of the dialog used to save Horta movie frames
 * @author brunsc
 */
public class SaveFramesPanel extends JPanel
{
    private final NotifyDescriptor notifyDescriptor;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String cancelOption = "Cancel";
    private final String saveOption = "Save";
    private final String[] options = {saveOption, cancelOption};
    
    public SaveFramesPanel() 
    {
        buildGui();
        
        this.notifyDescriptor = new NotifyDescriptor(
                this, // instance of your panel
                "Save all Horta movie frame images", // title of the dialog
                NotifyDescriptor.OK_CANCEL_OPTION, // it is Yes/No dialog ...
                NotifyDescriptor.QUESTION_MESSAGE, // ... of a question type => a question mark icon
                options,
                cancelOption // default option is "Cancel"
        );
    }
    
    private final JFileChooser folderChooser = new JFileChooser();

    private File chooseOutputFolder() 
    {
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION)
            return null;
        File file = folderChooser.getSelectedFile();
        sanityCheckOutputFolder(file);
        return file;
    }
    
    final JTextField outFolderField = new JTextField();
    final JTextField movieNameField = new JTextField("mymovie");
    final JComboBox<Float> fpsBox = new JComboBox<>(new Float[] {24.0f, 30.0f, 60.0f});
    final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
    
    private void buildGui() 
    {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Output folder field
        JPanel folderPanel = new JPanel();
        folderPanel.setBorder(BorderFactory.createTitledBorder("Movie frame image output folder"));
        folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.LINE_AXIS));
        folderPanel.add(outFolderField);
        JButton browseFolderButton = new JButton("...");
        folderPanel.add(browseFolderButton);
        browseFolderButton.setAction(new AbstractAction() {
            {
                putValue(Action.NAME, "..."); // needed for button label
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                File folder = chooseOutputFolder();
                if (folder != null)
                    outFolderField.setText(folder.getAbsolutePath());        
            }
        });
        folderPanel.setMaximumSize(new Dimension(
                folderPanel.getMaximumSize().width,
                folderPanel.getPreferredSize().height));
        add(folderPanel);
        
        // Movie name field
        JPanel movieNamePanel = new JPanel();
        movieNamePanel.setBorder(BorderFactory.createTitledBorder("Name of movie"));
        movieNamePanel.setLayout(new BoxLayout(movieNamePanel, BoxLayout.LINE_AXIS));
        movieNamePanel.add(movieNameField);
        movieNamePanel.setMaximumSize(new Dimension(
                movieNamePanel.getMaximumSize().width,
                movieNamePanel.getPreferredSize().height));
        add(movieNamePanel);
        
        // delete frames toggle
        JPanel deleteFramesPanel = new JPanel();
        deleteFramesPanel.setLayout(new BoxLayout(deleteFramesPanel, BoxLayout.LINE_AXIS));
        JCheckBox deleteCheckBox = new JCheckBox("Delete frame images?");
        deleteCheckBox.setEnabled(false); // not implemented yet... TODO:
        deleteFramesPanel.add(deleteCheckBox);
        deleteFramesPanel.add(Box.createHorizontalGlue());
        add(deleteFramesPanel);
        
        // Frame rate combo box
        JPanel frameRatePanel = new JPanel();
        frameRatePanel.setBorder(BorderFactory.createTitledBorder("Movie Frame Rate"));
        frameRatePanel.setLayout(new BoxLayout(frameRatePanel, BoxLayout.LINE_AXIS));
        fpsBox.setMaximumSize(fpsBox.getPreferredSize());
        frameRatePanel.add(fpsBox);
        frameRatePanel.add(new JLabel("frames per second"));
        frameRatePanel.add(Box.createHorizontalGlue());
        add(frameRatePanel);
        
        // progress bar
        add(progressBar);        
        
        add(Box.createVerticalGlue());
    }

    private boolean sanityCheckOutputFolder(File file) {        
        if (! file.exists()) {
            JOptionPane.showMessageDialog(this,
                    "No such folder '" + file.getAbsolutePath() + "'",
                    "Folder not found",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (! file.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "That's is a regular file, not a folder: '" + file.getAbsolutePath() + "'",
                    "That's not a folder",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (! file.canWrite()) {
            JOptionPane.showMessageDialog(this,
                    "You cannot write to this folder: '" + file.getAbsolutePath() + "'",
                    "You cannot write to that folder",
                    JOptionPane.ERROR_MESSAGE);
            return false;                    
        }
        return true;
    }
    
    // e.g. "/Users/"
    private File fileForFrameImage(File folder, String baseName, int frameNumber) 
    {
        String imageName = baseName + "_" + String.format("%05d", frameNumber) + ".jpg";
        File result = new File(folder, imageName);
        return result;
    }
    
    private boolean sanityCheckFrameName(File folder, String baseName) 
    {
        File firstFrameImage = fileForFrameImage(folder, baseName, 1);
        if (firstFrameImage.exists()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Overwrite existing image: '" + firstFrameImage.getAbsolutePath() + "'?",
                    "Overwrite existing files?",
                    JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION)
                return false;        
        }
        return true;
    }
    
    private void updateProgressBar(final int percent) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setValue( percent );
        }});                           
    }
    
    private void reportSuccess(final File frameFolder, final String baseFileName, final float frameRate) 
    {
        final JComponent parent = this;
        // Report successful completion, in the GUI thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {                        
                // Assuming all went well...
                progressBar.setValue(100);

                // Use a JTextPane so the user can select the text with a mouse
                JTextPane message = new JTextPane();
                message.setEditable(false);
                message.setBackground(null); // same as JLabel
                message.setBorder(null);
                message.setContentType("text/html");
                message.setText(""
                        + "<html>Finished saving frame images"
                        + "<br>in folder &quot;" + frameFolder + "&quot;"
                        + "<br>To create a movie file, run ffmpeg from the command line:"
                        + "<br><br> <b>ffmpeg" // program name
                        + " -i " + baseFileName + "_%5d.jpg" // input image file name pattern
                        + " -r " + frameRate // input frame rate
                        + " -b:v 5M" // use a decent bit rate
                        // + " -y" // always say "yes" to overwriting files
                        + " " + baseFileName +".mp4</b> <html>" // output file name
                );
                
                // Reduce save dialog
                // parent.setVisible(false);
                
                JOptionPane.showMessageDialog(parent, 
                        message,
                        "Finished saving frame images",
                        JOptionPane.INFORMATION_MESSAGE);
                progressBar.setValue(0);
            }
        });      
    }
    
    private void reportCancel() 
    {
        final JComponent parent = this;
        // Report successful completion, in the GUI thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(
                        parent, 
                        "Movie frame image saving was cancelled", 
                        "Movie frame image saving was cancelled", 
                        JOptionPane.WARNING_MESSAGE
                        );
                progressBar.setValue(0);
            }
        });      
    }
    
    private void reportError(final String message) 
    {
        final JComponent parent = this;
        // Report successful completion, in the GUI thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(
                        parent, 
                        message, 
                        "Error saving movie image files", 
                        JOptionPane.ERROR_MESSAGE
                        );
                progressBar.setValue(0);
            }
        });      
    }
    
    private BufferedImage currentImage;
    private BufferedImage getFrame(final MoviePlayState playState) 
            throws InterruptedException, InvocationTargetException 
    {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                currentImage = playState.getCurrentFrameImageNow();
            }
        });
        
        // FFMpeg h264 requires that image width be even
        int w = currentImage.getWidth();
        int h = currentImage.getHeight();
        // ensure width and height are even
        if (w % 2 == 1)
            w -= 1;
        if (h % 2 == 1)
            h -= 1;
        currentImage = currentImage.getSubimage(0, 0, w, h);
        
        return currentImage;
    }
    
    private void saveFrameImages(final MoviePlayState playState) 
    {
        final File frameFolder = new File(outFolderField.getText());
        final String baseFileName = movieNameField.getText();
        final float frameRate = fpsBox.getItemAt(fpsBox.getSelectedIndex());
        
        if (! sanityCheckOutputFolder(frameFolder))
            return;

        if (! sanityCheckFrameName(frameFolder, baseFileName))
            return;
        // logger.info("Hey! I should save frames here...");
        
        progressBar.setValue(1);
        final JComponent dialog = this;

        // launch a separate save thread
        Runnable saveFramesTask = new Runnable() {
            @Override
                public void run() {
                    float movieDuration = playState.getTotalDuration();
                    // frameCount is ALL rendered frames, not just Key Frames
                    int frameCount = (int) Math.ceil(frameRate * (movieDuration
                            // smidgen added to round up to one from zero for zero-duration, single-frame movies
                            + 0.2/frameRate)); 
                    
                    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    // float oldQuality = param.getCompressionQuality(); // default seems to be 0.75
                    param.setCompressionQuality(0.95f); // use rather high quality
                    
                    for (int f = 0; f < frameCount; ++f) {
                        float progressRatio = 0;
                        if (f > 0) // avoid divide by zero
                            progressRatio = f / (float)(frameCount - 1);
                        float frameInstant = movieDuration * progressRatio;
                        final File imageFile = fileForFrameImage(frameFolder, baseFileName, f+1);
                        
                        playState.skipToTime(frameInstant);
                        
                        try {
                            // Collect and save the frames
                            BufferedImage frameImage = getFrame(playState);
                            if (frameImage != null) {
                                try {
                                    writer.setOutput(ImageIO.createImageOutputStream(imageFile));
                                    writer.write(frameImage);
                                    // ImageIO.write(frameImage, "JPG", imageFile);
                                } catch (IOException ex) {
                                    reportError(ex.getMessage());
                                    return;
                                }
                            }
                        } catch (InterruptedException ex) {
                            updateProgressBar(0);
                            reportCancel();
                            return;
                        } catch (InvocationTargetException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        
                        // TODO: check for "Cancel" operation
                        
                        updateProgressBar(Math.round(100 * progressRatio));                
                    }
                    
                    // TODO: post run sanity checks
                    
                    // Report successful completion, in the GUI thread
                    reportSuccess(frameFolder, baseFileName, frameRate);
                }
        };

        // Show the save dialog, so user can see progress bar
        // setVisible(true);
        
        new Thread(saveFramesTask).start();
    }
    
    public void showDialog(MoviePlayState playState) {
        Object result = DialogDisplayer.getDefault().notify(notifyDescriptor);
        if (result == saveOption) {
            saveFrameImages(playState);
        } 
        else {
            // logger.info("Frame saving was cancelled");
        }
    }
}
