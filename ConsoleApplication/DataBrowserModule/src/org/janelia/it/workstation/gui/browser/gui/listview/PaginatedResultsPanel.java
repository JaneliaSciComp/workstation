package org.janelia.it.workstation.gui.browser.gui.listview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.UIManager;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.it.workstation.gui.browser.events.model.PreferenceChangeEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import de.javasoft.swing.SimpleDropDownButton;

/**
 * A panel that builds pagination and selection features around an AnnotatedDomainObjectListViewer. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class PaginatedResultsPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(PaginatedResultsPanel.class);
    
    // Splash panel
    private final JLabel splashPanel;
    
    // Status bar
    private final JPanel statusBar;
    private final JLabel statusLabel;
    private final JPanel selectionButtonContainer;
    private final JButton prevPageButton;
    private final JButton nextPageButton;
    private final JButton endPageButton;
    private final JButton startPageButton;
    private final JButton selectAllButton;
    private final JLabel pagingStatusLabel;
    private final SimpleDropDownButton viewTypeButton;
    
    // Result view
    private AnnotatedDomainObjectListViewer resultsView;
    
    // Content
    private SearchResults searchResults;
    private ResultPage resultPage;
    private int numPages = 0;
    private int currPage = 0;
    
    // State
    protected DomainObjectSelectionModel selectionModel;
    protected SearchProvider searchProvider;
    	
    // Hud dialog
//    protected Hud hud;
    
    public PaginatedResultsPanel(DomainObjectSelectionModel selectionModel, SearchProvider searchProvider) {
                
        this.selectionModel = selectionModel;
        this.searchProvider = searchProvider;
        
        setLayout(new BorderLayout());
        
        splashPanel = new JLabel(Icons.getIcon("workstation_logo_white.png"));
        add(splashPanel);
        
        viewTypeButton = new SimpleDropDownButton("Choose Viewer...");
        viewTypeButton.setPopupMenu(getViewerPopupMenu());

        prevPageButton = new JButton(Icons.getIcon("arrow_back.gif"));
        prevPageButton.setToolTipText("Back a page");
        prevPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goPrevPage();
            }
        });

        nextPageButton = new JButton(Icons.getIcon("arrow_forward.gif"));
        nextPageButton.setToolTipText("Forward a page");
        nextPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goNextPage();
            }
        });

        startPageButton = new JButton(Icons.getIcon("arrow_double_left.png"));
        startPageButton.setToolTipText("Jump to start");
        startPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goStartPage();
            }
        });

        endPageButton = new JButton(Icons.getIcon("arrow_double_right.png"));
        endPageButton.setToolTipText("Jump to end");
        endPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goEndPage();
            }
        });

        selectAllButton = new JButton("Select All");
        selectAllButton.setToolTipText("Select all items on all pages");
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectAll();
            }
        });

        statusLabel = new JLabel("");
        pagingStatusLabel = new JLabel("");

        selectionButtonContainer = new JPanel();
        selectionButtonContainer.setLayout(new BoxLayout(selectionButtonContainer, BoxLayout.LINE_AXIS));

        selectionButtonContainer.add(selectAllButton);
        selectionButtonContainer.add(Box.createRigidArea(new Dimension(10, 20)));
        selectionButtonContainer.setVisible(false);

        statusBar = new JPanel();
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
        statusBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, (Color) UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));

        statusBar.add(viewTypeButton);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(statusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10, 20)));
        statusBar.add(selectionButtonContainer);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(pagingStatusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10, 20)));
        statusBar.add(startPageButton);
        statusBar.add(prevPageButton);
        statusBar.add(nextPageButton);
        statusBar.add(endPageButton);

//        hud = Hud.getSingletonInstance();
//        hud.addKeyListener(keyListener);
        
        setViewerType(ListViewerType.IconViewer);
    }
    
    private JPopupMenu getViewerPopupMenu() {

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        JMenuItem iconViewItem = new JMenuItem("Icon View");
        iconViewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setViewerType(ListViewerType.IconViewer);
            }
        });
        popupMenu.add(iconViewItem);

        JMenuItem tableViewItem = new JMenuItem("Table View");
        tableViewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setViewerType(ListViewerType.TableViewer);
            }
        });
        popupMenu.add(tableViewItem);

        return popupMenu;
    }
    
    private void setViewerType(ListViewerType viewerType) {
        this.viewTypeButton.setText(viewerType.getName());
        try {
            if (viewerType.getViewerClass()==null) {
                setViewer(null);
            }
            else {
                AnnotatedDomainObjectListViewer viewer = viewerType.getViewerClass().newInstance();
                setViewer(viewer);
            }
        }
        catch (InstantiationException | IllegalAccessException e) {
            log.error("Error instantiating viewer class",e);
            setViewer(null);
        }
        
        updateResultsView(true);
        
        // Reselect the items that were selected
        List<DomainObject> selectedDomainObjects = new ArrayList<>(); 
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        for(Reference id : selectionModel.getSelectedIds()) {
            DomainObject domainObject = model.getDomainObject(id);
            if (domainObject!=null) {
                selectedDomainObjects.add(domainObject);
            }
        }
        resultsView.selectDomainObjects(selectedDomainObjects, true, false);
        
    }

    private void setViewer(AnnotatedDomainObjectListViewer viewer) {
        viewer.setSelectionModel(selectionModel);
        viewer.setSearchProvider(searchProvider);
        this.resultsView = viewer;
    }

    private void updatePagingStatus() {
        startPageButton.setEnabled(currPage != 0);
        prevPageButton.setEnabled(currPage > 0);
        nextPageButton.setEnabled(currPage < numPages - 1);
        endPageButton.setEnabled(currPage != numPages - 1);
    }

    @Subscribe
    public void preferenceChanged(PreferenceChangeEvent event) {
        resultsView.preferenceChanged(event.getPreference());
    }
    
    @Subscribe
    public void domainObjectSelected(DomainObjectSelectionEvent event) {
        if (event.getSource()!=resultsView) return;
        updateStatusBar();
//        updateHud(false);
    }

    @Subscribe
    public void annotationsChanged(DomainObjectAnnotationChangeEvent event) {
                
        for(final ResultPage page : searchResults.getPages()) {
            final Long domainObjectId = event.getDomainObject().getId();
            final DomainObject pageObject = page.getDomainObject(domainObjectId);
            if (pageObject!=null) {

                SimpleWorker annotationUpdateWorker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                        page.updateAnnotations(domainObjectId, model.getAnnotations(Reference.createFor(pageObject)));
                    }

                    @Override
                    protected void hadSuccess() {
                        resultsView.refreshDomainObject(pageObject);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                annotationUpdateWorker.execute();
                break;
            }
        }
    }
    
    private void updateStatusBar() {
        int s = selectionModel.getSelectedIds().size();
        if (resultPage==null) {
            statusLabel.setText("");
        }
        else {
            statusLabel.setText(s + " of " + resultPage.getNumPageResults() + " selected");
        }
    }
    
    private synchronized void goPrevPage() {
        this.currPage -= 1;
        if (currPage < 0) {
            currPage = 0;
        }
        showCurrPage();
    }

    private synchronized void goNextPage() {
        this.currPage += 1;
        if (currPage >= numPages) {
            currPage = numPages-1;
        }
        showCurrPage();
    }

    private synchronized void goStartPage() {
        this.currPage = 0;
        showCurrPage();
    }

    private synchronized void goEndPage() {
        this.currPage = numPages - 1;
        showCurrPage();
    }

    private synchronized void selectAll() {
    	// TODO: implement 
//        for (T imageObject : allImageObjects) {
//            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), false);
//        }
        selectionButtonContainer.setVisible(false);
    }
    
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }
    
    public void showResultsView() {
        if (resultsView==null) {
            showNothing();
            return;
        }
        removeAll();
        add(resultsView.getPanel(), BorderLayout.CENTER);
        pagingStatusLabel.setText("Page " + (currPage + 1) + " of " + numPages);
        updateStatusBar();
        add(statusBar, BorderLayout.SOUTH);
        updateUI();
    }
    
    public void showNothing() {
        removeAll();
        add(splashPanel, BorderLayout.CENTER);
        updateUI();
    }
    
    public void showSearchResults(SearchResults searchResults, boolean isUserDriven) {
        this.searchResults = searchResults;
        numPages = searchResults.getNumTotalPages();
        this.currPage = 0;
        showCurrPage(isUserDriven);
    }

    protected void showCurrPage() {
        showCurrPage(true);
    }
    
    protected void showCurrPage(final boolean isUserDriven) {

        updatePagingStatus();
        showLoadingIndicator();
                
        if (searchResults==null) {
            throw new IllegalStateException("Cannot show page when there are no search results");
        }
        
        SimpleWorker worker = new SimpleWorker() {
        
            @Override
            protected void doStuff() throws Exception {
                resultPage = getPage(searchResults, currPage);
            }

            @Override
            protected void hadSuccess() {
                updateResultsView(isUserDriven);
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        worker.execute();
    }
    
    private void updateResultsView(final boolean isUserDriven) {
        if (resultPage!=null) {
            resultsView.showDomainObjects(resultPage, new Callable<Void>() {   
                @Override
                public Void call() throws Exception {
                    if (isUserDriven) {
                        //
                        // If and only if this is a user driven selection, we should automatically select the first item.
                        //
                        // This behavior is disabled for auto-generated events, in order to enable the browsing behavior where a user 
                        // walks through a list of domain objects (e.g. Samples) with the object set viewer, and each one triggers a 
                        // load in the domain object viewer, which automatically selects its first result, which in turn triggers another 
                        // load (of, say, Neuron Fragments). If we selected the first Neuron Fragment, then it would generate a selection 
                        // event that would overwrite the Sample in the Data Inspector.
                        //
                        List<DomainObject> objects = resultPage.getDomainObjects();
                        if (!objects.isEmpty()) {
                            resultsView.selectDomainObjects(Arrays.asList(objects.get(0)), true, true);
                        }
                    }
                    return null;
                }
            });
            showResultsView();
        }
        else {
            showNothing();
        }
    }
    
    protected abstract ResultPage getPage(SearchResults searchResults, int page) throws Exception;
    
}
