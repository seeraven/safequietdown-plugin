/*
 *  The MIT License
 *
 *  Copyright (c) 2014 Sony Mobile Communications Inc. All rights reserved.
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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;

/**
 * Prevents builds from running when lenient shutdown mode is active.
 */
@Extension
public class BuildPreventer extends QueueTaskDispatcher {

    private static final Logger LOGGER = Logger.getLogger(BuildPreventer.class.getName());

    /**
     * Handles prevention of builds for safe quietdown on the Jenkins master.
     * @param item QueueItem to build
     * @return CauseOfBlockage if a build is prevented, otherwise null
     */
    @Override
    public CauseOfBlockage canRun(final Queue.Item item) {
        CauseOfBlockage blockage = null;

        SafeQuietdownManagementLink managementLink = SafeQuietdownManagementLink.get();

        if (managementLink.isQuietdownActive()) {
            if (!managementLink.isPermittedQueueId(item.getId())) {
                Set<Long> upstreamQueueIds = QueueUtils.getUpstreamQueueIds(item);

                if (managementLink.isAnyPermittedQueueId(upstreamQueueIds)) {
                    managementLink.addPermittedQueueId(item.getId());
                    LOGGER.log(Level.FINE, "Allowing downstream project {0} with queue id {1}.",
                               new Object[] {item.task.getFullDisplayName(), item.getId()});
                } else {
                    if ((item.task.getOwnerTask() != item.task)
                        && (item.task.getOwnerTask() instanceof WorkflowJob)) {
                        managementLink.addPermittedQueueId(item.getId());
                        LOGGER.log(Level.FINE, "Allowing pipeline step {0} with queue id {1}.",
                                   new Object[] {item.task.getFullDisplayName(), item.getId()});
                    } else {
                        LOGGER.log(Level.FINE, "Preventing project {0} from running during safe quiet down mode.",
                                   item.task.getFullDisplayName());
                        LOGGER.log(Level.FINE, "Its queue id is {0} and its upstream queue ids are:", item.getId());
                        for (long id : upstreamQueueIds) {
                            LOGGER.log(Level.FINE, " - {0}", id);
                        }
                        for (Cause cause : item.getCauses()) {
                            LOGGER.log(Level.FINE, "Cause: {0}", cause.getClass().getName());
                        }
                        LOGGER.log(Level.FINE, "The Queue.Item class is: {0}", item.getClass().getName());
                        LOGGER.log(Level.FINE, "The Queue.Task class is: {0}", item.task.getClass().getName());
                        if (item.task.getOwnerTask() != item.task) {
                            LOGGER.log(Level.FINE, "The Owner class is:      {0}",
                                       item.task.getOwnerTask().getClass().getName());
                        }

                        blockage = new SafeQuietdownBlockage();
                    }
                }
            }
        }

        return blockage;
    }
}
