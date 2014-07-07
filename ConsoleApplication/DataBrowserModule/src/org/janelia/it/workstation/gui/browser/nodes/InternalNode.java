package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.actions.CopyAction;
import org.openide.nodes.BeanNode;
import org.openide.util.datatransfer.ExTransferable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InternalNode<T> extends BeanNode<T> {

    private final static Logger log = LoggerFactory.getLogger(InternalNode.class);
    
    public InternalNode(T object) throws Exception {
        super(object);
    }
    
    public String getPrimaryLabel() {
        return getBean().toString();
    }
    
    public String getSecondaryLabel() {
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
        List<Action> actions = new ArrayList<Action>();
        actions.add(CopyAction.get(CopyAction.class));
        return actions.toArray(new Action[0]);
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("package.png").getImage();
    }
    
    @Override
    public String getHtmlDisplayName() {
        String primary = getPrimaryLabel();
        String secondary = getSecondaryLabel();
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
        return sb.toString();
    }
    
    @Override
    public Transferable clipboardCopy() throws IOException {
        log.info("clipboard COPY "+getBean());
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
    
}
