/*
 *  The MIT License
 *
 *  Copyright (c) 2021 Clemens Rabe. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.clemensrabe.jenkins.plugins.safequietdown.cli;

import com.clemensrabe.jenkins.plugins.safequietdown.Messages;
import com.clemensrabe.jenkins.plugins.safequietdown.QueueUtils;
import com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownManagementLink;

import org.kohsuke.args4j.Option;

import hudson.Extension;
import hudson.cli.CLICommand;
import jenkins.model.Jenkins;

/**
 * Cli command <code>finished-safe-quiet-down</code>.
 */
@Extension
public class FinishedSafeQuietDownCommand extends CLICommand {

    /** Return code when all jobs are finished. */
    private static final int RETURN_CODE_ALL_FINISHED = 0;

    /** Return code when not all jobs are finished. */
    private static final int RETURN_CODE_NOT_ALL_FINISHED = 1;

    /**
     * Print detailed information.
     */
    @Option(name = "-v", aliases = { "--verbose" }, usage = "Print detailed information.",
            required = false)
    private boolean verboseOption;

    /**
     * Get the short description of this command used for the <code>help</code>
     * listing.
     * @return description of this command.
     */
    @Override
    public String getShortDescription() {
        return Messages.FinishedSafeQuietDownTitle();
    }

    /**
     * Executes the command.
     * @return return code of this command.
     * @throws Exception on errors.
     */
    @Override
    protected int run() throws Exception {
        SafeQuietdownManagementLink management = SafeQuietdownManagementLink.get();
        Jenkins.get().checkPermission(management.getRequiredPermission());

        int numActiveBuilds = QueueUtils.getNumberOfActiveBuilds();
        int numBuildableItems = QueueUtils.getNumberOfBuildableQueueItems();
        boolean allFinished = ((numActiveBuilds + numBuildableItems) == 0);

        if (verboseOption) {
            if (management.isQuietdownActive()) {
                stdout.println(Messages.SafeQuietdownActivated());
            } else {
                stderr.println(Messages.SafeQuietdownDeactivated());
            }
            stdout.format("Number of active builds:         %d", numActiveBuilds);
            stdout.println();
            stdout.format("Number of buildable queue items: %d", numBuildableItems);
            stdout.println();

            if (allFinished) {
                stdout.println("All (allowed) builds seems to be finished.");
            } else {
                stdout.println("Not all (allowed) builds seems to be finished.");
            }
        }

        if (allFinished) {
            return RETURN_CODE_ALL_FINISHED;
        }
        return RETURN_CODE_NOT_ALL_FINISHED;
    }
}
