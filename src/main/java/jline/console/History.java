/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

/**
 * Console history.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public interface History
{
    void setHistoryFile(File historyFile) throws IOException;

    void load(InputStream in) throws IOException;
    
    void load(Reader reader) throws IOException;
    
    int size();
    
    void clear();
    
    void addToHistory(String buffer);

    void flushBuffer() throws IOException;

    boolean moveToFirstEntry();
    
    boolean moveToLastEntry();

    void moveToEnd();

    void setMaxSize(int maxSize);
    
    int getMaxSize();
    
    void setOutput(PrintWriter output);

    PrintWriter getOutput();
    
    int getCurrentIndex();
    
    String current();
    
    boolean previous();
    
    boolean next();
    
    List<String> getHistoryList();
}
