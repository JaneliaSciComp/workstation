package org.janelia.it.FlyWorkstation.gui.util;

import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.text_component.StandardTextArea;
import org.janelia.it.jacs.shared.utils.MailHelper;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 5/22/12
 * Time: 2:51 PM
 */
public class MailDialogueBox {

    private String fromEmail;
    private String subject = "";
    private String messagePrefix = "";
    private String messageSuffix = "";
    private JFrame parentFrame;

    public MailDialogueBox(JFrame parentFrame, String fromEmail, String subject) {
        this.fromEmail = fromEmail;
        this.subject = subject;
        this.parentFrame = parentFrame;
    }

    public MailDialogueBox(JFrame parentFrame, String fromEmail, String subject, String messagePrefix){
        this.fromEmail = fromEmail;
        this.subject = subject;
        this.parentFrame = parentFrame;
        this.messagePrefix = messagePrefix;
    }

    public void showPopupThenSendEmail(){
        String desc = null;
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Please give a quick description of the problem and any other useful information."));
        panel.add(Box.createVerticalStrut(15));
        JTextArea textArea = new StandardTextArea(4, 20);
        panel.add(new JScrollPane(textArea));
        int ans;
        while (desc == null || desc.equals("")) {
            ans = getOptionPane().showConfirmDialog(parentFrame, panel, "Problem Description", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans == JOptionPane.CANCEL_OPTION) return;
            desc = messagePrefix + textArea.getText() +"\n";
        }
        desc+=messageSuffix;
        ConsoleProperties.getString("console.HelpEmail");
        MailHelper helper = new MailHelper();
        helper.sendEmail(fromEmail, ConsoleProperties.getString("console.HelpEmail"),
                subject, desc);
    }

    private JOptionPane getOptionPane() {
        JFrame mainFrame = new JFrame();
        if (parentFrame != null) mainFrame.setIconImage(parentFrame.getIconImage());
        JOptionPane optionPane = new JOptionPane();
        mainFrame.getContentPane().add(optionPane);
        return optionPane;
    }

    /**
     * This method gives the caller a way to place text at the end of the message.
     * @param messageSuffixInformation Text to display after everything else.
     */
    public void addMessageSuffix(String messageSuffixInformation) {
        this.messageSuffix = messageSuffixInformation;
    }
}
