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

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsSessionRule;

public class SafeQuietdownManagementLinkTest {

    /** The test sessions. */
    @Rule
    public JenkinsSessionRule sessions = new JenkinsSessionRule();

    /** XPath to the icon on the manage page. */
    private static final String ACTIVATION_IMG_XPATH = "//a[@href='safequietdown']"
        + "/img[contains(@src,'system-log-out.png')]";

    /** XPath to the link. */
    private static final String ACTIVATION_LINK_XPATH = "//a[@href='safequietdown']";

    /** XPath to the icon on the manage page. */
    private static final String CANCEL_IMG_XPATH = "//a[@href='cancelsafequietdown']"
        + "/img[contains(@src,'system-log-out.png')]";

    /** XPath to the link. */
    private static final String CANCEL_LINK_XPATH = "//a[@href='cancelsafequietdown']";

    /** XPath to the decorator message. */
    private static final String DECORATOR_XPATH = "//div[@id='safe-quietdown-msg']";

    /**
     * Test the quietdown management link and decorator.
     */
    @Test
    public void testQuietdownManagementLinks() throws Throwable {
        sessions.then(r -> {
            HtmlPage managePage = r.createWebClient().goTo("manage");

            assertNull("decorator for cancel found", managePage.getFirstByXPath(DECORATOR_XPATH));
            assertNotNull("link for activation not found", managePage.getFirstByXPath(ACTIVATION_LINK_XPATH));
            assertNotNull("img for activation not found", managePage.getFirstByXPath(ACTIVATION_IMG_XPATH));

            r.createWebClient().goTo("safequietdown");
            managePage = r.createWebClient().goTo("manage");
            assertNotNull("decorator for cancel not found", managePage.getFirstByXPath(DECORATOR_XPATH));
            assertNotNull("link for cancel not found", managePage.getFirstByXPath(CANCEL_LINK_XPATH));
            assertNotNull("img for cancel not found", managePage.getFirstByXPath(CANCEL_IMG_XPATH));
        });
    }

    /**
     * Test the addPermittedQueueId(), isPermittedQueueId() and
     * isAnyPermittedQueueId() methods.
     */
    @Test
    public void testPermittedQueueIdManagement() throws Throwable {
        SafeQuietdownManagementLink managementLink = new SafeQuietdownManagementLink();
        assertFalse(managementLink.isPermittedQueueId(1L));

        managementLink.addPermittedQueueId(1);
        assertTrue(managementLink.isPermittedQueueId(1L));

        Set<Long> queueIds = new HashSet<Long>();
        queueIds.add(2L);
        assertFalse(managementLink.isAnyPermittedQueueId(queueIds));

        queueIds.add(1L);
        assertTrue(managementLink.isAnyPermittedQueueId(queueIds));
    }
}
