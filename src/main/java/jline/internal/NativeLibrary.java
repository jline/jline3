/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.MessageFormat;

/**
 * Manages native library muck.
 *
 * Currently only handles Windows bits, since that is all the native goo that JLine needs.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public final class NativeLibrary
{
    public static final String I386 = "i386";

    public static final String X86 = "x86";

    public static final String DOT_DLL = ".dll";
    
    public static final String DOT_TMP = ".tmp";

    public static File load(final String name) throws IOException {
        assert name != null;

        String libName = System.mapLibraryName(name);
        String arch = System.getProperty("os.arch");
        if (I386.equals(arch)) {
            arch = X86;
        }

        String resourceName = MessageFormat.format("/jline/win32-{0}/{1}", arch, libName);
        URL url = NativeLibrary.class.getResource(resourceName);

        InputStream is = url.openStream();
        if (is == null) {
            throw new Error(MessageFormat.format("Unable to open resource stream: {0}", url));
        }

        // Figure out where our tmp files will go
        File tmp = File.createTempFile(name, DOT_TMP);
        File dir = new File(tmp.getParentFile(), name);

        //noinspection ResultOfMethodCallIgnored
        tmp.delete();
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        
        // Attempt to delete any there already
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }

        File lib;
        OutputStream os = null;
        try {
            lib = File.createTempFile(name, DOT_DLL, dir);
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
            throw new Error(MessageFormat.format("Failed to extract resource: {0}", url), e);
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