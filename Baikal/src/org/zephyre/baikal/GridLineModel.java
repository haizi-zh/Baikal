package org.zephyre.baikal;

import java.util.ArrayList;

/**
 * 描述一条栅格线，包括其坐标，类型，以及polyfit的信息
 * 
 * @author Zephyre
 * 
 */
public class GridLineModel {
	/**
	 * 坐标数据
	 */
	private double[][] dataPoints_;
	/**
	 * polyfit的分段信息
	 */
	private ArrayList<int[]> polyFitSegments_;
	/**
	 * polyfit的结果，系数：a0, a1, a2,...
	 */
	private ArrayList<double[]> polyFitCoeffs_;

	public static int GRID_LINE_HORIZONTAL = 0;
	public static int GRID_LINE_VERTICAL = 1;

	private int gridLineType_;

	public GridLineModel(double[][] dataPoints, int type) {
		dataPoints_ = dataPoints;
		gridLineType_ = type;
	}

	public int getType() {
		return gridLineType_;
	}

	public double[][] getDataPoints() {
		return dataPoints_;
	}

	public ArrayList<int[]> getPolyFitSegments() {
		return polyFitSegments_;
	}

	public void setPolyFitSegments(ArrayList<int[]> segments) {
		polyFitSegments_ = segments;
	}

	public ArrayList<double[]> getPolyFitCoeffs() {
		return polyFitCoeffs_;
	}

	public void setPolyFitCoeffs(ArrayList<double[]> coeffs) {
		polyFitCoeffs_ = coeffs;
	}

	public int getLength() {
		return dataPoints_.length;
	}
}
