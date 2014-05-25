package net.fornwall.eclipsecoder.archive;

public class Submission {

	public final int coderId;

	public final String coderHandle;

	public final int coderNewRating;

	public final int points;

	public final String language;

	public final int level;

	public Submission(int coderId, String coderHandle, int coderNewRating, int points, String language, int level) {
		this.coderId = coderId;
		this.coderHandle = coderHandle;
		this.coderNewRating = coderNewRating;
		this.points = points;
		this.language = language;
		this.level = level;
	}

}
