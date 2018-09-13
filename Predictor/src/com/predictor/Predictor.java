package com.predictor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * This is the main class , Predictor
 * @author Rajesh
 *
 */
public class Predictor {

	//This is used to get json and parse the json
	private static Gson gson = new Gson();
	
	private static final String URL = "https://demo4729673.mockable.io/";
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		Scanner sc = new Scanner(System.in);
		
		String investmentPlan = sc.next();
		
		String urlToCall = URL + investmentPlan;
	
		List<InvestmentHistoryPojo> investmentHist = getInvestmentHistory(urlToCall);
		
		if(investmentHist != null){
			
			
		}
		
	}
	
	
	public static List<InvestmentHistoryPojo> getInvestmentHistory(String url){
		
		try{
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			int responseCode = con.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
			    System.out.println("Server returned response code " + responseCode + ". Request failed.");
			    return null;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			List<InvestmentHistoryPojo> investHist = gson.fromJson(response.toString(), new TypeToken<List<InvestmentHistoryPojo>>(){}.getType());
			return investHist;
			
		}catch(MalformedURLException mue){
			System.out.println("Url is not valid.");
		} catch (IOException e) {
			System.out.println("Cannnot read input from the URL");
		}
		return null;
		
	}
	
	
}

/**
 * 
 * @author Rajesh
 *
 */
class InvestmentHistoryPojo{
	
	//This represents the investment plan
	private String investmentPlan;
	//This represents the return date
	private Date returnDate;
	//This represents the return amount
	private String returnAmount;
	public String getInvestmentPlan() {
		return investmentPlan;
	}
	public void setInvestmentPlan(String investmentPlan) {
		this.investmentPlan = investmentPlan;
	}
	public Date getReturnDate() {
		return returnDate;
	}
	public void setReturnDate(Date returnDate) {
		this.returnDate = returnDate;
	}
	public String getReturnAmount() {
		return returnAmount;
	}
	public void setReturnAmount(String returnAmount) {
		this.returnAmount = returnAmount;
	}	
	
}

/**
 * These represents the different investment plans
 * @author Rajesh
 */
enum InvestmentPlans{
	LOWCAP, MIDCAP, HIGHCAP, SHORTTERM, MIDTERM, LONGTERM;
}
