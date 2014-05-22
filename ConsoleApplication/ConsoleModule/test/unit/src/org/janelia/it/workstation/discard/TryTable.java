/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.discard;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author fosterl
 */
public class TryTable extends JFrame {
    public static void main( String[] args ) throws Exception {
        new TryTable();
    }
    public TryTable() {
        super( "See Selection" );
        setSize( 500, 500 );
        setLocation( 500, 200 );
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        String[][] rowData = new String[][] {
            {"Ingots","Smelt"},
            {"Ore","Mine"},
            {"Wood","Cut"},
            {"Dirt","Dig"},
            {"Bread","Craft"},
            {"Horse","Train"}
        };
        String[] colNames = new String[] {
            "Item", "Operation"
        };

        JTable table = new JTable( rowData, colNames );
        this.setLayout( new BorderLayout() );
        this.add( table, BorderLayout.CENTER );
        table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        setVisible( true );
    }
}
