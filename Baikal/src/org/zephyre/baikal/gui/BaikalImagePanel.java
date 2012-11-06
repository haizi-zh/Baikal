/**
 * 
 */
package org.zephyre.baikal.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.GridLineModel;

/**
 * @author Zephyre
 * 
 */
public class BaikalImagePanel extends JPanel {
	private BufferedImage bufImage_;
	private Object renderLock_;
	/**
	 * The start corner of the ROI rectangle.
	 */
	private Point startDragPoint_;
	/**
	 * 用户选择的ROI区域
	 */
	private Rectangle roiRect_;
	/**
	 * 表示了原图像的显示范围。默认为原图像的全图。
	 */
	private Rectangle displayRect_;

	@Override
	protected void paintComponent(Graphics g) {
		BaikalCore.log("BaikalImage");
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setBackground(Color.BLACK);
		g2d.clearRect(0, 0, getWidth(), getHeight());

		synchronized (renderLock_) {
			if (bufImage_ == null)
				return;

			// 渲染bufImage
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);

			// 获得clipping区域
			Rectangle clippingRc = g2d.getClipBounds();
			// 有图像显示的有效区域：
			Insets insets = getInsets();
			Rectangle imgRenderRc = new Rectangle(insets.left, insets.right,
					getWidth() - insets.left - insets.right, getHeight()
							- insets.top - insets.bottom);
			Rectangle effRc = clippingRc.intersection(imgRenderRc);
			if (effRc.isEmpty())
				return;

			Rectangle srcClippingRc = new Rectangle(
					(int) Math.round((double) (effRc.x - imgRenderRc.x)
							/ imgRenderRc.width * displayRect_.width
							+ displayRect_.x),
					(int) Math.round((double) (effRc.y - imgRenderRc.y)
							/ imgRenderRc.height * displayRect_.height
							+ displayRect_.y),
					(int) Math.round((double) effRc.width / imgRenderRc.width
							* displayRect_.width),
					(int) Math.round((double) effRc.height / imgRenderRc.height
							* displayRect_.height));
			g2d.drawImage(bufImage_, effRc.x, effRc.y, effRc.x + effRc.width,
					effRc.y + effRc.height, srcClippingRc.x, srcClippingRc.y,
					srcClippingRc.x + srcClippingRc.width, srcClippingRc.y
							+ srcClippingRc.height, null);

			// 绘制ROI
			if (roiRect_ != null) {
				double[] pt1 = imageToScreen(roiRect_.x, roiRect_.y);
				double[] pt2 = imageToScreen(roiRect_.x + roiRect_.width,
						roiRect_.y + roiRect_.height);
				Rectangle rc = new Rectangle((int) Math.round(pt1[0]),
						(int) Math.round(pt1[1]), (int) Math.round(pt2[0]
								- pt1[0]), (int) Math.round(pt2[1] - pt1[1]));
				g2d.setColor(Color.green);
				g2d.drawRect(rc.x, rc.y, rc.width, rc.height);
			}
		}

	}

	public enum PanelStatus {
		FREE_MOVE, ROI_DRAGGING, ABOUT_ROI_RESIZING, ABOUT_ROI_MOVING, ROI_MOVING, ROI_RESIZING
	}

	private PanelStatus panelStatus_;
	/**
	 * 表明resize操作针对的是哪条边（或者是哪两条边）。四条边(left, top, right, bottom)
	 * 分别对应该变量的4个位：[left][top][right][bottom]
	 */
	private BitSet roiEdge_;

	BaikalImagePanel() {
		renderLock_ = new Object();
		panelStatus_ = PanelStatus.FREE_MOVE;
		roiEdge_ = new BitSet(4);
		addMouseMotionListener(new MouseAdapter() {
			public void mouseMoved(MouseEvent e) {
				// 定义图标的形状
				int x = e.getX();
				int y = e.getY();
				Cursor defCursor = Cursor
						.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

				// 图像显示区域
				Insets insets = getInsets();
				Rectangle imgRenderRc = new Rectangle(insets.left, insets.top,
						getWidth() - insets.left - insets.right, getHeight()
								- insets.top - insets.bottom);

				if (!imgRenderRc.contains(x, y)) {
					// 位于有效的图像区域之外
					setCursor(defCursor);
					return;
				}

				// 放大&缩小
				if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK) {
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					return;
				} else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					return;
				}

				switch (panelStatus_) {
				case FREE_MOVE:
				case ABOUT_ROI_RESIZING:
				case ABOUT_ROI_MOVING:
					if (roiRect_ != null && !roiRect_.isEmpty()) {
						// 将roiRc_换到screen表象中
						double[] roiULScr = imageToScreen(roiRect_.x,
								roiRect_.y);
						double[] roiBRScr = imageToScreen(roiRect_.x
								+ roiRect_.width, roiRect_.y + roiRect_.height);
						Rectangle roiScrRc = new Rectangle(
								(int) Math.round(roiULScr[0]),
								(int) Math.round(roiULScr[1]),
								(int) Math.round(roiBRScr[0] - roiULScr[0]),
								(int) Math.round(roiBRScr[1] - roiULScr[1]));

						// resizing操作时，鼠标需要放在边框上的宽容度
						final int RESIZING_TORLERANCE = 2;

						// 判断是否处于边框上
						roiEdge_.set(0, 4, false);
						if (x >= roiScrRc.x - RESIZING_TORLERANCE
								&& x < roiScrRc.x + roiScrRc.width
										+ RESIZING_TORLERANCE
								&& y >= roiScrRc.y - RESIZING_TORLERANCE
								&& y < roiScrRc.y + roiScrRc.width
										+ RESIZING_TORLERANCE) {
							// 坐标落在矩形框之内
							if (Math.abs(x - roiScrRc.x) <= RESIZING_TORLERANCE)
								roiEdge_.set(0);
							if (Math.abs(x - roiScrRc.x - roiScrRc.width) <= RESIZING_TORLERANCE)
								roiEdge_.set(2);
							if (Math.abs(y - roiScrRc.y) <= RESIZING_TORLERANCE)
								roiEdge_.set(1);
							if (Math.abs(y - roiScrRc.y - roiScrRc.height) <= RESIZING_TORLERANCE)
								roiEdge_.set(3);
						}
						byte onBrim = roiEdge_.toByteArray()[0];
						if (onBrim == 0) {
							// 不在边框上面
							if (roiScrRc.contains(x, y)) {
								setCursor(Cursor
										.getPredefinedCursor(Cursor.MOVE_CURSOR));
								panelStatus_ = PanelStatus.ABOUT_ROI_MOVING;
							} else {
								setCursor(Cursor
										.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
								panelStatus_ = PanelStatus.FREE_MOVE;
							}
						} else {
							panelStatus_ = PanelStatus.ABOUT_ROI_RESIZING;
							switch (onBrim) {
							case 2:
							case 8:
								setCursor(Cursor
										.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
								break;
							case 1:
							case 4:
								setCursor(Cursor
										.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
								break;
							case 3:
							case 12:
								setCursor(Cursor
										.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
								break;
							case 6:
							case 9:
								setCursor(Cursor
										.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
								break;
							}
						}

					} else {
						setCursor(Cursor
								.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
						panelStatus_ = PanelStatus.FREE_MOVE;
					}
					break;
				default:
					break;
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				double[] coord = screenToImage(e.getX(), e.getY());
				if (coord == null)
					return;

				Point stopDragPoint;
				switch (panelStatus_) {
				case ROI_DRAGGING:
					if (startDragPoint_ != null) {
						stopDragPoint = new Point((int) Math.round(coord[0]),
								(int) Math.round(coord[1]));
						int[] xPt = new int[] { startDragPoint_.x,
								stopDragPoint.x };
						Arrays.sort(xPt);
						int[] yPt = new int[] { startDragPoint_.y,
								stopDragPoint.y };
						Arrays.sort(yPt);
						// If the dimension is zero, the ROI rectangle is
						// cleared.
						roiRect_ = new Rectangle(xPt[0], yPt[0], xPt[1]
								- xPt[0], yPt[1] - yPt[0]);
						if (roiRect_.isEmpty())
							roiRect_ = null;
						repaint();
					}
					break;
				case ROI_RESIZING:
					if (roiEdge_.get(0)) {
						// 左侧
						int newX = (int) Math.round(coord[0]);
						if (newX < roiRect_.x + roiRect_.width)
							roiRect_ = new Rectangle(newX, roiRect_.y,
									roiRect_.x + roiRect_.width - newX,
									roiRect_.height);
					}
					if (roiEdge_.get(1)) {
						// 上
						int newY = (int) Math.round(coord[1]);
						if (newY < roiRect_.y + roiRect_.height)
							roiRect_ = new Rectangle(roiRect_.x, newY,
									roiRect_.width, roiRect_.y
											+ roiRect_.height - newY);
					}
					if (roiEdge_.get(2)) {
						// 右
						int newX = (int) Math.round(coord[0]);
						if (newX > roiRect_.x)
							roiRect_ = new Rectangle(roiRect_.x, roiRect_.y,
									newX - roiRect_.x, roiRect_.height);
					}
					if (roiEdge_.get(3)) {
						// 下
						int newY = (int) Math.round(coord[1]);
						if (newY > roiRect_.y)
							roiRect_ = new Rectangle(roiRect_.x, roiRect_.y,
									roiRect_.width, newY - roiRect_.y);
					}
					repaint();
					break;
				case ROI_MOVING:
					stopDragPoint = new Point((int) Math.round(coord[0]),
							(int) Math.round(coord[1]));
					int newPosX = stopDragPoint.x - startDragPoint_.x
							+ roiRect_.x;
					int newPosY = stopDragPoint.y - startDragPoint_.y
							+ roiRect_.y;
					// 检查是否超限
					Rectangle oldRoiRc = new Rectangle(roiRect_.x, roiRect_.y,
							roiRect_.width, roiRect_.height);

					roiRect_.setLocation(newPosX, newPosY);
					synchronized (BaikalImagePanel.this) {
						if (roiRect_.x < displayRect_.x + 1
								|| roiRect_.y < displayRect_.y + 1
								|| roiRect_.x + roiRect_.width > displayRect_.x
										+ displayRect_.width - 1
								|| roiRect_.y + roiRect_.height > displayRect_.y
										+ displayRect_.height - 1) {
							roiRect_ = oldRoiRc;
							break;
						}
					}
					startDragPoint_ = stopDragPoint;
					repaint();
					break;
				default:
					break;
				}
			}
		});
		addMouseListener(new MouseAdapter() {
			// While being pressed, the ROI's start-corner is recored.
			@Override
			public void mousePressed(MouseEvent evt) {
				// 如果有Ctrl和Shift的modifier，说明用户执行的是缩放操作。跳过。
				if ((evt.getModifiersEx() & (MouseEvent.CTRL_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK)) != 0)
					return;

				double[] coord = screenToImage(evt.getX(), evt.getY());
				if (coord == null)
					return;

				switch (panelStatus_) {
				case FREE_MOVE:
					// 开始roi dragging
					panelStatus_ = PanelStatus.ROI_DRAGGING;
					startDragPoint_ = new Point((int) Math.round(coord[0]),
							(int) Math.round(coord[1]));
					break;
				case ABOUT_ROI_RESIZING:
					// 开始roi resizing
					panelStatus_ = PanelStatus.ROI_RESIZING;
					startDragPoint_ = new Point((int) Math.round(coord[0]),
							(int) Math.round(coord[1]));
					break;
				case ABOUT_ROI_MOVING:
					// 开始移动roi
					panelStatus_ = PanelStatus.ROI_MOVING;
					startDragPoint_ = new Point((int) Math.round(coord[0]),
							(int) Math.round(coord[1]));
					break;
				}
			}

			// When being clicked, the ROI is cleared
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (bufImage_ == null)
					return;

				synchronized (BaikalImagePanel.this) {
					if ((evt.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
						// 放大
						double[] imgPos = screenToImage(evt.getX(), evt.getY());
						int x1 = (int) Math.round(imgPos[0]
								- displayRect_.width / 4.0);
						int x2 = (int) Math.round(imgPos[0]
								+ displayRect_.width / 4.0 - 1);
						if (x1 < 0) {
							x1 = 0;
							x2 = (int) Math.round(x1 + displayRect_.width / 2.0
									- 1);
						} else if (x2 >= displayRect_.x + bufImage_.getWidth()) {
							x2 = bufImage_.getWidth() - 1;
							x1 = (int) Math.round(x2 - displayRect_.width / 2.0
									+ 1);
						}
						int y1 = (int) Math.round(imgPos[1]
								- displayRect_.height / 4.0);
						int y2 = (int) Math.round(imgPos[1]
								+ displayRect_.height / 4.0 - 1);
						if (y1 < 0) {
							y1 = 0;
							y2 = (int) Math.round(y1 + displayRect_.height
									/ 2.0 - 1);
						} else if (y2 >= displayRect_.y + bufImage_.getHeight()) {
							y2 = bufImage_.getHeight() - 1;
							y1 = (int) Math.round(y2 - displayRect_.height
									/ 2.0 + 1);
						}
						displayRect_ = new Rectangle(x1, y1, x2 - x1 + 1, y2
								- y1 + 1);
						roiRect_ = null;
						repaint();
					} else if ((evt.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK) {
						// 缩小
						double[] imgPos = screenToImage(evt.getX(), evt.getY());
						int x1 = (int) Math.round(imgPos[0]
								- displayRect_.width);
						int x2 = (int) Math.round(imgPos[0]
								+ displayRect_.width - 1);
						if (x1 < 0) {
							x1 = 0;
							x2 = (int) Math.round(x1 + 2 * displayRect_.width
									- 1);
							if (x2 >= bufImage_.getWidth())
								x2 = bufImage_.getWidth() - 1;
						} else if (x2 >= bufImage_.getWidth()) {
							x2 = bufImage_.getWidth() - 1;
							x1 = (int) Math.round(x2 - 2 * displayRect_.width
									+ 1);
							if (x1 < 0)
								x1 = 0;
						}
						int y1 = (int) Math.round(imgPos[1]
								- displayRect_.height);
						int y2 = (int) Math.round(imgPos[1]
								+ displayRect_.height - 1);
						if (y1 < 0) {
							y1 = 0;
							y2 = (int) Math.round(y1 + 2 * displayRect_.height
									- 1);
							if (y2 >= bufImage_.getHeight())
								y2 = bufImage_.getHeight() - 1;
						} else if (y2 >= bufImage_.getHeight()) {
							y2 = bufImage_.getHeight() - 1;
							y1 = (int) Math.round(y2 - 2 * displayRect_.height
									+ 1);
							if (y1 < 0)
								y1 = 0;
						}
						displayRect_ = new Rectangle(x1, y1, x2 - x1 + 1, y2
								- y1 + 1);
						repaint();
					} else if (roiRect_ != null) {
						roiRect_ = null;
						repaint();
					}
				}
				panelStatus_ = PanelStatus.FREE_MOVE;
			}

			@Override
			public void mouseReleased(MouseEvent evt) {
				panelStatus_ = PanelStatus.FREE_MOVE;
			}
		});
	}

	/**
	 * 坐标转换：屏幕到图像
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	double[] screenToImage(double x, double y) {
		// 有效图片显示区域
		Insets insets = getInsets();
		Rectangle imageRenderRc = new Rectangle(insets.left, insets.top,
				getWidth() - insets.left - insets.right, getHeight()
						- insets.top - insets.bottom);
		if (!imageRenderRc.contains(x, y) || bufImage_ == null)
			return null;

		synchronized (renderLock_) {
			double[] ret = new double[2];
			ret[0] = (x - insets.left) / imageRenderRc.width
					* displayRect_.width + displayRect_.x;
			ret[1] = (y - insets.top) / imageRenderRc.height
					* displayRect_.height + displayRect_.y;
			return ret;
		}
	}

	/**
	 * 获得指定位置的像素值
	 * 
	 * @return
	 */
	byte[] getPixelValue(int x, int y) {
		int pixVal;
		synchronized (renderLock_) {
			if (bufImage_ == null)
				return null;

			pixVal = bufImage_.getRGB(x, y);
		}
		byte[] rgbVal = new byte[3];
		for (int i = 0; i < 3; i++) {
			int mask = (0xff << (i * 8));
			rgbVal[2 - i] = (byte) ((pixVal & mask) >> (i * 8));
		}
		return rgbVal;
	}

	/**
	 * 坐标转换：图像到屏幕
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	double[] imageToScreen(double x, double y) {
		synchronized (renderLock_) {
			if (bufImage_ == null || !displayRect_.contains(x, y))
				return null;

			// 有效图片显示区域
			Insets insets = getInsets();
			Rectangle imageRenderRc = new Rectangle(insets.left, insets.top,
					getWidth() - insets.left - insets.right, getHeight()
							- insets.top - insets.bottom);

			double[] ret = new double[2];
			ret[0] = (x - displayRect_.x) / displayRect_.width
					* imageRenderRc.width + insets.left;
			ret[1] = (y - displayRect_.y) / displayRect_.height
					* imageRenderRc.height + insets.top;

			return ret;
		}
	}

	/**
	 * 将BufferedImage绘制出来
	 * 
	 * @param bi
	 *            处理后的图像
	 * @param markerList
	 *            marker列表
	 */
	void drawImage(BufferedImage bi) {
		synchronized (renderLock_) {
			bufImage_ = bi;
			displayRect_ = new Rectangle(0, 0, bi.getWidth(), bi.getHeight());
		}
		repaint();
	}
}
