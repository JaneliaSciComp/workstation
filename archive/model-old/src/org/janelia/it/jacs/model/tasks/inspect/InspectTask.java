
package org.janelia.it.jacs.model.tasks.inspect;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 * From jacs.properties
 * # Inspect Command line
 InspectPrep.Cmd=proteogenomic/ClusterSub.py
 Inspect.Cmd=proteogenomic/sgeInspect.sh
 Inspect.PValueCmd=proteogenomic/PValue.py
 */
public class InspectTask extends Task {
    transient public static final String TASK_NAME = "inspect";
    transient public static final String DISPLAY_NAME = "Inspect Peptide Mapper";

    // Parameter Keys
    transient public static final String PARAM_referenceFiles = "reference files";
    transient public static final String PARAM_pathToMzXmlFiles = "mzXML file path";
    transient public static final String PARAM_archiveFilePath = "archive file path";

    public InspectTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public InspectTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_referenceFiles, "");
        setParameter(PARAM_pathToMzXmlFiles, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_pathToMzXmlFiles) || key.equals(PARAM_referenceFiles)) {
            return new TextParameterVO(value, 455);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return !super.isParameterRequired(parameterKeyName);
    }


}