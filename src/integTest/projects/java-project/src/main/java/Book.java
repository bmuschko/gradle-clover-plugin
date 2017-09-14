/*
 * Copyright 2011 the original author or authors.
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

import org.apache.commons.lang3.JavaVersion;

public class Book {

    // tested
    public boolean open() {
        return true;
    }

    public void throwsRuntimeException() {
        throw new RuntimeException("Testing");
    }
    
    public boolean isAtLeastJavaVersion() {
        return JavaVersion.JAVA_1_8.atLeast(JavaVersion.JAVA_1_7);
    }

    // untested
    public boolean close() {
        return false;
    }
}
