/*
 * Copyright (C) 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jline.console.completers;

import jline.console.ConsoleReaderTestSupport;
import org.junit.Test;

import static jline.console.Operation.COMPLETE;

/**
 * Tests for {@link ArgumentCompleter}.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ArgumentCompleterTest
    extends ConsoleReaderTestSupport
{
    @Test
    public void test1() throws Exception {
        console.addCompleter(new ArgumentCompleter(new StringsCompleter("foo", "bar", "baz")));

        assertBuffer("foo foo ", new Buffer("foo f").op(COMPLETE));
        assertBuffer("foo ba", new Buffer("foo b").op(COMPLETE));
        assertBuffer("foo ba", new Buffer("foo ba").op(COMPLETE));
        assertBuffer("foo baz ", new Buffer("foo baz").op(COMPLETE));

        // test completion in the mid range
        assertBuffer("foo baz", new Buffer("f baz").left().left().left().left().op(COMPLETE));
        assertBuffer("ba foo", new Buffer("b foo").left().left().left().left().op(COMPLETE));
        assertBuffer("foo ba baz", new Buffer("foo b baz").left().left().left().left().op(COMPLETE));
        assertBuffer("foo foo baz", new Buffer("foo f baz").left().left().left().left().op(COMPLETE));
    }
}