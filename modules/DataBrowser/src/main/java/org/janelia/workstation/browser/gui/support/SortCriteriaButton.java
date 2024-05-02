package org.janelia.workstation.browser.gui.support;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObjectAttribute;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;

import javax.swing.*;
import java.util.List;

/**
 * Drop-down button that displays DomainObjectAttributes and allows the user to select one to sort on.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SortCriteriaButton extends DropDownButton {

    private static final String ASC = "ascending";
    private static final String DESC = "descending";

    public SortCriteriaButton() {
        setText("Sort");
        setToolTipText("Set the sorting criteria for the results");
    }

    public void populate(SearchProvider searchProvider,
                         List<DomainObjectAttribute> displayAttrs) {
        removeAll();

        // TODO: Currently it's not possible to truly reset because most implementations ignore null sorting criteria
        //       We could just move preference saving into this class, since it's no longer context dependent.
        JMenuItem clearMenuItem = new JMenuItem("Reset sort criteria");
        clearMenuItem.addActionListener(e -> {
            searchProvider.setSortField("+id");
            setText("Sort by: GUID ("+ASC+")");
            searchProvider.search();
        });
        addMenuItem(clearMenuItem);

        JMenuItem reverseMenuItem = new JMenuItem("Reverse sorting direction");
        reverseMenuItem.addActionListener(e -> {
            String sortCriteria = searchProvider.getSortField();
            if (StringUtils.isNotBlank(sortCriteria)) {
                if (sortCriteria.startsWith("-")) {
                    searchProvider.setSortField(sortCriteria.replace('-', '+'));
                    setText(getText().replace(DESC, ASC));
                }
                else if (sortCriteria.startsWith("+")) {
                    searchProvider.setSortField(sortCriteria.replace('+', '-'));
                    setText(getText().replace(ASC, DESC));
                }
                else {
                    searchProvider.setSortField("-"+sortCriteria);
                    setText(getText().replace(ASC, DESC));
                }
                searchProvider.search();
            }
        });
        addMenuItem(reverseMenuItem);

        JMenuItem blankMenuItem = new JMenuItem("");
        blankMenuItem.setEnabled(false);
        addMenuItem(blankMenuItem);

        ButtonGroup group = new ButtonGroup();

        for (DomainObjectAttribute displayAttr : displayAttrs) {

            // Only add attrs that can be sorted by
            if (StringUtils.isBlank(displayAttr.getSearchKey())) continue;

            boolean selected = displayAttr.getName().equals(searchProvider.getSortFieldName());
            if (selected) {
                String sortField = searchProvider.getSortField();
                if (sortField != null) {
                    String dir = searchProvider.getSortField().startsWith("-") ? DESC : ASC;
                    setText("Sort by: " + displayAttr.getLabel() + " (" + dir + ")");
                }
                else {
                    setText("Sort by: " + displayAttr.getLabel());
                }
            }
            JMenuItem menuItem = new JRadioButtonMenuItem(displayAttr.getLabel(), selected);
            menuItem.addActionListener(e -> {
                menuItem.setSelected(true);
                searchProvider.setSortField("+"+displayAttr.getName());
                setText("Sort by: "+displayAttr.getLabel()+" ("+ASC+")");
                searchProvider.search();
                group.add(menuItem);
            });
            addMenuItem(menuItem);
        }
    }
}
