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

import org.janelia.it.FlyWorkstation.gui.util.SystemInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;

/**
 * Adapted from IDEA code base.
 */
public class KeymapUtil {

    private static final String APPLE_LAF_AQUA_LOOK_AND_FEEL_CLASS_NAME = "apple.laf.AquaLookAndFeel";
    private static final String GET_KEY_MODIFIERS_TEXT_METHOD = "getKeyModifiersText";
    private static final String CANCEL_KEY_TEXT = "Cancel";
    private static final String BREAK_KEY_TEXT = "Break";
    private static final String SHIFT = "shift";
    private static final String CONTROL = "control";
    private static final String CTRL = "ctrl";
    private static final String META = "meta";
    private static final String ALT = "alt";
    private static final String ALT_GRAPH = "altGraph";

    private KeymapUtil() {
    }

    public static String getTextByKeyStroke(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return "";
        }
        return getKeystrokeText(keyStroke);
    }

    public static String getShortcutText(KeyboardShortcut keyboardShortcut) {
        String acceleratorText = getKeystrokeText(keyboardShortcut.getFirstKeyStroke());
        if (acceleratorText.length() > 0) {
            return acceleratorText;
        }
        return "";
    }

    public static boolean isModifier(KeyEvent e) {
        int keyCode = e.getKeyCode();
        return (keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_ALT_GRAPH || keyCode == KeyEvent.VK_META);
    }

    public static String getKeystrokeText(KeyStroke accelerator) {
        if (accelerator == null) return "";
        String acceleratorText = "";
        int modifiers = accelerator.getModifiers();
        if (modifiers > 0) {
            acceleratorText = getModifiersText(modifiers);
        }

        String keyText = KeyEvent.getKeyText(accelerator.getKeyCode());
        if (CANCEL_KEY_TEXT.equals(keyText)) {
            keyText = BREAK_KEY_TEXT;
        }

        acceleratorText += keyText;
        return acceleratorText.trim();
    }

    private static String getModifiersText(int modifiers) {
        if (SystemInfo.isMac) {
            try {
                Class appleLaf = Class.forName(APPLE_LAF_AQUA_LOOK_AND_FEEL_CLASS_NAME);
                Method getModifiers = appleLaf.getMethod(GET_KEY_MODIFIERS_TEXT_METHOD, int.class, boolean.class);
                return (String) getModifiers.invoke(appleLaf, modifiers, Boolean.FALSE);
            }
            catch (Exception e) {
                if (SystemInfo.isMacOSLeopard) {
                    return getKeyModifiersTextForMacOSLeopard(modifiers);
                }

                // OK do nothing here.
            }
        }

        final String keyModifiersText = KeyEvent.getKeyModifiersText(modifiers);
        if (keyModifiersText.length() > 0) {
            return keyModifiersText + "+";
        }
        else {
            return keyModifiersText;
        }
    }

    private static String getKeyModifiersTextForMacOSLeopard(int modifiers) {
        StringBuilder buf = new StringBuilder();
        if ((modifiers & InputEvent.META_MASK) != 0) {
            buf.append(Toolkit.getProperty("AWT.meta", "Meta"));
        }
        if ((modifiers & InputEvent.CTRL_MASK) != 0) {
            buf.append(Toolkit.getProperty("AWT.control", "Ctrl"));
        }
        if ((modifiers & InputEvent.ALT_MASK) != 0) {
            buf.append(Toolkit.getProperty("AWT.alt", "Alt"));
        }
        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            buf.append(Toolkit.getProperty("AWT.shift", "Shift"));
        }
        if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            buf.append(Toolkit.getProperty("AWT.altGraph", "Alt Graph"));
        }
        if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
            buf.append(Toolkit.getProperty("AWT.button1", "Button1"));
        }
        return buf.toString();
    }
}
