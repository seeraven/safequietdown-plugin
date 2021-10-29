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
import hudson.model.PageDecorator;
import jenkins.model.Jenkins;

/**
 * Adds a header about the safe quietdown mode when it's active.
 */
@Extension
public class SafeQuietdownDecorator extends PageDecorator {

    /**
     * The singleton instance registered in the Jenkins extension list.
     * @return the instance.
     */
    public static SafeQuietdownDecorator getInstance() {
        ExtensionList<SafeQuietdownDecorator> list = Jenkins.get().getExtensionList(SafeQuietdownDecorator.class);
        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            throw new IllegalStateException("Extensions are not loaded yet.");
        }
    }

   /**
     * Gets the quietdown message to be displayed in header.
     * @return message to display in header
     */
    public String getQuietdownMessage() {
        return SafeQuietdownConfiguration.get().getQuietdownMessage();
    }

   /**
     * Checks if the safe quietdown mode is active.
     * @return true if the safe quietdown mode is active
     */
    public boolean isQuietdownActive() {
        return SafeQuietdownManagementLink.get().isQuietdownActive();
    }
}
