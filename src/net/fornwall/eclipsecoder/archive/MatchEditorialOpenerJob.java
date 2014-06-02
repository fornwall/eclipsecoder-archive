package net.fornwall.eclipsecoder.archive;

import java.net.URL;
import java.net.UnknownHostException;

import javax.security.auth.login.LoginException;

import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class MatchEditorialOpenerJob extends Job {

	private final ProblemStats stats;

	public MatchEditorialOpenerJob(ProblemStats stats) {
		super(Messages.checkingOutProblem);
		this.stats = stats;
		setUser(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(Messages.checkingOutProblem, 100);
		try {
			monitor.subTask(Messages.loggingIntoProblemArchive);
			monitor.worked(10);
			ProblemScraper scraper = null;
			try {
				scraper = new ProblemScraper(EclipseCoderPlugin.tcUserName(), EclipseCoderPlugin.tcPassword());
			} catch (LoginException loginProblem) {
				return ProblemScraper.createLoginFailedStatus(loginProblem);
			} catch (UnknownHostException e) {
				return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK,
						Messages.unableToConnectToTopCoder, e);
			}
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			monitor.worked(20);
			monitor.subTask(Messages.downloadingPage);

			final String url = scraper.getEditorialUrl(stats);
			if (url == null) {
				Utilities.showMessageDialog(Messages.noMatchEditorialFound, Messages.noMatchEditorialFoundDescription);
			} else {
				Utilities.runInDisplayThread(new Runnable() {
					@Override
					public void run() {
						try {
							PlatformUI
									.getWorkbench()
									.getBrowserSupport()
									.createBrowser(IWorkbenchBrowserSupport.AS_VIEW,
											ArchiveListView.class.getCanonicalName(), "", "").openURL( //$NON-NLS-1$ //$NON-NLS-2$
											new URL(url));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
			}
			return Status.OK_STATUS;
		} catch (Exception exc) {
			Utilities.showException(exc);
			return Status.CANCEL_STATUS;
		}
	}
}
