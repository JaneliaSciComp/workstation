package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.EnumText;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 6/12/12
 * Time: 10:28 AM
 /*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


public class SpecialAnnotationChooserDialog extends JDialog{

    public static JPanel annotationPanel = new JPanel(new GridLayout(1,0));
    private DefaultTableModel model;
    JComboBox comboBox;

    public SpecialAnnotationChooserDialog() {
        super(SessionMgr.getBrowser(),"Special Annotation Session", false);

        model = new DefaultTableModel(){
            @Override
            public boolean isCellEditable(int row, int column) {
                //all cells false
                return false;
            }
        };
        model.addColumn("Annotation");
        model.addColumn("Enumeration");

        JTable table = new JTable(model);
        TableColumn comboColumn = table.getColumnModel().getColumn(1);

        List<OntologyElement> list = ModelMgr.getModelMgr().getCurrentOntology().getChildren();
        for(OntologyElement element:list){
            if(element.getType() instanceof EnumText){
                model.addRow(new Object[]{element.getName()});
                OntologyElement valueEnum = ((EnumText) element.getType()).getValueEnum();

                if (valueEnum==null) {
                    Exception error = new Exception(element.getName()+" has no supporting enumeration.");
                    SessionMgr.getSessionMgr().handleException(error);
                    return;
                }

                List<OntologyElement> children = valueEnum.getChildren();

                int i = 0;
                Object[] selectionValues = new Object[children.size()];
                for(OntologyElement child : children) {
                    selectionValues[i++] = child;
                }

                JComboBox comboBox = new JComboBox(selectionValues);
                comboColumn.setCellEditor(new DefaultCellEditor(comboBox));
            }
        }

        table.setPreferredScrollableViewportSize(new Dimension(500, 500));
        table.setFillsViewportHeight(true);
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        annotationPanel.add(scrollPane);
        createAndShowGUI();

    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Special Annotation Session");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        //Create and set up the content pane.

        annotationPanel.setOpaque(true); //content panes must be opaque
        frame.setContentPane(annotationPanel);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private void iterateAndAddRows(OntologyElement element){
        if(element.getType() instanceof EnumText){
            model.addRow(new Object[]{element.getName()});
            OntologyElement valueEnum = ((EnumText) element.getType()).getValueEnum();

            if (valueEnum==null) {
                Exception error = new Exception(element.getName()+" has no supporting enumeration.");
                SessionMgr.getSessionMgr().handleException(error);
                return;
            }

            List<OntologyElement> children = valueEnum.getChildren();

            int i = 0;
            Object[] selectionValues = new Object[children.size()];
            for(OntologyElement child : children) {
                selectionValues[i++] = child;
            }

            comboBox = new JComboBox(selectionValues);

        }
    }


}
