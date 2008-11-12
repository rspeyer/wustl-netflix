package runner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;

import org.apache.log4j.Logger;

import predictors.KNN;

import rating.Movie;
import rating.Rating;

import connection.Struct;
import connection.connectionHelper;

public class Runner {
	
	static Logger log = Logger.getLogger("Runner");

	public static void main(String[] args) {
		double timeStart = System.currentTimeMillis();
		if (args.length != 2) {
			help();
			return;
		}
		try{
			Connection conn = connectionHelper.makeDefaultConnection();
			Struct neighborData = connectionHelper.loadData(conn, Integer.parseInt(args[0]), true);
			Struct unKnownData = connectionHelper.loadData(conn, Integer.parseInt(args[0]), false);
			double RMSE = predict(neighborData, unKnownData, args[1]);
			log.info("Root Mean Square Error: " + RMSE);
			log.info("Time(seconds) for exeuction: " + ((System.currentTimeMillis() - timeStart)/1000));
		} catch (Exception e) {
			log.error("An Exeception has occured while attempting to predict: ", e);
		}
	}

	/**
	 * Perform the predictions and return the RMSE
	 * Also writes all the predictions to a file (can use this to view error distribution)
	 * 
	 * @param neighborData
	 * @param unKnownData
	 * @param predFileName
	 * @return RMSE
	 * @throws IOException
	 */
	private static double predict(Struct neighborData, Struct unKnownData, String predFileName) throws IOException {
		
		//Get the number of predictions we need to make
		int numToPred=0;
		for (Movie movie : unKnownData.getMovies().values()){
			numToPred += movie.getRatings().size();
		}
		
		//Create the file writer
		File file = new File(predFileName);
		FileWriter fw= new FileWriter(file);
		if (!file.exists())
			throw new FileNotFoundException("File " + predFileName + " does not exist");
		fw.write("User\tMovie\tRating\tPrediction\tError\n");
		
		double sumSqError = 0, pred =0, predTimeStart=0.0, predTimeSum=0.0;
		int predsMade = 0;
		
		//Create our predictor
		KNN knn = new KNN();
		knn.NormalizeZeke(neighborData);
		
		//Do all the predictions
		for (Movie movie : unKnownData.getMovies().values()){
			for (Rating unKnownRating : movie.getRatings().values()) {
				predTimeStart=System.currentTimeMillis();
				
				pred = knn.predict(unKnownRating, neighborData);
				sumSqError += Math.pow(pred - unKnownRating.getRating(), 2);
				predsMade++;
				fw.write(unKnownRating.getUser().getUserId() + "\t" + unKnownRating.getMovie().getMovieId() + "\t" + unKnownRating.getRating() + "\t" + pred + "\t" + (pred - unKnownRating.getRating()) + "\n");
				fw.flush();
				log.info(predsMade + "/" + numToPred + ":\tActual: " + unKnownRating.getRating() + "\tPrediction: " + pred + "\tError: " + (pred - unKnownRating.getRating()));
				
				predTimeSum += (System.currentTimeMillis() - predTimeStart);
				if(predsMade%10==0) {
					log.info("--------------------------------------------");
					log.info("Avg Prediction Time(seconds): " + ((predTimeSum / (double)predsMade)/1000));
					log.info("Estimated Prediction Time Remaining(seconds): " + (((predTimeSum / (double)predsMade)/1000) * (numToPred - predsMade)));
					log.info("Estimated Root Mean Square Error: " + Math.sqrt(sumSqError/(double)predsMade));
					log.info("--------------------------------------------");
				}
			}
		}
		fw.close();
		return Math.sqrt(sumSqError/(double)predsMade);
	}

	private static void help() {
		log.warn("Predction Code for the Netflix Challenge");
		log.warn("Arguments Needed to Run the program:");
		log.warn("1. The number of most rated movies to use (ex 200 - use top 200 most rated movies");
		log.warn("2. The name of a file to store predictions and errors");
	}

}
