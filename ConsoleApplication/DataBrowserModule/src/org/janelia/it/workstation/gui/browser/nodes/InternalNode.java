package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;

import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InternalNode<T> extends AbstractNode {
        
    private final static Logger log = LoggerFactory.getLogger(InternalNode.class);
    
    private final ChildFactory parentChildFactory;
    private final InstanceContent lookupContents;
    
    public InternalNode(ChildFactory parentChildFactory, Children children, T object) {
        this(new InstanceContent(), parentChildFactory, children, object);
    }

    public InternalNode(InstanceContent lookupContents, ChildFactory parentChildFactory, Children children, T object) {
        super(children, new AbstractLookup(lookupContents));
        this.parentChildFactory = parentChildFactory;
        this.lookupContents = lookupContents;
        lookupContents.add(object);
    }
    
    protected InstanceContent getLookupContents() {
        return lookupContents;
    }
    
    public T getObject() {
        return (T)getLookup().lookup(Object.class);
    }
    
    public String getPrimaryLabel() {
        return getObject().toString();
    }
    
    public String getSecondaryLabel() {
        return null;
    }
    
    public String getExtraLabel() {
        return null;
    }
    
    @Override
    public boolean canCut() {
        return false;
    }

    @Override
    public boolean canCopy() {
        return true;
    }

    @Override
    public boolean canRename() {
        return false;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(new CopyNameAction());
        return actions.toArray(new Action[actions.size()]);
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("package.png").getImage();
    }
    
    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }
    
    @Override
    public String getDisplayName() {
        return getPrimaryLabel();
    }
    
    @Override
    public String getHtmlDisplayName() {
        String primary = getPrimaryLabel();
        String secondary = getSecondaryLabel();
        String extra = getExtraLabel();
        StringBuilder sb = new StringBuilder();
        if (primary!=null) {
            sb.append("<font color='!Label.foreground'>");
            sb.append(primary);
            sb.append("</font>");
        }
        if (secondary!=null) {
            sb.append(" <font color='#957D47'><i>");
            sb.append(secondary);
            sb.append("</i></font>");
        }
        if (extra!=null) {
            sb.append(" <font color='#959595'>");
            sb.append(extra);
            sb.append("</font>");
        }
        return sb.toString();
    }
    
    @Override
    public Transferable clipboardCopy() throws IOException {
        log.info("clipboard COPY "+getObject());
        Transferable deflt = super.clipboardCopy();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        return added;
    }
    
    protected final class CopyNameAction extends AbstractAction {

        public CopyNameAction() {
            putValue (NAME, "Copy Name To Clipboard");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Transferable t = new StringSelection(getDisplayName());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
        }
    }
}
