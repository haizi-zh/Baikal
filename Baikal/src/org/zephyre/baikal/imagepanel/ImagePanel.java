/**
 * 
 */
package org.zephyre.baikal.imagepanel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
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
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * @author Zephyre
 * 
 */
public class ImagePanel extends JPanel {
	protected LinkedList<MouseMotionListener> mouseMotionListeners;
	protected LinkedList<MouseListener> mouseListeners;
	private BufferedImage bufImage;
	public ReentrantLock lock;

	/*
	 * The start corner of the ROI rectangle.
	 */
	protected Point startPt;
	/*
	 * The end corner of the ROI rectangle.
	 */
	protected Point endPt;
	protected Rectangle roiRc;

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (bufImage == null)
			return;
		
		Graphics2D g2d = (Graphics2D) g;

		if (roiRc != null) {
			int[] pt1 = imageToScreen(roiRc.x, roiRc.y);
			int[] pt2 = imageToScreen(roiRc.x + roiRc.width, roiRc.y
					+ roiRc.height);
			Rectangle rc = new Rectangle(pt1[0], pt1[1], pt2[0] - pt1[0],
					pt2[1] - pt1[1]);
			g2d.setColor(Color.green);
			g2d.drawRect(rc.x, rc.y, rc.width, rc.height);
		}

//		try {
//			lock.lockInterruptibly();
//			Graphics2D g2d = (Graphics2D) g;
//			g2d.drawImage(bufImage, 0, 0, getWidth(), getHeight(), null);
//
//			if (roiRc != null) {
//				int[] pt1 = imageToScreen(roiRc.x, roiRc.y);
//				int[] pt2 = imageToScreen(roiRc.x + roiRc.width, roiRc.y
//						+ roiRc.height);
//				Rectangle rc = new Rectangle(pt1[0], pt1[1], pt2[0] - pt1[0],
//						pt2[1] - pt1[1]);
//				g2d.setColor(Color.green);
//				g2d.drawRect(rc.x, rc.y, rc.width, rc.height);
//			}
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		} finally {
//			if (lock.isHeldByCurrentThread())
//				lock.unlock();
//		}
	}

	/*
	 * Initialize the internal BufferedImage object. Should be called before any
	 * drawing.
	 * 
	 * @param bitdepth Should be 8, 24 or 32
	 */
	public void initImage(int width, int height, int bitdepth) {
		int bytespp = bitdepth / 8;

		if (bufImage != null && bufImage.getWidth() == width
				&& bufImage.getHeight() == height
				&& bytespp == bufImage.getRaster().getNumDataElements()) {
			return;
		}

		// Initalize a new image
		int[] bandOffsets = new int[bytespp];
		PixelInterleavedSampleModel sm = new PixelInterleavedSampleModel(
				DataBuffer.TYPE_BYTE, width, height, bytespp, bytespp * width,
				bandOffsets);
		byte[] pixels = new byte[width * height * bytespp];
		DataBuffer dbuf = new DataBufferByte(pixels, bytespp * width * height);
		WritableRaster raster = Raster.createWritableRaster(sm, dbuf, null);
		ColorModel cm = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
				ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
		bufImage = new BufferedImage(cm, raster, false, null);
	}

	public byte[] getInternalBuffer() {
		if (bufImage == null)
			return null;

		return ((DataBufferByte) bufImage.getRaster().getDataBuffer())
				.getData();
	}

	public ImagePanel() {
		lock = new ReentrantLock();
		mouseMotionListeners = new LinkedList<MouseMotionListener>();
		mouseListeners = new LinkedList<MouseListener>();
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				int[] coord = screenToImage(e.getX(), e.getY());
				if (coord != null && startPt != null) {

					endPt = new Point(coord[0], coord[1]);
					int x1 = startPt.x;
					int y1 = startPt.y;
					int x2 = endPt.x;
					int y2 = endPt.y;
					if (endPt.x < x1) {
						x1 = endPt.x;
						x2 = startPt.x;
					}
					if (endPt.y < y1) {
						y1 = endPt.y;
						y2 = startPt.y;
					}

					// If the dimension is zero, the ROI rectangle is cleared.
					if (x2 - x1 > 0 && y2 - y1 > 0)
						roiRc = new Rectangle(x1, y1, x2 - x1, y2 - y1);
					else
						roiRc = null;

					// paintImmediately(new Rectangle(0, 0, getWidth(),
					// getHeight()));
					repaint();
				}

				ImagePanelMouseEvent evt = new ImagePanelMouseEvent(e);
				if (coord != null)
					evt.setImageCoordinates(coord[0], coord[1]);
				for (MouseMotionListener l : mouseMotionListeners) {
					l.mouseMoved(evt);
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				int[] coord = screenToImage(e.getX(), e.getY());
				ImagePanelMouseEvent evt = new ImagePanelMouseEvent(e);
				if (coord != null)
					evt.setImageCoordinates(coord[0], coord[1]);
				for (MouseMotionListener l : mouseMotionListeners) {
					l.mouseMoved(evt);
				}
			}
		});
		this.addMouseListener(new MouseAdapter() {
			// While being pressed, the ROI's start-corner is recored.
			@Override
			public void mousePressed(MouseEvent evt) {
				int[] coord = screenToImage(evt.getX(), evt.getY());
				if (coord == null)
					return;
				startPt = new Point(coord[0], coord[1]);
			}

			// When being clicked, the ROI is cleared
			@Override
			public void mouseClicked(MouseEvent e) {
				roiRc = null;
				repaint();
			}
		});
	}

	/*
	 * Transfer the screen-based coordinates to image-based and vice-versa.
	 */
	public int[] screenToImage(int x, int y) {
		if (bufImage == null)
			return null;

		int width = getWidth();
		int height = getHeight();

		int[] ret = new int[2];
		ret[0] = (int) ((double) x / width * bufImage.getWidth());
		ret[1] = (int) ((double) y / height * bufImage.getHeight());

		return ret;
	}

	public int[] imageToScreen(int x, int y) {
		if (bufImage == null)
			return null;

		int width = getWidth();
		int height = getHeight();

		int[] ret = new int[2];
		ret[0] = (int) ((double) x / bufImage.getWidth() * width);
		ret[1] = (int) ((double) y / bufImage.getHeight() * height);

		return ret;
	}

	public void addImagePanelMouseMotionListener(MouseMotionListener l) {
		mouseMotionListeners.add(l);
	}

	public void removeImagePanelMouseMotionListener(MouseMotionListener l) {
		mouseMotionListeners.remove(l);
	}

	public void addImagePanelMouseListener(MouseListener l) {
		mouseListeners.add(l);
	}

	public void removeImagePanelMouseListener(MouseListener l) {
		mouseListeners.remove(l);
	}
}
