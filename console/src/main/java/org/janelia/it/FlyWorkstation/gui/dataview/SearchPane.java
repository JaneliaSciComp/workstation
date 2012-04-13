package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchParametersPanel;

/**
 * The top search pane with a tab for every search type. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SearchPane extends JPanel {
	
	private SearchParametersPanel solrPanel; // haha, get it? 
	private JPanel groovyPanel;
	private JTextArea groovyArea;
	private JTextArea groovyExamples;
	
	public SearchPane(final SearchConfiguration searchConfig) {
		
		setLayout(new BorderLayout());
		
		solrPanel = new SearchParametersPanel() {
        	@Override
        	protected void performSearch(boolean clear) {
        		performSolrSearch(clear);
        	}
        };
        searchConfig.addConfigurationChangeListener(solrPanel);
        
		groovyPanel = new JPanel(new BorderLayout());
		groovyArea = new JTextArea(5, 10);
		groovyExamples = new JTextArea(5, 10);
		groovyExamples.setEditable(false);
		
		StringBuffer examples = new StringBuffer();
		examples.append("Ex1: a.getEntity(id)\n");
		examples.append("Ex1: a.getEntityTree(id)\n");
		
		groovyExamples.setText(examples.toString());

		JPanel groovyBody = new JPanel(new FlowLayout());
		groovyBody.setLayout(new BoxLayout(groovyBody, BoxLayout.LINE_AXIS));
		groovyBody.add(groovyArea);
		groovyBody.add(Box.createRigidArea(new Dimension(10,1)));
		groovyBody.add(groovyExamples);

		JButton groovyButton = new JButton("Execute Groovy Code");
		groovyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				performGroovySearch(groovyArea.getText());
			}
		});
		
		JPanel groovyButtons = new JPanel();
		groovyButtons.setLayout(new BoxLayout(groovyButtons, BoxLayout.LINE_AXIS));
		groovyButtons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        groovyButtons.add(groovyButton);
        groovyButtons.add(Box.createHorizontalGlue());
        
		groovyPanel.add(groovyBody, BorderLayout.CENTER);
		groovyPanel.add(groovyButtons, BorderLayout.SOUTH);
		groovyPanel.setVisible(false);
		
		JPanel solrTab = new JPanel(new BorderLayout());
		solrTab.add(solrPanel, BorderLayout.CENTER);
		JPanel groovyTab = new JPanel(new BorderLayout());
		groovyTab.add(groovyPanel, BorderLayout.CENTER);
		
		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Solr Search", solrTab);
		tabbedPane.addTab("Groovy Search", groovyTab);
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				switch (tabbedPane.getSelectedIndex()) {
				case 0:
					solrPanel.setVisible(true);
					groovyPanel.setVisible(false);
					break;
				case 1:
					solrPanel.setVisible(false);
					groovyPanel.setVisible(true);
					break;
				}
				
			}
		});
		add(tabbedPane, BorderLayout.CENTER);
	}

	public abstract void performSolrSearch(boolean clear);
	
	public abstract void performGroovySearch(String code);
	
	public SearchParametersPanel getSolrPanel() {
		return solrPanel;
	}
}
