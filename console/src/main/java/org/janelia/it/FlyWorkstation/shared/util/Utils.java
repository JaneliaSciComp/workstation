/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/26/11
 * Time: 7:16 PM
 */
package org.janelia.it.FlyWorkstation.shared.util;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.janelia.it.FlyWorkstation.gui.framework.outline.DynamicTree;

import java.awt.*;
import java.io.FileNotFoundException;
import java.net.URL;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Utils {

    public static boolean isEmpty(String str) {
        return (str == null || "".equals(str));
    }

    public static Icon getImage(String filename) throws FileNotFoundException {
    	try {
    	URL picURL = DynamicTree.class.getResource("/images/"+filename);
    	return new ImageIcon(picURL);
    	}
    	catch (Exception e) {
    		throw new FileNotFoundException("/images/"+filename);
    	}
    }
    
    /**
     * Borrowed from http://www.pikopong.com/blog/2008/08/13/auto-resize-jtable-column-width/
     */
    public static void autoResizeColWidth(JTable table) {

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        DefaultTableCellRenderer defaultRenderer = (DefaultTableCellRenderer)table.getTableHeader().getDefaultRenderer();

        int margin = 5;

        for (int i = 0; i < table.getColumnCount(); i++) {
            DefaultTableColumnModel colModel = (DefaultTableColumnModel)table.getColumnModel();
            TableColumn col = colModel.getColumn(i);
            int width = 0;

            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer();
            if (renderer == null) renderer = defaultRenderer;

            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
            width = comp.getPreferredSize().width;

            // Get maximum width of column data
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, i);
                comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, i), false, false, r, i);
                width = Math.max(width, comp.getPreferredSize().width);
            }

            width += 2 * margin;
            col.setPreferredWidth(width);
        }

        defaultRenderer.setHorizontalAlignment(SwingConstants.LEFT);
    }


}
