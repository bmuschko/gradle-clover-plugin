/*
 * Copyright 2011, 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmuschko.gradle.clover;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Supported Clover report types.
 *
 * @author Benjamin Muschko
 */
public enum ReportType {
    XML("xml"), JSON("json"), HTML("html"), PDF("pdf");

    private final String format;

    public String getFormat() {
        return format;
    }
    
    private ReportType(String format) {
        this.format = format;
    }

    public static Collection<String> getAllFormats() {
        ArrayList<String> formats = new ArrayList<String>(4);
        for (ReportType value : values()) {
            formats.add(value.format);
        }
        return formats;
    }
}