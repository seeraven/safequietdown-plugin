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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.CollectionUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import jenkins.security.SecurityContextExecutorService;

/**
 * Adds a link on the manage Jenkins page for safe quietdown.
 */
@Extension
public class SafeQuietdownManagementLink extends ManagementLink {

    private static final Logger LOGGER = Logger.getLogger(BuildPreventer.class.getName());

    /** @return the singleton instance */
    public static SafeQuietdownManagementLink get() {
        return ExtensionList.lookupSingleton(SafeQuietdownManagementLink.class);
    }

    /** URL to this plugin to activate it. */
    private static final String URL_ACTIVATE = "safequietdown";

    /** URL to this plugin to deactivate it. */
    private static final String URL_CANCEL = "cancelsafequietdown";

    /** Icon used for the link. */
    private static final String ICON = "system-log-out.png";

    /**
     * The list of queue ids, that belong to projects that where running at time of lenient shutdown
     * and any of the downstream builds.
     */
    private Set<Long> permittedQueueIds = Collections.synchronizedSet(new HashSet<Long>());

    /** Flag indicating whether we are currently in the quietdown mode. */
    private boolean isQuietdownActive = false;

    /**
     * Gets the icon for this plugin.
     * @return the icon
     */
    @Override
    public String getIconFileName() {
        return ICON;
    }

    /**
     * Gets the display name for this plugin on the management page.
     * Varies depending on if safe quietdown is active or not.
     * @return display name
     */
    @Override
    public String getDisplayName() {
        if (isQuietdownActive()) {
            return Messages.DeactivateSafeQuietdownTitle();
        }
        return Messages.ActivateSafeQuietdownTitle();
    }

    /**
     * Gets the url name for this plugin.
     * @return url name
     */
    @Override
    public String getUrlName() {
        if (isQuietdownActive()) {
            return URL_CANCEL;
        } else {
            return URL_ACTIVATE;
        }
    }

    /**
     * Gets the description of this plugin.
     * Varies depending on if safe quietdown is active or not.
     * @return description
     */
    @Override
    public String getDescription() {
        String description = null;
        if (!isQuietdownActive()) {
            description = Messages.Description();
        }
        return description;
    }

    /**
     * Returns required permission to activate or deactivate the safe quietdown.
     * @return Jenkins administer permission.
     */
    @Override
    public Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    /**
     * Checks if quietdown mode is activated.
     * @return true if Jenkins is in safe quietdown mode, otherwise false
     */
    public boolean isQuietdownActive() {
        return isQuietdownActive;
    }

    /**
     * Toggle the safe quietdown state.
     */
    public void toggleQuietdown() {
        isQuietdownActive = !isQuietdownActive;
    }

   /**
     * Method triggered when pressing the management link.
     * Toggles the safe quietdown mode.
     *
     * @param req StaplerRequest
     * @param rsp StaplerResponse
     * @throws IOException if unable to redirect
     */
    public synchronized void doIndex(final StaplerRequest req,
                                     final StaplerResponse rsp) throws IOException {
        Jenkins.get().checkPermission(getRequiredPermission());

        performToggleQuietdown();
        rsp.sendRedirect2(req.getContextPath() + "/manage");
    }

   /**
     * Toggles the flag and prepares for safe quietdown if needed.
     *
     */
    public void performToggleQuietdown() {
        toggleQuietdown();
        if (isQuietdownActive()) {
            ExecutorService service = new SecurityContextExecutorService(Executors.newSingleThreadExecutor());
            service.submit(new Runnable() {
                @Override
                public void run() {
                    permittedQueueIds.clear();
                    permittedQueueIds.addAll(QueueUtils.getPermittedQueueItemIds());
                    permittedQueueIds.addAll(QueueUtils.getRunningQueueItemIds());
                    LOGGER.log(Level.FINE, "Activated safe quiet mode. "
                               + "The following queue item ids are permitted to continue:");
                    for (long id : permittedQueueIds) {
                        LOGGER.log(Level.FINE, "  - {0}", id);
                    }
                }
            });
        }
    }

    /**
     * Adds a queue id to the set of permitted upstream queue ids.
     *
     * @param id the queue id to add to white list
     */
    public void addPermittedQueueId(final long id) {
        permittedQueueIds.add(id);
    }

   /**
     * Returns true if id is a permitted queue id.
     *
     * @param id the queue item id to check for
     * @return true if it was queued
     */
    public boolean isPermittedQueueId(final long id) {
        return permittedQueueIds.contains(id);
    }

    /**
     * Checks if any of the queue ids in argument list is in the list of permitted queue ids.
     *
     * @param queueIds the list of queue ids to check
     * @return true if at least one of the projects is white listed
     */
    public boolean isAnyPermittedQueueId(final Set<Long> queueIds) {
        Collection<?> intersection = CollectionUtils.intersection(queueIds, permittedQueueIds);
        return !intersection.isEmpty();
    }
}
