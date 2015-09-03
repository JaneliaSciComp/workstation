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

package org.janelia.horta;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * PickIdManager assigns temporarily unique positive integer IDs 
 * for display objects, from a potentially small range of possible IDs.
 * @author Christopher Bruns
 */
public class PickIdManager
{
    private final int maxId;
    private final Map<Object, Integer> idForItem = new HashMap<>();
    private final Map<Integer, Object> itemForId = new HashMap<>();
    private final Queue<Integer> recycledIds;

    private int maxUsedId = 0;
    
    public PickIdManager(int maxId) {
        this.maxId = maxId;
        recycledIds  = new PriorityQueue<>(255);
    }
    
    public int idForItem(Object item) {
        if (! idForItem.containsKey(item)) {
            int newId = maxUsedId + 1;
            if (! recycledIds.isEmpty()) {
                newId = recycledIds.remove();
            }
            if (newId > maxUsedId)
                maxUsedId = newId;
            idForItem.put(item, newId);
            itemForId.put(newId, item);
        }
        return idForItem.get(item);
    }
    
    public Object itemForId(int id) {
        return itemForId.get(id);
    }
    
    public void remove(Object item) {
        if (! idForItem.containsKey(item))
            return;
        int id = idForItem.get(item);
        idForItem.remove(item);
        itemForId.remove(id);
        recycledIds.add(id);
    }
}
