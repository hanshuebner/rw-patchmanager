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
package com.ruinwesen.patch.metadata;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import name.cs.csutils.CSUtils;

import com.ruinwesen.patch.metadata.PatchMetadataIDInfo.DeviceId;
import com.ruinwesen.patch.metadata.PatchMetadataIDInfo.EnvironmentId;


public class DefaultPatchMetadata implements PatchMetadata {

    private String patchId;
    private String author;
    private String comment;
    private String name;
    private String title;
    private EnvironmentId environmentId;
    private DeviceId deviceId;
    private String version = PatchMetadata.VERSION_1_0;
    private Tagset tags = new Tagset();
    private Date lastModifiedDate = CSUtils.now();
    private Map<String, Path> paths = new HashMap<String, Path>();
    
    public DefaultPatchMetadata() {
        super();
    }
    
    public DefaultPatchMetadata(PatchMetadata src) {
        this();
        setAll(src);
    }

    public PatchMetadata copy() {
        return new DefaultPatchMetadata(this);
    }
    

    public DeviceId getDeviceId() {
        return deviceId;
    }
    

    public EnvironmentId getEnvironmentId() {
        return environmentId;
    }
    

    public String getAuthor() {
        return author;
    }


    public String getComment() {
        return comment;
    }


    public String getName() {
        return name;
    }


    public Path getPath(String name) {
        return paths.get(name);
    }


    public Map<String, Path> getPaths() {
        return Collections.unmodifiableMap(paths);
    }


    public String getTitle() {
        return title;
    }


    public String getVersion() {
        return version;
    }


    public void removePath(String name) {
        paths.remove(name);
    }


    public void setAuthor(String value) {
        this.author = value;
    }


    public void setComment(String value) {
        this.comment = value;
    }


    public void setName(String value) {
        this.name = value;
    }


    public void setDeviceId(DeviceId value) {
        this.deviceId = value;
    }
    

    public void setEnvironmentId(EnvironmentId value) {
        this.environmentId = value;
    }

    public void setPath(String name, Path value) {
        if (value == null) {
            paths.remove(name);
        } else {
            paths.put(name, value);
        }
    }


    public void setTitle(String value) {
        this.title = value;
    }


    public void setVersion(String value) {
        // no operation!!!
    }


    public void setAll(PatchMetadata src) {
        PatchMetadataUtils.setAll(this,src);
    }


    public void setPaths(Map<String, Path> paths) {
        this.paths.clear();
        this.paths.putAll(paths);
    }


    public void addPath(Path path) {
        setPath(path.getName(), path);
    }


    public Tagset getTags() {
        return new Tagset(tags);
    }


    public void setTags(Tagset set) {
        tags = new Tagset(set);
    }


    public void addTag(Tag tag) {
        tags.add(tag);
    }


    public void addTag(String tagname) {
        tags.add(tagname);
    }


    public String getLastModifiedDateString() {
        return lastModifiedDate == null ?
                CSUtils.dateToString(CSUtils.now())
                : CSUtils.dateToString(lastModifiedDate);
    }


    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }


    public void setLastModifiedDate(Date d) {
        this.lastModifiedDate = d;
    }
    
    @Override
    public String toString() {
        return PatchMetadataUtils.toString(this);
    }


    public void setPaths(Collection<Path> paths) {
        PatchMetadataUtils.setPaths(this, paths);
    }


    public String getPatchId() {
        return patchId;
    }


    public void setPatchId(String patchId) {
        this.patchId = patchId;
    }
    
    
}
