/*
 * Copyright (C) 2022 the original author(s).
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
package org.jline.terminal.impl.jep424;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractPty;
import org.jline.terminal.spi.TerminalProvider;

class NativePty extends AbstractPty
{
    private final int master;
    private final int slave;
    private final int slaveOut;
    private final String name;
    private final FileDescriptor masterFD;
    private final FileDescriptor slaveFD;
    private final FileDescriptor slaveOutFD;

    public NativePty( int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, String name )
    {
        this( master, masterFD, slave, slaveFD, slave, slaveFD, name );
    }

    public NativePty( int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, int slaveOut,
                      FileDescriptor slaveOutFD, String name )
    {
        this.master = master;
        this.slave = slave;
        this.slaveOut = slaveOut;
        this.name = name;
        this.masterFD = masterFD;
        this.slaveFD = slaveFD;
        this.slaveOutFD = slaveOutFD;
    }

    @Override
    public void close() throws IOException
    {
        if ( master > 0 )
        {
            getMasterInput().close();
        }
        if ( slave > 0 )
        {
            getSlaveInput().close();
        }
    }

    public int getMaster()
    {
        return master;
    }

    public int getSlave()
    {
        return slave;
    }

    public int getSlaveOut()
    {
        return slaveOut;
    }

    public String getName()
    {
        return name;
    }

    public FileDescriptor getMasterFD()
    {
        return masterFD;
    }

    public FileDescriptor getSlaveFD()
    {
        return slaveFD;
    }

    public FileDescriptor getSlaveOutFD()
    {
        return slaveOutFD;
    }

    public InputStream getMasterInput()
    {
        return new FileInputStream( getMasterFD() );
    }

    public OutputStream getMasterOutput()
    {
        return new FileOutputStream( getMasterFD() );
    }

    protected InputStream doGetSlaveInput()
    {
        return new FileInputStream( getSlaveFD() );
    }

    public OutputStream getSlaveOutput()
    {
        return new FileOutputStream( getSlaveOutFD() );
    }

    @Override
    public Attributes getAttr() throws IOException
    {
        return CLibrary.getAttributes( slave );
    }

    @Override
    protected void doSetAttr( Attributes attr ) throws IOException
    {
        CLibrary.setAttributes( slave, attr );
    }

    @Override
    public Size getSize() throws IOException
    {
        return CLibrary.getTerminalSize( slave );
    }

    @Override
    public void setSize( Size size ) throws IOException
    {
        CLibrary.setTerminalSize( slave, size );
    }

    @Override
    public String toString()
    {
        return "NativePty[" + getName() + "]";
    }

    protected static FileDescriptor newDescriptor( int fd )
    {
        try
        {
            Constructor<FileDescriptor> cns = FileDescriptor.class.getDeclaredConstructor( int.class );
            cns.setAccessible( true );
            return cns.newInstance( fd );
        }
        catch ( Throwable e )
        {
            throw new RuntimeException( "Unable to create FileDescriptor", e );
        }
    }

    public static boolean isPosixSystemStream( TerminalProvider.Stream stream )
    {
        return switch ( stream )
                {
                    case Input -> CLibrary.isTty( 0 );
                    case Output -> CLibrary.isTty( 1 );
                    case Error -> CLibrary.isTty( 2 );
                };
    }

    public static String posixSystemStreamName( TerminalProvider.Stream stream )
    {
        return switch ( stream )
                {
                    case Input -> CLibrary.ttyName( 0 );
                    case Output -> CLibrary.ttyName( 1 );
                    case Error -> CLibrary.ttyName( 2 );
                };
    }
}
