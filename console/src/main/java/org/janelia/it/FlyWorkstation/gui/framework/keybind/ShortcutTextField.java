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
public class ShortcutTextField extends JTextField {
    private KeyStroke myKeyStroke;

    public ShortcutTextField() {
        enableEvents(KeyEvent.KEY_EVENT_MASK);
        setFocusTraversalKeysEnabled(false);
    }

    protected void processKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (KeymapUtil.isModifier(e)) return;
            if (e.getKeyCode()==KeyEvent.VK_BACK_SPACE) {
            	setKeyStroke(null);
            }
            else {
            	setKeyStroke(KeyboardShortcut.createShortcut(e).getFirstKeyStroke());	
            }
        }
    }

    public void setKeyStroke(KeyStroke keyStroke) {
        myKeyStroke = keyStroke;
        setText(keyStroke==null?"":KeymapUtil.getTextByKeyStroke(keyStroke));
        updateCurrentKeyStrokeInfo();
    }

    protected void updateCurrentKeyStrokeInfo() {
    }

    public KeyStroke getKeyStroke() {
        return myKeyStroke;
    }
}