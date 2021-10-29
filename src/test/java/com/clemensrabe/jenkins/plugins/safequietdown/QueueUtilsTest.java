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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;

import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.tasks.BuildTrigger;

import jenkins.model.Jenkins;

/**
 * Test class for queue utils helper class.
 */
public class QueueUtilsTest {

    /**
     * Jenkins rule instance.
     */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private static final int TIMEOUT_SECONDS = 60;
    private static final int QUIET_PERIOD = 5;
    private static final int JOB_SLEEP_TIME = 5000;
    private static final int NUM_EXECUTORS = 4;

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
     * Test the getPermittedQueueItemIds() method when the configuration flag
     * allQueuedItems is set to true.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetPermittedQueueItemIdsWithAllQueuedItems() throws Exception {
        SafeQuietdownConfiguration.get().setAllowAllQueuedItems(true);

        // Put a project in the queue
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.scheduleBuild2(QUIET_PERIOD);
        long queueId = waitForProjectInQueue(project);

        // The project is permitted as the flag allowAllQueuedItems is set to true
        Set<Long> queueIds = QueueUtils.getPermittedQueueItemIds();
        assertTrue(queueIds.contains(queueId));
    }

    /**
     * Test the getPermittedQueueItemIds() method when the configuration flag
     * allQueuedItems is set to false.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetPermittedQueueItemIdsWithoutAllQueuedItems() throws Exception {
        SafeQuietdownConfiguration.get().setAllowAllQueuedItems(false);

        // Put a project in the queue
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.scheduleBuild2(QUIET_PERIOD);
        long queueId = waitForProjectInQueue(project);

        // The project is not permitted as the flag allowAllQueuedItems is set to false
        Set<Long> queueIds = QueueUtils.getPermittedQueueItemIds();
        assertFalse(queueIds.contains(queueId));
    }

    /**
     * Test the getPermittedQueueItemIds() method when the configuration flag
     * allQueuedItems is set to false and a downstream project of a running
     * build is in the queue.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetPermittedQueueItemIdsWithoutAllQueuedItemsWithUpstreamProject() throws Exception {
        SafeQuietdownConfiguration.get().setAllowAllQueuedItems(false);

        // Create a parent and a child project
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.setQuietPeriod(QUIET_PERIOD);
        Jenkins.get().rebuildDependencyGraph();

        // Start the parent project and wait for the child
        parent.scheduleBuild2(0).waitForStart();
        long queueId = waitForProjectInQueue(child);

        // The project is permitted as it is a downstream job of a finished job
        Set<Long> queueIds = QueueUtils.getPermittedQueueItemIds();
        assertTrue(queueIds.contains(queueId));
    }

    /**
     * Test the getRunningQueueItemIds() method.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetRunningQueueItemIds() throws Exception {
        assertTrue(QueueUtils.getRunningQueueItemIds().isEmpty());

        // Put a project in the queue
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        QueueTaskFuture buildFuture = project.scheduleBuild2(QUIET_PERIOD);

        // As long as it is in the queue, it is not returned as running
        long queueId = waitForProjectInQueue(project);
        assertTrue(QueueUtils.getRunningQueueItemIds().isEmpty());

        // It must be returned when it is building
        buildFuture.waitForStart();
        assertTrue(QueueUtils.getRunningQueueItemIds().contains(queueId));
    }

    /**
     * Test the getUpstreamQueueIds() method.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetUpstreamQueueIds() throws Exception {
        // Create a parent and a child project
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.setQuietPeriod(QUIET_PERIOD);
        Jenkins.get().rebuildDependencyGraph();

        // Start the parent project and ensure it has no upstream queue ids
        parent.scheduleBuild2(QUIET_PERIOD);
        long parentQueueId = waitForProjectInQueue(parent);
        assertTrue(QueueUtils.getUpstreamQueueIds(Queue.getInstance().getItem(parentQueueId)).isEmpty());

        // Wait for the child and ensure its parent is retrieved correctly.
        long childQueueId = waitForProjectInQueue(child);
        assertTrue(QueueUtils.getUpstreamQueueIds(Queue.getInstance().getItem(childQueueId)).contains(parentQueueId));
    }

    /**
     * Test the getNumberOfActiveBuilds() method.
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testGetNumberOfActiveBuilds() throws Exception {
        assertEquals(QueueUtils.getNumberOfActiveBuilds(), 0);

        // Put a project into the queue. As long as it is not running,
        // getNumberOfActiveBuilds() must return 0.
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));
        QueueTaskFuture buildFuture = project.scheduleBuild2(QUIET_PERIOD);
        assertEquals(QueueUtils.getNumberOfActiveBuilds(), 0);

        // When it is building, getNumberOfActiveBuilds() must return 1.
        buildFuture.waitForStart();
        assertEquals(QueueUtils.getNumberOfActiveBuilds(), 1);
    }

    /**
     * Wait for a project to appear in the queue and return its
     * queue item ID.
     * @param project the project to wait for.
     * @return the queue item id.
     */
    private long waitForProjectInQueue(final FreeStyleProject project) throws InterruptedException {
        long id = -1L;
        int elapsedSeconds = 0;
        while ((elapsedSeconds <= TIMEOUT_SECONDS) && (id == -1)) {
            for (Queue.Item item : Queue.getInstance().getItems()) {
                if (item.task instanceof AbstractProject) {
                    AbstractProject queuedProject = (AbstractProject) item.task;
                    if (queuedProject.equals(project)) {
                        id = item.getId();
                        break;
                    }
                }
            }
            if (id == -1) {
                TimeUnit.SECONDS.sleep(1);
                elapsedSeconds++;
            }
        }
        if (elapsedSeconds >= TIMEOUT_SECONDS) {
            fail("Project was not queued up within time limit");
        }
        return id;
    }
}
