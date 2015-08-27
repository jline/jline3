package org.jline.console;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.fusesource.jansi.Pty;
import org.jline.utils.InputStreamReader;
import org.jline.utils.NonBlockingReader;

public class PosixSysConsole extends AbstractPosixConsole {

    private final InputStream in;
    private final OutputStream out;
    private final String encoding;
    private final NonBlockingReader reader;
    private final PrintWriter writer;

    public PosixSysConsole(String type, String encoding) throws IOException {
        super(type, Pty.current());
        this.in = new FileInputStream(FileDescriptor.in);
        this.out = new FileOutputStream(FileDescriptor.out);
        this.encoding = encoding;
        this.reader = new NonBlockingReader(new InputStreamReader(in, encoding));
        this.writer = new PrintWriter(new OutputStreamWriter(out, encoding));
        parseInfoCmp();
    }

    public NonBlockingReader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    public InputStream getInput() {
        return in;
    }

    public OutputStream getOutput() {
        return out;
    }

    public String getEncoding() {
        return encoding;
    }

}
