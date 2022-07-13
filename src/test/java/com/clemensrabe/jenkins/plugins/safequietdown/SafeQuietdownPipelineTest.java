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

import static org.junit.Assert.assertEquals;

import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import org.jenkinsci.plugins.matrixauth.AuthorizationType;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
// import org.jvnet.hudson.test.SleepBuilder;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

/**
 * Test class for high-level tests of the safe quietdown plugin using pipeline
 * jobs.
 */
public class SafeQuietdownPipelineTest extends SafeQuietdownTestBase {

    /** Jenkins rule instance. */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    /** Maximum sleep time between start of upstream and toggle of safe quietdown mode. */
    private static final int MAX_SLEEP_TIME_SECONDS = 10;

    /**
     * Changes the number of executors on the Jenkins master.
     * Runs before every test.
     * @throws IOException if something goes wrong
     */
    @Before
    public void setUp() throws IOException {
        Jenkins jenkins = jenkinsRule.getInstance();
        GlobalMatrixAuthorizationStrategy authStategy = new GlobalMatrixAuthorizationStrategy();
        authStategy.add(Jenkins.ADMINISTER, new PermissionEntry(AuthorizationType.EITHER, "alice"));
        jenkins.setAuthorizationStrategy(authStategy);
        jenkins.setSecurityRealm(jenkinsRule.createDummySecurityRealm());
        jenkins.setNumExecutors(NUM_EXECUTORS);
    }

    /**
     * Tests the internal readResource() method.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testReadResource() throws Exception {
        String content = readResource("readResourceTest.txt");
        assertEquals(content, "readResource() test\nNext line");
    }

    /**
     * Tests that a build is started as normal when the safe quietdown mode has
     * not been initiated.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testBuildsDirectlyWhenSafeQuietdownIsInactive() throws Exception {
        WorkflowJob project = jenkinsRule.getInstance().createProject(WorkflowJob.class, "p");
        project.setDefinition(new CpsFlowDefinition(readResource("threeStages.pipeline"), true));

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
        WorkflowJob project = jenkinsRule.getInstance().createProject(WorkflowJob.class, "p");
        project.setDefinition(new CpsFlowDefinition(readResource("threeStages.pipeline"), true));

        project.scheduleBuild2(0);
        assertBlockedTasks(project);
    }

    /**
     * Tests that a pipeline continues when the safe quietdown mode has been
     * deactivated again.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testContinuesBuildWhenSafeQuietdownIsDeactivated() throws Exception {
        toggleSafeQuietdown();
        WorkflowJob project = jenkinsRule.getInstance().createProject(WorkflowJob.class, "p");
        project.setDefinition(new CpsFlowDefinition(readResource("triggerChild.pipeline"), true));
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        assertEquals(child.getName(), "child");

        project.scheduleBuild2(0);
        assertBlockedTasks(project);

        toggleSafeQuietdown();
        assertSuccessfulJobs(project);
    }

    /**
     * Tests that downstream builds of already running pipelines are
     * built when safe quietdown mode is active.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testDownstreamIsBuild() throws Exception {
        int sleepTime = 0;

        SafeQuietdownConfiguration.get().setAllowAllQueuedItems(true);

        WorkflowJob parent = jenkinsRule.getInstance().createProject(WorkflowJob.class, "p");
        parent.setDefinition(new CpsFlowDefinition(readResource("triggerChild.pipeline"), true));
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        Jenkins.get().rebuildDependencyGraph();

        while (sleepTime < MAX_SLEEP_TIME_SECONDS) {
            QueueTaskFuture parentFuture = parent.scheduleBuild2(1);
            waitForProjectInQueue(parent);
            TimeUnit.SECONDS.sleep(sleepTime);
            toggleSafeQuietdown();

            parentFuture.get(1, TimeUnit.MINUTES);
            assertSuccessfulJobs(parent, child);
            toggleSafeQuietdown();

            ++sleepTime;
        }
    }

    /**
     * Tests that downstream builds of already finished upstream builds are
     * built when safe quietdown mode is active.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testDownstreamIsBuildWhileSafeQuietdownIsActivated() throws Exception {
        WorkflowJob parent = jenkinsRule.getInstance().createProject(WorkflowJob.class, "p");
        parent.setDefinition(new CpsFlowDefinition(readResource("triggerChildAtEnd.pipeline"), true));
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        child.setQuietPeriod(QUIET_PERIOD);
        Jenkins.get().rebuildDependencyGraph();

        parent.scheduleBuild2(0).get();
        waitForProjectInQueue(child);
        toggleSafeQuietdown();

        assertSuccessfulJobs(parent, child);
    }

    /**
     * Tests that queued builds without upstream project are blocked if allowAllQueued is
     * not set and safe quietdown mode is active.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testBlocksQueuedWithoutUpstream() throws Exception {
        WorkflowJob project = jenkinsRule.getInstance().createProject(WorkflowJob.class, "p");
        project.setDefinition(new CpsFlowDefinition(readResource("threeStages.pipeline"), true));
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

        WorkflowJob project = jenkinsRule.getInstance().createProject(WorkflowJob.class, "p");
        project.setDefinition(new CpsFlowDefinition(readResource("threeStages.pipeline"), true));
        project.scheduleBuild2(QUIET_PERIOD);

        waitForProjectInQueue(project);
        toggleSafeQuietdown();
        assertSuccessfulJobs(project);
    }
}
