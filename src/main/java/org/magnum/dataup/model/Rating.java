package org.magnum.dataup.model;

public class Rating {

	private double average;
	private int noOfRatings;
	
	public Rating(){
		this.average = 0;
		this.noOfRatings = 0;
	}

	public void updateRating(double rating){
		double total = average * noOfRatings;
		noOfRatings++;
		average = (total + rating)/noOfRatings;
	}
	
	
	
	public int getNoOfRatings() {
		return noOfRatings;
	}

	public void setNoOfRatings(int noOfRatings) {
		this.noOfRatings = noOfRatings;
	}

	public double getAverage() {
		return average;
	}

	public void setAverage(double average) {
		this.average = average;
	}
	
	
	
}
