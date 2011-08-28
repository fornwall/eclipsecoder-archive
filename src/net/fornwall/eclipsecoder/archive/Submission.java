package net.fornwall.eclipsecoder.archive;

public class Submission {

	private int coderId;

	private String coderHandle;

	private int coderNewRating;

	private int points;

	private String language;

	private int level;

	public void setLanguage(String language) {
		this.language = language;
	}

	public Submission(int coderId, String coderHandle, int coderNewRating,
			int points, String language, int level) {
		this.coderId = coderId;
		this.coderHandle = coderHandle;
		this.coderNewRating = coderNewRating;
		this.points = points;
		this.language = language;
		this.level = level;
	}

	public int getCoderId() {
		return coderId;
	}

	public void setCoderId(int coderId) {
		this.coderId = coderId;
	}

	public String getCoderHandle() {
		return coderHandle;
	}

	public void setCoderHandler(String coderHandler) {
		this.coderHandle = coderHandler;
	}

	public int getCoderNewRating() {
		return coderNewRating;
	}

	public void setCoderNewRating(int coderNewRating) {
		this.coderNewRating = coderNewRating;
	}

	public int getPoints() {
		return points;
	}

	public String getLanguage() {
		return language;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public int getLevel() {
		return level;
	}

}
