package org.codehaus.mojo.natives.linker;

/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codehaus.mojo.natives.NativeBuildException;
import org.codehaus.mojo.natives.util.CommandLineUtil;
import org.codehaus.mojo.natives.util.EnvUtil;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.cli.Commandline;

public abstract class AbstractLinker
    extends AbstractLogEnabled
    implements Linker
{

    protected abstract Commandline createLinkerCommandLine( List objectFiles, LinkerConfiguration config )
        throws NativeBuildException;

    public File link( LinkerConfiguration config, List compilerOutputFiles )
        throws NativeBuildException, IOException
    {
        if ( isStaled( config, compilerOutputFiles ) )
        {
            // TODO validate config to make sure required fields are available
            Commandline cl = this.createLinkerCommandLine( compilerOutputFiles, config );
            EnvUtil.setupCommandlineEnv( cl, config.getEnvFactory() );
            CommandLineUtil.execute( cl, this.getLogger() );
        }

        return config.getOutputFile();
    }

    private boolean isStaled( LinkerConfiguration config, List compilerOutputFiles )
    {
        if ( !config.isCheckStaleLinkage() )
        {
            // user dont care
            return true;
        }

        File previousDestination = config.getOutputFile();

        if ( !previousDestination.exists() )
        {
            return true;
        }

        if ( previousDestination.exists() )
        {
            for ( int i = 0; i < compilerOutputFiles.size(); ++i )
            {
                if ( previousDestination.lastModified() < ( (File) compilerOutputFiles.get( i ) ).lastModified() )
                {
                    if ( this.getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "Stale relative to compilerOutputFiles: "
                                               + ( (File) compilerOutputFiles.get( i ) ).getAbsolutePath() );
                    }

                    return true;
                }
            }

            for ( int i = 0; i < config.getExternalLibFileNames().size(); ++i )
            {
                File extLib =
                    new File( config.getExternalLibDirectory(), (String) config.getExternalLibFileNames().get( i ) );

                if ( previousDestination.lastModified() < extLib.lastModified() )
                {
                    if ( this.getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "Stale relative to extLib: " + extLib.getAbsolutePath() );
                    }

                    return true;
                }
            }
        }

        return false;
    }

}
