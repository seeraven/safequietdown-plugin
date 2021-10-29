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
import com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownManagementLink;
import hudson.Extension;
import hudson.cli.CLICommand;
import jenkins.model.Jenkins;

/**
 * Cli command <code>cancel-safe-quiet-down</code>.
 */
@Extension
public class CancelSafeQuietDownCommand extends CLICommand {

    /**
     * Get the short description of this command used for the <code>help</code>
     * listing.
     * @return description of this command.
     */
    @Override
    public String getShortDescription() {
        return Messages.DeactivateSafeQuietdownTitle();
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

        if (management.isQuietdownActive()) {
            management.performToggleQuietdown();
            stdout.println(Messages.SafeQuietdownDeactivated());
        } else {
            stderr.println(Messages.Err_QuietdownNotActive());
            return 1;
        }
        return 0;
    }
}
