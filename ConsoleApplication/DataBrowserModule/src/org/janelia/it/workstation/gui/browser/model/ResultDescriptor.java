package org.janelia.it.workstation.gui.browser.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifies a particular result within a Sample.
 * 
 * The key is parsed as follows:
 * [objective] [result name prefix] [group name] 
 * 
 * Some example keys:
 * "20x JBA Alignment"
 * "63x Sample Processing Results (Brain)"
 * 
 * One special value is "Latest" which always identifies the latest result in a sample.
 */
public class ResultDescriptor {

    public static final ResultDescriptor LATEST = new ResultDescriptor(DomainConstants.PREFERENCE_VALUE_LATEST);
    
    private final String resultKey;
    private final String objective;
    private final String resultName;
    private final String resultNamePrefix;
    private final String groupName;

    public ResultDescriptor(String resultKey) {
        this.resultKey = resultKey;
        if (!DomainConstants.PREFERENCE_VALUE_LATEST.equals(resultKey)) {
            String[] parts = resultKey.split(" ", 2);
            this.objective = parts[0];
            this.resultName = parts[1];

            Pattern p = Pattern.compile("(.*?)\\s*(\\((.*?)\\))?");
            Matcher m = p.matcher(resultName);
            if (!m.matches()) {
                throw new IllegalStateException("Result name cannot be parsed: " + parts[1]);
            }
            this.resultNamePrefix = m.matches() ? m.group(1) : null;
            this.groupName = m.matches() ? m.group(3) : null;
        } 
        else {
            this.objective = null;
            this.resultName = null;
            this.resultNamePrefix = null;
            this.groupName = null;
        }
    }

    public String getResultKey() {
        return resultKey;
    }

    public String getObjective() {
        return objective;
    }

    public String getResultName() {
        return resultName;
    }

    public String getResultNamePrefix() {
        return resultNamePrefix;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public String toString() {
        return resultKey;
    }
}