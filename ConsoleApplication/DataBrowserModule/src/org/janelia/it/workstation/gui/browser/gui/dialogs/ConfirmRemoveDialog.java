package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

import net.miginfocom.swing.MigLayout;

/**
 * A dialog for confirming deletion of an ObjectSet, Filter, or TreeNode that isn't referenced elsewhere in the system.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfirmRemoveDialog extends ModalDialog {

    private final static Logger log = LoggerFactory.getLogger(ConfirmRemoveDialog.class);
    private final JPanel mainPanel;
    private JLabel confirmRemoveLabel;
    private final JButton cancelButton;
    private final JButton okButton;
    private List<Reference> deleteObjectRefs;
    Multimap<TreeNode,DomainObject> removeFromFolders;

    public ConfirmRemoveDialog() {
        setTitle("Confirm Remove");
        
        mainPanel = new JPanel(new MigLayout("wrap 2"));

        cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Don't remove this item");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        okButton = new JButton("OK");
        okButton.setToolTipText("Remove this item");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeAndRemove();
            }
        });
                
        getRootPane().setDefaultButton(okButton);
                
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
        
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                okButton.requestFocus();
            }
        });
    }

    public void showDialog() {
        confirmRemoveLabel = new JLabel("There are " + deleteObjectRefs.size() + " items in your remove list that will be deleted permanently.");
        mainPanel.add(confirmRemoveLabel);
        packAndShow();
    }

    private void closeAndRemove() {
        try {
            DomainModel model = DomainMgr.getDomainMgr().getModel();
            for (TreeNode treeNode : removeFromFolders.keySet()) {
                try {
                    for (DomainObject domainObject: removeFromFolders.get(treeNode)) {
                        model.removeChild(treeNode, domainObject);
                    }
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
            // we're removed all parent references, now send the list to be deleted from the system
            model.remove(deleteObjectRefs);
        } catch (Exception e) {
            log.error("Issue deleting domainObjects " + deleteObjectRefs,e);
        }

        setVisible(false);
    }

    public Multimap<TreeNode, DomainObject> getRemoveFromFolders() {
        return removeFromFolders;
    }

    public void setRemoveFromFolders(Multimap<TreeNode, DomainObject> removeFromFolders) {
        this.removeFromFolders = removeFromFolders;
    }

    public List<Reference> getDeleteObjectList() {
        return deleteObjectRefs;
    }

    public void setDeleteObjectList(List<Reference> deleteObjectList) {
        this.deleteObjectRefs = deleteObjectList;
    }


}
