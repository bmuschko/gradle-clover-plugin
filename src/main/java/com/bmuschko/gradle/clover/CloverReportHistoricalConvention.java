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
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;

/**
 * Defines the Clover Historical convention properties
 *
 * @author Alex Volanis
 */
public class CloverReportHistoricalConvention {
    private boolean enabled = false;
    private String historyIncludes = "clover-*.xml.gz";
    private String packageFilter = null;
    private String from = null;
    private String to = null;

    private HistoricalAdded added = null;
    private final List<HistoricalMover> movers = new ArrayList<>();

    private final ObjectFactory objectFactory;

    @Inject
    public CloverReportHistoricalConvention(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    public String getJsonAdded() throws JsonProcessingException {
        if (added == null) {
            return null;
        }
        return added.toJson();
    }

    public Collection<String> getJsonMovers() throws JsonProcessingException {
        Collection<String> jsonMovers = new ArrayList<>();
        for (HistoricalMover mover : movers) {
            jsonMovers.add(mover.toJson());
        }
        return jsonMovers;
    }

    public void added(Action<HistoricalAdded> action) {
        added = objectFactory.newInstance(HistoricalAdded.class);
        action.execute(added);
    }

    public void mover(Action<HistoricalMover> action) {
        HistoricalMover mover = objectFactory.newInstance(HistoricalMover.class);
        action.execute(mover);
        movers.add(mover);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHistoryIncludes() {
        return historyIncludes;
    }

    public void setHistoryIncludes(String historyIncludes) {
        this.historyIncludes = historyIncludes;
    }

    public String getPackageFilter() {
        return packageFilter;
    }

    public void setPackageFilter(String packageFilter) {
        this.packageFilter = packageFilter;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public HistoricalAdded getAdded() {
        return added;
    }

    public List<HistoricalMover> getMovers() {
        return movers;
    }
}
