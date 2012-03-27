package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 3/13/12
 * Time: 1:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class PatternSearchDialog extends ModalDialog {


    private final JPanel mainPanel;
	private final JPanel inputPanel;
	private final JLabel titleLabel;
    private final JTextField inputField;
    private final JTextField folderNameField;

    private String lastSearchString;
    private List<Entity> results;

    private DynamicTable resultsTable;

    public class PatternAnnotationQuantified {
        public double v1;
        public double v2;
    }

    private static Map<String, PatternAnnotationQuantified> patternQMap = new HashMap<String, PatternAnnotationQuantified>();

    public PatternSearchDialog() {

        setTitle("Pattern Annotation Search");

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        inputPanel = new JPanel(new BorderLayout());
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        titleLabel = new JLabel("Search for:");
        inputPanel.add(titleLabel, BorderLayout.NORTH);

        inputField = new JTextField(40);
        inputField.setText("");
        inputField.setToolTipText("Enter search terms...");
        inputPanel.add(inputField, BorderLayout.CENTER);

//        inputField.getDocument().addDocumentListener(new DocumentListener() {
//
//			@Override
//			public void removeUpdate(DocumentEvent e) {
//				performSearch();
//			}
//
//			@Override
//			public void insertUpdate(DocumentEvent e) {
//				performSearch();
//			}
//
//			@Override
//			public void changedUpdate(DocumentEvent e) {
//				performSearch();
//			}
//		});

//        inputField.addKeyListener(new KeyAdapter() {
//			@Override
//			public void keyReleased(KeyEvent e) {
//				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//					performSearch();
//				}
//			}
//		});

        add(mainPanel, BorderLayout.NORTH);

        resultsTable = new DynamicTable() {
			@Override
			public Object getValue(Object userObject, DynamicColumn column) {
				if (userObject instanceof Entity) {
					Entity entity = (Entity)userObject;
					if ("Name".equals(column.getName())) {
						return entity.getName();
					}
					else if ("Type".equals(column.getName())) {
						return entity.getEntityType().getName();
					}
					else if ("Owner".equals(column.getName())) {
						return entity.getUser().getUserLogin();
					}
				}
				return null;
			}
		};

		resultsTable.setPreferredSize(new Dimension(800,600));
		resultsTable.addColumn("Name", "Name", true, false, false, false);
		resultsTable.addColumn("Type", "Type", true, false, false, false);
		resultsTable.addColumn("Owner", "Owner", true, false, false, false);
		resultsTable.updateTableModel();

		add(resultsTable, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());

		JLabel folderNameLabel = new JLabel("Save selected objects in folder: ");
		buttonPane.add(folderNameLabel);

        folderNameField = new JTextField(10);
        folderNameField.setToolTipText("Enter the folder name to save the results in");
        buttonPane.add(folderNameField);

        JButton okButton = new JButton("Save");
        okButton.setToolTipText("Save the results");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveResults();
			}
		});
        buttonPane.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this dialog");
        cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});
        buttonPane.add(cancelButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void performSearch() {

//    	resultsTable.removeAllRows();
//
//    	String searchString = inputField.getText();
//    	if (searchString.equals(lastSearchString)) return;
//    	lastSearchString = searchString;
//    	if (searchString==null || "".equals(searchString)) return;
//
//    	searchString = "-entity_type:Ontology* AND (" + searchString +")";
//
//    	try {
//        	this.results = ModelMgr.getModelMgr().searchEntities(searchRoot==null?null:searchRoot.getId(), searchString, null, 30);
//        	for(Entity entity : results) {
//        		resultsTable.addRow(entity);
//        	}
//        	resultsTable.updateTableModel();
//    	}
//    	catch (Exception e) {
//    		e.printStackTrace();
//    	}
    }

    public void saveResults() {

//    	Utils.setWaitingCursor(this);
//
//    	SimpleWorker worker = new SimpleWorker() {
//
//    		private Entity newFolder;
//
//			@Override
//			protected void doStuff() throws Exception {
//
//				String folderName = folderNameField.getText();
//				this.newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
//				newFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
//				ModelMgr.getModelMgr().saveOrUpdateEntity(newFolder);
//
//		        for (Object obj : resultsTable.getSelectedObjects()) {
//		        	Entity entity = (Entity)obj;
//					EntityData newEd = newFolder.addChildEntity(entity);
//					ModelMgr.getModelMgr().saveOrUpdateEntityData(newEd);
//				}
//			}
//
//			@Override
//			protected void hadSuccess() {
//				final EntityOutline entityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline();
//				entityOutline.refresh(new Callable<Void>() {
//					@Override
//					public Void call() throws Exception {
//		        		ModelMgr.getModelMgr().selectOutlineEntity("/e_"+newFolder.getId(), true);
//						return null;
//					}
//
//				});
//		    	Utils.setDefaultCursor(PatternSearchDialog.this);
//	            setVisible(false);
//			}
//
//			@Override
//			protected void hadError(Throwable error) {
//				ModelMgr.getModelMgr().handleException(error);
//		    	Utils.setDefaultCursor(PatternSearchDialog.this);
//			}
//		};
//
//		worker.execute();
    }

    public void showDialog() {
    	titleLabel.setText("Search for anatomical patterns");
    	init();
        try {
            System.out.println("PatterSearchDialog getPatternAnnotationQuantifierMapsFromSummary() start");
            ModelMgr.getModelMgr().getPatternAnnotationQuantifierMapsFromSummary();
            System.out.println("PatterSearchDialog getPatternAnnotationQuantifierMapsFromSummary() end");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

//    public void showForEntity(Entity entity) {
//    	this.searchRoot = entity;
//    	titleLabel.setText("Search within "+searchRoot.getName());
//    	init();
//    }

    private void init() {

//		final EntityOutline entityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline();
//
//		int maxNum = 0;
//		for(EntityData ed : entityOutline.getRootEntity().getEntityData()) {
//			Entity topLevelFolder = ed.getChildEntity();
//			if (topLevelFolder != null) {
//				Pattern p = Pattern.compile("^Search Results #(\\d+)$");
//				Matcher m = p.matcher(topLevelFolder.getName());
//				if (m.matches()) {
//					String num = m.group(1);
//					if (num!=null && !"".equals(num)) {
//						int n = Integer.parseInt(num);
//						if (n>maxNum) {
//							maxNum = n;
//						}
//					}
//				}
//			}
//		}
//		folderNameField.setText("Search Results #"+(maxNum+1));

    	packAndShow();
    }

}
