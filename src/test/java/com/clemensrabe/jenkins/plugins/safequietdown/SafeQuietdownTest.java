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

package com.clemensrabe.jenkins.plugins.safequietdown;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.tasks.BuildTrigger;
import jenkins.model.Jenkins;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;

/**
 * Test class for high-level tests of the safe quietdown plugin.
 */
public class SafeQuietdownTest extends SafeQuietdownTestBase {

    /** Jenkins rule instance. */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

   /**
     * Changes the number of executors on the Jenkins master.
     * Runs before every test.
     * @throws IOException if something goes wrong
     */
    @Before
    public void setUp() throws IOException {
        Jenkins jenkins = jenkinsRule.getInstance();
        GlobalMatrixAuthorizationStrategy authStategy = new GlobalMatrixAuthorizationStrategy();
        authStategy.add(Jenkins.ADMINISTER, "alice");
        jenkins.setAuthorizationStrategy(authStategy);
        jenkins.setSecurityRealm(jenkinsRule.createDummySecurityRealm());
        jenkins.setNumExecutors(NUM_EXECUTORS);
    }

    /**
     * Tests that a build is started as normal when the safe quietdown mode has
     * not been initiated.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testBuildsDirectlyWhenSafeQuietdownIsInactive() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        project.scheduleBuild2(0);
        assertSuccessfulJobs(project);
    }

    /**
     * Tests that a new build is blocked when the safe quietdown mode has been
     * activated.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testBlocksBuildWhenSafeQuietdownIsActive() throws Exception {
        toggleSafeQuietdown();
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        project.scheduleBuild2(0);
        assertBlockedTasks(project);
    }

    /**
     * Tests that a build continues when the safe quietdown mode has been
     * deactivated again.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testContinuesBuildWhenSafeQuietdownIsDeactivated() throws Exception {
        toggleSafeQuietdown();
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        project.scheduleBuild2(0);
        assertBlockedTasks(project);

        toggleSafeQuietdown();
        assertSuccessfulJobs(project);
    }

    /**
     * Tests that downstream builds of already running upstream builds are
     * built when safe quietdown mode is active.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testDownstreamIsBuild() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        FreeStyleProject grandChild = jenkinsRule.createFreeStyleProject("grandchild");

        parent.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));
        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        Jenkins.get().rebuildDependencyGraph();

        parent.scheduleBuild2(0).waitForStart();
        toggleSafeQuietdown();

        assertSuccessfulJobs(parent, child, grandChild);
    }

    /**
     * Tests that downstream builds of already finished upstream builds are
     * built when safe quietdown mode is active.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testDownstreamIsBuildWhileSafeQuietdownIsActivated() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        FreeStyleProject grandChild = jenkinsRule.createFreeStyleProject("grandchild");

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        child.setQuietPeriod(QUIET_PERIOD);
        Jenkins.get().rebuildDependencyGraph();

        parent.scheduleBuild2(0).get();
        waitForProjectInQueue(child);
        toggleSafeQuietdown();

        assertSuccessfulJobs(parent, child, grandChild);
    }

    /**
     * Tests that queued builds without upstream project are blocked if allowAllQueued is
     * not set and safe quietdown mode is active.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testBlocksQueuedWithoutUpstream() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.scheduleBuild2(QUIET_PERIOD);

        waitForProjectInQueue(project);
        toggleSafeQuietdown();
        assertBlockedTasks(project);
    }

    /**
     * Tests that queued builds without upstream project are not blocked if allowAllQueued is
     * set and safe quietdown mode is active.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testContinuesQueuedWithoutUpstreamWhenAllowAllQueuedIsSet() throws Exception {
        SafeQuietdownConfiguration.get().setAllowAllQueuedItems(true);

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.scheduleBuild2(QUIET_PERIOD);

        waitForProjectInQueue(project);
        toggleSafeQuietdown();
        assertSuccessfulJobs(project);
    }
}
