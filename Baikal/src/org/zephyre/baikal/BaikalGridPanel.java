package org.zephyre.baikal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;
import java.util.Random;

import javax.swing.JPanel;

import org.json.simple.JSONObject;

public class BaikalGridPanel extends JPanel {
	private BaikalCore core;
	// Whether to draw grid lines
	public boolean drawXGridLines;
	public boolean drawYGridLines;
	// Whether to draw markers
	public boolean drawMarkers;

	public int segCount = 1;

	public Color markerColor = Color.RED;
	public Color gridLineColor = Color.GREEN;
	public double xOffset;
	public double yOffset;

	public BaikalGridPanel() {
		super();
		setBackground(Color.BLACK);
		setPreferredSize(new Dimension(800, 600));
		core = BaikalMainFrame.getInstance().getCore();
	}

	private void renderMarkers(Graphics2D g2d) {
		g2d.setColor(markerColor);
		Insets insets = getInsets();
		int margin = 24;
		int radius = ((Long) core.getUserData().get("MarkerRadius")).intValue();
		int w = getWidth();
		int h = getHeight();

		g2d.fillOval(insets.left + margin, insets.top + margin, 2 * radius + 1,
				2 * radius + 1);
		g2d.fillOval(w - insets.right - margin - 2 * radius - 1, insets.top
				+ margin, 2 * radius + 1, 2 * radius + 1);
		g2d.fillOval(insets.left + margin, h - insets.bottom - margin - 2
				* radius - 1, 2 * radius + 1, 2 * radius + 1);
		g2d.fillOval(w - insets.left - margin - 2 * radius - 1, h
				- insets.bottom - margin - 2 * radius - 1, 2 * radius + 1,
				2 * radius + 1);

		int d = 2 * radius + 1;
		int l = insets.left + margin;
		int r = w - insets.right - margin - d;
		int t = insets.top + margin;
		int b = h - insets.bottom - margin - d;
		int cx = (l + r) / 2;
		int cy = (t + b) / 2;

		g2d.fillOval(l, cy, d, d);
		g2d.fillOval(cx, t, d, d);
		g2d.fillOval(cx, cy, d, d);
		g2d.fillOval(cx, b, d, d);
		g2d.fillOval(r, cy, d, d);
	}

	private void renderGridLines(Graphics2D g2d) {
		g2d.setColor(gridLineColor);
		int w = getWidth();
		int h = getHeight();

		int densityX = ((Long) core.getUserData().get("DensityX")).intValue();
		int densityY = ((Long) core.getUserData().get("DensityY")).intValue();

		double dx = w / (double) (densityX);
		double dy = h / (double) (densityY);
		g2d.setColor(Color.GREEN);
		if (drawYGridLines) {
			for (int i = 0; i < Math.round(densityX / segCount); i++) {
				int x = (int) ((i * segCount + xOffset) * dx);
				g2d.drawLine(x, 0, x, h);
			}
		}
		if (drawXGridLines) {
			for (int i = 0; i < Math.round(densityY / segCount); i++) {
				int y = (int) ((i * segCount + yOffset) * dy);
				g2d.drawLine(0, y, w, y);
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		if (drawXGridLines || drawYGridLines)
			renderGridLines(g2d);
		if (drawMarkers)
			renderMarkers(g2d);
	}
}
