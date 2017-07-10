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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.gradle.api.InvalidUserDataException;

public class CloverReportColumn {
    public CloverReportColumn(String column, Map<String, String> attributes) {
        this.column = column;
        this.attributes = new HashMap<>(attributes.size());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (key.equals("format")) {
                assertValidFormat(column, entry.getValue());
            } else if (key.equals("min") || key.equals("max")) {
                assertValidNumber(entry.getValue());
            } else if (key.equals("scope")) {
                assertValidScope(entry.getValue());
            } else {
                throw new InvalidUserDataException("Invalid column attribute name: " + key);
            }
            this.attributes.put(key, entry.getValue());
        }
    }

    private final String column;
    public String getColumn() {
        return column;
    }
    
    private final Map<String, String> attributes;
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
    
    private void assertValidFormat(String name, String format) {
        FormatValidator validator = validColumns.get(name);
        if (!validator.valid(format)) {
            throw new InvalidUserDataException("Invalid column format specification: " + format);
        }        
    }
    
    private void assertValidNumber(String value) {
        Integer.parseInt(value, 10);
    }
    
    private static final Pattern scopeValidator = Pattern.compile("(package|class|method)");
    private void assertValidScope(String scope) {
        if (!scopeValidator.matcher(scope).matches()) {
            throw new InvalidUserDataException("Invalid column scope specification: " + scope);
        }
    }
    
    public static boolean validColumn(String column) {
        return validColumns.containsKey(column);
    }
    
    private static final Map<String, FormatValidator> validColumns;
    static {
        RawFormatValidator raw = new RawFormatValidator();
        MultiFormatValidator multi = new MultiFormatValidator();
        
        validColumns = new HashMap<String, FormatValidator>();
        validColumns.put("avgClassesPerFile", raw);
        validColumns.put("avgMethodComplexity", raw);
        validColumns.put("avgMethodsPerClass", raw);
        validColumns.put("avgStatementsPerMethod", raw);
        validColumns.put("complexity", raw);
        validColumns.put("complexityDensity", raw);
        validColumns.put("coveredBranches", multi);
        validColumns.put("coveredElements", multi);
        validColumns.put("coveredMethods", multi);
        validColumns.put("coveredStatements", multi);
        //validColumns.put("expression", raw);
        validColumns.put("filteredElements", multi);
        validColumns.put("ncLineCount", raw);
        validColumns.put("lineCount", raw);
        validColumns.put("SUM", raw);
        validColumns.put("percentageCoveredContribution", multi);
        validColumns.put("percentageUncoveredContribution", multi);
        validColumns.put("totalBranches", raw);
        validColumns.put("totalChildren", raw);
        validColumns.put("totalClasses", raw);
        validColumns.put("totalElements", raw);
        validColumns.put("totalFiles", raw);
        validColumns.put("totalMethods", raw);
        validColumns.put("totalPercentageCovered", multi);
        validColumns.put("totalStatements", raw);
        validColumns.put("uncoveredBranches", multi);
        validColumns.put("uncoveredElements", multi);
        validColumns.put("uncoveredMethods", multi);
        validColumns.put("uncoveredStatements", multi);

        validColumns.put("files", raw);
        validColumns.put("methods", raw);
    }

    private interface FormatValidator {
        boolean valid(String format);
    }
    
    private static class RawFormatValidator implements FormatValidator {
        @Override
        public boolean valid(String format) {
            return "raw".equals(format);
        }
    }
    
    private static class MultiFormatValidator implements FormatValidator {
        private static final Pattern formatValidator = Pattern.compile("(raw|bar|longbar|%)");
        @Override
        public boolean valid(String format) {
            return formatValidator.matcher(format).matches();
        }
    }
}
