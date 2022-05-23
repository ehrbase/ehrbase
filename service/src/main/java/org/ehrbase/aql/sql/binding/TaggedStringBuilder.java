/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql.binding;

public class TaggedStringBuilder implements I_TaggedStringBuilder {

    private StringBuilder stringBuffer;
    private TagField tagField;

    public TaggedStringBuilder(TagField tagField) {
        this.stringBuffer = new StringBuilder();
        this.tagField = tagField;
    }

    public TaggedStringBuilder() {
        this.stringBuffer = new StringBuilder();
    }

    public TaggedStringBuilder(String string, TagField tagField) {
        this.stringBuffer = new StringBuilder();
        stringBuffer.append(string);
        this.tagField = tagField;
    }

    @Override
    public StringBuilder append(String string) {
        return stringBuffer.append(string);
    }

    @Override
    public void replaceLast(String previous, String newString) {
        int lastPos = stringBuffer.lastIndexOf(previous);
        if (lastPos >= 0) {
            stringBuffer.delete(lastPos, lastPos + previous.length());
            stringBuffer.insert(lastPos, newString);
        }
    }

    @Override
    public int lastIndexOf(String string) {
        return stringBuffer.lastIndexOf(string);
    }

    @Override
    public int indexOf(String string) {
        return stringBuffer.indexOf(string);
    }

    @Override
    public void replace(String previous, String newString) {
        int indexOf = stringBuffer.indexOf(previous);
        if (indexOf >= 0) {
            stringBuffer.delete(indexOf, indexOf + previous.length());
            stringBuffer.insert(indexOf, newString);
        }
    }

    @Override
    public void replace(Integer start, Integer end, String replacement) {
        stringBuffer.replace(start, end, replacement);
    }

    @Override
    public void insert(Integer offset, String toInsert) {
        stringBuffer.insert(offset, toInsert);
    }

    @Override
    public String toString() {
        return stringBuffer.toString();
    }

    @Override
    public int length() {
        return stringBuffer.length();
    }

    @Override
    public TagField getTagField() {
        return tagField;
    }

    @Override
    public void setTagField(TagField tagField) {
        this.tagField = tagField;
    }

    @Override
    public boolean startWith(String tag) {
        return stringBuffer.indexOf(tag) == 1; // starts with a quote!
    }
}
