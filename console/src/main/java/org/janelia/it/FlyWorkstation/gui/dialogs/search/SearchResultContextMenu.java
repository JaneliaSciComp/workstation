package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.JMenuItem;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.choose.EntityChooser;
import org.janelia.it.FlyWorkstation.gui.dialogs.choose.MultiTreeEntityChooser;
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
        JMenuItem detailsMenuItem = new JMenuItem("  View Details");
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
        JMenuItem detailsMenuItem = new JMenuItem("  Map All Results To Related Entities...");
        detailsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        
				SimpleWorker worker = new SimpleWorker() {

					List<List<Object>> paths = new ArrayList<List<Object>>();
					MultiTreeEntityChooser entityChooser;
					
					@Override
					protected void doStuff() throws Exception {
						List<List<Object>> allPaths = getRootPaths(entity, new HashSet<Long>());
						for(List<Object> path : allPaths) {
							if (!path.isEmpty()) {
								paths.add(path);
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
								// Order by id to get oldest entities first
								return root1.getId().compareTo(root2.getId());
							}
						});
					}
					
					@Override
					protected void hadSuccess() {
						
						List<EntityTree> trees = new ArrayList<EntityTree>();
						Map<EntityTree,String> startingPaths = new HashMap<EntityTree,String>();
						
						for(List<Object> path : paths) {
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
							
							trees.add(tree);
							startingPaths.put(tree, selected);
						}
						
						entityChooser = new MultiTreeEntityChooser("Select relative", trees);
						
						int returnVal = entityChooser.showDialog(null);
		                if (returnVal != EntityChooser.CHOOSE_OPTION) return;
						String uniqueId = entityChooser.getUniqueIds().get(0);
						EntityTree selectedTree = entityChooser.getSelectedTree();
						String startingPath = startingPaths.get(selectedTree);
						if (startingPath.equals(uniqueId)) {
							hadError(new Exception("Cannot map entity to itself."));
						}
						else {
							ResultTreeMapping projection = new ResultTreeMapping(selectedTree, startingPath, uniqueId);
							searchResultsPanel.projectResults(projection);	
						}
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
			parent = ModelMgr.getModelMgr().getEntityById(parent.getId());
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