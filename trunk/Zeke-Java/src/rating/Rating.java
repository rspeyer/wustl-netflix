package rating;

/**
 * One rating instance which is unique for each user movie combination
 * 
 * Basically, this is one row of the database
 * 
 * @author zeke
 */
public class Rating {

	public Rating(User user, Movie movie, int rating, int date) {
		this.user = user;
		this.movie = movie;
		this.rating = rating;
		this.date = date;
	}

	private Movie movie;

	private User user;

	private double rating;

	private int date;
	
	//Norm Variables
	private double userTimeDelayUser;
	private double userTimeDelayMovie;
	private double movieTimeDelayUser;
	private double movieTimeDelayMovie;
	
	public double getUserTimeDelay(boolean user) {
		if (user)
			return userTimeDelayUser;
		else
			return userTimeDelayMovie;
	}

	public void setUserTimeDelay(double userTimeDelay, boolean user) {
		if (user)
			this.userTimeDelayUser = userTimeDelay;
		else
			this.userTimeDelayMovie = userTimeDelay;
	}
	
	public double getMovieTimeDelay(boolean user) {
		if (user)
			return movieTimeDelayUser;
		else
			return movieTimeDelayMovie;
	}

	public void setMovieTimeDelay(double movieTimeDelay, boolean user) {
		if (user)
			this.movieTimeDelayUser = movieTimeDelay;
		else
			this.movieTimeDelayMovie = movieTimeDelay;
	}

	public int getDate() {
		return date;
	}

	public void setDate(int date) {
		this.date = date;
	}

	public Movie getMovie() {
		return movie;
	}

	public void setMovie(Movie movie) {
		this.movie = movie;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return "Movie: " + this.getMovie() + "\tUser: " + this.getUser()
				+ "\tRating: " + this.getRating() + "\tDate: " + this.getDate();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Rating other = (Rating) obj;
		if (this.getUser() != other.getUser())
			return false;
		else if (this.getMovie() != other.getMovie())
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 23;
		int hash = 1;
		hash = PRIME * hash + this.getUser().hashCode();
		hash = PRIME * hash + this.getMovie().hashCode();
		return hash;
	}
}
