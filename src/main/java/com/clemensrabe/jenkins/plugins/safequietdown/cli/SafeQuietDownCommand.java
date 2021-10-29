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
import com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownConfiguration;
import com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownManagementLink;

import org.kohsuke.args4j.Option;

import hudson.Extension;
import hudson.Util;
import hudson.cli.CLICommand;
import jenkins.model.Jenkins;

/**
 * Cli command <code>safe-quiet-down</code>.
 */
@Extension
public class SafeQuietDownCommand extends CLICommand {

    /**
     * The system message to show on every page.
     */
    @Option(name = "-m", aliases = { "--message" }, usage = "The system message to display on every page.",
            required = false)
    private String message;

    /**
     * Allow all queued items or only those triggered by an upstream project.
     */
    @Option(name = "-a", aliases = { "--allow-queued" }, usage = "Allow all queued items to finish.",
            required = false)
    private boolean allowAllQueuedItemsOption;

   /**
     * Get the short description of this command used for the <code>help</code>
     * listing.
     * @return description of this command.
     */
    @Override
    public String getShortDescription() {
        return Messages.ActivateSafeQuietdownTitle();
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

        if (!management.isQuietdownActive()) {
            configure();
            management.performToggleQuietdown();
            stdout.println(Messages.SafeQuietdownActivated());
        } else {
            stderr.println(Messages.Err_QuietdownAlreadyActive());
            return 1;
        }
        return 0;
    }

    /**
     * Transfer the options to the Jenkins configuration.
     */
    private void configure() {
        SafeQuietdownConfiguration config = SafeQuietdownConfiguration.get();
        if (Util.fixEmpty(message) != null) {
            config.setQuietdownMessage(message);
            config.save();
        }
        config.setAllowAllQueuedItems(allowAllQueuedItemsOption);
    }
}
