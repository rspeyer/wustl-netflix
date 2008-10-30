package rating;

import java.util.HashMap;
import java.util.Map;


/**
 * Models a movie in the netflix dataset.
 * Each movie has a set of ratings, sorted by date
 * 
 * Note: Movie uniqness is determined only by movieId
 * 
 * @author zeke
 */
public class Movie {

	public Movie(int movieId) {
		this.movieId = movieId;
		this.ratings = new HashMap<Integer,Rating>();
		this.avg = 0.0;
		this.variance = -10.0;
	} 
	
	private int movieId;
	private Map<Integer,Rating> ratings;
	private double avg;
	private double variance;
	
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
		if (avg != 0.0)
			return avg;
		else
			avg = calcAvg();
		return avg;
	}
	private double calcAvg() {
		int sum=0;
		for (Rating rating : ratings.values())
			sum += rating.getRating();
		return (double)sum/(double)ratings.size();
	}

	public int getMovieId() {
		return movieId;
	}
	public void setMovieId(int movieId) {
		this.movieId = movieId;
	}
	public Map<Integer,Rating> getRatings() {
		return ratings;
	}
	public void setRatings(Map<Integer,Rating> ratings) {
		this.ratings = ratings;
	}

	@Override
	public String toString() {
		return "Movie: " +this.getMovieId();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Movie other = (Movie) obj;
		if (this.getMovieId() != other.getMovieId())
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 23;
		int hash = 1;
		hash = PRIME * hash + this.getMovieId();
		return hash;
	}
}
