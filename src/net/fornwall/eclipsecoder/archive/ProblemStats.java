package net.fornwall.eclipsecoder.archive;

import java.io.Serializable;

public class ProblemStats implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int COLUMN_CLASS_NAME = 0;
	public static final int COLUMN_CONTEST_NAME = 1;
	public static final int COLUMN_DATE = 2;
	public static final int COLUMN_DIVISION = 3;
	public static final int COLUMN_LEVEL = 4;
	public static final int COLUMN_SUCCESS_RATE = 5;
	public static final int COLUMN_CATEGORIES = 6;

	static final String[] COLUMN_NAMES = { Messages.columnNameClassName, Messages.columnNameContestName,
			Messages.columnNameDate, Messages.columnNameDivision, Messages.columnNameLevel,
			Messages.columnNameSuccessRate, Messages.columnNameCategories };

	String categories;
	String className;
	String contestDate;
	String contestName;
	int div1Level;
	double div1Succ;
	int div2Level;
	double div2Succ;
	int problemId;
	int roundId;
	int submissions;

	public ProblemStats() {
		// to allow instantiation during deserialization
	}

	public ProblemStats(String className, int problemId, int roundId, String contestName, String contestDate,
			String categories, int div1Level, double div1Succ, int div2Level, double div2Succ) {
		this.className = className;
		this.roundId = roundId;
		this.problemId = problemId;
		this.contestName = contestName;
		this.contestDate = contestDate;
		this.categories = categories;
		this.div1Level = div1Level;
		this.div1Succ = div1Succ;
		this.div2Level = div2Level;
		this.div2Succ = div2Succ;
	}

	public int compareTo(ProblemStats other, int column) {
		switch (column) {
		case COLUMN_CLASS_NAME:
		case COLUMN_CONTEST_NAME:
		case COLUMN_DATE:
		case COLUMN_CATEGORIES:
			return getFieldString(column).compareTo(other.getFieldString(column));
		case COLUMN_DIVISION:
			return getDivisionString().compareTo(other.getDivisionString());
		case COLUMN_SUCCESS_RATE:
			return getSuccessRateString().compareTo(other.getSuccessRateString());
		case COLUMN_LEVEL:
			return getLevelString().compareTo(other.getLevelString());
		default:
			System.out.println("ERROR IN COMPARETO: " + column); //$NON-NLS-1$
			return 0;
		}
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof ProblemStats && ((ProblemStats) other).problemId == problemId);
	}

	public String getFieldString(int which) {
		switch (which) {
		case COLUMN_CLASS_NAME:
			return className;
		case COLUMN_CONTEST_NAME:
			return contestName;
		case COLUMN_DATE:
			return contestDate;
		case COLUMN_DIVISION:
			return getDivisionString();
		case COLUMN_SUCCESS_RATE:
			return getSuccessRateString();
		case COLUMN_LEVEL:
			return getLevelString();
		case COLUMN_CATEGORIES:
			return categories;
		default:
			return "DEFAULT"; //$NON-NLS-1$
		}
	}

	private String getDivisionString() {
		if (div1Level < 0) {
			return "2"; //$NON-NLS-1$
		} else if (div2Level < 0) {
			return "1"; //$NON-NLS-1$
		} else {
			return "1 & 2"; //$NON-NLS-1$
		}
	}

	private String getSuccessRateString() {
		if (div1Level < 0) {
			return String.valueOf(div2Succ);
		} else if (div2Level < 0) {
			return String.valueOf(div1Succ);
		} else {
			return String.valueOf(div1Succ) + " & " + String.valueOf(div2Succ); //$NON-NLS-1$
		}
	}

	private String getLevelString() {
		if (div1Level < 0) {
			return String.valueOf(div2Level);
		} else if (div2Level < 0) {
			return String.valueOf(div1Level);
		} else {
			return String.valueOf(div1Level) + " & " //$NON-NLS-1$
					+ String.valueOf(div2Level);
		}
	}

	@Override
	public int hashCode() {
		return problemId;
	}

	@Override
	public String toString() {
		return "Problem[className=" + className + ",problemId=" + problemId //$NON-NLS-1$ //$NON-NLS-2$
				+ "]"; //$NON-NLS-1$
	}

}
