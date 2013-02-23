package org.janelia.it.FlyWorkstation.gui.util;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.text_component.StandardTextArea;
import org.janelia.it.jacs.shared.utils.MailHelper;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 5/22/12
 * Time: 2:51 PM
 */
public class MailDialogueBox {

    private String subject = "";
    private String messagePrefix = "";

    public MailDialogueBox(String subject) {
        this.subject = subject;
    }

    public MailDialogueBox(String subject, String messagePrefix){
        this.subject = subject;
        this.messagePrefix = messagePrefix;
    }

    public void show(){
        String desc = null;
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Please give a quick description of the problem and any other useful information."));
        panel.add(Box.createVerticalStrut(15));
        JTextArea textArea = new StandardTextArea(4, 20);
        panel.add(new JScrollPane(textArea));
        int ans;
        while (desc == null || desc.equals("")) {
            ans = getOptionPane().showConfirmDialog(getParentFrame(), panel, "Problem Description", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans == JOptionPane.CANCEL_OPTION) return;
            desc = messagePrefix + textArea.getText();
        }
        ConsoleProperties.getString("console.HelpEmail");
        MailHelper helper = new MailHelper();
        helper.sendEmail((String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL), ConsoleProperties.getString("console.HelpEmail"),
                subject, desc);
    }

    private JOptionPane getOptionPane() {
        JFrame mainFrame = new JFrame();
        JFrame parent = getParentFrame();
        if (parent != null) mainFrame.setIconImage(getParentFrame().getIconImage());
        JOptionPane optionPane = new JOptionPane();
        mainFrame.getContentPane().add(optionPane);
        return optionPane;
    }

    private JFrame getParentFrame() {
        return SessionMgr.getSessionMgr().getActiveBrowser();
    }
}
