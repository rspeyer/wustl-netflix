package predictors;

import org.apache.log4j.Logger;

import rating.Movie;
import rating.Rating;
import rating.User;
import connection.Struct;

public abstract class Prediction {

	public abstract double predict(Rating unKnownRating, Struct trainingData);
	
	public double overallAvg;
	static Logger log = Logger.getLogger("Prediction");
	
	public void NormalizeKorBell(Struct data) {
//		TODO Implement the actual regression aspect as a method and call for the other effects such as time
		//Get the Overall Avg for all movies and all users
		double amount=0.0;
		for (Movie movie : data.getMovies().values()) {
			for (Rating rating : movie.getRatings().values()) {
				overallAvg += rating.getRating();
				amount++;
			}
		}
		overallAvg /= amount;
		
		//Remove the overall avg from each rating
		for (Movie movie : data.getMovies().values()) {
			for (Rating rating : movie.getRatings().values()) {
				rating.setRating(rating.getRating()-overallAvg);
			}
		}
		
		getMovieAvgs(data);
		for (Movie movie : data.getMovies().values()) {
			for (Rating rating : movie.getRatings().values()) {
				rating.setRating(rating.getRating()-movie.getAvg());
			}
		}
		
		getUserAvgs(data);
		
		for (User user : data.getUsers().values()) {
			for (Rating rating : user.getRatings().values()) {
				rating.setRating(rating.getRating()-user.getAvg());
			}
		}
		
		//UserxTime(user)
		double avgTimeDelay=0.0;
		for (User user : data.getUsers().values()) {
			double earliestDate = (double)user.getEarliestRating().getDate();
			for (Rating rating : user.getRatings().values()) {
				rating.setUserTimeDelay(Math.sqrt(rating.getDate() - earliestDate));
				avgTimeDelay+=rating.getUserTimeDelay();
			}
			user.setAvgTimeDelay(avgTimeDelay/(double)user.getRatings().size());
			for (Rating rating : user.getRatings().values()) {
				rating.setUserTimeDelay(rating.getUserTimeDelay()-user.getAvgTimeDelay());
			}
		}	
		double theta=0.0, numerator=0.0, denomenator=0.0;
		for (User user : data.getUsers().values()) {
			theta=0.0; numerator=0.0; denomenator=0.0;
			for (Rating rating : user.getRatings().values()) {
				numerator+=rating.getRating()*rating.getUserTimeDelay();
				denomenator+=rating.getUserTimeDelay()*rating.getUserTimeDelay();
			}
			theta = numerator/denomenator;
			for (Rating rating : user.getRatings().values()) {
				rating.setUserTimeDelay(theta*rating.getUserTimeDelay());
				rating.setRating(rating.getRating()-rating.getUserTimeDelay());
			}
		}
		
		//User x Time(movie)
		
	}
	
	protected double unNormalizeKorBell(Rating rating) {
		return (rating.getRating() + rating.getMovie().getAvg() + rating.getUser().getAvg() + rating.getUserTimeDelay());
	}
	
	private void getUserAvgs(Struct data) {
		for (User user : data.getUsers().values())
			user.getVariance();
	}

	private void getMovieAvgs(Struct data) {
		for (Movie movie : data.getMovies().values())
			movie.getVariance();
	}
}
