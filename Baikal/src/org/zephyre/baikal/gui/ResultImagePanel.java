package org.zephyre.baikal.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Iterator;

import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.GridLineModel;

/**
 * 继承自BaikalImagePanel，除了显示图像以外，还显示一些结果的信息。包括： 1. 显示marker识别的结果。 2. 显示栅格线识别的结果。
 * 3. 显示交叉点识别的结果。 4. 显示相关的文字信息。
 * 
 * @author Zephyre
 * 
 */
public class ResultImagePanel extends BaikalImagePanel {
	/**
	 * 识别出来的marker
	 */
	private double[][] markerList_;
	private Object resultLock_ = new Object();
	/**
	 * 识别出的栅格线
	 */
	private Collection<? extends GridLineModel> gridLines_;
	/**
	 * 交点
	 */
	private double[][] intersections_;

	/**
	 * 将BufferedImage绘制出来
	 * 
	 * @param bi
	 *            处理后的图像
	 * @param markerList
	 *            marker列表
	 */
	void drawImage(BufferedImage bi, double[][] markerList,
			Collection<? extends GridLineModel> gridLines,
			double[][] intersections) {
		synchronized (resultLock_) {
			markerList_ = markerList;
			gridLines_ = gridLines;
			intersections_ = intersections;
		}
		super.drawImage(bi);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		synchronized (resultLock_) {
			Graphics2D g2d = (Graphics2D) g;
			// 绘制marker中心标尺
			if (markerList_ != null) {
				for (int i = 0; i < markerList_.length; i++) {
					g2d.setColor(Color.CYAN);
					double[] pos = imageToScreen(markerList_[i][0],
							markerList_[i][1]);
					if (pos == null)
						continue;

					// 左上角/右下角
					double[] upperLeft = imageToScreen(markerList_[i][0]
							- markerList_[i][2], markerList_[i][1]
							- markerList_[i][2]);
					double[] topRight = imageToScreen(markerList_[i][0]
							+ markerList_[i][2], markerList_[i][1]
							+ markerList_[i][2]);
					double[] center = new double[] {
							(upperLeft[0] + topRight[0]) / 2,
							(upperLeft[1] + topRight[1]) / 2 };
					double crossRadius = (center[0] - upperLeft[0]) / 2;
					if (upperLeft != null && topRight != null) {

						Rectangle markerRc = new Rectangle(
								(int) Math.round(upperLeft[0]),
								(int) Math.round(upperLeft[1]),
								(int) Math.round(topRight[0] - upperLeft[0]),
								(int) Math.round(topRight[1] - upperLeft[1]));
						g2d.drawOval(markerRc.x, markerRc.y, markerRc.width,
								markerRc.height);
						int xc = (int) Math.round(center[0]);
						int yc = (int) Math.round(center[1]);
						int intRadius = (int) Math.round(crossRadius);
						g2d.drawLine(xc - intRadius, yc, xc + intRadius, yc);
						g2d.drawLine(xc, yc - intRadius, xc, yc + intRadius);

						// marker信息
						g2d.drawString(String.format(
								"#%d: Loc: (%.2f, %.2f), R: %.2f)", i,
								markerList_[i][0], markerList_[i][1],
								markerList_[i][2]), xc + 3 * intRadius, yc + 3
								* intRadius);
					}
				}
			}

			if (gridLines_ != null) {
				Iterator<? extends GridLineModel> it = gridLines_.iterator();
				final int NUM_ANCHORS = 10;
				final int HALF_CROSS_LENGTH = 5;
				g2d.setColor(Color.MAGENTA);
				while (it.hasNext()) {
					GridLineModel line = it.next();
					// 等距离取10个点
					for (int i = 0; i < NUM_ANCHORS; i++) {
						int index = (int) Math.round((line.getLength() - 1)
								/ (double) (NUM_ANCHORS - 1) * i);
						double[][] pts = line.getDataPoints();
						double[] pt = imageToScreen(pts[index][0],
								pts[index][1]);
						int x = (int) Math.round(pt[0]);
						int y = (int) Math.round(pt[1]);
						g2d.drawLine(x - HALF_CROSS_LENGTH, y
								- HALF_CROSS_LENGTH, x + HALF_CROSS_LENGTH, y
								+ HALF_CROSS_LENGTH);
						g2d.drawLine(x + HALF_CROSS_LENGTH, y
								- HALF_CROSS_LENGTH, x - HALF_CROSS_LENGTH, y
								+ HALF_CROSS_LENGTH);
					}
				}
			}

			// 画交点
			if (intersections_ != null) {
				g2d.setColor(Color.MAGENTA);
				for (int i = 0; i < intersections_.length; i++) {
					double[] ptD = intersections_[i];
					double[] ptScr = imageToScreen(ptD[0], ptD[1]);
					int x = (int) Math.round(ptScr[0]);
					int y = (int) Math.round(ptScr[1]);
					g2d.drawOval(x - 1, y - 1, 2, 2);
				}
			}
		}
	}
}
