package predictors;

import rating.Movie;
import rating.Rating;
import rating.User;
import connection.Struct;

public abstract class Prediction {

	public abstract double predict(Rating unKnownRating, Struct trainingData);
	
	/**
	 * This is a really simple normalization for now
	 * Just subtract the user acg from the rating
	 * 
	 * @param data
	 */
	public void Normalize(Struct data) {
		getAllAvgs(data);//lame!
		
		double rate;
		for (Movie movie : data.getMovies().values()) {
			for (Rating rating : movie.getRatings().values()) {
				rate = rating.getRating();
				rating.setRating((rate-movie.getAvg())+(rate - rating.getUser().getAvg()));
			}
		}
	}
	
	protected double unNormalize(Rating rating) {
		return (rating.getRating() + rating.getMovie().getAvg() + rating.getUser().getAvg())/2;
	}

	/**
	 * Really lame, but I need all the averages and variances 
	 * calculated before normalizing the individual ratings
	 * 
	 * @param data
	 */
	private void getAllAvgs(Struct data) {
		for (Movie movie : data.getMovies().values())
			movie.getVariance();
		for (User user : data.getUsers().values())
			user.getVariance();
	}
}
