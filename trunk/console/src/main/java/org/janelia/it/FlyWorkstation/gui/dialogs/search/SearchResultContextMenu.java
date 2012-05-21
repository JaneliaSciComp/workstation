package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.JMenuItem;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.choose.EntityChooser;
import org.janelia.it.FlyWorkstation.gui.framework.context_menu.AbstractContextMenu;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTree;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTreeCellRenderer;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.ExpansionState;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Context menu for general search results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchResultContextMenu extends AbstractContextMenu<Entity> {

	private SearchResultsPanel searchResultsPanel;
	
	public SearchResultContextMenu(SearchResultsPanel searchResultsPanel, List<Entity> selectedEntities, String label) {
		super(selectedEntities, label);
		this.searchResultsPanel = searchResultsPanel;
	}
	
	@Override
	protected void addSingleSelectionItems() {
		Entity entity = getSelectedElement();
		add(getTitleItem("Entity '"+entity.getName()+"'"));
        add(getDetailsItem());
        setNextAddRequiresSeparator(true);
        add(getRelativesItem());
	}
	
	@Override
	protected void addMultipleSelectionItems() {
        // None
	}
	
	protected JMenuItem getDetailsItem() {
		final Entity entity = getSelectedElement();
        JMenuItem detailsMenuItem = new JMenuItem("  View details");
        detailsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        new EntityDetailsDialog().showForEntity(entity);
			}
		});
        return detailsMenuItem;
	}

	protected JMenuItem getRelativesItem() {
		final Entity entity = getSelectedElement();
        JMenuItem detailsMenuItem = new JMenuItem("  Map all results to related entities...");
        detailsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        
				SimpleWorker worker = new SimpleWorker() {

					List<List<Object>> paths = new ArrayList<List<Object>>();
					EntityChooser entityChooser;
					
					@Override
					protected void doStuff() throws Exception {
						List<List<Object>> allPaths = getRootPaths(entity, new HashSet<Long>());
						for(List<Object> path : allPaths) {
							if (!path.isEmpty()) {
								Entity root = ((Entity)path.get(0));
								if (ModelMgr.getModelMgr().hasAccess(root)) {
									paths.add(path);
								}
							}
						}
						if (paths.isEmpty()) {
							throw new Exception("No access to a root for this entity");
						}
						Collections.sort(paths, new Comparator<List<Object>>() {
							@Override
							public int compare(List<Object> o1, List<Object> o2) {
								Entity root1 = (Entity)o1.get(0);
								Entity root2 = (Entity)o2.get(0);
								// Order by id desc to get newest entities first
								return root2.getId().compareTo(root1.getId());
							}
						});
					}
					
					@Override
					protected void hadSuccess() {
						
						// TODO: add support for multiple paths 
						List<Object> path = paths.get(0);
						
						ExpansionState expansion = new ExpansionState();
						expansion.addExpandedUniqueId("/");
						
						StringBuffer sb = new StringBuffer();
						for(Object p : path) {
							sb.append("/");
							if (p instanceof Entity) {
								sb.append("e_");
								sb.append(((Entity)p).getId());	
							}
							else {	
								sb.append("ed_");
								sb.append(((EntityData)p).getId());	
							}

							expansion.addExpandedUniqueId(sb.toString());
						}
						
						String selected = sb.toString();
						expansion.setSelectedUniqueId(selected);

						EntityTree tree = new EntityTree(true);
						tree.initializeTree((Entity)path.get(0));
						tree.getDynamicTree().setCellRenderer(new EntityTreeCellRenderer() {
							@Override
							protected boolean isHighlighted(Entity entity2) {
								return entity2.getId().equals(entity.getId());
							}
							
						});
						expansion.restoreExpansionState(tree.getDynamicTree(), true);
						
						entityChooser = new EntityChooser("Select relative", tree);
						entityChooser.getEntityTree().getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
						
						int returnVal = entityChooser.showDialog(null);
		                if (returnVal != EntityChooser.CHOOSE_OPTION) return;
						String uniqueId = entityChooser.getUniqueIds().get(0);
						
						ResultTreeMapping projection = new ResultTreeMapping(entityChooser.getEntityTree(), selected, uniqueId);
						searchResultsPanel.projectResults(projection);
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				
				worker.execute();
			}
		});
        return detailsMenuItem;
	}
	
	private List<List<Object>> getRootPaths(Entity entity, Set<Long> visited) throws Exception {
		
		List<List<Object>> rootPaths = new ArrayList<List<Object>>();
		if (visited.contains(entity.getId())) return rootPaths;
		visited.add(entity.getId());
		
		List<EntityData> parents = ModelMgr.getModelMgr().getParentEntityDatas(entity.getId());
		
		if (parents.isEmpty()) {
			List<Object> path = new ArrayList<Object>();
			path.add(entity);
			rootPaths.add(path);
			return rootPaths;
		}
		
		for (EntityData parentEd : parents) {
			Entity parent = parentEd.getParentEntity();
			parent = ModelMgr.getModelMgr().getEntityById(parent.getId().toString());
			List<List<Object>> parentPaths = getRootPaths(parent, visited);
			for(List<Object> parentPath : parentPaths) {
				parentPath.add(parentEd);
				parentPath.add(entity);
				rootPaths.add(parentPath);
			}
		}
		
		return rootPaths;
	}
}