package net.fornwall.eclipsecoder.archive;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A job to fetch a list of submissions for a specified TopCoder problem.
 */
public class SubmissionListFetcherJob extends Job {

	private static final String[] LEVEL_NAMES = { "PADDING", Messages.columnNameOne, Messages.columnNameTwo, //$NON-NLS-1$
			Messages.columnNameThree };

	ProblemStats stats;

	public SubmissionListFetcherJob(ProblemStats stats) {
		super(Messages.retrievingSubmissionList);
		this.stats = stats;
		setUser(true);
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask(Messages.retrievingSubmissionList, 100);

		final List<Submission> solutionList = new ArrayList<Submission>();
		final List<Integer> divisions = new ArrayList<Integer>();
		final List<String> levelStrings = new ArrayList<String>();
		final List<String> pointsStrings = new ArrayList<String>();
		final List<String> languageStrings = new ArrayList<String>();

		// rough approximation used for progress monitor
		final int expected = (stats.div1Level > 0 && stats.div2Level > 0) ? 1400 : 700;

		if (stats.div1Level != -1) {
			divisions.add(1);
			levelStrings.add("level_" + LEVEL_NAMES[stats.div1Level] //$NON-NLS-1$
					+ "_status"); //$NON-NLS-1$
			pointsStrings.add("level_" + LEVEL_NAMES[stats.div1Level] //$NON-NLS-1$
					+ "_final_points"); //$NON-NLS-1$
			languageStrings.add("level_" + LEVEL_NAMES[stats.div1Level] //$NON-NLS-1$
					+ "_language"); //$NON-NLS-1$
		}
		if (stats.div2Level != -1) {
			divisions.add(2);
			levelStrings.add("level_" + LEVEL_NAMES[stats.div2Level] //$NON-NLS-1$
					+ "_status"); //$NON-NLS-1$
			pointsStrings.add("level_" + LEVEL_NAMES[stats.div2Level] //$NON-NLS-1$
					+ "_final_points"); //$NON-NLS-1$
			languageStrings.add("level_" + LEVEL_NAMES[stats.div2Level] //$NON-NLS-1$
					+ "_language"); //$NON-NLS-1$

		}

		try {
			monitor.subTask(Messages.openingConnection);
			URL url = new URL("http://www.topcoder.com/tc?module=BasicData&c=dd_round_results&rd=" //$NON-NLS-1$
					+ stats.roundId);
			URLConnection connection = url.openConnection();
			monitor.worked(3);

			monitor.subTask(Messages.creatingXmlParser);
			XMLReader parser = XMLReaderFactory.createXMLReader();
			parser.setContentHandler(new DefaultHandler() {
				private int count;

				private String currentElement;

				private Map<String, String> map = new HashMap<String, String>();

				@Override
				public void characters(char[] ch, int start, int length) throws SAXException {
					if (currentElement == null || currentElement.equals("dd_round_results") //$NON-NLS-1$
							|| currentElement.equals("row")) { //$NON-NLS-1$
						return;
					}
					map.put(currentElement, new String(ch, start, length));
				}

				@Override
				public void endElement(String uri, String localName, String name) throws SAXException {
					if (monitor.isCanceled()) {
						throw new CanceledException();
					}
					for (int levelIndex = 0; levelIndex < levelStrings.size(); levelIndex++) {
						Integer division = divisions.get(levelIndex);
						if (localName.equals("row") //$NON-NLS-1$
								&& division.toString().equals(map.get("division")) //$NON-NLS-1$
								&& ("Passed System Test".equals(map //$NON-NLS-1$
										.get(levelStrings.get(levelIndex))))) {
							count++;
							if (count * 18 >= expected) {
								count = 0;
								monitor.worked(5);
							}

							Submission s = new Submission(Integer.parseInt(map.get("coder_id")), map.get("handle"), //$NON-NLS-1$ //$NON-NLS-2$
									Integer.parseInt(map.get("new_rating")), //$NON-NLS-1$
									Integer.parseInt(map.get(pointsStrings.get(levelIndex)).replace(".", "")), map //$NON-NLS-1$ //$NON-NLS-2$
											.get(languageStrings.get(levelIndex)), division);
							solutionList.add(s);
						}
					}
				}

				@Override
				public void startElement(String uri, String localName, String name, Attributes atts)
						throws SAXException {
					currentElement = localName;
				}
			});

			monitor.worked(2);
			monitor.subTask(Messages.parsingDataFeed);

			try {
				parser.parse(new InputSource(connection.getInputStream()));
			} catch (CanceledException e) {
				return Status.CANCEL_STATUS;
			}

			monitor.subTask(Messages.updatingTable);
			Utilities.runInDisplayThread(new Runnable() {
				@Override
				public void run() {
					SubmissionListView.showSolutions(solutionList, stats);
				}
			});
			return Status.OK_STATUS;
		} catch (Exception e1) {
			return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK,
					Messages.failedToRetrieveSubmissionList + e1.getMessage(), e1);
		}
	}

}
