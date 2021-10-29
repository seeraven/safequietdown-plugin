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

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * The global configuration of the safequietdown plugin.
 */
@Extension
public class SafeQuietdownConfiguration extends GlobalConfiguration {

    /** @return the singleton instance */
    public static SafeQuietdownConfiguration get() {
        return ExtensionList.lookupSingleton(SafeQuietdownConfiguration.class);
    }

    private String quietdownMessage = Messages.GoingToShutDown();
    private boolean allowAllQueuedItems;

    /**
     * Constructor of the class SafeQuietdownConfiguration.
     * Loads the configuration.
     */
    public SafeQuietdownConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    /**
     * Gets the quietdown message to be displayed in header.
     *
     * @return message to display in header
     */
    public String getQuietdownMessage() {
        return quietdownMessage;
    }

    /**
     * Checks if all queued items are allowed to finish when safe quietdown mode
     * is enabled.
     *
     * @return true if all queued itmes will build, false otherwise
     */
    public boolean isAllowAllQueuedItems() {
        return allowAllQueuedItems;
    }

    /**
     * Sets the quietdown message to be displayed in header.
     *
     * @param quietdownMessage message to display in header
     */
    @DataBoundSetter
    public void setQuietdownMessage(final String quietdownMessage) {
        this.quietdownMessage = quietdownMessage;
        save();
    }

    /**
     * Sets the flag if all queued items are allowed to finish or not.
     *
     * @param allowAllQueuedItems true - enabled, false - disabled
     */
    @DataBoundSetter
    public void setAllowAllQueuedItems(final boolean allowAllQueuedItems) {
        this.allowAllQueuedItems = allowAllQueuedItems;
        save();
    }

    /**
     * Perform the form validation of the given value.
     * @param value the value to check.
     * @return the form validation result.
     */
    public FormValidation doCheckLabel(final @QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a label.");
        }
        return FormValidation.ok();
    }

}
