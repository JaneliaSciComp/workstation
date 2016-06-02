package org.janelia.it.workstation.gui.alignment_board.swing;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.workstation.gui.alignment_board.util.ABItem;
import org.janelia.it.workstation.gui.alignment_board.util.RenderUtils;
import org.janelia.it.workstation.gui.util.ColorSwatch;

/**
 * Panel holds data about an alignment board item.
 * @author fosterl
 */
public class AlignmentBoardItemPanel extends JPanel {
    private AlignmentBoardItem item;
    private ABIControls controls;
    public AlignmentBoardItemPanel(AlignmentBoardItem item) {
        this.item = item;
        this.controls = new ABIControls(item);
        this.initForGui();
    }
    
    public ABIControls getControls() {
        return controls;
    }
    
    private void initForGui() {
        this.setLayout(new BorderLayout());
        JPanel parentPanel = new JPanel();
        parentPanel.setLayout(new GridLayout(5, 1));
        controls.getVisible().setBorder(new TitledBorder("Visible"));
        controls.getVisible().setEnabled(false);
        controls.getRefType().setBorder(new TitledBorder("Type"));
        controls.getInclusionStatus().setBorder(new TitledBorder("Included"));
        controls.getRenderMethod().setBorder(new TitledBorder("Render Method"));
        parentPanel.add(controls.getRefType());
        parentPanel.add(controls.getVisible());
        parentPanel.add(controls.getInclusionStatus());
        parentPanel.add(controls.getRenderMethod());

        if (controls.getChildrenTable() != null) {
            JScrollPane sp = new JScrollPane(controls.getChildrenTable());
            sp.setBorder(new TitledBorder("Child Items"));
            this.add(parentPanel, BorderLayout.NORTH);
            this.add(sp, BorderLayout.CENTER);
        }
        else {
            this.add(parentPanel, BorderLayout.CENTER);
        }
    }
    
    public static class ABIControls {
        private JCheckBox visible = new JCheckBox();
        private JLabel inclusionStatus = new JLabel();
        private ColorSwatch colorSwatch = new ColorSwatch();
        private JLabel renderMethod = new JLabel();
        private JTable childrenTable;
        private JLabel refType = new JLabel();

        public ABIControls(AlignmentBoardItem item) {
            ABItem abItem = RenderUtils.getObjectForItem(item);
            visible.setSelected(item.isVisible());
            inclusionStatus.setText(item.getInclusionStatus());
            colorSwatch.setColor(RenderUtils.getColorFromRGBStr(item.getColor()));
            renderMethod.setText(item.getRenderMethod());
            refType.setText(abItem.getType());
            
            // Establish the children table.
            if ( item.getChildren() != null  &&  item.getChildren().size() > 0 ) {
                Vector<String> headers = new Vector<>();
                headers.add("Color");
                headers.add("Type");
                headers.add("Visible");
                headers.add("Included");
                headers.add("Render Method");

                // TODO: eventually WILL need a better column model, to deal
                // with color swatches.
                for (AlignmentBoardItem childItem: item.getChildren()) {
                    ABItem abChildItem = RenderUtils.getObjectForItem(childItem);
                    Vector<String> row = new Vector<>();
                    row.add(childItem.getColor());
                    try {
                        row.add(RenderUtils.getViewableClassName(abChildItem.getType()));
                    } catch (ClassNotFoundException cnfe) {
                        row.add(abChildItem.getType());
                    }
                    row.add(Boolean.toString(childItem.isVisible()));
                    row.add(childItem.getInclusionStatus());
                    row.add(childItem.getRenderMethod());
                }
            }
        }
        
        /**
         * @return the visible
         */
        public JCheckBox getVisible() {
            return visible;
        }

        /**
         * @param visible the visible to set
         */
        public void setVisible(JCheckBox visible) {
            this.visible = visible;
        }

        /**
         * @return the inclusionStatus
         */
        public JLabel getInclusionStatus() {
            return inclusionStatus;
        }

        /**
         * @param inclusionStatus the inclusionStatus to set
         */
        public void setInclusionStatus(JLabel inclusionStatus) {
            this.inclusionStatus = inclusionStatus;
        }

        /**
         * @return the colorSwatch
         */
        public ColorSwatch getColorSwatch() {
            return colorSwatch;
        }

        /**
         * @param colorSwatch the colorSwatch to set
         */
        public void setColorSwatch(ColorSwatch colorSwatch) {
            this.colorSwatch = colorSwatch;
        }

        /**
         * @return the renderMethod
         */
        public JLabel getRenderMethod() {
            return renderMethod;
        }

        /**
         * @param renderMethod the renderMethod to set
         */
        public void setRenderMethod(JLabel renderMethod) {
            this.renderMethod = renderMethod;
        }

        /**
         * @return the childrenTable
         */
        public JTable getChildrenTable() {
            return childrenTable;
        }

        /**
         * @param childrenTable the childrenTable to set
         */
        public void setChildrenTable(JTable childrenTable) {
            this.childrenTable = childrenTable;
        }

        /**
         * @return the refType
         */
        public JLabel getRefType() {
            return refType;
        }

        /**
         * @param refType the refType to set
         */
        public void setRefType(JLabel refType) {
            this.refType = refType;
        }
    }
}
