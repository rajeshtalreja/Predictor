package com.predictor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

/**
 * This is the main class , Predictor.This just predicts the next date based on the average.
 * Certain investment types are biMonthly or biaverage based and certain are tri average based.
 * The logic is that we can create any type of Calculator into it and plug into it.
 * There is a factory that gives the object of NextDateCalculator.
 * @author Rajesh
 *
 */
public class Predictor {

	// This URL needs to be called with investment plan
	private static final String URL = "https://demo4729673.mockable.io/";

	// This represents the Investment type for which average needs to be
	// calculated based on last two dates
	private static final List<String> investTypeApplicableForBiAverage = Arrays
			.asList("LOWCAP", "MIDCAP", "HIGHCAP");

	// This represents the Investment type for which average needs to be
	// calculated based on last two dates
	private static final List<String> investTypeApplicableForTriAverage = Arrays
			.asList("SHORTTERM", "MIDTERM", "LONGTERM");

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		// Take the investment plan input from the user.
		String investmentPlan = sc.next();
		//Create the URL
		String urlToCall = URL + investmentPlan;
		try {
			//Get the Investment history
			InvestMentResponse investmentHist = getInvestmentHistory(urlToCall);
			//Get the return dates
			List<InvestmentReturn> returns = investmentHist.getReturns();
			
			if(returns == null || returns.size() == 0){
				throw new InvestmentException(InvestmentErrorCodes.NO_TRANSACTIONS_FOUND, "Transactions not found for this investment.");
			}
			
			//This is the factory class to get the correct average calculator
			SimpleAverageFactory factory = new SimpleAverageFactory();
			factory.setInvestTypeApplicableForBiAverage(investTypeApplicableForBiAverage);
			factory.setInvestTypeApplicableForTriAverage(investTypeApplicableForTriAverage);
			
			SimpleAverageGenerator averageGenerator = factory
					.getSimpleAverageGenerator(investmentPlan);
			
			if (averageGenerator != null) {
				
				List<Date> transDates = returns.stream()
						.map(r -> r.getTranDate()).collect(Collectors.toList());
				Date nextDate = averageGenerator.getNextDate(transDates);
				SimpleDateFormat format = new SimpleDateFormat(
						"dd-MM-yyyy HH:mm:ss");
				System.out.println("Next Date : " + format.format(nextDate));
			}

		} catch (InvestmentException e) {
			System.out.println("Error occured with error code = "
					+ e.getErrorCode() + " . Error Message = "
					+ e.getErrorMessage());
		}catch(Exception e){
			System.out.println("Error occured with error code = "
					+ InvestmentErrorCodes.UNKNOWN_EXCEPTION + " . Error Message = "
					+ e.getMessage());
		}
	}

	/**
	 * This method returns the InvestmentResponse object after parsing the JSON
	 * which we get from the URL.
	 * 
	 * @param url - url which will return JSON response.
	 * @return - InvestMentResponse object
	 * @throws InvestmentException
	 */
	public static InvestMentResponse getInvestmentHistory(String url)
			throws InvestmentException {

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			int responseCode = con.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				throw new InvestmentException(
						InvestmentErrorCodes.STATUS_NOT_OK,
						"URL returned with status not OK");
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// This is the JSON deserializer for date
			JsonDeserializer<Date> deser = new JsonDeserializer<Date>() {
				@Override
				public Date deserialize(JsonElement json, Type typeOfT,
						JsonDeserializationContext context)
						throws JsonParseException {

					if (json == null) {
						return null;
					}
					try {
						// Get the date from the date string
						String date = json.toString()
								.substring(1, json.toString().length() - 1)
								.trim();
						String[] dateTime = date.split(" ");
						String datePart = dateTime[0];
						Date date1 = new SimpleDateFormat("dd-MM-yyyy")
								.parse(datePart);
						// if date contains the time then add the time
						if (dateTime.length > 1) {
							date1 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
									.parse(date);
						}
						return date1;
					} catch (Exception e) {

					}

					return null;

				}

			};

			Gson gson = new GsonBuilder()
					.registerTypeAdapter(Date.class, deser).create();
			InvestMentResponse investmentResp = gson.fromJson(
					response.toString(), InvestMentResponse.class);
			return investmentResp;

		} catch (MalformedURLException mue) {
			throw new InvestmentException(InvestmentErrorCodes.MALFORMED_URL,
					"URL is not valid");
		} catch (IOException e) {
			throw new InvestmentException(
					InvestmentErrorCodes.I_O_ERROR,
					"Input/Output exception. Something went wrong while reading the data from the url.");
		} catch (JsonSyntaxException jsonSyntaxException) {
			throw new InvestmentException(InvestmentErrorCodes.INVALID_JSON,
					"Invalid JSON returned by the URL.");
		} catch (Exception e) {
			throw new InvestmentException(
					InvestmentErrorCodes.UNKNOWN_EXCEPTION, "Unknown error");
		}

	}

}

/**
 * This represents the error codes.
 * @author Rajesh
 */
