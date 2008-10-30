package connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rating.Movie;
import rating.Rating;
import rating.User;

public final class connectionHelper {
	
	private static final String LOGIN = "netflix";
	private static final String PASSWORD = "gpurulez";
	private static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
	private static final String DB_CONN_STRING = "jdbc:mysql://Guppy.cse.wustl.edu:3306/netflix";
	private static final int FETCH_SIZE = 100;
	
	static Logger log = Logger.getLogger("connectionHelper");
	
	public static Connection makeDefaultConnection() throws SQLException {
		Connection result = null;
		try {
			Class.forName(DRIVER_CLASS_NAME).newInstance();
		}
		catch (Exception ex){
			log.error("Check classpath. Cannot load db driver: " + DRIVER_CLASS_NAME, ex);
		}
		log.debug("Attempting to Establish Connection");
		try {
			result = DriverManager.getConnection(DB_CONN_STRING, LOGIN, PASSWORD);
		}
		catch (SQLException e){
			log.error( "Driver loaded, but cannot connect to db: " + DB_CONN_STRING);
			throw e;
		}
		log.debug("Connection Established");
		return result;
	}
	
	public static Struct loadData(Connection conn, int limit, boolean getKnownData ) throws Exception {
		PreparedStatement ps;
		if (getKnownData)
			ps = conn.prepareStatement( "select users.user, users.movie, users.rating, users.date from users left outer join probe on users.user = probe.user and users.movie = probe.movie inner join (select movie as mov from users group by mov order by count(*) desc limit ?) as less_movie on less_movie.mov = users.movie where probe.user is null;");
		else
			ps = conn.prepareStatement( "select users.user, users.movie, users.rating, users.date from users inner join probe on users.user = probe.user and users.movie = probe.movie inner join (select movie as mov from users group by mov order by count(*) desc limit ?) as less_movie on less_movie.mov = users.movie;");
		Struct retStruct = new Struct();
		Map<Integer,User> users= new HashMap<Integer,User>();
		Map<Integer,Movie> movies= new HashMap<Integer,Movie>();
		ps.setFetchSize(FETCH_SIZE);
		try {
		    ps.setInt(1, limit);
		    long startTime = System.currentTimeMillis();
		    log.debug("Attempting to Execute Query");
		    ResultSet rs = ps.executeQuery();
		    log.debug("Query Exected in Time(Seconds): " + (System.currentTimeMillis() - startTime)/1000 );
		    try {
		    	if (rs.getMetaData().getColumnCount() != 4)
		    		throw new Exception ("Too many columns(" + rs.getMetaData().getColumnCount() +") in query results");
		        while ( rs.next() ) {
		        	User user = new User(rs.getInt(1));
		        	if (users.containsKey(user.getUserId()))
		        		user = users.get(user.getUserId());
		        	Movie movie = new Movie(rs.getInt(2));
		        	if (movies.containsKey(movie.getMovieId()))
		        		movie = movies.get(movie.getMovieId());
		        	Rating rating = new Rating(user, movie, rs.getInt(3), rs.getInt(4));
		        	user.getRatings().put(movie.getMovieId(), rating);
		        	movie.getRatings().put(user.getUserId(), rating);
		        	//XXX: May not need to do this
		        	users.put(user.getUserId(), user);
		        	movies.put(movie.getMovieId(), movie);
		        } 
		        retStruct.setMovies(movies);
		        retStruct.setUsers(users);
		    } finally {
		        rs.close();
		    }
		} finally {
		   ps.close();
		}
		return retStruct;
	}
}
