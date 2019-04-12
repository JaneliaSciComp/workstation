@OptionsPanelController.ContainerRegistration(
        id = "Core",
        categoryName = "#OptionsCategory_Name_Core",
        iconBase = "images/workstation_32_icon.png",
        keywords = "#OptionsCategory_Keywords_Core",
        keywordsCategory = "Core",
        position = 1)
@NbBundle.Messages(value = {"OptionsCategory_Name_Core=Core", "OptionsCategory_Keywords_Core=workstation core"})
package org.janelia.workstation.common.gui.options;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;
