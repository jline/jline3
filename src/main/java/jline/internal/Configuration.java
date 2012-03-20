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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * Provides access to configuration values.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 2.4
 */
public class Configuration
{
    public static final String JLINE_INPUTRC = "jline.inputrc";

    public static final String JLINE_RC = ".jline.rc";
    public static final String INPUT_RC = ".inputrc";

    private static Configuration configuration;

    public static Configuration getConfig() {
        return getConfig((URL) null);
    }

    public static Configuration getConfig(String fileOrUrl) {
        return getConfig(getUrlFrom(fileOrUrl));
    }
    
    public static Configuration getConfig(URL url) {
        
        if (url != null || configuration == null) {
            
            if (url ==  null) {
                url = getUrlFrom(new File(getUserHome(), JLINE_RC));
            }
            if (configuration == null || !url.equals(configuration.jlinercUrl)) {
                configuration = new Configuration(url);
            }
        }
        
        return configuration;
    }

    private final Properties props;
    private final URL jlinercUrl;

    public Configuration() {
        this(getUrlFrom(new File(getUserHome(), JLINE_RC)));
    }
    
    public Configuration(File inputRc) {
        this(getUrlFrom(inputRc));
    }

    public Configuration(String fileOrUrl) {
        this(getUrlFrom(fileOrUrl));
    }

    public Configuration(URL jlinercUrl) {
        this.jlinercUrl = jlinercUrl;
        this.props = loadProps();
    }

    protected Properties loadProps() {
        // Load jline resources
        Properties props = new Properties();
        try {
            InputStream input = this.jlinercUrl.openStream();
            try {
                props.load(new BufferedInputStream(input));
            }
            finally {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        } catch (IOException e) {
            if (this.jlinercUrl.getProtocol().equals("file")) {
                File file = new File(this.jlinercUrl.getPath());
                if (file.exists()) {
                    Log.warn("Unable to read user configuration: ", this.jlinercUrl, e);
                }
            } else {
                Log.warn("Unable to read user configuration: ", this.jlinercUrl, e);
            }
        }
        return props;
    }

    public static URL getUrlFrom(String fileOrUrl) {
        if (fileOrUrl == null) {
            return null;
        }
        try {
            return new URL(fileOrUrl);
        } catch (MalformedURLException e) {
            return getUrlFrom(new File(fileOrUrl));
        }
    }

    public static URL getUrlFrom(File inputRc) {
        try {
            return inputRc != null ? inputRc.toURI().toURL() : null;
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public String string(final String name, final String defaultValue) {
        assert name != null;

        String value;

        // Check sysprops first, it always wins
        value = System.getProperty(name);

        if (value == null) {
            // Next try userprops
            value = props.getProperty(name);

            if (value == null) {
                // else use the default
                value = defaultValue;
            }
        }

        return value;
    }

    public String string(final String name) {
        return string(name, null);
    }

    public boolean bool(final String name, final boolean defaultValue) {
        String value = string(name, null);
        if (value == null) {
            return defaultValue;
        }
        return value.length() == 0 || value.equalsIgnoreCase("1")
                || value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true");
    }

    public boolean bool(final String name) {
        return bool(name, false);
    }
    
    public void setString(final String name, final String value) {
        props.setProperty (name,  value);
    }

    public static String getString(final String name, final String defaultValue) {
        return Configuration.getConfig().string(name, defaultValue);
    }

    public static String getString(final String name) {
        return getString(name, null);
    }

    public static boolean getBoolean(final String name, final boolean defaultValue) {
        return Configuration.getConfig().bool(name, defaultValue);
    }
    
    public static int getInteger(final String name, final int defaultValue) {
        String str = Configuration.getConfig().props.getProperty(name);
        if (name == null) {
            return defaultValue;
        }
        return Integer.parseInt(str);
    }
    
    public static long getLong(final String name, final long defaultValue) {
        String str = Configuration.getConfig().props.getProperty(name);
        if (str == null) {
            return defaultValue;
        }
        return Long.parseLong(str);
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