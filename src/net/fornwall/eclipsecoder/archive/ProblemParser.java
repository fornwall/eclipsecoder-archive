package net.fornwall.eclipsecoder.archive;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fornwall.eclipsecoder.stats.ProblemStatement;
import net.fornwall.eclipsecoder.util.Utilities;

public class ProblemParser {

	/**
	 * Parse a problem info from the html statement.
	 * 
	 * @param html
	 *            The problem statement in html form from the TopCoder server.
	 */
	public static ProblemStatement parseProblem(String html, List<ProblemScraper.StringPair> testCasesStrings) {
		ProblemStatement result = new ProblemStatement();

		result.setHtmlDescription(html);
		result.setClassName(Utilities.getMatch(html, "<title>(.*)</title>", 1)); //$NON-NLS-1$
		result.setMethodName(Utilities.getMatch(html, "(?s)(?i)Method:.*?\"statText\">(.*?)</td>", 1)); //$NON-NLS-1$

		result.setReturnType(classFromString(Utilities.getMatch(html, "(?s)(?i)>Returns:.*?\"statText\">(.*?)</td>", 1))); //$NON-NLS-1$

		String parametersString = Utilities.getMatch(html, "(?s)(?i)>Parameters:.*?\"statText\">(.*?)</td>", 1); //$NON-NLS-1$
		for (String part : parametersString.split(", ")) { //$NON-NLS-1$
			result.getParameterTypes().add(classFromString(part));
		}

		String parameterNamesString = Utilities.getMatch(html, result.getMethodName() + "\\(.*?\\)", 0); //$NON-NLS-1$
		Matcher nameMatcher = Pattern.compile("\\w+?(?=(,|\\)))").matcher( //$NON-NLS-1$
				parameterNamesString);
		while (nameMatcher.find()) {
			result.getParameterNames().add(nameMatcher.group(0));
		}

		if (testCasesStrings != null && !testCasesStrings.isEmpty()) {
			for (ProblemScraper.StringPair pair : testCasesStrings) {
				result.getTestCases().add(parseTestCase(result, pair.first, pair.second));
			}
		} else {
			// TODO: Extract test cases from html problem description
			// instead (or show error/warning message to user)
		}
		return result;
	}

	public static ProblemStatement.TestCase parseTestCase(ProblemStatement statement, String expectedString,
			String parametersString) {
		// parametersString is extracted from html problem statement
		parametersString = parametersString.replaceAll("&gt;", ">").replaceAll( //$NON-NLS-1$ //$NON-NLS-2$
				"&lt;", "<").replaceAll("&amp;", "&"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		Object expected = ProblemStatement.parseType(statement.getReturnType(), expectedString);
		Object[] testCaseParameters = new Object[statement.getParameterTypes().size()];

		parametersString = parametersString.replaceAll("\n", "").replaceAll( //$NON-NLS-1$ //$NON-NLS-2$
				"\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (statement.getParameterTypes().size() == 1) {
			testCaseParameters[0] = ProblemStatement.parseType(statement.getParameterTypes().get(0), parametersString);
			return new ProblemStatement.TestCase(expected, testCaseParameters);
		}

		Matcher matcher = Pattern.compile("\\{\".*?\"}|\\{.*?\\}|(\".*?\")|[^,]+").matcher( //$NON-NLS-1$
				parametersString);
		int i = 0;
		while (matcher.find()) {
			testCaseParameters[i] = ProblemStatement.parseType(statement.getParameterTypes().get(i), matcher.group(0));
			i++;
		}

		return new ProblemStatement.TestCase(expected, testCaseParameters);
	}

	private static Class<?> classFromString(String className) {
		if (className == null) {
			throw new IllegalArgumentException("null for class"); //$NON-NLS-1$
		}
		className = className.trim();
		if (className.equals("String")) { //$NON-NLS-1$
			return String.class;
		} else if (className.equals("char")) { //$NON-NLS-1$
			return Character.class;
		} else if (className.equals("int")) { //$NON-NLS-1$
			return Integer.class;
		} else if (className.equals("long")) { //$NON-NLS-1$
			return Long.class;
		} else if (className.equals("double")) { //$NON-NLS-1$
			return Double.class;
		} else if (className.equals("String[]")) { //$NON-NLS-1$
			return String[].class;
		} else if (className.equals("char[]")) { //$NON-NLS-1$
			return Character[].class;
		} else if (className.equals("int[]")) { //$NON-NLS-1$
			return Integer[].class;
		} else if (className.equals("long[]")) { //$NON-NLS-1$
			return Long[].class;
		} else if (className.equals("double[]")) { //$NON-NLS-1$
			return Double[].class;
		} else if (className.equals("boolean")) { //$NON-NLS-1$
			return Boolean.class;
		} else if (className.equals("boolean[]")) { //$NON-NLS-1$
			return Boolean[].class;
		} else {
			throw new IllegalArgumentException("Got \"" + className + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
