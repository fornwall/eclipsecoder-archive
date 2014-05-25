package net.fornwall.eclipsecoder.archive;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import net.fornwall.eclipsecoder.archive.ArchiveListView.ListReference;
import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.stats.ProblemStatement;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

/**
 * Scraping problem archive on www.topcoder.com with regular expressions.
 */
class ProblemScraper {

	/** Utility class for pair of strings */
	public static class StringPair {
		public String first;

		public String second;

		public StringPair(String first, String second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public String toString() {
			return "StringPair[first=" + first + ", second=" + second + "]\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	static IStatus createLoginFailedStatus(LoginException e) {
		return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK, Messages.unableToLoginToTopCoder, e);
	}

	/**
	 * Download the list of problem and serialize it it to disk.
	 * 
	 * @param listReference
	 *            a reference to a list which will be filled in upon success
	 */
	public static IStatus downloadProblemStats(IProgressMonitor monitor, ListReference listReference) {
		ObjectOutputStream out = null;
		try {
			monitor.subTask(Messages.loggingIn);
			ProblemScraper connection = new ProblemScraper(EclipseCoderPlugin.tcUserName(),
					EclipseCoderPlugin.tcPassword());
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			monitor.worked(20);

			monitor.subTask(Messages.openingOutputStream);
			out = new ObjectOutputStream(new FileOutputStream(getProblemListFile()));
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			monitor.worked(5);

			// getProblemStatsList() calls monitor.worked() itself
			List<ProblemStats> list = connection.getProblemStatsList(monitor);
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;

			monitor.subTask(Messages.savingToDisk);
			out.writeObject(list);
			listReference.problemStats = list;
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			monitor.worked(10);

			return Status.OK_STATUS;
		} catch (LoginException e) {
			return createLoginFailedStatus(e);
		} catch (Exception e) {
			return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK,
					Messages.failedToDownloadProblemStats + e.getMessage(), e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					Utilities.showException(e);
				}
			}
		}
	}

	/** Utility method for encoding key-value parameters */
	private static String getEncoded(Map<String, String> parameters) throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, String> mapEntry : parameters.entrySet()) {
			if (builder.length() != 0)
				builder.append('&');
			builder.append(URLEncoder.encode(mapEntry.getKey(), "UTF-8") + "=" //$NON-NLS-1$ //$NON-NLS-2$
					+ URLEncoder.encode(mapEntry.getValue(), "UTF-8")); //$NON-NLS-1$
		}
		return builder.toString();
	}

	/**
	 * Return the file used to store the problem list in a serialized format.
	 * Note that this file may not exist (problem list not yet downloaded) or be
	 * incorrupt (aborted write or change of java version).
	 */
	private static File getProblemListFile() {
		return EclipseCoderPlugin.getDefault().getStateLocation().append("problemStats.ser").toFile(); //$NON-NLS-1$
	}

	/**
	 * Load the problem stats list from local storage. Return null if there was
	 * a problem loading this file (file corrupted or not existing).
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	public static List<ProblemStats> loadProblemStats() {
		List<ProblemStats> result = null;
		File storageFile = getProblemListFile();
		try {
			if (!storageFile.exists())
				return null;
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(storageFile));
			result = (List<ProblemStats>) in.readObject();
			in.close();
		} catch (EOFException e) {
			// invalid serialization file - need to be recreated
			storageFile.delete();
			return null;
		} catch (Exception e) {
			Utilities.showException(e);
			return null;
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		ProblemScraper connection = new ProblemScraper("Voldemort", "XXX"); //$NON-NLS-1$ //$NON-NLS-2$

		// File listFile = new
		// File("src/net/fornwall/eclipsecoder/archive/problems/list.xml");
		List<ProblemStats> problemStats;
		problemStats = connection.getProblemStatsList(new NullProgressMonitor());

		// if (listFile.exists()) {
		// XMLDecoder decoder = new XMLDecoder(new FileInputStream(listFile));
		// problemStats = (List) decoder.readObject();
		// decoder.close();
		// } else {
		// XMLEncoder encoder = new XMLEncoder(new FileOutputStream(listFile));
		// problemStats = connection.getProblemStatsList(new
		// NullProgressMonitor());
		// encoder.writeObject(problemStats);
		// encoder.close();
		// }

		for (ProblemStats stats : problemStats) {
			File file = new File("src/net/fornwall/eclipsecoder/archive/problems/problem-" //$NON-NLS-1$
					+ stats.getProblemId() + ".xml"); //$NON-NLS-1$
			if (file.exists()) {
				System.out.println("skipping file " + stats.getClassName()); //$NON-NLS-1$
				continue;
			} else if (stats.getClassName().equals("BishopOnTheBoard")) { //$NON-NLS-1$
				// broken html in problem view (empty column)
				continue;
			} else if (stats.getClassName().equals("BlockDistance") //$NON-NLS-1$
					|| stats.getClassName().equals("Foobar") //$NON-NLS-1$
					|| stats.getClassName().equals("Graduation")) { //$NON-NLS-1$
				// string escaping, needs to be fixed
				continue;
			} else if (stats.getClassName().equals("Connected")) { //$NON-NLS-1$
				// broken page (just empty)
				continue;
			} else if (stats.getClassName().equals("DecodeMoveToFront")) { //$NON-NLS-1$
				// broken test cases on page - first two examples has three
				// arguments, not two
				continue;
			}
			System.out.println("Getting problem " + stats.getClassName()); //$NON-NLS-1$
			// ProblemStatement statement =
			connection.getProblemStatement(stats);
			// XMLEncoder encoder = new XMLEncoder(new FileOutputStream(file));
			// encoder.writeObject(statement);
			// encoder.close();
		}
	}

	/** Read all text from an InputStream */
	private static String readAll(InputStream in) throws IOException {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append('\n');
		}
		return builder.toString();
	}

	/**
	 * Try to parse a string containing an integer, optionally surrounded with
	 * whitespace.
	 * 
	 * @param intString
	 *            the string to try to parse an int from
	 * @return the int value or -1 if the string is blank
	 */
	private static int tryParseInt(String intString) {
		intString = intString.trim();
		if (intString.length() > 0) {
			return Integer.parseInt(intString);
		}
		return -1;
	}

	/**
	 * Parse a string containing a double followed by a percentage, optionally
	 * surrounded with whitespace.
	 * 
	 * @param percentageString
	 *            the string to parse a percentage from
	 * @return the percentage value as a double or -1 if the percentageString is
	 *         blank
	 */
	private static double tryParsePercentage(String percentageString) {
		percentageString = percentageString.trim();
		if (percentageString.length() > 0) {
			// remove trailing percentage character
			percentageString = percentageString.substring(0, percentageString.length() - 1);
			return Double.parseDouble(percentageString);
		}
		return -1;
	}

	private String sessionIdCookie;

	ProblemScraper(String username, String password) throws Exception {
		URL url = new URL("https://community.topcoder.com/tc"); //$NON-NLS-1$

		URLConnection urlConn = url.openConnection();
		urlConn.setDoOutput(true);
		urlConn.setUseCaches(false);

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("username", username); //$NON-NLS-1$
		parameters.put("password", password); //$NON-NLS-1$
		parameters.put("module", "Login"); //$NON-NLS-1$ //$NON-NLS-2$
		parameters.put("rem", "on"); //$NON-NLS-1$ //$NON-NLS-2$
		parameters.put("nextpage", "http://www.topcoder.com/tc"); //$NON-NLS-1$ //$NON-NLS-2$

		DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
		printout.writeBytes(getEncoded(parameters));
		printout.flush();
		printout.close();

		List<String> setCookieList = urlConn.getHeaderFields().get("Set-Cookie"); //$NON-NLS-1$
		if (setCookieList != null) {
			for (String aCookie : setCookieList) {
				if (aCookie.startsWith("JSESSIONID")) { //$NON-NLS-1$
					sessionIdCookie = aCookie;
					break;
				}
			}
		}
		if (sessionIdCookie == null || readAll(urlConn.getInputStream()).contains("TopCoder | Login")) { //$NON-NLS-1$
			throw new LoginException(Messages.noSessionIdCheckYourCredentials);
		}
	}

	/**
	 * First element is expected result, second the parameters.
	 * 
	 * @param problem
	 *            The problem whose examples should be retrieved.
	 * @return A list of pairs [expected, parameters]
	 * @throws Exception
	 */
	public List<StringPair> getExamples(ProblemStats problem) throws Exception {
		List<StringPair> result = new ArrayList<StringPair>();

		// we need a coder and room id of a correct submission to obtain the
		// test cases.
		String page = getPage("tc?module=ProblemDetail&rd=" //$NON-NLS-1$
				+ problem.getRoundId() + "&pm=" + problem.getProblemId()); //$NON-NLS-1$

		// the problem detail page, links to the top submission of each language
		// - any will do
		String coderIdMatch = Utilities.getMatch(page, "problem_solution.*cr=(\\d+)", 1); //$NON-NLS-1$
		if (coderIdMatch == null) {
			// no solution found - no examples can be extracted
			return result;
		}
		int coderId = Integer.parseInt(coderIdMatch);
		page = getPage("stat?c=problem_solution&cr=" + coderId + "&rd=" //$NON-NLS-1$ //$NON-NLS-2$
				+ problem.getRoundId() + "&pm=" //$NON-NLS-1$
				+ problem.getProblemId());

		if (page.contains("Solution Not Available")) { //$NON-NLS-1$
			return null;
		}

		Matcher matcher = Pattern.compile("(?i)(?s)<tr valign=\"top\">.*?</tr>").matcher(page); //$NON-NLS-1$
		while (matcher.find()) {
			Matcher m = Pattern.compile("(?i)(?s)<td[^>]*?statText[^>]+>([^<]+)<").matcher( //$NON-NLS-1$
					matcher.group());
			m.find();
			String parameterString = m.group(1);
			m.find();
			String expectedString = m.group(1);
			if (expectedString.startsWith("<span class=bigRed>")) { //$NON-NLS-1$
				// failed system test - abort
				return result;
			}
			result.add(new StringPair(expectedString, parameterString));
		}

		return result;
	}

	/**
	 * Get the HTML problem statement for the problem.
	 */
	public String getHtmlProblemStatement(ProblemStats problem) throws Exception {
		String page = getPage("stat?c=problem_statement&pm=" //$NON-NLS-1$
				+ problem.getProblemId() + "&rd=" + problem.getRoundId()); //$NON-NLS-1$

		// the problem statement is embedded in a page
		Matcher matcher = Pattern.compile("<TD CLASS=\"problemText\" VALIGN=\"middle\" ALIGN=\"left\">(.*?) </TD>", //$NON-NLS-1$
				Pattern.DOTALL).matcher(page);
		matcher.find();
		try {
			return "<html><head><title>" + problem.getClassName() //$NON-NLS-1$
					+ "</title></head><body>" + matcher.group(1) //$NON-NLS-1$
					+ "</body></html>"; //$NON-NLS-1$
		} catch (IllegalStateException e) {
			System.out.println("PAGE: " + page); //$NON-NLS-1$
			throw e;
		}
	}

	private String getPage(String path) throws Exception {
		URL url = new URL("http://community.topcoder.com/" + path); //$NON-NLS-1$
		URLConnection connection = url.openConnection();
		// FIXME: when to set this (breaks i.e. editorial fetching)?
		connection.setRequestProperty("Cookie", sessionIdCookie); //$NON-NLS-1$
		return readAll(connection.getInputStream());
	}

	public ProblemStatement getProblemStatement(final ProblemStats problemStats) throws Exception {
		return ProblemParser.parseProblem(getHtmlProblemStatement(problemStats), getExamples(problemStats));
	}

	public List<ProblemStats> getProblemStatsList(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.downloadingPage);
		String page = getPage("tc?module=ProblemArchive&sc=0&sd=asc&er=10000"); //$NON-NLS-1$
		monitor.worked(30);

		monitor.subTask(Messages.parsingPage);
		page = page.substring(page.lastIndexOf("Success Rate") + 20, page //$NON-NLS-1$
				.lastIndexOf("&lt;&lt;")); //$NON-NLS-1$
		Matcher matcher = Pattern.compile("(?:statText|left|right)\">([^<>]*?)</").matcher(page); //$NON-NLS-1$
		List<ProblemStats> result = new ArrayList<ProblemStats>();

		Pattern roundIdPattern = Pattern.compile("rd=([0-9]*)"); //$NON-NLS-1$
		Pattern problemIdPattern = Pattern.compile("pm=([0-9]*)"); //$NON-NLS-1$

		// Example of a row:
		// <TR>
		// <TD VALIGN="middle" WIDTH="10"><IMG SRC="/i/clear.gif" ALT=""
		// WIDTH="10" HEIGHT="1" BORDER="0"></TD>
		// <TD CLASS="statText" HEIGHT="13">
		// <A HREF="/stat?c=problem_statement&pm=71" class="statText">
		// Area</A></TD>
		//
		// <TD CLASS="statText" HEIGHT="13" ALIGN="left" NOWRAP="on">
		//
		// <A HREF="/stat?c=round_overview&rd=2009" class="statText">
		// TCCC &#039;01 Finals</A>
		//
		// </TD>
		// <TD CLASS="statText" HEIGHT="13" ALIGN="left">
		// 06.07.2001</TD>
		// <TD CLASS="statText" HEIGHT="13" ALIGN="left">
		// ### possibly a writer link (<a>....</a>
		// </TD>
		//
		// <TD CLASS="statText" HEIGHT="13" ALIGN="left">Geometry</TD>
		// <TD CLASS="statText" HEIGHT="13" ALIGN="right">3</TD>
		// <TD CLASS="statText" HEIGHT="13" ALIGN="right">
		// 50.00%</TD>
		// <TD CLASS="statText" HEIGHT="13" ALIGN="right"></TD>
		// <TD CLASS="statText" HEIGHT="13" ALIGN="right">
		// </TD>
		//
		// <TD CLASS="statText" HEIGHT="13" ALIGN="right">
		// <A HREF="/tc?module=ProblemDetail&rd=2009&pm=71"
		// class="statText">details</A>
		// </TD>
		// <TD VALIGN="top" WIDTH="10"><IMG SRC="/i/clear.gif" ALT="" WIDTH="10"
		// HEIGHT="1" BORDER="0"></TD>
		// </TR>
		while (matcher.find()) {
			String className = matcher.group(1).trim();

			Matcher idMatcher = roundIdPattern.matcher(page.substring(matcher.start()));
			idMatcher.find();
			int roundId = Integer.parseInt(idMatcher.group(1).trim());
			idMatcher = problemIdPattern.matcher(page.substring(matcher.start() - 50, matcher.start()));

			idMatcher.find();

			int problemId = Integer.parseInt(idMatcher.group(1).trim());
			matcher.find();
			String contestName = matcher.group(1).replace("&#039;", "'").trim(); //$NON-NLS-1$ //$NON-NLS-2$
			matcher.find();
			String contestDate = matcher.group(1).trim();
			// change from MM/DD/YYYY to YYYY.MM.DD
			String[] parts = contestDate.split("\\."); //$NON-NLS-1$
			contestDate = parts[2] + "." + parts[0] + "." + parts[1]; //$NON-NLS-1$ //$NON-NLS-2$
			matcher.find();
			if (matcher.group(1).trim().length() == 0) {
				// empty writer column - just skip. If it was non-empty there
				// would be no match and
				// we would move on to the categories column (which is never
				// empty)
				matcher.find();
			}

			String categories = matcher.group(1).trim();

			matcher.find();
			int div1Level = tryParseInt(matcher.group(1));

			matcher.find();
			double div1Succ = tryParsePercentage(matcher.group(1));

			matcher.find();
			int div2Level = tryParseInt(matcher.group(1));

			matcher.find();
			double div2Succ = tryParsePercentage(matcher.group(1));

			matcher.find();

			ProblemStats problem = new ProblemStats(className, problemId, roundId, contestName, contestDate,
					categories, div1Level, div1Succ, div2Level, div2Succ);
			result.add(problem);
		}

		monitor.worked(20);
		return result;
	}

	public String getSubmission(int coderId, int roundId, int problemId, IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.loadingPage);
		String page = getPage("stat?c=problem_solution&cr=" + coderId + "&rd=" //$NON-NLS-1$ //$NON-NLS-2$
				+ roundId + "&pm=" + problemId); //$NON-NLS-1$
		monitor.worked(40);
		monitor.subTask(Messages.parsingPage);

		if (page.contains(Messages.solutionNotAvailable)) {
			return null;
		}

		Matcher matcher = Pattern.compile("(?i)(?s)<td class=\"problemText\".*?>(.*?)</td>") //$NON-NLS-1$
				.matcher(page);
		if (!matcher.find()) {
			Utilities.showMessageDialog(Messages.errorInParsing, Messages.errorInParsingPleaseReport);
			return null;
		}

		monitor.worked(30);
		monitor.subTask(Messages.postProcessingSubmission);

		String submission = matcher.group(1);
		submission = submission.replaceAll("\n", ""); //$NON-NLS-1$ //$NON-NLS-2$
		submission = submission.replaceAll("<BR>", System //$NON-NLS-1$
				.getProperty("line.separator")); //$NON-NLS-1$
		submission = submission.replaceAll("&#160;", " "); //$NON-NLS-1$ //$NON-NLS-2$
		submission = submission.replaceAll("&gt;", ">"); //$NON-NLS-1$ //$NON-NLS-2$
		submission = submission.replaceAll("&lt;", "<"); //$NON-NLS-1$ //$NON-NLS-2$
		submission = submission.replaceAll("&amp;", "&");
		return submission.trim();
	}

}
