/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ian Stewart-Binks (Red Hat, Inc.) - Bug 471095
 *     Etienne Studer & Donát Csikós (Gradle Inc.) - Bug 471095
 */

package org.eclipse.buildship.ui.test.fixtures

import spock.lang.Specification

import org.eclipse.core.runtime.jobs.Job
import org.eclipse.swt.widgets.Display
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable
import org.eclipse.swtbot.swt.finder.results.VoidResult
import org.eclipse.swtbot.swt.finder.results.BoolResult
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell
import org.eclipse.ui.PlatformUI

import org.eclipse.buildship.ui.UiPlugin

abstract class SwtBotSpecification extends Specification {

    protected static SWTWorkbenchBot bot = new SWTWorkbenchBot()

    def setupSpec() {
        closeWelcomePageIfAny()
    }

    def setup() {
        closeAllShellsExceptTheApplicationShellAndForceShellActivation()
    }

    private static void closeWelcomePageIfAny() {
        try {
            SWTBotView view = bot.activeView()
            if (view.getTitle().equals("Welcome")) {
                view.close()
            }
        } catch (WidgetNotFoundException e) {
            UiPlugin.logger().info('No active view', e)
        }
    }

    private static void closeAllShellsExceptTheApplicationShellAndForceShellActivation() {
        // in case a UI test fails some shells might not be closed properly, therefore we close
        // these here and log it
        SWTBotShell[] shells = bot.shells()
        for (SWTBotShell swtBotShell : shells) {
            if (swtBotShell.isOpen() && !isEclipseApplicationShell(swtBotShell)) {
                bot.captureScreenshot(swtBotShell.getText() + " NotClosed.jpg")
                UiPlugin.logger().warn(swtBotShell.getText() + " was not closed properly.")
                swtBotShell.close()
            }
        }

        // http://wiki.eclipse.org/SWTBot/Troubleshooting#No_active_Shell_when_running_SWTBot_tests_in_Xvfb
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().forceActive()
            }
        })
    }

    protected static boolean isEclipseApplicationShell(final SWTBotShell swtBotShell) {
        return UIThreadRunnable.syncExec(new BoolResult() {
            @Override
            public Boolean run() {
                return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().equals(swtBotShell.widget)
            }
        })
    }

    protected static void waitForJobsToFinish() {
        while (!Job.getJobManager().isIdle()) {
            delay(100)
        }
    }

    protected static void delay(long waitTimeMillis) {
        Display display = Display.getCurrent()
        if (display != null) {
            long endTimeMillis = System.currentTimeMillis() + waitTimeMillis
            while (System.currentTimeMillis() < endTimeMillis) {
                if (!display.readAndDispatch()) {
                    display.sleep()
                }
            }
            display.update()
        } else {
            try {
                Thread.sleep(waitTimeMillis)
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

}