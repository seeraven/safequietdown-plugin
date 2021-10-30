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

import com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownConfiguration
import com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownDecorator
import com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownManagementLink
import org.junit.Test

/**
 * Tests for {@link SafeQuietDownCommand}.
 */
class SafeQuietDownCommandTest extends CliTestBase {

    /**
     * Tests the command without any arguments.
     */
    @Test
    void testRunWithoutMessage() {
        // Return error if not in quietdown mode
        assert !SafeQuietdownManagementLink.get().isQuietdownActive() : "Safe quietdown mode not inactive at start"
        assert cmd("safe-quiet-down").execute().waitFor() == 0 : "Command exited wrongly"
        assert SafeQuietdownManagementLink.get().isQuietdownActive() : "Quietdown mode not activated"
        assert !SafeQuietdownConfiguration.get().isAllowAllQueuedItems() : "AllowAllQueuedItems set"
    }

    /**
     * Tests the command with a custom message.
     */
    @Test
    void testRunWithMessage() {
        // Return error if not in quietdown mode
        assert !SafeQuietdownManagementLink.get().isQuietdownActive() : "Safe quietdown mode not inactive at start"
        assert cmd("safe-quiet-down", "-m", "Wow that is nice").execute().waitFor() == 0 : "Command exited wrongly"
        assert SafeQuietdownManagementLink.get().isQuietdownActive() : "Quietdown mode not activated"
        assert !SafeQuietdownConfiguration.get().isAllowAllQueuedItems() : "AllowAllQueuedItems set"
        assert SafeQuietdownDecorator.getInstance().quietdownMessage == "Wow that is nice" : "Message not set"
    }

   /**
     * Tests the command with the '--allow-queued' option.
     */
    @Test
    void testRunWithAllowQueuedOption() {
        // Return error if not in quietdown mode
        assert !SafeQuietdownManagementLink.get().isQuietdownActive() : "Safe quietdown mode not inactive at start"
        assert cmd("safe-quiet-down", "-a").execute().waitFor() == 0 : "Command exited wrongly"
        assert SafeQuietdownManagementLink.get().isQuietdownActive() : "Quietdown mode not activated"
        assert SafeQuietdownConfiguration.get().isAllowAllQueuedItems() : "AllowAllQueuedItems not set"
    }
}
