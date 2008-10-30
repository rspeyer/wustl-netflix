package predictors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.log4j.Logger;

import rating.Movie;
import rating.Rating;
import rating.User;
import connection.Struct;

public class KNN extends Prediction {
	
	private final double MAX_AVG = 5.0;
	private final double MAX_VAR = 3.0;
	private final double MAX_NUM_USER_RATING = 17653.0;
	private final double MAX_NUM_MOVIE_RATING = 232934.0;
	private final double INTERSECTION_WEIGHT = 3.0;
	static Logger log = Logger.getLogger("KNN");

	public double predict(Rating unKnownRating, Struct trainingData) {
		double totalWeight = 0, weight = 0, predRating=0, avgMovieRating=0, avgUserRating=0;
		//log.debug("Unknown Rating: " + unKnownRating);
		//Add in the user and movie averages
		avgMovieRating += trainingData.getMovies().get(unKnownRating.getMovie().getMovieId()).getAvg();
		
		//XXX: this can be removed if we are predicting over the entire probe and training sets
		if (!trainingData.getUsers().containsKey(unKnownRating.getUser().getUserId()))
			return avgMovieRating;
		
		avgUserRating += trainingData.getUsers().get(unKnownRating.getUser().getUserId()).getAvg();
		
		PriorityQueue<Neighbor> neighbors = getNearestNeighbors(unKnownRating, trainingData);
		
		//TODO: Weighting: Num people who saw both/num people who saw smaller
		for (Neighbor neighbor : neighbors) {
			//log.debug("Neighbor: " + neighbor.rating + "\tDistance: " + neighbor.distance);
			weight = 1.0;/// (Math.pow(neighbor.distance, 2.0));
			totalWeight += weight;
			predRating += unNormalize(neighbor.rating) * weight;
		}
		return ((predRating/totalWeight) + avgMovieRating + avgUserRating)/3.0;
	}

	private PriorityQueue<Neighbor> getNearestNeighbors(Rating unKnownRating, Struct trainingData) {
		PriorityQueue<Neighbor> neighborsSemi = new PriorityQueue<Neighbor>(35, new Comparator<Neighbor>() {
			public int compare(Neighbor neighbor0, Neighbor neighbor1) {
				/* Note: These are inversed from a normal comparator to make it easier
				 * to bound the size of this Priority Queue
				 */
				return (int)(neighbor1.distance - neighbor0.distance);}
		});
		
		PriorityQueue<NeighborMovie> neighborMovie = new PriorityQueue<NeighborMovie>(20, new Comparator<NeighborMovie>() {
			public int compare(NeighborMovie neighbor0, NeighborMovie neighbor1) {
				/* Note: These are inversed from a normal comparator to make it easier
				 * to bound the size of this Priority Queue
				 */
				return (int)(neighbor1.distance - neighbor0.distance);}
		});
		
		PriorityQueue<NeighborPeople> neighborPeople = new PriorityQueue<NeighborPeople>(20, new Comparator<NeighborPeople>() {
			public int compare(NeighborPeople neighbor0, NeighborPeople neighbor1) {
				/* Note: These are inversed from a normal comparator to make it easier
				 * to bound the size of this Priority Queue
				 */
				return (int)(neighbor1.distance - neighbor0.distance);}
		});
		
		double movieDistances=0.0, userDistances =0.0;
		Movie unknownMovie = trainingData.getMovies().get(unKnownRating.getMovie().getMovieId());
		User unknownUser = trainingData.getUsers().get(unKnownRating.getUser().getUserId());

		//Getting a group of similar movies
		//TODO: Reduce Time constraints: Currently 2.0 seconds to compute
		for (Rating rating : unknownUser.getRatings().values()) {
			Movie knownMovie = rating.getMovie();
			//Get the intersection of users that rated both movies
			Set<Integer> intersectingUsers = new HashSet<Integer>(unknownMovie.getRatings().keySet()); 
			intersectingUsers.retainAll(knownMovie.getRatings().keySet());
			movieDistances = movieDistance(unknownMovie, knownMovie, intersectingUsers);
			neighborMovie.offer(new NeighborMovie(knownMovie, movieDistances, intersectingUsers));
			if (neighborMovie.size() > 10)
				neighborMovie.poll();
		}
		
		//Picking similar people out who have watched both movies
		for(Rating rating : unknownMovie.getRatings().values()){
			User similarUser = rating.getUser();
			//Get the intersection of movies rated by both users
			Set<Integer> intersectingMovies = new HashSet<Integer>(unknownUser.getRatings().keySet()); 
			intersectingMovies.retainAll(similarUser.getRatings().keySet());
			userDistances = userDistance(unknownUser, similarUser, intersectingMovies);
			neighborPeople.offer(new NeighborPeople(similarUser, userDistances, intersectingMovies));
			if (neighborPeople.size() > 20)
				neighborPeople.poll();
		}

		//Now with our Similar Users and Movies let's find similar ratings!
		//TODO: Reduce Time constraints: Currently 5.3 seconds to compute!
		for (NeighborMovie similarMovie : neighborMovie) {
			for (NeighborPeople similarPeople : neighborPeople) {
				if (similarMovie.movie.getRatings().containsKey(similarPeople.user.getUserId())){
					Neighbor ratingNeighbor = new Neighbor(similarMovie.movie.getRatings().get(similarPeople.user.getUserId()), intersectSimilarity(similarMovie, similarPeople, unknownMovie, unknownUser));
					neighborsSemi.offer(ratingNeighbor);
				}
				if(neighborsSemi.size() > 35)
					neighborsSemi.poll();
			}
		}

		return neighborsSemi;
	}
	
