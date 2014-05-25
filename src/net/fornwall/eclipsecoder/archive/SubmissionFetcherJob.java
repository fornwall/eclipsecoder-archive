package net.fornwall.eclipsecoder.archive;

import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A job to retrieve a specified submission to a TopCoder problem from the
 * online archive and display it in an editor.
 */
public class SubmissionFetcherJob extends Job {

	Submission submission;

	private int roundId;

	private int problemId;

	public SubmissionFetcherJob(Submission submission, int roundId, int problemId) {
		super(Messages.retrievingSubmission);
		this.submission = submission;
		this.roundId = roundId;
		this.problemId = problemId;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(Messages.retrievingSubmission, 100);
		try {
			monitor.subTask(Messages.loggingIn);
			ProblemScraper scraper = new ProblemScraper(EclipseCoderPlugin.tcUserName(),
					EclipseCoderPlugin.tcPassword());
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			monitor.worked(20);
			final String submissionString = scraper.getSubmission(submission.coderId, roundId, problemId, monitor);
			if (submissionString == null) {
				return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK,
						Messages.submissionNotAvailable, null);
			}

			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			monitor.subTask(Messages.openingEditor);
			Utilities.runInDisplayThread(new Runnable() {

				public void run() {
					String fileExtension = "txt"; //$NON-NLS-1$
					if (submission.language.equals("Java")) { //$NON-NLS-1$
						fileExtension = "java"; //$NON-NLS-1$
					} else if (submission.language.equals("C++")) { //$NON-NLS-1$
						fileExtension = "cpp"; //$NON-NLS-1$
					} else if (submission.language.equals("C#")) { //$NON-NLS-1$
						fileExtension = "cs"; //$NON-NLS-1$
					} else if (submission.language.equals("VB")) { //$NON-NLS-1$
						fileExtension = "vb"; //$NON-NLS-1$
					}

					EditorOpener.openEditor(submissionString, submission.coderHandle + Messages.sSubmission,
							submission.coderHandle + Messages.sSubmissionToProblem, fileExtension);
				}

			});
			return Status.OK_STATUS;
		} catch (Exception e1) {
			return new Status(IStatus.WARNING, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK,
					Messages.failedRetrievingSubmission + e1.getMessage(), e1);
		}
	}

}
