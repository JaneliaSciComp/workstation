package org.janelia.it.FlyWorkstation.gui.framework.navigation_tools;
/**
 * Title:        AutoNavigator
 * Description:  Handler for positioning client on search results.
 * @author       Peter Davies and Todd Safford
 * @version $Id: AutoNavigator.java,v 1.2 2011/03/08 16:16:49 saffordt Exp $
 */

import org.janelia.it.FlyWorkstation.api.entity_model.access.LoadRequestStatusObserverAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestState;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestStatus;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.*;
import java.awt.*;

/**
 * Autonavigator handles placing the entire client into the state dictated
 * by the navigation path selected by the user.  Said path will be the
 * result of a user search.  Said search will be one of a number of different
 * kinds of search.
 */
public class AutoNavigator {
    private Browser browser;
    private static String BAD_NAVIGATION_RANGE = "Range given for navigation not in bounds of the Genomic Axis";
    private static AutoNavigator lastUserAutoNavigationChoice;

    public AutoNavigator(Browser browser) {
        lastUserAutoNavigationChoice = this;
        this.browser = browser;
    }

    public void autoNavigate(NavigationPath path, boolean showCompleteMsg) {
        Thread autoNavThread = new AutoNavThread(path, showCompleteMsg);
        autoNavThread.start();
    }

    public String toString() {
        return "Auto Navigator for " + browser.toString();
    }

    class AutoNavThread extends Thread {
        NavigationNode[] navPath;
        boolean showCompleteMsg;
//        GenomeVersion genomeVersion;

        public AutoNavThread(NavigationPath path, boolean showCompleteMsg) {
          this.showCompleteMsg = showCompleteMsg;
          navPath=path.getNavigationNodeArray();
//          Collection genomeVersions = ModelMgr.getModelMgr().getAvailableGenomeVersions();
//
//          for (Iterator it = genomeVersions.iterator();it.hasNext();) {
//            GenomeVersion tmpGV = (GenomeVersion)it.next();
//            if (navPath[0].getNodeType() == navPath[0].GENOME) {
//              if (tmpGV.getOid().equals(navPath[0].getOID())) {
//                genomeVersion = tmpGV;
//                break;
//              } // Right OID
//            } // Right type
//          } // For all found versions.
//
        } // End constructor

        public void run() {
            autoNavigate(navPath);
        }

        /** Return selected genome version or null. */
        private Entity findEntityAssociatedWithRootNode(NavigationNode[] navPath) {

//            Set genomeVersions = ModelMgr.getModelMgr().getAvailableGenomeVersions();
            Entity genomicEntity = null;
//            GenomeVersion nextIteration = null;
//            for (Iterator it = genomeVersions.iterator(); it.hasNext(); ) {
//                nextIteration = (GenomeVersion)it.next();
//                if (nextIteration.getOid().equals(navPath[0].getOID())) {
//                    genomicEntity = nextIteration;
//                    break;
//                } // This node actually IS a genome version.
//                else if (null != (genomicEntity = nextIteration.getLoadedGenomicEntityForOid(navPath[0].getOID())))
//                    break;
//            } // For all genome versions user has selected.
//
            return genomicEntity;
        } // End method


