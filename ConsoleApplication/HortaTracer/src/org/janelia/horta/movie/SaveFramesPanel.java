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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * part of the dialog used to save Horta movie frames
 * @author brunsc
 */
public class SaveFramesPanel extends JPanel
{
    private final NotifyDescriptor notifyDescriptor;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String cancelOption = "Cancel";
    private final String saveOption = "Save";
    private final String[] options = {saveOption, cancelOption};
    
    public SaveFramesPanel() 
    {
        buildGui();
        
        this.notifyDescriptor = new NotifyDescriptor(
                this, // instance of your panel
                "Save all Horta movie frames", // title of the dialog
                NotifyDescriptor.OK_CANCEL_OPTION, // it is Yes/No dialog ...
                NotifyDescriptor.QUESTION_MESSAGE, // ... of a question type => a question mark icon
                options,
                cancelOption // default option is "Cancel"
        );
    }
    
    private void buildGui() 
    {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Output folder field
        JPanel folderPanel = new JPanel();
        folderPanel.setBorder(BorderFactory.createTitledBorder("Movie frame image output folder"));
        folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.LINE_AXIS));
        JTextField outFolderField = new JTextField();
        folderPanel.add(outFolderField);
        JButton browseFolderButton = new JButton("...");
        folderPanel.add(browseFolderButton);
        add(folderPanel);
        
        // Movie name field
        JPanel movieNamePanel = new JPanel();
        movieNamePanel.setBorder(BorderFactory.createTitledBorder("Name of movie"));
        movieNamePanel.add(new JTextField());
        add(movieNamePanel);
        
        // delete frames toggle
        JPanel deleteFramesPanel = new JPanel();
        deleteFramesPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.LINE_AXIS));
        deleteFramesPanel.add(new JCheckBox("Delete frame images?"));
        deleteFramesPanel.add(Box.createHorizontalGlue());
        add(deleteFramesPanel);
        
        // Frame rate combo box
        JPanel frameRatePanel = new JPanel();
        frameRatePanel.setBorder(BorderFactory.createTitledBorder("Movie Frame Rate"));
        frameRatePanel.setLayout(new BoxLayout(frameRatePanel, BoxLayout.LINE_AXIS));
        frameRatePanel.add(new JComboBox());
        frameRatePanel.add(Box.createHorizontalGlue());
        add(frameRatePanel);
        
        // progress bar
        JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        add(progressBar);        
    }
    
    public void showDialog() {
        Object result = DialogDisplayer.getDefault().notify(notifyDescriptor);
        if (result == saveOption) {
            logger.info("Hey! I should save frames here...");
        } 
        else {
            logger.info("Frame saving was cancelled");
        }
    }
}
