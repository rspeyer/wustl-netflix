package connection;

import java.util.HashMap;
import java.util.Map;

import rating.Movie;
import rating.User;

public class Struct {
		
	public Struct() {
		this.movies = new HashMap<Integer, Movie>();
		this.users = new HashMap<Integer, User>();
	}
	
	private Map<Integer, Movie> movies;
	private Map<Integer, User> users;
	
	public Map<Integer, Movie> getMovies() {
		return movies;
	}
	public void setMovies(Map<Integer, Movie> movies) {
		this.movies = movies;
	}
	public  Map<Integer, User> getUsers() {
		return users;
	}
	public void setUsers( Map<Integer, User> users) {
		this.users = users;
	}
}
