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
		// TODO Implement the actual regression aspect as a method and call for
		// the other effects such as time
		// Get the Overall Avg for all movies and all users
		double amount = 0.0;
		for (Movie movie : data.getMovies().values()) {
			for (Rating rating : movie.getRatings().values()) {
				overallAvg += rating.getRating();
				amount++;
			}
		}
		overallAvg /= amount;

		// Remove the overall avg from each rating
		for (Movie movie : data.getMovies().values()) {
			for (Rating rating : movie.getRatings().values()) {
				rating.setRating(rating.getRating() - overallAvg);
			}
		}

		getMovieAvgs(data);
		for (Movie movie : data.getMovies().values()) {
			for (Rating rating : movie.getRatings().values()) {
				rating.setRating(rating.getRating() - movie.getAvg());
			}
		}

		getUserAvgs(data);

		for (User user : data.getUsers().values()) {
			for (Rating rating : user.getRatings().values()) {
				rating.setRating(rating.getRating() - user.getAvg());
			}
		}

		/*
		 * UserxTime(user): allowing each user’s rating to change linearly with
		 * the square root of the number of days since the user’s first rating
		 */
		userByTimeNorm(data, true);
		/*
		 * UserxTime(movie): allowing each user’s rating to change linearly with
		 * the square root of the number of days since the movie's first rating
		 */
		userByTimeNorm(data, false);

		/*
		 * MoviexTime(user): allowing each movie’s rating to change linearly
		 * with the square root of the number of days since the user’s first
		 * rating
		 */
		movieByTimeNorm(data, true);
		/*
		 * MoviexTime(movie): allowing each movie’s rating to change linearly
		 * with the square root of the number of days since the movie's first
		 * rating
		 */
		movieByTimeNorm(data, false);

		/*
		 * UserxMovie(average): Measurses how users change their ratings based
		 * on the popularity of the movie (average rating)
		 */
		userByMovieNorm(data, true);
		/*
		 * UserxMovie(support): Measurses how users change their ratings based
		 * on the popularity of the movie (# of ratings for a movie)
		 */
		userByMovieNorm(data, false);
		
		/*
		 * TODO: Test with and without the use of these last 2. Hard to explain
		 * what they are doing so it's hard to justify their existence
		 * 
		 */
		/*
		 * MoviexUser(average): Regress the ratings of each movie on the mean of
		 * the users Hard to give a good intuition for what this is doing ,but
		 * Korbell seems to think it has an impact
		 */
		movieByUserNorm(data, true);
		/*
		 * MoviexUser(support): Regress the ratings of each movie on the support
		 * of the users Hard to give a good intuition for what this is doing
		 * ,but Korbell seems to think it has an impact
		 */
		movieByUserNorm(data, false);
	}

	private void movieByUserNorm(Struct data, boolean useAverage) {
		double movieUserAvg = 0.0, userAvg = 0.0;
		for (Movie movie : data.getMovies().values()) {
			for (Rating rating : movie.getRatings().values()) {
				if (useAverage) {
					userAvg = rating.getUser().getAvgNorm();
				} else {
					userAvg = (double) rating.getUser().getRatings().size();
				}
				rating.setMovieUserPop(userAvg, useAverage);
				movieUserAvg += rating.getMovieUserPop(useAverage);
			}
			movie.setAvgUserPop(movieUserAvg / (double) movie.getRatings().size(), useAverage);

			for (Rating rating : movie.getRatings().values()) {
				rating.setMovieUserPop(rating
						.getMovieUserPop(useAverage)
						- movie.getAvgUserPop(useAverage),
						useAverage);
			}
		}
		double theta = 0.0, numerator = 0.0, denomenator = 0.0;
		for (Movie movie : data.getMovies().values()) {
			theta = 0.0;
			numerator = 0.0;
			denomenator = 0.0;
			for (Rating rating : movie.getRatings().values()) {
				numerator += rating.getRating()
						* rating.getMovieUserPop(useAverage);
				denomenator += rating.getMovieUserPop(useAverage)
						* rating.getMovieUserPop(useAverage);
			}
			theta = numerator / denomenator;
			for (Rating rating : movie.getRatings().values()) {
				rating.setMovieUserPop(theta
						* rating.getMovieUserPop(useAverage),
						useAverage);
				rating.setRating(rating.getRating()
						- rating.getMovieUserPop(useAverage));
			}
		}
	}

	private void userByMovieNorm(Struct data, boolean useAverage) {
		double userMovieAvg = 0.0, movieAvg = 0.0;
		for (User user : data.getUsers().values()) {
			for (Rating rating : user.getRatings().values()) {
				if (useAverage) {
					movieAvg = rating.getMovie().getAvgNorm();
				} else {
					movieAvg = (double) rating.getMovie().getRatings().size();
				}
				rating.setUserMoviePop(movieAvg, useAverage);
				userMovieAvg += rating.getUserMoviePop(useAverage);
			}
			user.setAvgMoviePop(userMovieAvg / (double) user.getRatings().size(), useAverage);

			for (Rating rating : user.getRatings().values()) {
				rating.setUserMoviePop(rating
						.getUserMoviePop(useAverage)
						- user.getAvgMoviePop(useAverage),
						useAverage);
			}
		}
		double theta = 0.0, numerator = 0.0, denomenator = 0.0;
		for (User user : data.getUsers().values()) {
			theta = 0.0;
			numerator = 0.0;
			denomenator = 0.0;
			for (Rating rating : user.getRatings().values()) {
				numerator += rating.getRating()
						* rating.getUserMoviePop(useAverage);
				denomenator += rating.getUserMoviePop(useAverage)
						* rating.getUserMoviePop(useAverage);
			}
			theta = numerator / denomenator;
			for (Rating rating : user.getRatings().values()) {
				rating.setUserMoviePop(theta
						* rating.getUserMoviePop(useAverage),
						useAverage);
				rating.setRating(rating.getRating()
						- rating.getUserMoviePop(useAverage));
			}
		}
	}

	private void movieByTimeNorm(Struct data, boolean userEarliestRating) {
		double avgTimeDelay = 0.0, earliestDate = 0.0;
		for (Movie movie : data.getMovies().values()) {
			for (Rating rating : movie.getRatings().values()) {
				if (userEarliestRating) {
					earliestDate = (double) rating.getUser()
							.getEarliestRating().getDate();
				} else {
					earliestDate = (double) movie.getEarliestRating().getDate();
				}
				rating.setMovieTimeDelay(Math.sqrt(rating.getDate()
						- earliestDate), userEarliestRating);
				avgTimeDelay += rating.getMovieTimeDelay(userEarliestRating);
			}
			movie.setAvgTimeDelay(avgTimeDelay
					/ (double) movie.getRatings().size(), userEarliestRating);

			for (Rating rating : movie.getRatings().values()) {
				rating.setMovieTimeDelay(rating
						.getMovieTimeDelay(userEarliestRating)
						- movie.getAvgTimeDelay(userEarliestRating),
						userEarliestRating);
			}
		}
		double theta = 0.0, numerator = 0.0, denomenator = 0.0;
		for (Movie movie : data.getMovies().values()) {
			theta = 0.0;
			numerator = 0.0;
			denomenator = 0.0;
			for (Rating rating : movie.getRatings().values()) {
				numerator += rating.getRating()
						* rating.getMovieTimeDelay(userEarliestRating);
				denomenator += rating.getMovieTimeDelay(userEarliestRating)
						* rating.getMovieTimeDelay(userEarliestRating);
			}
			theta = numerator / denomenator;
			for (Rating rating : movie.getRatings().values()) {
				rating.setMovieTimeDelay(theta
						* rating.getMovieTimeDelay(userEarliestRating),
						userEarliestRating);
				rating.setRating(rating.getRating()
						- rating.getMovieTimeDelay(userEarliestRating));
			}
		}
	}

	private void userByTimeNorm(Struct data, boolean userEarliestRating) {
		double avgTimeDelay = 0.0, earliestDate = 0.0;
		for (User user : data.getUsers().values()) {
			for (Rating rating : user.getRatings().values()) {
				if (userEarliestRating) {
					earliestDate = (double) user.getEarliestRating().getDate();

				} else {
					earliestDate = (double) rating.getMovie()
							.getEarliestRating().getDate();
				}
				rating.setUserTimeDelay(Math.sqrt(rating.getDate()
						- earliestDate), userEarliestRating);
				avgTimeDelay += rating.getUserTimeDelay(userEarliestRating);
			}
			user.setAvgTimeDelay(avgTimeDelay
					/ (double) user.getRatings().size(), userEarliestRating);

			for (Rating rating : user.getRatings().values()) {
				rating.setUserTimeDelay(rating
						.getUserTimeDelay(userEarliestRating)
						- user.getAvgTimeDelay(userEarliestRating),
						userEarliestRating);
			}
		}
		double theta = 0.0, numerator = 0.0, denomenator = 0.0;
		for (User user : data.getUsers().values()) {
			theta = 0.0;
			numerator = 0.0;
			denomenator = 0.0;
			for (Rating rating : user.getRatings().values()) {
				numerator += rating.getRating()
						* rating.getUserTimeDelay(userEarliestRating);
				denomenator += rating.getUserTimeDelay(userEarliestRating)
						* rating.getUserTimeDelay(userEarliestRating);
			}
			theta = numerator / denomenator;
			for (Rating rating : user.getRatings().values()) {
				rating.setUserTimeDelay(theta
						* rating.getUserTimeDelay(userEarliestRating),
						userEarliestRating);
				rating.setRating(rating.getRating()
						- rating.getUserTimeDelay(userEarliestRating));
			}
		}
	}

	protected double unNormalizeKorBell(Rating rating) {
		return (this.overallAvg
				+ rating.getRating() 
				+ rating.getMovie().getAvg()
				+ rating.getUser().getAvg() 
				+ rating.getUserTimeDelay(true)
				+ rating.getUserTimeDelay(false)
				+ rating.getMovieTimeDelay(true) 
				+ rating.getMovieTimeDelay(false)
				+ rating.getUserMoviePop(true)
				+ rating.getUserMoviePop(false)
				+ rating.getMovieUserPop(true)
				+ rating.getMovieUserPop(false));
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
