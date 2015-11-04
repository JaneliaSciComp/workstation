/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.console.viewerapi;

import java.util.Observable;

/**
 * Exposes protected methods, so Observable can be used
 * via composition, in addition to use by inheritance.
 * Efficient implementations should implement bulk updates by automatically
 * calling setChanged() many times, and then manually calling 
 * notifyObservers() once, after
 * all the relevant changes have been registered.
 * 
 * @author cmbruns
 */
public class ComposableObservable extends Observable 
implements ObservableInterface
{
    /**
     * Potentially slow notification of all listeners. For efficiency,
     * notifyObservers() only notifies listeners IF setChanged() has been
     * called since the previous call to notifyObservers().
     */
    @Override
    public void notifyObservers() {
        super.notifyObservers();
    }

    /**
     * Exposes setChanged() publicly, so we can use Observable by composition, not just by inheritance.
     * setChanged() is a fast inexpensive operation that marks the Observable as "dirty",
     * but does NOT automatically notify listeners. 
     * It should be OK to call "setChanged()" whenever the Observable is known to have
     * changes to its internal state. 
     */
    @Override
    public void setChanged() {
        super.setChanged();
    }
}
