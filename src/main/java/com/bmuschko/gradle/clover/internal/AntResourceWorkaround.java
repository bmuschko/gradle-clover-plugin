/*
 * Copyright 2020 the original author or authors.
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
package com.bmuschko.gradle.clover.internal;

import java.io.InputStream;

import org.gradle.api.internal.DynamicObjectAware;
import org.gradle.internal.IoActions;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.metaobject.BeanDynamicObject;
import org.gradle.internal.metaobject.DynamicObject;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;

public class AntResourceWorkaround {

    private final DynamicObject builder;
    private final ClassLoader antlibClassLoader;

    public AntResourceWorkaround(Object builder) {
        this.builder = asDynamicObject(builder);
        this.antlibClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @SuppressWarnings("resource")
    public void taskdef(String resource) {
        InputStream instr = antlibClassLoader.getResourceAsStream(resource);
        try {
            Node xml = new XmlParser().parse(instr);
            for (Object taskdefObject : (NodeList) xml.get("taskdef")) {
                Node taskdef = (Node) taskdefObject;
                String name = (String) taskdef.get("@name");
                String className = (String) taskdef.get("@classname");
                addTaskDefinition(name, className);
            }
            for (Object typedefObject : (NodeList) xml.get("typedef")) {
                Node typedef = (Node) typedefObject;
                String name = (String) typedef.get("@name");
                String className = (String) typedef.get("@classname");
                addDataTypeDefinition(name, className);
            }
        } catch (Exception ex) {
            throw UncheckedException.throwAsUncheckedException(ex);
        } finally {
            IoActions.closeQuietly(instr);
        }
    }
    
    private void addTaskDefinition(String name, String className) throws ClassNotFoundException {
        DynamicObject project = asDynamicObject(builder.getProperty("project"));
        project.invokeMethod("addTaskDefinition", name, antlibClassLoader.loadClass(className));
    }
    
    private void addDataTypeDefinition(String name, String className) throws ClassNotFoundException {
        DynamicObject project = asDynamicObject(builder.getProperty("project"));
        project.invokeMethod("addDataTypeDefinition", name, antlibClassLoader.loadClass(className));
    }
    
    private DynamicObject asDynamicObject(Object object) {
        if (object instanceof DynamicObject) {
            return (DynamicObject)object;
        } else if (object instanceof DynamicObjectAware) {
            return ((DynamicObjectAware) object).getAsDynamicObject();
        } else {
            return new BeanDynamicObject(object);
        }
    }
}