	private double intersectSimilarity(NeighborMovie similarMovie, NeighborPeople similarPeople, Movie unknownMovie, User unknownUser) {
		double distance=0.0;
		
		//Get the user intersection between the movies
		Set<Integer> intersectUsersBetweenMovies = similarMovie.intersectUsersBetweenMovies;
		List<Double> knownMovieRatings = new ArrayList<Double>();
		List<Double> unknownMovieRatings = new ArrayList<Double>();
		
		//From the user intersection we can get the rating intersection
		for (Integer userId : intersectUsersBetweenMovies) {
			knownMovieRatings.add(similarMovie.movie.getRatings().get(userId).getRating());
			unknownMovieRatings.add(unknownMovie.getRatings().get(userId).getRating());
		}
		//So we can calculate the cosine similarity (on the movie side)
		//The extra math is to normalize the distance back to a 0 to 1 scale
		distance += Math.abs(cosineSimilarity(knownMovieRatings, unknownMovieRatings)-1)/2.0;
		
		//Get the movie intersection between the users
		Set<Integer> intersectMoviesBetweenUsers = similarPeople.intersectMoviesBetweenUsers;
		List<Double> knownUserRatings = new ArrayList<Double>();
		List<Double> unknownUserRatings = new ArrayList<Double>();
		
		//From the user intersection we can get the rating intersection
		for (Integer movieId : intersectMoviesBetweenUsers) {
			knownUserRatings.add(similarPeople.user.getRatings().get(movieId).getRating());
			unknownUserRatings.add(unknownUser.getRatings().get(movieId).getRating());
		}
		//So we can calculate the cosine similarity (on the movie side)
		//The extra math is to normalize the distance back to a 0 to 1 scale
		distance += Math.abs(cosineSimilarity(knownUserRatings, unknownUserRatings)-1)/2.0;
		
		return distance/2;
	}

	private double cosineSimilarity(List<Double> knownRatings, List<Double> unknownRatings) {
		double dotProduct=0.0, knownMag=0.0, unknownMag=0.0;
		for (int i=0; i<knownRatings.size(); i++) {
			dotProduct += knownRatings.get(i)*unknownRatings.get(i);
			knownMag+=Math.pow(knownRatings.get(i), 2.0);
			unknownMag+=Math.pow(unknownRatings.get(i), 2.0);
		}
		
		return dotProduct/(Math.sqrt(knownMag)*Math.sqrt(unknownMag));
	}

	/**
	 * Using simple Euclidean distance for now
	 * 
	 * @param movieToRate
	 * @param neighborMovie
	 * @return
	 */
	private double movieDistance (Movie movieToRate, Movie neighborMovie, Set<Integer> intersectingUsers){
		double distance = 0.0;
		//TODO: Implement a distance calcualtion between the two sets of ratings (coorelation of some sort)
		distance += Math.pow(movieToRate.getAvg()/MAX_AVG - neighborMovie.getAvg()/MAX_AVG, 2.0);
		distance += Math.pow(movieToRate.getVariance()/MAX_VAR - neighborMovie.getVariance()/MAX_VAR, 2.0);
		distance += Math.pow((((double)movieToRate.getRatings().size())/MAX_NUM_MOVIE_RATING) - (((double)neighborMovie.getRatings().size())/MAX_NUM_MOVIE_RATING), 2.0);
		
		//Get any user that rated either movie
		Set<Integer> allUsers = new HashSet<Integer>(movieToRate.getRatings().keySet());
		allUsers.addAll(neighborMovie.getRatings().keySet());
		//XXX: Tune this parameter (how much to weight the intersection size)
		distance += INTERSECTION_WEIGHT*Math.pow(1.0-((double)intersectingUsers.size()/(double)allUsers.size()), 2.0);
		
		return distance;
	}
	
	/**
	 * Using simple Euclidean distance for now
	 * 
	 * @param userToRate
	 * @param userNeighbor
	 * @return
	 */
	private double userDistance (User userToRate, User userNeighbor, Set<Integer>intersectingMovies){
		double distance = 0.0;
		//TODO: Implement a distance calcualtion between the two sets of ratings (coorelation of some sort)
		distance += Math.pow(userToRate.getAvg()/MAX_AVG - userNeighbor.getAvg()/MAX_AVG, 2.0);
		distance += Math.pow(userToRate.getVariance()/MAX_VAR - userNeighbor.getVariance()/MAX_VAR, 2.0);
		distance += Math.pow(userToRate.getRatings().size()/MAX_NUM_USER_RATING - userNeighbor.getRatings().size()/MAX_NUM_USER_RATING, 2.0);
		
		//Get any movie rated by either user
		Set<Integer> allMovies = new HashSet<Integer>(userToRate.getRatings().keySet());
		allMovies.addAll(userNeighbor.getRatings().keySet());
		//XXX: Tune this parameter (how much to weight the intersection size)
		distance += INTERSECTION_WEIGHT*Math.pow(1.0-((double)intersectingMovies.size()/(double)allMovies.size()), 2.0);
		
		return distance;
	}

	class Neighbor {
		Neighbor(Rating r, double d) {
			rating = r;
			distance = d;
		}

		Rating rating;
		double distance;
	}
	
	class NeighborMovie {
		NeighborMovie(Movie m, double d, Set<Integer> i) {
			movie = m;
			distance = d;
			intersectUsersBetweenMovies = i;
		}

		Movie movie;
		double distance;
		Set<Integer> intersectUsersBetweenMovies;
	}
	
	class NeighborPeople {
		NeighborPeople(User u, double d, Set<Integer> i) {
			user = u;
			distance = d;
			intersectMoviesBetweenUsers = i;
		}

		User user;
		double distance;
		Set<Integer> intersectMoviesBetweenUsers;
	}
}
