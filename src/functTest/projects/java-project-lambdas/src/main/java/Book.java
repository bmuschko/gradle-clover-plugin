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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Book {

    // https://confluence.atlassian.com/cloverkb/java-8-code-instrumented-by-clover-fails-to-compile-442270815.html
    public static List<String> testMapAndCollectBounds(List<String> input) {
        return input.stream()
            .map(e -> e.toUpperCase())
            .collect(Collectors.toList());
    }

    // tested
    public boolean open() {
        List<String> strings = Arrays.asList("foo", "bar");
        testMapAndCollectBounds(strings);
        return true;
    }

    // untested
    public boolean close() {
        return false;
    }
}
