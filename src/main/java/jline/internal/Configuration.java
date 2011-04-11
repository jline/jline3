/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jline.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jline.console.KeyMap;
import jline.console.Operation;

/**
 * Provides access to configuration values.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.4
 */
public class Configuration
{
    public static final String JLINE_RC = ".inputrc";

    private static Configuration configuration;

    public static Configuration getConfig() {
        return getConfig(null, (URL) null);
    }

    public static Configuration getConfig(String name) {
        return getConfig(name, (URL) null);
    }

    public static Configuration getConfig(String name, String fileOrUrl) {
        return getConfig(name, getUrlFrom(fileOrUrl));
    }

    public static Configuration getConfig(String name, URL url) {
        if (name == null) {
            name = "JLine";
        }
        if (url ==  null) {
            url = getUrlFrom(new File(getUserHome(), JLINE_RC));
        }
        if (configuration == null || !name.equals(configuration.appName) || !url.equals(configuration.url)) {
            configuration = new Configuration(name, url);
        }
        return configuration;
    }



    private final Properties props;

    private final String appName;

    private final KeyMap keys;

    private final URL url;

    public Configuration(String appName) {
        this(appName, (String) null);
    }

    public Configuration(String appName, File inputRc) {
        this(appName, getUrlFrom(inputRc));
    }

    public Configuration(String appName, String fileOrUrl) {
        this(appName, getUrlFrom(fileOrUrl));
    }

    public Configuration(String appName, URL url) {
        this.appName = appName;
        this.props = new Properties();
        this.keys = new KeyMap();
        this.url = url;
        load();
    }

