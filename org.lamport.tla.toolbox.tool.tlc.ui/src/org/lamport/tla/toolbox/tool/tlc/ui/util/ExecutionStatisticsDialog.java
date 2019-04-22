/*******************************************************************************
 * Copyright (c) 2019 Microsoft Research. All rights reserved. 
 *
 * The MIT License (MIT)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 ******************************************************************************/
package org.lamport.tla.toolbox.tool.tlc.ui.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.lamport.tla.toolbox.tool.tlc.ui.TLCUIActivator;

import util.ExecutionStatisticsCollector;
import util.ExecutionStatisticsCollector.Selection;

public class ExecutionStatisticsDialog extends MessageDialog {

	private static final String KEY = "BUTTON_KEY";

	public ExecutionStatisticsDialog(final Shell parentShell) {
		super(parentShell, "TLA+ execution statistics", (Image) null, "The TLA+ project needs your help!",
				MessageDialog.QUESTION, new String[0], 0);
		
		// Do not block the Toolbox's main window.
		setShellStyle(getShellStyle() ^ SWT.APPLICATION_MODAL | SWT.MODELESS);
		setBlockOnOpen(false);
	}

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.MessageDialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
	protected void createButtonsForButtonBar(Composite parent) {
        final Button[] buttons = new Button[3];
        buttons[0] = createButton(parent, 0, "&Always Share\nExecution Statistics", false);
        buttons[0].setData(KEY, ExecutionStatisticsCollector.Selection.ON);
        buttons[1] = createButton(parent, 1, "Share Without\n&Installation Identifier", false);
        buttons[1].setData(KEY, ExecutionStatisticsCollector.Selection.RANDOM_IDENTIFIER);
        buttons[2] = createButton(parent, 2, "&Never Share\nExecution Statistics", false);
        buttons[2].setData(KEY, ExecutionStatisticsCollector.Selection.NO_ESC);
        buttons[2].setFocus();
        setButtons(buttons);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
    protected Control createCustomArea(Composite parent) {
		final Composite c = new Composite(parent, SWT.BORDER);
		c.setLayout(new GridLayout());

		final String txt = ("Please opt-in and share (TLC) execution statistics to help us make informed decisions\n"
				+ "about future research and development directions. Execution statistics contain the\n"
				+ "following information:\n\n"
				+ "• Total number of cores and cores assigned to TLC\n"
				+ "• Heap and off-heap memory allocated to TLC\n"
				+ "• TLC's version (git commit SHA)\n"
				+ "• If breadth-first search, depth-first search or simulation mode is active\n"
				+ "• TLC's implementation for the sets of seen and unseen states\n"
				+ "• If TLC has been launched from the TLA Toolbox\n"
				+ "• Name, version, and architecture of your operating system\n"
				+ "• Vendor, version, and architecture of your Java virtual machine\n"
				+ "• The current date\n"
				+ "• An installation identifier which allows us to group execution statistics\n\n"
				+ "TLC will report execution statistics in the background during startup. It will not\n"
				+ "slow down model checking.\n\n"
				+ "The execution statistics do not contain personal information. If you wish to revoke\n"
				+ "your consent to share execution statistics, please delete the file\n"
				+ "\t" + ExecutionStatisticsCollector.PATH + "\n"
				+ "and chose “Never Share Execution Statistics” during the next Toolbox start.");
		
		final StyledText st = new StyledText(c, SWT.SHADOW_NONE | SWT.WRAP);
		st.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		st.setEnabled(true);
		st.setEditable(false);
		st.setText(txt);
		
		final StyleRange[] ranges = new StyleRange[3];
		ranges[0] = new StyleRange(txt.indexOf("(TLC) execution statistics"), "(TLC) execution statistics".length(), null, null);
		ranges[0].underline = true;
		ranges[0].underlineStyle = SWT.UNDERLINE_LINK;
		ranges[0].data = "https://exec-stats.tlapl.us";
				
		ranges[1] = new StyleRange(txt.indexOf("git commit SHA"), "git commit SHA".length(), null, null);
		ranges[1].underline = true;
		ranges[1].underlineStyle = SWT.UNDERLINE_LINK;
		ranges[1].data = "https://git-scm.com/book/en/v2/Git-Internals-Git-Objects";

		ranges[2] = new StyleRange(txt.indexOf(ExecutionStatisticsCollector.PATH), ExecutionStatisticsCollector.PATH.length(), null, null);
		ranges[2].underline = true;
		ranges[2].underlineStyle = SWT.UNDERLINE_SINGLE;

		st.setStyleRanges(ranges);
		st.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(final MouseEvent event) {
                final StyleRange style = st.getStyleRangeAtOffset(st.getOffsetAtPoint(new Point (event.x, event.y)));
                if (style != null && style.underline && style.underlineStyle == SWT.UNDERLINE_LINK && style.data instanceof String) {
                    try {
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL((String) style.data));
					} catch (PartInitException | MalformedURLException notExpectedToHappen) {
						notExpectedToHappen.printStackTrace();
					}
                }
			}
		});
		
        return c;
    }

	@Override
	protected void buttonPressed(final int buttonId) {
		final ExecutionStatisticsCollector.Selection col = (Selection) getButton(buttonId).getData(KEY);
		final Job j = new Job("Write Execution Statistics Preference.") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ExecutionStatisticsCollector.set(col);
				} catch (IOException e) {
					return new Status(ERROR, TLCUIActivator.PLUGIN_ID, e.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();

		super.buttonPressed(buttonId);
	}
}
