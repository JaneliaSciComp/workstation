package com.revivius.nb.darcula.ui;

import com.bulenkov.darcula.DarculaTableHeaderBorder;
import java.awt.Component;
import java.awt.Insets;

/**
 * Increases table header insets.
 * @author Revivius
 */
public class InreasedInsetsTableHeaderBorder extends DarculaTableHeaderBorder {

    @Override
    public Insets getBorderInsets(Component c) {
        // KR: increased even more
        return new Insets(2, 3, 2, 3);
    }
}
