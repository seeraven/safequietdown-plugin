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

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlTextInput;
import org.htmlunit.html.HtmlCheckBoxInput;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsSessionRule;

public class SafeQuietdownConfigurationTest {

    /** The test sessions. */
    @Rule
    public JenkinsSessionRule sessions = new JenkinsSessionRule();

    /**
     * Test the quietdown message configuration setting.
     */
    @Test
    public void testQuietdownMessageSetting() throws Throwable {
        sessions.then(r -> {
            assertEquals("default value initially", Messages.GoingToShutDown(),
                         SafeQuietdownConfiguration.get().getQuietdownMessage());

            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput textbox = config.getInputByName("_.quietdownMessage");
            textbox.setText("hello");
            r.submit(config);
            assertEquals("global config page let us edit it", "hello",
                         SafeQuietdownConfiguration.get().getQuietdownMessage());
        });
        sessions.then(r -> {
            assertEquals("still there after restart of Jenkins", "hello",
                         SafeQuietdownConfiguration.get().getQuietdownMessage());
        });
    }

    /**
     * Test the allow all queued items configuration setting.
     */
    @Test
    public void testAllowAllQueuedItemsSetting() throws Throwable {
        sessions.then(r -> {
            assertEquals("default value initially", false,
                         SafeQuietdownConfiguration.get().isAllowAllQueuedItems());

            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlCheckBoxInput checkbox = config.getInputByName("_.allowAllQueuedItems");
            checkbox.setChecked(true);
            r.submit(config);
            assertEquals("global config page let us edit it", true,
                         SafeQuietdownConfiguration.get().isAllowAllQueuedItems());
        });
        sessions.then(r -> {
            assertEquals("still there after restart of Jenkins", true,
                         SafeQuietdownConfiguration.get().isAllowAllQueuedItems());
        });
    }
}
