package net.fornwall.eclipsecoder.archive;

import java.net.UnknownHostException;
import java.util.List;

import javax.security.auth.login.LoginException;

import net.fornwall.eclipsecoder.languages.LanguageSupport;
import net.fornwall.eclipsecoder.languages.LanguageSupportFactory;
import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.stats.ProblemStatement;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A job to fetch a problem description from the online TopCoder problem
 * archive.
 */
public class ProblemFetcherJob extends Job {

	private ProblemStats stats;

	public ProblemFetcherJob(ProblemStats stats) {
		super(Messages.checkingOutProblem);
		this.stats = stats;
		setUser(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(Messages.checkingOutProblem, 100);
		try {
			String language = EclipseCoderPlugin.getDefault().getPreferenceStore()
					.getString(EclipseCoderPlugin.PREFERENCE_LANGUAGE);
			List<String> availabe = LanguageSupportFactory.supportedLanguages();
			if (language == null || !availabe.contains(language)) {

				if (availabe.isEmpty()) {
					return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK,
							Messages.noLanguageSupportFound, null);
				}

				// fall back to first available if it exists
				language = availabe.get(0);
			}

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;

			final String finalLanguage = language;
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
			monitor.subTask(Messages.downloadingProblem);
			String html = scraper.getHtmlProblemStatement(stats);

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			monitor.worked(30);
			monitor.subTask(Messages.downloadingTestCases);
			List<ProblemScraper.StringPair> testCasesStrings = scraper.getExamples(stats);

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			monitor.worked(15);
			monitor.subTask(Messages.parsingProblem);
			final ProblemStatement problemStatement = ProblemParser.parseProblem(html, testCasesStrings);

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			monitor.worked(15);
			monitor.subTask(Messages.creatingLanguageSupport);
			final LanguageSupport languageSupport = LanguageSupportFactory.createLanguageSupport(finalLanguage);

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;

			Utilities.runInDisplayThread(new Runnable() {
				@Override
				public void run() {
					languageSupport.createProject(problemStatement).openSourceFileInEditor();
				}
			});

			return Status.OK_STATUS;
		} catch (Exception exc) {
			Utilities.showException(exc);
			return Status.CANCEL_STATUS;
		}
	}

}