        /*
         * This method is not thread safe because it is possible to enter a block
         * assuming the browserModel to be a GenomicAxis, and upon execution of that block,
         * the current Selection might have changed to an improper entity type.
         */
        private synchronized void autoNavigate(NavigationNode[] navPath) {
            if (lastUserAutoNavigationChoice!=AutoNavigator.this) return;
            final Entity entity = findEntityAssociatedWithRootNode(navPath);

//            if (entity != null)  {
//                if(!(entity instanceof GenomeVersion)) {
//                  try {
//                    EventQueue.invokeAndWait(new Runnable() {
//                        public void run() {
//                            browser.getBrowserModel().setCurrentSelection(entity);
//                        }});
//                  }
//                  catch (Exception ex) {
//                    SessionMgr.getSessionMgr().handleException(ex);
//                  }
//                }
//                if (entity instanceof GenomeVersion) {
//                    ModelMgr.getModelMgr().addSelectedGenomeVersion((GenomeVersion)entity);
//                }
//                else if ((entity instanceof Species) && navPath.length > 1) {
//                    LoadRequest request = ((Species)entity).getChromosomeLoadRequest();
//                    waitForLoading (((Axis)entity).loadAlignmentsToEntitiesBackground(request));
//                } // Species is next in path
//                else if ((entity instanceof Chromosome) && navPath.length>1) {
//                    LoadRequest request = ((Chromosome)entity).getGenomicAxisLoadRequest();
//                    waitForLoading (((Axis)entity).loadAlignmentsToEntitiesBackground(request));
//                } // Chromosome is next in path
//                else if ((entity instanceof GenomicAxis) && navPath.length > 1) {
//                  try {
//                    EventQueue.invokeAndWait(new Runnable() {
//                        public void run() {
//                            browser.getBrowserModel().setCurrentSelection(entity);
//                            browser.drillDownToEntityUsingDefaultEditor(entity);
//                            browser.setView(browser.isOutlineCollapsed());
//                        }});
//                    }
//                    catch (Exception ex) {
//                      SessionMgr.getSessionMgr().handleException(ex);
//                    }
//                    GenomicAxis axis = (GenomicAxis)entity;
//                    if ( genomeVersion.getLoadedGenomicEntityForOid(navPath[1].getOID()) == null) {
//                        LoadRequest request = buildLoadRequest(navPath[1], axis);
//
//                        if ((request != null) && reasonableRequestForAxis(request, axis))
//                            waitForLoading (axis.loadAlignmentsToEntitiesBackground(request));
//
//                        if (genomeVersion.getLoadedGenomicEntityForOid(navPath[1].getOID()) == null) {
//                            if (navPath[1].getNodeType() == NavigationNode.NON_CURATED) {
//                                // Re-try whole process with low-priority.
//                                LoadFilter filter = axis.getLowPriPreComputeLoadFilter();
//                                request = new LoadRequest(navPath[1].getRangeOnParent(), filter);
//                                if (reasonableRequestForAxis(request, axis))
//                                    waitForLoading (axis.loadAlignmentsToEntitiesBackground(request));
//                             } // non-curation node
//                        } // STILL not loaded after first attempt.
//
//                    } // Need to load this entity.
//
//                } // Genomic axis is next in path
//            }
//            else {
//              //  If the node is not found, abort all navigation and alert user.
//              System.out.println("Error did not find node");
//                EventQueue.invokeLater(new Runnable() {
//                public void run() {
//                    JOptionPane.showMessageDialog(browser, "The target feature could not be reached.",
//                        "Navigation Failed", JOptionPane.WARNING_MESSAGE);
//                 }});
//              return;
//            }
            if (navPath.length == 1) {
                if (showCompleteMsg) {
                   try {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog(browser, "Navigation Complete!", "Information", JOptionPane.INFORMATION_MESSAGE);
                        }});
                    }
                    catch (Exception ex) {
                      SessionMgr.getSessionMgr().handleException(ex);
                    }
                }
                browser.getBrowserModel().setModelProperty("NavigationComplete", entity);
                return; //terminate recursion
            }

            NavigationNode[] newNavPath = new NavigationNode[navPath.length - 1];
            System.arraycopy(navPath, 1, newNavPath, 0, navPath.length - 1);
            autoNavigate(newNavPath);
        } // End method

        /**
         * Create a load request based on the navigation node given.
         * Load request created is to be used for loading entities
         * against a genomic axis.
         */
//        private LoadRequest buildLoadRequest(NavigationNode node, GenomicAxis axis) {
//            LoadFilter filter = null;
//            if (node.getNodeType() == node.CURATED) {
//                filter = axis.getCurationLoadFilter();
//            } // Curation
//            else if (node.getNodeType() == node.PRECOMPUTE_HIGH_PRI) {
//                filter = axis.getHighPriPreComputeLoadFilter();
//            } // High-pri
//            else if (node.getNodeType() == node.PRECOMPUTE_LOW_PRI) {
//                filter = axis.getLowPriPreComputeLoadFilter();
//            } // Low-pri
//            else if (node.getNodeType() == node.NON_CURATED) {
//                filter = axis.getHighPriPreComputeLoadFilter();
//            } // Non-curated
//            else if (node.getNodeType() == node.CONTIG) {
//                filter = axis.getContigLoadFilter();
//            } // Contig
//            else {
//                return null; // Null for now.
//            } // Unknown
//
//            LoadRequest request = new LoadRequest(node.getRangeOnParent(), filter);
//            return request;
//        } // End method
//
        private void waitForLoading(LoadRequestStatus ls) {
            if (ls.getLoadRequestState() != LoadRequestStatus.COMPLETE &&
                ls.getLoadRequestState() != LoadRequestStatus.INACTIVE) {
                ls.addLoadRequestStatusObserver(new LoadObserver(this),true);
                synchronized(this) {
                    try {
                        wait();
                    }
                    catch(Exception ex) {
                    //  System.out.println(ex.toString());
                    } //expect inturrupted exception
                }
            }
        }

//        /** Is this request sensible to run against this axis? */
//        private boolean reasonableRequestForAxis(LoadRequest request, Axis axis) {
//            int maxOfAllRanges = Integer.MIN_VALUE;
//            Range nextRange = null;
//            for (Iterator it = request.getRequestedRanges().iterator(); it.hasNext(); ) {
//                nextRange = (Range)it.next();
//                maxOfAllRanges = (nextRange.getMaximum() > maxOfAllRanges) ? nextRange.getMaximum() : maxOfAllRanges;
//            } //
//            if (axis.getMagnitude() < maxOfAllRanges) {
//                SessionMgr.getSessionMgr().handleException(new IllegalArgumentException(BAD_NAVIGATION_RANGE));
//                return false;
//            } // Not reasonable.
//            else
//                return true;
//        }
    } // End inner class

    class LoadObserver extends LoadRequestStatusObserverAdapter {
        Thread waitingThread;

        public LoadObserver(Thread waitingThread) {
            this.waitingThread = waitingThread;
        }

        public void stateChanged(LoadRequestStatus loadRequestStatus, LoadRequestState newState){
         //   System.out.println("Received "+newState+" status Sending notify for "+loadRequestStatus);
            if (newState == LoadRequestStatus.COMPLETE) {
                loadRequestStatus.removeLoadRequestStatusObserver(this);
                waitingThread.interrupt();
            }
        }
    }
}
