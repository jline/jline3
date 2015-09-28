/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jline.utils;

import java.io.IOError;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;

public class AnsiHelper {

    public static String strip(CharSequence text) {
        try {
            boolean hasEsc = false;
            for (int i = 0; i < text.length(); i++) {
                if ((hasEsc = text.charAt(i) == '\u001b')) {
                    break;
                }
            }
            if (!hasEsc) {
                return text.toString();
            }
            StringWriter sw = new StringWriter();
            AnsiWriter aw = new AnsiWriter(sw);
            aw.write(text);
            aw.close();
            return sw.toString();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static List<String> splitLines(String text, int maxLength, int tabs) {
        try {
            AnsiSplitterWriter splitter = new AnsiSplitterWriter(maxLength);
            splitter.setTabs(tabs);
            splitter.write(text);
            splitter.close();
            return splitter.getLines();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static String substring(String text, int begin, int end, int tabs) {
        try {
            AnsiSplitterWriter splitter = new AnsiSplitterWriter(begin, end, Integer.MAX_VALUE);
            splitter.setTabs(tabs);
            splitter.write(text);
            splitter.close();
            return splitter.getLines().get(0);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static int length(String curLine, int tabs) {
        try {
            AnsiSplitterWriter splitter = new AnsiSplitterWriter(0, Integer.MAX_VALUE, Integer.MAX_VALUE);
            splitter.setTabs(tabs);
            splitter.write(curLine);
            return splitter.getRealLength();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static String cut(String text, int maxLength, int tabs) {
        return splitLines(text, maxLength, tabs).get(0);
    }

    public static AnsiBufferedReader window(Reader is, int begin, int end, int tabs) {
        AnsiBufferedReader reader = new AnsiBufferedReader(is, begin, end, Integer.MAX_VALUE);
        reader.setTabs(tabs);
        return reader;
    }

    public static AnsiBufferedReader splitter(Reader is, int maxLength, int tabs) throws IOException {
        AnsiBufferedReader reader = new AnsiBufferedReader(is, 0, Integer.MAX_VALUE, maxLength);
        reader.setTabs(tabs);
        return reader;
    }


}
