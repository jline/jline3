/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Manages native library muck.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public class NativeLibrary
{
    public static File load(final String name) throws IOException {
        assert name != null;

        String libname = System.mapLibraryName(name);
        String arch = System.getProperty("os.arch");
        if ("i386".equals(arch)) {
            arch = "x86";
        }

        String resourceName = "/jline/win32-" + arch + "/" + libname;
        URL url = NativeLibrary.class.getResource(resourceName);

        InputStream is = url.openStream();
        if (is == null) {
            throw new Error("Unable to open resource stream: " + url);
        }

        // Figure out where our tmp files will go
        File tmp = File.createTempFile(name, ".tmp");
        File dir = new File(tmp.getParentFile(), name);
        tmp.delete();
        dir.mkdirs();
        
        // Attempt to delete any there already
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }

        File lib;
        OutputStream os = null;
        try {
            lib = File.createTempFile(name, ".dll", dir);
            lib.deleteOnExit();

            os = new BufferedOutputStream(new FileOutputStream(lib));
            int count;
            byte[] buff = new byte[1024];
            while ((count = is.read(buff, 0, buff.length)) > 0) {
                os.write(buff, 0, count);
            }

            os.flush();
        }
        catch(IOException e) {
            throw new Error("Failed to extract resource: " + url, e);
        }
        finally {
            try {
                is.close();
            }
            catch(IOException e) {
                // ignore
            }
            if (os != null) {
                try {
                    os.close();
                }
                catch(IOException e) {
                    // Ignore
                }
            }
        }

        System.load(lib.getAbsolutePath());
        
        return lib;
    }
}