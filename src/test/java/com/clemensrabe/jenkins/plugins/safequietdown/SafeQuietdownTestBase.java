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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Test base class for high-level tests of the safe quietdown plugin.
 */
public class SafeQuietdownTestBase {

    protected static final int JOB_SLEEP_TIME = 5000;
    protected static final int TIMEOUT_SECONDS = 60;
    protected static final int QUIET_PERIOD = 5;
    protected static final int NUM_EXECUTORS = 4;

    /**
     * Read a resource and return its content as a string.
     * @param fileName the file name of the file to read.
     * @return the resource content or null if the resource is not available
     * @throws IOException if something goes wrong
     */
    protected String readResource(final String fileName) throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(
                 this.getClass().getName().replaceAll("\\.", "/") + "/" + fileName)) {
            if (is == null) {
                return null;
            }
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    /**
     * Wait for a job to appear in the queue as a run and return its
     * queue item ID.
     * @param job the job to wait for.
     * @return the queue item.
     */
    protected Queue.Item waitForProjectInQueue(final Job job) throws InterruptedException {
        Queue.Item foundItem = null;
        int elapsedSeconds = 0;
        while ((elapsedSeconds <= TIMEOUT_SECONDS) && (foundItem == null)) {
            for (Queue.Item item : Queue.getInstance().getItems()) {
                if (item.task instanceof Job) {
                    Job itemJob = (Job) item.task;
                    if (itemJob.equals(job)) {
                      foundItem = item;
                      break;
                    }
                }
            }
            if (foundItem == null) {
                TimeUnit.SECONDS.sleep(1);
                elapsedSeconds++;
            }
        }
        if (elapsedSeconds >= TIMEOUT_SECONDS) {
            fail("Project was not queued up within time limit");
        }
        return foundItem;
    }

    /**
     * Waits that the given project shows up in the queue and that it is blocked.
     *
     * @param project The project to wait for
     * @param timeout seconds to wait before aborting
     * @return the found item, null if the item didn't show up in the queue until timeout
     * @throws InterruptedException if interrupted
     */
    protected Queue.Item waitForBlockedItem(final Queue.Task project,
                                            final int timeout) throws InterruptedException {
        Queue jenkinsQueue = Jenkins.get().getQueue();
        Queue.Item queueItem = jenkinsQueue.getItem(project);

        int elapsedSeconds = 0;
        while (elapsedSeconds <= timeout) {
            queueItem = jenkinsQueue.getItem(project);
            if (queueItem != null && queueItem.isBlocked()) {
                return queueItem;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        if (jenkinsQueue.getItem(project) == null) {
            fail("Project did not show up in the queue!");
        } else {
            fail("Project is in the queue but not blocked!");
        }
        return queueItem;
    }

    /**
     * Toggles the safe quietdown mode.
     *
     * @throws Exception if something goes wrong
     */
    protected void toggleSafeQuietdown() throws Exception {
        SafeQuietdownManagementLink.get().performToggleQuietdown();
    }

    /**
     * Asserts that argument jobs are successfully built within a timely manner.
     * @param argumentJobs the projects to assert for success
     * @throws InterruptedException if something goes wrong
     */
    protected void assertSuccessfulJobs(final Job... argumentJobs) throws InterruptedException {
        List<Job> jobs = Arrays.asList(argumentJobs);
        List<Run> runs = new ArrayList<Run>(Collections.<Run>nCopies(argumentJobs.length, null));

        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            boolean allFinished = true;
            for (int i = 0; i < argumentJobs.length; i++) {
                Job job = jobs.get(i);
                Run run = job.getLastBuild();
                runs.set(i, run);

                if (run == null || run.isBuilding()) {
                    allFinished = false;
                    break;
                }
            }

            if (allFinished) {
                break;
            }

            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }

        for (Run run : runs) {
            assertNotNull(run);
            assertEquals(Result.SUCCESS, run.getResult());
        }
        assertEquals(argumentJobs.length, runs.size());
    }

    /**
     * Asserts that argument tasks are blocked.
     * @param argumentTasks the Queue.Tasks to assert for being blocked
     * @throws InterruptedException if something goes wrong
     */
    protected void assertBlockedTasks(final Queue.Task... argumentTasks) throws InterruptedException {
        List<Queue.Task> tasks = Arrays.asList(argumentTasks);

        for (Queue.Task task : tasks) {
            Queue.Item queueItem = waitForBlockedItem(task, TIMEOUT_SECONDS);
            assertThat(queueItem.isBlocked(), is(true));
            assertThat(Messages.GoingToShutDown(), is(queueItem.getWhy()));
        }
    }
}
