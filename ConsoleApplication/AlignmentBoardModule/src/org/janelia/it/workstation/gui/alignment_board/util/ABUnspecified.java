package org.janelia.it.workstation.gui.alignment_board.util;

import java.awt.Color;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;

public class ABUnspecified extends ABItem {
    private static int _numberCounter = 1;
    private DomainObject wrappedObject;
    private int number;

    public ABUnspecified(DomainObject domainObject) {
        super(domainObject);
        this.number = _numberCounter++;
        this.wrappedObject = domainObject;
    }

    public Long getId() {
        return (long)number;
    }

    public String getName() {
        return "Unspecified";
    }

    public Integer getNumber() {
        return number;
    }

    public String getDefaultColor() {
        return RenderUtils.getRGBStrFromColor(Color.white);
    }

    @Override
    public String getMaskPath() {
        return null;
    }

    @Override
    public String getChanPath() {
        return null;
    }

    @Override
    public String getType() {
        return UNSPECIFIED_NAME_TYPE;
    }
    public static final String UNSPECIFIED_NAME_TYPE = "Unspecified";
}
