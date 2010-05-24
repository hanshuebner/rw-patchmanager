/**
 * Copyright (c) 2009, Christian Schneider
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the names of the authors nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.ruinwesen.patch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.ruinwesen.patch.directory.Directory;
import com.ruinwesen.patch.directory.Entry;
import com.ruinwesen.patch.metadata.PatchMetadata;
import com.ruinwesen.patch.metadata.XMLPatchMetadata;

public abstract class AbstractPatch implements Patch {
    
    public PatchMetadata getMetadata() throws PatchDataException {
        Directory dir = openDirectory();
        PatchMetadata result = null; 
        try {
            try {
                Entry entry = dir.getEntry(PatchMetadata.FILENAME);
                if (entry == null || !entry.isFile()) {
                    throw new FileNotFoundException(PatchMetadata.FILENAME);
                }
                InputStream is = dir.getInputStream(entry);
                try {
                    result = new XMLPatchMetadata(is);
                } finally {
                    is.close();
                }
            } finally {
                dir.close();
            }
        } catch (IOException ex) {
            throw new PatchDataException(ex);
        }
        return result;
    }
    
    public abstract Directory openDirectory() throws PatchDataException;
    
}
