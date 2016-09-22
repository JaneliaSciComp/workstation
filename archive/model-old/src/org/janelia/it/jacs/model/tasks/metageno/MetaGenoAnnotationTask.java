
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 12:30:38 PM
 * From jacs.properties
 * # Mg Annotation
 MgAnnotation.Server=false
 MgAnnotation.Queue=-l default
 MgAnnotation.Hmmpfam2HtabCmd=camera_htab.pl
 MgAnnotation.HmmpfamHtabValidationCmd=compare_ldhmmpfam_raw_vs_htab.pl
 MgAnnotationHmmpfamHtabValidationThreshold=100.0
 MgAnnotation.ChangeHtabParsedFullToFragCmd=change_htab_parsed_full_to_frag.pl
 #MgAnnotation.LdhmmpfamCmd=/usr/local/common/hmmpfam_cell.sh
 MgAnnotation.LdhmmpfamCmd=/home/ccbuild/bin/hmmpfam_cell.sh
 MgAnnotation.LdhmmpfamRetryTimeoutMinutes=180
 MgAnnotation.Hmmpfam2BsmlCmd=hmmpfam2bsml.pl
 MgAnnotation.LdhmmpfamThreads=1
 # /usr/local/db/HMM_LIB/ALL_LIB.HMM.clc NodeId=1346555862474295989
 #MgAnnotation.LdhmmpfamFullDb=/usr/local/db/HMM_LIB/ALL_LIB.HMM.clc
 MgAnnotation.HmmpfamFullDbId=1346555862474295989
 # /usr/local/db/HMM_LIB/ALL_LIB.FRAG.clc NodeId=1346556562486856373
 #MgAnnotation.LdhmmpfamFragDb=/usr/local/db/HMM_LIB/ALL_LIB.FRAG.clc
 MgAnnotation.HmmpfamFragDbId=1346556562486856373
 MgAnnotation.PandaBlastp.BlastOptions=-e 1e-5 -F T -b 10 -v 10
 # The next line is the BlastableDatabaseNodeId for the file /usr/local/annotation/METAGENOMIC/DB/panda/2009-05/AllGroup.niaa
 #MgAnnotation.PandaBlastpDbId=1349541912494738101
 # The next line is the BlastableDatabaseNodeId for the file /usr/local/annotation/METAGENOMIC/DB/panda/AllGroup/AllGroup.niaa
 # FastaFileNode=1351340348516862645
 MgAnnotation.PandaBlastpDbId=1351610549003094709
 #MgAnnotation.PandaBlastp.Db=/usr/local/db/calit_db/panda/AllGroup/AllGroup.niaa
 MgAnnotation.BlastCmd=blast-2.2.15/bin/blastall
 MgAnnotation.BlastRetryTimeoutMinutes=360
 MgAnnotation.Blast2BtabCmd=wu-blast2btab.pl
 MgAnnotation.Parser=camera_parse_annotation_results_to_text_table.pl
 MgAnnotation.RpsBlastCmd=blast-2.2.15/bin/rpsblast
 MgAnnotation.Lipoprotein.IsMycoplasma=0
 MgAnnotation.Lipoprotein.Cmd=lipoprotein_motif.pl
 MgAnnotation.TmHmmCmd=/home/ccbuild/bin/tmhmm
 MgAnnotation.TmHmmBsmlCmd=tmhmm2bsml.pl
 MgAnnotation.RulesAnnotateCmd=camera_annotate_from_sorted_table.pl
 */
public class MetaGenoAnnotationTask extends Task {

    transient public static final String PARAM_input_node_id = "Query node id";


    public MetaGenoAnnotationTask() {
        super();
        setTaskName("MetaGenoAnnotationTask");
        setParameter(PARAM_input_node_id, "");
    }

    public MetaGenoAnnotationTask(Long queryNodeId) {
        setParameter(PARAM_input_node_id, queryNodeId.toString());
    }

    public String getDisplayName() {
        return "Metagenomic Annotation Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // no match
        return null;
    }

}