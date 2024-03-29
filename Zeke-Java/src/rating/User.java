package rating;

import java.util.HashMap;
import java.util.Map;


/**
 * Models a user in the netflix dataset.
 * Each user has a set of ratings, sorted by date
 * 
 * Note: User uniqness is determined only by userId
 * 
 * @author zeke
 */
public class User {

	public User(int userId) {
		this.userId = userId;
		this.ratings = new HashMap<Integer,Rating>();
		avg = -10.0;
		variance = -10.0;
		earliestRating = null;
		avgMovieByUser = -10.0;
	}
	
	private int userId;
	private Map<Integer,Rating> ratings;
	private double avg;
	private double variance;
	
	//Norm Variables
	private double avgTimeDelayUser;
	private double avgTimeDelayMovie;
	private Rating earliestRating;
	private double avgMoviePopAvg;
	private double avgMoviePopSupport;
	private double avgMovieByUser;

	public double getVariance() {
		if (variance != -10.0)
			return variance;
		else
			variance = calcVar();
		return variance;
	}
	
	private double calcVar() {
		double variance =0.0;
		for (Rating rating : ratings.values())
			variance += Math.pow(rating.getRating() - getAvg(), 2);
		return (variance/(double)(ratings.size() -1));
	}

	public double getAvg() {
		if (avg != -10.0)
			return avg;
		else
			avg = calcAvg();
		return avg;
	}
	
	public double getAvgNorm() {
		if (avgMovieByUser != -10.0)
			return avgMovieByUser;
		else
			avgMovieByUser = calcAvg();
		return avgMovieByUser;
	}
	
	private double calcAvg() {
		double sum=0;
		for (Rating rating : ratings.values())
			sum += rating.getRating();
		return sum/(double)ratings.size();
	}
	
	public Map<Integer,Rating> getRatings() {
		return ratings;
	}
	public void setRatings(Map<Integer,Rating> ratings) {
		this.ratings = ratings;
	}
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	@Override
	public String toString() {
		return "User: " +this.getUserId();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final User other = (User) obj;
		if (this.getUserId() != other.getUserId())
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 23;
		int hash = 1;
		hash = PRIME * hash + this.getUserId();
		return hash;
	}
	
	public double getAvgTimeDelay(boolean user) {
		if (user)
			return avgTimeDelayUser;
		else
			return avgTimeDelayMovie;
	}

	public void setAvgTimeDelay(double avgTimeDelay, boolean user) {
		if (user)
			this.avgTimeDelayUser = avgTimeDelay;
		else
			this.avgTimeDelayMovie = avgTimeDelay;
	}
	
	public double getAvgMoviePop(boolean avg) {
		if (avg)
			return avgMoviePopAvg;
		else
			return avgMoviePopSupport;
	}

	public void setAvgMoviePop(double avgMoviePop, boolean avg) {
		if (avg)
			this.avgMoviePopAvg = avgMoviePop;
		else
			this.avgMoviePopSupport = avgMoviePop;
	}

	public Rating getEarliestRating() {
		if (earliestRating != null)
			return earliestRating;
		for (Rating rating : getRatings().values()) {
			if (earliestRating == null || earliestRating.getDate() > rating.getDate())
				earliestRating = rating;
		}
		return earliestRating;
	}
}
