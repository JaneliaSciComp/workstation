/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.janelia.it.FlyWorkstation.gui.framework.keybind;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * Adapted from IDEA code base.
 */
public final class KeyboardShortcut {
    private final KeyStroke myFirstKeyStroke;

    public static KeyboardShortcut createShortcut(KeyEvent e) {
        return new KeyboardShortcut(KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers()));
    }

    /**
     * @throws IllegalArgumentException if <code>firstKeyStroke</code> is <code>null</code>
     */
    public KeyboardShortcut(KeyStroke firstKeyStroke) {
        myFirstKeyStroke = firstKeyStroke;
    }

    public KeyStroke getFirstKeyStroke() {
        return myFirstKeyStroke;
    }

    public int hashCode() {
        int hashCode = myFirstKeyStroke.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyboardShortcut that = (KeyboardShortcut) o;

        if (myFirstKeyStroke != null ? !myFirstKeyStroke.equals(that.myFirstKeyStroke) : that.myFirstKeyStroke != null)
            return false;

        return true;
    }

    public boolean isKeyboard() {
        return true;
    }

    public static KeyboardShortcut fromString(String s) {
        return new KeyboardShortcut(KeyStroke.getKeyStroke(s));
    }

    @Override
    public String toString() {
        return myFirstKeyStroke.toString();
    }
}
