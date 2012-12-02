package org.zephyre.baikal.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.BaikalCore.PrefConst;

public class BaikalGridPanel extends JPanel {
	// Whether to draw grid lines
	public volatile boolean drawHorzGridLines_;
	public volatile boolean drawVertGridLines_;
	// Whether to draw markers
	public volatile boolean drawMarkers_;

	// public volatile int segCount = 1;

	private final Color MARKER_COLOR = Color.RED;
	private final Color GRID_LINE_COLOR = Color.GREEN;
	public volatile int vertOffset_;
	public volatile int horzOffset_;
	/**
	 * 是否显示所有的栅格
	 */
	private boolean displayFullGrids_;

	public BaikalGridPanel() {
		super();
		setBackground(Color.BLACK);
	}

	private void renderMarkers(Graphics2D g2d) {
		BaikalCore core = BaikalCore.getInstance();
		Insets insets = getInsets();
		int margin = ((Number) core.getEntry(PrefConst.MARKER_MARGIN))
				.intValue();
		int radius = ((Number) core.getEntry(PrefConst.MARKER_RADIUS))
				.intValue();
		int diameter = 2 * radius;

		int left = insets.left + margin;
		int right = getWidth() - insets.right - margin - diameter;
		int top = insets.top + margin;
		int bottom = getHeight() - insets.bottom - margin - diameter;
		int center_x = (left + right) / 2;
		int center_y = (top + bottom) / 2;

		g2d.setColor(MARKER_COLOR);
		g2d.fillOval(left, center_y, diameter, diameter);
		g2d.fillOval(center_x, top, diameter, diameter);
		g2d.fillOval(center_x, center_y, diameter, diameter);
		g2d.fillOval(center_x, bottom, diameter, diameter);
		g2d.fillOval(right, center_y, diameter, diameter);
		g2d.fillOval(left, top, diameter, diameter);
		g2d.fillOval(right, top, diameter, diameter);
		g2d.fillOval(left, bottom, diameter, diameter);
		g2d.fillOval(right, bottom, diameter, diameter);
	}

	/**
	 * 将JPanel上的绘制内容保存为图像文件。
	 * 
	 * @param fileName
	 *            保存的图像文件名。
	 */
	public void saveContentAsBmp(String fileName) {
		int width = getWidth();
		int height = getHeight();
		BufferedImage image = (BufferedImage) this.createImage(width, height);
		Graphics g = image.getGraphics();
		this.paint(g);
		g.dispose();
		try {
			ImageIO.write(image, "jpg", new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void renderGridLines(Graphics2D g2d) {
		BaikalCore core = BaikalCore.getInstance();
		int densityX = ((Number) core.getEntry(PrefConst.HORZ_DENSITY))
				.intValue();
		int densityY = ((Number) core.getEntry(PrefConst.VERT_DENSITY))
				.intValue();
		int margin = ((Number) core.getEntry(PrefConst.MARKER_MARGIN))
				.intValue();
		int segCount = ((Number) core.getEntry(PrefConst.SEGMENT_COUNT))
				.intValue();
		int radius = ((Number) core.getEntry(PrefConst.MARKER_RADIUS))
				.intValue();

		int originalVertOffset = vertOffset_;
		int originalHorzOffset = horzOffset_;
		if (displayFullGrids_) {
			segCount = 1;
			vertOffset_ = 0;
			horzOffset_ = 0;
		}
		Insets insets = getInsets();

		double dx = (getWidth() - 2 * radius - 2 * margin - insets.left - insets.right)
				/ (double) densityX;
		double dy = (getHeight() - 2 * radius - 2 * margin - insets.top - insets.bottom)
				/ (double) densityY;
		g2d.setColor(GRID_LINE_COLOR);
		if (drawVertGridLines_) {
			for (int i = 0; i < densityX / segCount; i++) {
				int x = (int) Math.round(insets.left + margin + radius
						+ (i * segCount + vertOffset_) * dx);
				g2d.drawLine(x, insets.top + margin + radius, x, getHeight()
						- insets.bottom - margin - radius);
			}
		}
		if (drawHorzGridLines_) {
			for (int i = 0; i < densityY / segCount; i++) {
				int y = (int) Math.round(insets.top + margin + radius
						+ (i * segCount + horzOffset_) * dy);
				g2d.drawLine(insets.left + margin + radius, y, getWidth()
						- insets.right - margin - radius, y);
			}
		}
		vertOffset_ = originalVertOffset;
		horzOffset_ = originalHorzOffset;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		if (drawHorzGridLines_ || drawVertGridLines_ || displayFullGrids_)
			renderGridLines(g2d);
		if (drawMarkers_)
			renderMarkers(g2d);
	}

	public void displayFullGrids(boolean val) {
		displayFullGrids_ = val;
	}
}
