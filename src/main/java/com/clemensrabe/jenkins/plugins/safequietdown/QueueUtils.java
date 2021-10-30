/*
 *  The MIT License
 *
 *  Copyright (c) 2014 Sony Mobile Communications Inc. All rights reserved.
 *  Copyright (c) 2016 Markus Winter. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hudson.model.Cause;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;

/**
 * Utility class for getting information about the build queue and ongoing builds.
 */
public final class QueueUtils {

    /**
     * Hiding utility class constructor.
     */
    private QueueUtils() { }

   /**
     * Returns the set of queue ids for items that are in the build queue.
     * Depending on the configuration this is either just those that have a completed upstream
     * project if they are a project build or all entries that are currently in the queue.
     * To permit always pipeline steps, the owner task is checked if it is
     * a WorkflowJob.
     * Note: This method locks the queue; don't use excessively.
     * @return set of item ids
     */
    public static Set<Long> getPermittedQueueItemIds() {
        Set<Long> queuedIds = new HashSet<Long>();
        boolean allowAllQueuedItems = SafeQuietdownConfiguration.get().isAllowAllQueuedItems();
        for (Queue.Item item : Queue.getInstance().getItems()) {
            if (item.task instanceof Job) {
                if (allowAllQueuedItems) {
                    queuedIds.add(item.getId());
                } else {
                    // Add item if it has an upstream build that is finished building
                    for (Run upstreamRun : getUpstreamRuns(item)) {
                        if (!upstreamRun.isBuilding()) {
                            queuedIds.add(item.getId());
                            break;
                        }
                    }
                    // Add item if it has an owner task that is WorkflowJob
                    if ((item.task.getOwnerTask() != item.task)
                        && (item.task.getOwnerTask() instanceof WorkflowJob)) {
                        queuedIds.add(item.getId());
                    }
                }
            }
        }
        return Collections.unmodifiableSet(queuedIds);
    }

    /**
     * Gets all upstream runs that triggered the argument queue item.
     * @param item the queue item to find upstream builds for
     * @return set of upstream builds
     */
    public static Set<Run> getUpstreamRuns(final Queue.Item item) {
        Set<Run> upstreamRuns = new HashSet<Run>();
        for (Cause cause : item.getCauses()) {
            if (cause instanceof Cause.UpstreamCause) {
                Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) cause;
                Run<?, ?> upstreamRun = upstreamCause.getUpstreamRun();

                if (upstreamRun != null) {
                    upstreamRuns.add(upstreamRun);
                }
            }
        }
        return Collections.unmodifiableSet(upstreamRuns);
    }

   /**
     * Return a set of queue ids of all currently running builds.
     *
     * @return set of running queue ids
     */
    public static Set<Long> getRunningQueueItemIds() {
        Set<Long> runningProjects = new HashSet<Long>();

        List<Node> allNodes = new ArrayList<Node>(Jenkins.get().getNodes());
        allNodes.add(Jenkins.get());

        for (Node node : allNodes) {
            runningProjects.addAll(getRunningQueueItemIds(node.getNodeName()));
        }
        return Collections.unmodifiableSet(runningProjects);
    }

    /**
     * Returns a set of queue ids of all currently running builds on a node.
     *
     * @param nodeName the node name to list running projects for
     * @return set of queue ids
     */
    public static Set<Long> getRunningQueueItemIds(final String nodeName) {
        Set<Long> runningProjects = new HashSet<Long>();

        Node node = Jenkins.get().getNode(nodeName);
        if (nodeName.isEmpty()) { // Special case when building on master
            node = Jenkins.get();
        }

        if (node != null) {
            Computer computer = node.toComputer();
            if (computer != null) {
                List<Executor> executors = new ArrayList<Executor>(computer.getExecutors());
                executors.addAll(computer.getOneOffExecutors());

                for (Executor executor : executors) {
                    Queue.Executable executable = executor.getCurrentExecutable();
                    if (executable instanceof Run) {
                        Run run = (Run) executable;
                        runningProjects.add(run.getQueueId());
                    }
                }
            }
        }

        return Collections.unmodifiableSet(runningProjects);
    }

    /**
     * Gets the queue ids of all upstream projects that triggered argument queue item.
     * @param item the queue item to find upstream projects for
     * @return set of upstream queue ids
     */
    public static Set<Long> getUpstreamQueueIds(final Queue.Item item) {
        Set<Long> upstreamQueueIds = new HashSet<Long>();
        for (Cause cause : item.getCauses()) {
            if (cause instanceof Cause.UpstreamCause) {
                Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) cause;
                Run<?, ?> run = upstreamCause.getUpstreamRun();

                if (run != null) {
                    upstreamQueueIds.add(run.getQueueId());
                }
            }
        }
        return Collections.unmodifiableSet(upstreamQueueIds);
    }

    /**
     * Gets the queue ids of all owner tasks that are found in the queue.
     * @param item the queue item to find owner queue ids for
     * @return set of upstream queue ids
     */
    public static Set<Long> getOwnerQueueIds(final Queue.Item item) {
        Set<Long> ownerQueueIds = new HashSet<Long>();
        Queue.Task childTask = item.task;
        while (childTask != null) {
            Queue.Task ownerTask = childTask.getOwnerTask();
            if ((ownerTask != null) && (ownerTask != childTask)) {
                for (Queue.Item ownerItem : Queue.getInstance().getItems(ownerTask)) {
                    ownerQueueIds.add(ownerItem.getId());
                }
                childTask = ownerTask;
            } else {
                childTask = null;
            }
        }
        return ownerQueueIds;
    }

    /**
     * Get the total number of buildable queue items.
     * @return the total number of buildable queue items.
     */
    public static int getNumberOfBuildableQueueItems() {
        Queue queue = Queue.getInstance();
        queue.maintain();
        int numberOfBuildableItems = queue.getPendingItems().size();
        numberOfBuildableItems += queue.getBuildableItems().size();

        // Actually, we don't know whether a waiting item is buildable.
        // But we count it as buildable...
        for (Queue.Item item : Queue.getInstance().getItems()) {
            if (item instanceof Queue.WaitingItem) {
                ++numberOfBuildableItems;
            }
        }

        return numberOfBuildableItems;
    }

    /**
     * Get the total number of active builds.
     * @return the total number of active builds.
     */
    public static int getNumberOfActiveBuilds() {
        int numActiveBuilds = 0;
        List<Node> allNodes = new ArrayList<Node>(Jenkins.get().getNodes());
        allNodes.add(Jenkins.get());

        for (Node node : allNodes) {
            Computer computer = node.toComputer();
            if (computer != null) {
                List<Executor> executors = new ArrayList<Executor>(computer.getExecutors());
                executors.addAll(computer.getOneOffExecutors());

                for (Executor executor : executors) {
                    Queue.Executable executable = executor.getCurrentExecutable();
                    if (executable instanceof Run) {
                        ++numActiveBuilds;
                    }
                }
            }
        }

        return numActiveBuilds;
    }
}