    public void load() {
        try {
            keys.from(KeyMap.emacs());
            InputStream input = this.url.openStream();
            try {
                load(input);
                Log.debug("Loaded user configuration: ", this.url);
            }
            finally {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            keys.bindArrowKeys();
        }
        catch (IOException e) {
            Log.warn("Unable to read user configuration: ", this.url, e);
        }
    }

    private static URL getUrlFrom(String fileOrUrl) {
        if (fileOrUrl == null) {
            return null;
        }
        try {
            return new URL(fileOrUrl);
        } catch (MalformedURLException e) {
            return getUrlFrom(new File(fileOrUrl));
        }
    }

    private static URL getUrlFrom(File inputRc) {
        try {
            return inputRc != null ? inputRc.toURI().toURL() : null;
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public KeyMap getKeys() {
        return keys;
    }

    private void load(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader( new InputStreamReader( input ) );
        String line;
        boolean parsing = true;
        List<Boolean> ifsStack = new ArrayList<Boolean>();
        while ( (line = reader.readLine()) != null ) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.charAt(0) == '#') {
                continue;
            }
            int i = 0;
            if (line.charAt(i) == '$') {
                String cmd;
                String args;
                for (++i; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                int s = i;
                for (; i < line.length() && (line.charAt(i) != ' ' && line.charAt(i) != '\t'); i++);
                cmd = line.substring(s, i);
                for (; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                s = i;
                for (; i < line.length() && (line.charAt(i) != ' ' && line.charAt(i) != '\t'); i++);
                args = line.substring(s, i);
                if ("if".equalsIgnoreCase(cmd)) {
                    ifsStack.add( parsing );
                    if (!parsing) {
                        continue;
                    }
                    if (args.startsWith("term=")) {
                        // TODO
                    } else if (args.startsWith("mode=")) {
                        // TODO
                    } else {
                        parsing = args.equalsIgnoreCase(appName);
                    }
                } else if ("else".equalsIgnoreCase(cmd)) {
                    if (ifsStack.isEmpty()) {
                        throw new IllegalArgumentException("$else found without matching $if");
                    }
                    boolean invert = true;
                    for (boolean b : ifsStack) {
                        if (!b) {
                            invert = false;
                            break;
                        }
                    }
                    if (invert) {
                        parsing = !parsing;
                    }
                } else if ("endif".equalsIgnoreCase(cmd)) {
                    if (ifsStack.isEmpty()) {
                        throw new IllegalArgumentException("endif found without matching $if");
                    }
                    parsing = ifsStack.remove( ifsStack.size() - 1 );
                } else if ("include".equalsIgnoreCase(cmd)) {
                    // TODO
                }
                continue;
            }
            if (!parsing) {
                continue;
            }
            boolean equivalency;
            String keySeq = "";
            if (line.charAt(i++) == '"') {
                boolean esc = false;
                for (;; i++) {
                    if (i >= line.length()) {
                        throw new IllegalArgumentException("Missing closing quote on line '" + line + "'");
                    }
                    if (esc) {
                        esc = false;
                    } else if (line.charAt(i) == '\\') {
                        esc = true;
                    } else if (line.charAt(i) == '"') {
                        break;
                    }
                }
            }
            for (; i < line.length() && line.charAt(i) != ':'
                    && line.charAt(i) != ' ' && line.charAt(i) != '\t'
                    ; i++);
            keySeq = line.substring(0, i);
            equivalency = (i + 1 < line.length() && line.charAt(i) == ':' && line.charAt(i + 1) == '=');
            i++;
            if (equivalency) {
                i++;
            }
            if (keySeq.equalsIgnoreCase("set")) {
                String key;
                String val;
                for (; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                int s = i;
                for (; i < line.length() && (line.charAt(i) != ' ' && line.charAt(i) != '\t'); i++);
                key = line.substring( s, i );
                for (; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                s = i;
                for (; i < line.length() && (line.charAt(i) != ' ' && line.charAt(i) != '\t'); i++);
                val = line.substring( s, i );
                props.put( key, val );
            } else {
                for (; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++);
                int start = i;
                if (i < line.length() && (line.charAt(i) == '\'' || line.charAt(i) == '\"')) {
                    char delim = line.charAt(i++);
                    boolean esc = false;
                    for (;; i++) {
                        if (i >= line.length()) {
                            break;
                        }
                        if (esc) {
                            esc = false;
                        } else if (line.charAt(i) == '\\') {
                            esc = true;
                        } else if (line.charAt(i) == delim) {
                            break;
                        }
                    }
                }
                for (; i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != '\t'; i++);
                String val = line.substring(Math.min(start, line.length()), Math.min(i, line.length()));
                if (keySeq.charAt(0) == '"') {
                    keySeq = translateQuoted(keySeq);

                } else {
                    // Bind key name
                    String keyName = keySeq.lastIndexOf('-') > 0 ? keySeq.substring( keySeq.lastIndexOf('-') + 1 ) : keySeq;
                    char key = getKeyFromName(keyName);
                    keyName = keySeq.toLowerCase();
                    keySeq = "";
                    if (keyName.contains("meta-") || keyName.contains("m-")) {
                        keySeq += "\u001b";
                    }
                    if (keyName.contains("control-") || keyName.contains("c-") || keyName.contains("ctrl-")) {
                        key = (char)(Character.toUpperCase( key ) & 0x1f);
                    }
                    keySeq += key;
                }
                if (val.length() > 0 && (val.charAt(0) == '\'' || val.charAt(0) == '\"')) {
                    keys.bind( keySeq, translateQuoted(val) );
                } else {
                    val = val.replace('-', '_').toUpperCase();
                    keys.bind( keySeq, Operation.valueOf(val) );
                }
            }
        }

    }

    private String translateQuoted(String keySeq) {
        int i;
        String str = keySeq.substring( 1, keySeq.length() - 1 );
        keySeq = "";
        for (i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\') {
                boolean ctrl = str.regionMatches(i, "\\C-", 0, 3)|| str.regionMatches(i, "\\M-\\C-", 0, 6);
                boolean meta = str.regionMatches(i, "\\M-", 0, 3)|| str.regionMatches(i, "\\C-\\M-", 0, 6);
                i += (meta ? 3 : 0) + (ctrl ? 3 : 0) + (!meta && !ctrl ? 1 : 0);
                if (i >= str.length()) {
                    break;
                }
                c = str.charAt(i);
                if (meta) {
                    keySeq += "\u001b";
                }
                if (ctrl) {
                    c = c == '?' ? 0x7f : (char)(Character.toUpperCase( c ) & 0x1f);
                }
                if (!meta && !ctrl) {
                    switch (c) {
                        case 'a': c = 0x07; break;
                        case 'b': c = '\b'; break;
                        case 'd': c = 0x7f; break;
                        case 'e': c = 0x1b; break;
                        case 'f': c = '\f'; break;
                        case 'n': c = '\n'; break;
                        case 'r': c = '\r'; break;
                        case 't': c = '\t'; break;
                        case 'v': c = 0x0b; break;
                        case '\\': c = '\\'; break;
                        case '0': case '1': case '2': case '3':
                        case '4': case '5': case '6': case '7':
                            c = 0;
                            for (int j = 0; j < 3; j++, i++) {
                                if (i >= str.length()) {
                                    break;
                                }
                                int k = Character.digit(str.charAt(i), 8);
                                if (k < 0) {
                                    break;
                                }
                                c = (char)(c * 8 + k);
                            }
                            c &= 0xFF;
                            break;
                        case 'x':
                            i++;
                            c = 0;
                            for (int j = 0; j < 2; j++, i++) {
                                if (i >= str.length()) {
                                    break;
                                }
                                int k = Character.digit(str.charAt(i), 16);
                                if (k < 0) {
                                    break;
                                }
                                c = (char)(c * 16 + k);
                            }
                            c &= 0xFF;
                            break;
                        case 'u':
                            i++;
                            c = 0;
                            for (int j = 0; j < 4; j++, i++) {
                                if (i >= str.length()) {
                                    break;
                                }
                                int k = Character.digit(str.charAt(i), 16);
                                if (k < 0) {
                                    break;
                                }
                                c = (char)(c * 16 + k);
                            }
                            break;
                    }
                }
                keySeq += c;
            } else {
                keySeq += c;
            }
        }
        return keySeq;
    }

    private static char getKeyFromName(String name) {
        if ("DEL".equalsIgnoreCase(name) || "Rubout".equalsIgnoreCase(name)) {
            return 0x7f;
        } else if ("ESC".equalsIgnoreCase(name) || "Escape".equalsIgnoreCase(name)) {
            return '\033';
        } else if ("LFD".equalsIgnoreCase(name) || "NewLine".equalsIgnoreCase(name)) {
            return '\n';
        } else if ("RET".equalsIgnoreCase(name) || "Return".equalsIgnoreCase(name)) {
            return '\r';
        } else if ("SPC".equalsIgnoreCase(name) || "Space".equalsIgnoreCase(name)) {
            return ' ';
        } else if ("Tab".equalsIgnoreCase(name)) {
            return '\t';
        } else {
            return name.charAt(0);
        }
    }


    public static String getString(final String name, final String defaultValue) {
        assert name != null;

        String value;

        // Check sysprops first, it always wins
        value = System.getProperty(name);

        if (value == null) {
            // Next try userprops
            value = Configuration.getConfig().props.getProperty(name);

            if (value == null) {
                // else use the default
                value = defaultValue;
            }
        }

        return value;
    }

    public static String getString(final String name) {
        return getString(name, null);
    }

    public static boolean getBoolean(final String name, final boolean defaultValue) {
        String value = getString(name);
        if (value == null) {
            return defaultValue;
        }
        return value.length() == 0 || value.equalsIgnoreCase("1")
                || value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true");
    }

    public static boolean getBoolean(final String name) {
        return getBoolean(name, false);
    }

    //
    // System property helpers
    //
    
    public static File getUserHome() {
        return new File(System.getProperty("user.home"));
    }

    public static String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }

    public static String getFileEncoding() {
        return System.getProperty("file.encoding");
    }

    public static String getEncoding() {
        // LC_CTYPE is usually in the form en_US.UTF-8
        String ctype = System.getenv("LC_CTYPE");
        if (ctype != null && ctype.indexOf('.') > 0) {
            return ctype.substring( ctype.indexOf('.') + 1 );
        }
        return System.getProperty("input.encoding", Charset.defaultCharset().name());
    }
}