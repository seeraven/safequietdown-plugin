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

import com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownManagementLink
import org.junit.Test
import jenkins.model.Jenkins
import java.util.concurrent.TimeUnit
import org.jvnet.hudson.test.SleepBuilder

/**
 * Tests for {@link FinishedSafeQuietDownCommand}.
 */
class FinishedSafeQuietDownCommandTest extends CliTestBase {

    /**
     * Tests the command while safe quietdown mode is not activated.
     */
    @Test
    void testNoQuietDownActive() {
        assert !SafeQuietdownManagementLink.get().isQuietdownActive() : "Safe quietdown mode not inactive at start"
        assert cmd("finished-safe-quiet-down").execute().waitFor() == 0 : "Command exited wrongly"

        def project = jenkins.createFreeStyleProject()
        project.buildersList.add(new SleepBuilder(5000))
        def buildFuture = project.scheduleBuild2(5)
        assert cmd("finished-safe-quiet-down").execute().waitFor() == 1 : "Command exited wrongly during waiting phase"

        buildFuture.waitForStart()
        assert cmd("finished-safe-quiet-down").execute().waitFor() == 1 : "Command exited wrongly during building"

        buildFuture.get();
        assert cmd("finished-safe-quiet-down").execute().waitFor() == 0 : "Command exited wrongly after building finished"
    }

    /**
     * Tests the command while safe quietdown mode is activated.
     */
    @Test
    void testQuietDownActive() {
        // Enable quietdown mode
        SafeQuietdownManagementLink.get().performToggleQuietdown()
        assert SafeQuietdownManagementLink.get().isQuietdownActive() : "Before condition not met"
        assert cmd("finished-safe-quiet-down").execute().waitFor() == 0 : "Command exited wrongly"

        def project = jenkins.createFreeStyleProject()
        project.buildersList.add(new SleepBuilder(5000))
        def buildFuture = project.scheduleBuild2(2)
        assert cmd("finished-safe-quiet-down").execute().waitFor() == 1 : "Command exited wrongly during waiting phase"

        TimeUnit.SECONDS.sleep(10);
        assert cmd("finished-safe-quiet-down").execute().waitFor() == 0 : "Command exited wrongly after waiting phase"
    }
}
