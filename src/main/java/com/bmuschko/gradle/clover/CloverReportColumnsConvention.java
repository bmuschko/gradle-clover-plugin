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

import org.gradle.internal.metaobject.InvokeMethodResult;
import org.gradle.internal.metaobject.MethodAccess;
import org.gradle.internal.metaobject.MethodMixIn;

import groovy.lang.GroovyObjectSupport;

public class CloverReportColumnsConvention extends GroovyObjectSupport implements MethodMixIn {
    private final Collection<CloverReportColumn> columns;
    private final DynamicMethods dynamicMethods;

    public CloverReportColumnsConvention() {
        columns = new ArrayList<CloverReportColumn>();
        dynamicMethods = new DynamicMethods();
    }

    public Collection<CloverReportColumn> getColumns() {
        return Collections.unmodifiableCollection(columns);
    }
    
    private void add(String column, Map<String, String> attributes) {
        columns.add(new CloverReportColumn(column, attributes));
    }

    @Override
    public MethodAccess getAdditionalMethods() {
        return dynamicMethods;
    }
    
    private class DynamicMethods implements MethodAccess {
        @Override
        public boolean hasMethod(String name, Object... arguments) {
            return arguments.length == 1 && arguments[0] instanceof Map && validColumnName(name);
        }

        private boolean validColumnName(String name) {
            return CloverReportColumn.validColumn(name);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void invokeMethod(String name, InvokeMethodResult result, Object... arguments) {
            CloverReportColumnsConvention.this.add(name, (Map<String, String>) arguments[0]);
            result.result(null);
        }
    }
}
