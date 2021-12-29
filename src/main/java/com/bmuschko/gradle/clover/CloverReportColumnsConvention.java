/*
 * Copyright 2017 the original author or authors.
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
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.InvalidUserDataException;

public class CloverReportColumnsConvention extends GroovyObjectSupport {
    private final Collection<CloverReportColumn> columns;

    public CloverReportColumnsConvention() {
        columns = new ArrayList<>();
    }

    public Collection<CloverReportColumn> getColumns() {
        return Collections.unmodifiableCollection(columns);
    }

    public Collection<String> getJsonColumns() throws JsonProcessingException {
        Collection<String> jsonColumns = new ArrayList<>();
        for (CloverReportColumn column : columns) {
            jsonColumns.add(column.toJson());
        }
        return jsonColumns;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invokeMethod(String name, Object args) {
        if (args instanceof Object[]) {
            Object[] arguments = (Object[]) args;
            if (arguments.length == 1 && arguments[0] instanceof Map) {
                if (CloverReportColumn.validColumn(name)) {
                    columns.add(new CloverReportColumn(name, (Map<String, String>) arguments[0]));
                    return null;
                }
                throw new InvalidUserDataException("Unsupported column name '" + name + "' for Clover report");
            }
        }
        return super.invokeMethod(name, args);
    }
}
