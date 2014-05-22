/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board_viewer;
import org.janelia.it.workstation.gui.alignment_board_viewer.channel_split.NBitChannelSplitStrategyTest;
import org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements.GpuSamplerTest;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.ConfigurableColorMappingTest;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTrackerTest;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.RBComparatorTest;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.RDComparatorTest;
import org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder.DownSamplerTest;
import org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder.VeryLargeVolumeTest;
import org.janelia.it.workstation.gui.alignment_board_viewer.volume_export.FilteringAcceptorDecoratorTest;
import org.janelia.it.workstation.gui.framework.viewer.alignment_board.UserSettingSerializerStringTest;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 *
 * @author fosterl
 */
@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        ChannelReadTest.class,
        ConfigurableColorMappingTest.class,
        DownSamplerTest.class,
        FilteringAcceptorDecoratorTest.class,
        GpuSamplerTest.class,
        MaskReadTest.class,
        MultiMaskTrackerTest.class,
        NBitChannelSplitStrategyTest.class,
        RBComparatorTest.class,
        RDComparatorTest.class,
        UserSettingSerializerStringTest.class,
        VeryLargeVolumeTest.class,
})
public class AlignmentBoardModuleFastTestsSuite {}