class InvestmentErrorCodes {
	public static final Integer MALFORMED_URL = 0;
	public static final Integer I_O_ERROR = 1;
	public static final Integer INVALID_JSON = 2;
	public static final Integer STATUS_NOT_OK = 3;
	public static final Integer UNKNOWN_EXCEPTION = 4;
	public static final Integer NO_TRANSACTIONS_FOUND = 5;
}

/**
 * This represents the investment JSON response returned by the URL
 * 
 * @author Rajesh
 */
class InvestMentResponse {

	private String name;
	private String investmentType;
	private Date investmentDate;
	private List<InvestmentReturn> returns;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInvestmentType() {
		return investmentType;
	}

	public void setInvestmentType(String investmentType) {
		this.investmentType = investmentType;
	}

	public Date getInvestmentDate() {
		return investmentDate;
	}

	public void setInvestmentDate(Date investmentDate) {
		this.investmentDate = investmentDate;
	}

	public List<InvestmentReturn> getReturns() {
		return returns;
	}

	public void setReturns(List<InvestmentReturn> returns) {
		this.returns = returns;
	}

}

/**
 * This class represents the investment returns.
 * 
 * @author Rajesh
 *
 */
class InvestmentReturn {

	// This represents the return date
	private Date tranDate;
	// This represents the return amount
	private String tranAmount;

	public Date getTranDate() {
		return tranDate;
	}

	public void setTranDate(Date tranDate) {
		this.tranDate = tranDate;
	}

	public String getTranAmount() {
		return tranAmount;
	}

	public void setTranAmount(String tranAmount) {
		this.tranAmount = tranAmount;
	}

}

/**
 * These represents the different investment plans.
 * 
 * @author Rajesh
 */
enum InvestmentPlans {
	LOWCAP, MIDCAP, HIGHCAP, SHORTTERM, MIDTERM, LONGTERM;
}

/**
 * This class represents the exception occured in the system
 * 
 * @author Rajesh
 *
 */
class InvestmentException extends Exception {

	private int errorCode;
	private String errorMessage;

	public InvestmentException() {
		super();
	}

	public InvestmentException(int errorCode, String errorMessage) {
		super(errorMessage);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}

/**
 * This represents the various
 * 
 * @author Rajesh
 *
 */
interface SimpleAverageGenerator {

	public Date getNextDate(List<Date> tranDates);

}

/**
 * This is bimonthly average generator. General logic is to add the last two dates and divide it by two.
 * @author Rajesh
 *
 */
class BiAverageNextDateGenerator implements SimpleAverageGenerator {

	@Override
	public Date getNextDate(List<Date> tranDates) {
		// Get last two dates.
		if (tranDates.size() > 2) {
			tranDates = new ArrayList<Date>(tranDates.subList(
					tranDates.size() - 2, tranDates.size()));
		}
		long time = tranDates.stream().mapToLong(d -> d.getTime()).sum()
				/ tranDates.size();
		long toAdd = tranDates.get(tranDates.size() - 1).getTime() - time;
		Date d = new Date(tranDates.get(tranDates.size() - 1).getTime() + toAdd);
		return d;
	}

}
/**
 * This is Trimonthly calculate the average of last three dates.
 * @author Rajesh
 *
 */
class TriAverageNextDateGenerator implements SimpleAverageGenerator {

	@Override
	public Date getNextDate(List<Date> tranDates) {
		// Get last two dates.
		if (tranDates.size() > 3) {
			tranDates = new ArrayList<Date>(tranDates.subList(
					tranDates.size() - 3, tranDates.size()));
		}
		long time = tranDates.stream().mapToLong(d -> d.getTime()).sum()
				/ tranDates.size();
		long toAdd = tranDates.get(tranDates.size() - 1).getTime() - time;
		Date d = new Date(tranDates.get(tranDates.size() - 1).getTime() + toAdd);
		return d;
	}

}

/**
 * This is the SimpleAverageFactory and is used to get the specific Average
 * calculator based on type of investment plan
 * 
 * @author Rajesh
 */
class SimpleAverageFactory {

	private List<String> investTypeApplicableForBiAverage;

	private List<String> investTypeApplicableForTriAverage;

	public SimpleAverageGenerator getSimpleAverageGenerator(
			String investmentType) {

		SimpleAverageGenerator averageGenerator = null;

		if (investTypeApplicableForBiAverage.contains(investmentType)) {
			averageGenerator = new BiAverageNextDateGenerator();
		} else if (investTypeApplicableForTriAverage.contains(investmentType)) {
			averageGenerator = new TriAverageNextDateGenerator();
		}
		return averageGenerator;

	}

	public List<String> getInvestTypeApplicableForBiAverage() {
		return investTypeApplicableForBiAverage;
	}

	public void setInvestTypeApplicableForBiAverage(
			List<String> investTypeApplicableForBiAverage) {
		this.investTypeApplicableForBiAverage = investTypeApplicableForBiAverage;
	}

	public List<String> getInvestTypeApplicableForTriAverage() {
		return investTypeApplicableForTriAverage;
	}

	public void setInvestTypeApplicableForTriAverage(
			List<String> investTypeApplicableForTriAverage) {
		this.investTypeApplicableForTriAverage = investTypeApplicableForTriAverage;
	}

}

