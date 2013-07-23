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
package org.eclipse.jgit.pgm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.pgm.internal.CLIText;
import org.eclipse.jgit.pgm.opt.CmdLineParser;
import org.eclipse.jgit.pgm.opt.SubcommandHandler;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.ExampleMode;
import org.kohsuke.args4j.Option;

/**
 * TODO Add javadoc
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class EmbeddedCommandRunner {
    @Option(name = "--help", usage = "usage_displayThisHelpText", aliases = { "-h" })
    private boolean help;

    @Option(name = "--show-stack-trace", usage = "usage_displayThejavaStackTraceOnExceptions")
    private boolean showStackTrace;

    @Option(name = "--git-dir", metaVar = "metaVar_gitDir", usage = "usage_setTheGitRepositoryToOperateOn")
    private String gitdir;

    @Argument(index = 0, metaVar = "metaVar_command", required = true, handler = SubcommandHandler.class)
    private TextBuiltin subcommand;

    @Argument(index = 1, metaVar = "metaVar_arg")
    private List<String> arguments = new ArrayList<String>();

    public void execute(final String[] argv, InputStream in, OutputStream out, OutputStream err) throws Exception {
        final CmdLineParser clp = new CmdLineParser(this);
        PrintWriter writer = new PrintWriter(err);
        try {
            clp.parseArgument(argv);
        } catch (CmdLineException e) {
            if (argv.length > 0 && !help) {
                writer.println(MessageFormat.format(CLIText.get().fatalError, e.getMessage()));
                writer.flush();
                throw new Die(true);
            }
        }

        if (argv.length == 0 || help) {
            final String ex = clp.printExample(ExampleMode.ALL, CLIText.get().resourceBundle());
            writer.println("jgit" + ex + " command [ARG ...]"); //$NON-NLS-1$
            if (help) {
                writer.println();
                clp.printUsage(writer, CLIText.get().resourceBundle());
                writer.println();
            } else if (subcommand == null) {
                writer.println();
                writer.println(CLIText.get().mostCommonlyUsedCommandsAre);
                final CommandRef[] common = CommandCatalog.common();
                int width = 0;
                for (final CommandRef c : common)
                    width = Math.max(width, c.getName().length());
                width += 2;

                for (final CommandRef c : common) {
                    writer.print(' ');
                    writer.print(c.getName());
                    for (int i = c.getName().length(); i < width; i++)
                        writer.print(' ');
                    writer.print(CLIText.get().resourceBundle().getString(c.getUsage()));
                    writer.println();
                }
                writer.println();
            }
            writer.flush();
            throw new Die(true);
        }

        final TextBuiltin cmd = subcommand;
        cmd.ins = in;
        cmd.outs = out;
        cmd.err = new PrintStream(err);
        if (cmd.requiresRepository())
            cmd.init(openGitDir(gitdir), null);
        else
            cmd.init(null, gitdir);
        try {
            cmd.execute(arguments.toArray(new String[arguments.size()]));
        } finally {
            if (cmd.outw != null)
                cmd.outw.flush();
            cmd.err.flush();
        }
    }

    /**
     * Evaluate the {@code --git-dir} option and open the repository.
     *
     * @param gitdir
     *            the {@code --git-dir} option given on the command line. May be
     *            null if it was not supplied.
     * @return the repository to operate on.
     * @throws IOException
     *             the repository cannot be opened.
     */
    protected Repository openGitDir(String gitdir) throws IOException {
        RepositoryBuilder rb = new RepositoryBuilder() //
                .setGitDir(gitdir != null ? new File(gitdir) : null) //
                .readEnvironment() //
                .findGitDir();
        if (rb.getGitDir() == null)
            throw new Die(CLIText.get().cantFindGitDirectory);
        return rb.build();
    }

}
