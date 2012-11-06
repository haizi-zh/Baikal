/**
 * 
 */
package org.zephyre.baikal.camera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.BaikalCore.PrefConst;
import org.zephyre.baikal.camera.BaikalCameraException.BaikalCameraErrorDesc;

import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * A simulated camera
 * 
 * @author Zephyre
 * 
 */
public class BaikalSimCamera extends BaikalAbstractCamera {
	private static BaikalSimCamera instance_;
	private final int BUFFER_SIZE = 4;
	/**
	 * 拍照的图像，放在队列中以供取用。
	 */
	private ArrayBlockingQueue<BufferedImage> bufferQ;

	private static final int MAX_EXPOSURE = 60000;
	private volatile boolean isConnected_;
	private int resolution_ = 1;
	private int exposure_ = 10;
	private int offset;
	// Various resolutions
	protected static HashMap<Integer, ArrayList<Integer>> resMap_;

	static {
		ArrayList<Integer> dim = new ArrayList<Integer>(2);
		resMap_ = new HashMap<Integer, ArrayList<Integer>>();
		dim.add(1200);
		dim.add(800);
		resMap_.put(1, dim);
		dim = new ArrayList<Integer>(2);
		dim.add(1800);
		dim.add(1200);
		resMap_.put(2, dim);
		dim = new ArrayList<Integer>(2);
		dim.add(3600);
		dim.add(2400);
		resMap_.put(3, dim);
	}

	/**
	 * @throws BaikalCameraException
	 * 
	 */
	protected BaikalSimCamera() throws BaikalCameraException {
		bufferQ = new ArrayBlockingQueue<BufferedImage>(BUFFER_SIZE);
		fillBufferQ();
	}

	public static BaikalSimCamera getInstance() throws BaikalCameraException {
		if (instance_ == null)
			instance_ = new BaikalSimCamera();
		return instance_;
	}

	/**
	 * 根据当前的设置，初始化相应数量的BufferedImage，填充于bufferQ中
	 * 
	 * @throws BaikalCameraException
	 */
	private void fillBufferQ() throws BaikalCameraException {
		int width = getWidth();
		int height = getHeight();
		int dep = getBitDepth();
		int[] bandOffsets = null;
		switch (dep) {
		case 24:
			bandOffsets = new int[] { 2, 1, 0 };
			break;
		case 32:
		case 8:
		case 16:
		default:
			throw createException(BaikalCameraErrorDesc.UNKNWON);
		}
		int ncolors = 3;
		int bytespp = dep / 8;
		int scanline = width * bytespp;
		PixelInterleavedSampleModel sm = new PixelInterleavedSampleModel(
				DataBuffer.TYPE_BYTE, width, height, bytespp, scanline,
				bandOffsets);
		ColorModel cm = new ComponentColorModel(

		ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
				ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);

		// 初始化BufferedImage
		for (int i = 0; i < BUFFER_SIZE; i++) {
			WritableRaster raster = Raster.createWritableRaster(sm, null);
			bufferQ.add(new BufferedImage(cm, raster, false, null));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.zephyre.baikal.BaikalAbstractCamera#connect()
	 */
	@Override
	public void connect() throws BaikalCameraException {
		isConnected_ = true;
		// exec = Executors.newCachedThreadPool();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.zephyre.baikal.BaikalAbstractCamera#disconnect()
	 */
	@Override
	public void disconnect() {
		isConnected_ = false;
		// exec.shutdownNow();
	}

	@Override
	public boolean isConnected() {
		return isConnected_;
	}

	@Override
	protected void finalize() throws Throwable {
		if (isConnected_) {
			System.err.println("Camera not closed!");
			disconnect();
		}
	}

	@Override
	public void setResolution(int resType) throws BaikalCameraException {
		if (!resMap_.containsKey(resType))
			throw createException(BaikalCameraErrorDesc.INVALID_ARGUMENT);
		resolution_ = resType;
	}

	@Override
	public int getWidth() throws BaikalCameraException {
		return resMap_.get(resolution_).get(0).intValue();
	}

	@Override
	public int getHeight() throws BaikalCameraException {
		return resMap_.get(resolution_).get(1).intValue();
	}

	@Override
	public void setExposureTime(int val) throws BaikalCameraException {
		if (val <= 0 || val > MAX_EXPOSURE)
			throw createException(BaikalCameraErrorDesc.INVALID_ARGUMENT);
		exposure_ = val;
	}

	@Override
	public int exposureMs() throws BaikalCameraException {
		return exposure_;
	}

	// 生成一个BufferedImage对象，供测试使用
	protected BufferedImage internalImage;
	private int bitDepth_ = 24;

	protected BaikalCameraException createException(BaikalCameraErrorDesc desc) {
		BaikalCameraException e = new BaikalCameraException();
		e.setDesc(desc);
		return e;
	}

	@Override
	public int getImageByteSize() throws BaikalCameraException {
		return getWidth() * getHeight() * getBitDepth() / 8;
	}

	@Override
	public int getBitDepth() {
		return bitDepth_;
	}

	@Override
	public synchronized BufferedImage snapshotAndWait()
			throws BaikalCameraException, InterruptedException {
		BufferedImage image = null;
		BaikalCore core = BaikalCore.getInstance();
		try {
			long tic = System.nanoTime();
			int width = getWidth();
			int height = getHeight();
			image = bufferQ.take();

			HashMap<String, Object> optData = getOptData();
			boolean drawMarkers = (Boolean) optData.get("DrawMarkers");
			boolean drawHorzGridLines = (Boolean) optData
					.get("DrawHorzGridLines");
			boolean drawVertGridLines = (Boolean) optData
					.get("DrawVertGridLines");
			int horzIndex = ((Number) optData.get("HorzIndex")).intValue();
			int vertIndex = ((Number) optData.get("VertIndex")).intValue();

			// 绘制图形
			Graphics2D g2d = null;
			g2d = (Graphics2D) image.getGraphics();
			g2d.setBackground(Color.DARK_GRAY);
			g2d.clearRect(0, 0, width, height);

			final int MARGIN = ((Number) core.getEntry(PrefConst.MARKER_MARGIN))
					.intValue();
			int diameter = 2 * ((Number) core.getEntry(PrefConst.MARKER_RADIUS))
					.intValue();
			int left = MARGIN;
			int right = getWidth() - MARGIN - diameter;
			int top = MARGIN;
			int bottom = getHeight() - MARGIN - diameter;
			int center_x = (left + right) / 2;
			int center_y = (top + bottom) / 2;

			// 绘制栅格线
			g2d.setStroke(new BasicStroke(2));
			g2d.setColor(Color.GREEN);
			// 确定纵横线的数量
			int horzDensity = ((Number) core.getEntry(PrefConst.HORZ_DENSITY))
					.intValue();
			int vertDensity = ((Number) core.getEntry(PrefConst.VERT_DENSITY))
					.intValue();
			int segCount = ((Number) core.getEntry(PrefConst.SEGMENT_COUNT))
					.intValue();
			int nHGrids = horzDensity / segCount;
			int nVGrids = vertDensity / segCount;
			// 栅格间距
			double dHGrid = (double) (bottom - top) / horzDensity;
			double dVGrid = (double) (right - left) / vertDensity;
			// 绘制横线
			if (drawHorzGridLines) {
				double dy = (bottom - top) / (double) nHGrids;
				for (int i = 0; i < nHGrids; i++) {
					int y = (int) (Math.round(top + 0.5 * diameter + dy * i
							+ horzIndex * dHGrid));
					g2d.drawLine((int) (Math.round((left + 0.5 * diameter))),
							y, (int) (Math.round((right + 0.5 * diameter))), y);
				}
			}
			// 绘制竖线
			if (drawVertGridLines) {
				double dx = (right - left) / (double) nVGrids;
				for (int i = 0; i < nVGrids; i++) {
					int x = (int) (Math.round(left + 0.5 * diameter + dx * i
							+ vertIndex * dVGrid));
					g2d.drawLine(x, (int) (Math.round((top + 0.5 * diameter))),
							x, (int) (Math.round((bottom + 0.5 * diameter))));
				}
			}

			// 绘制marker
			if (drawMarkers) {
				g2d.setColor(Color.RED);
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

			g2d.dispose();

			long toc = System.nanoTime();
			// Time cost: [exposure] ms
			long dt = 1000000L * (long) exposure_ - (toc - tic);
			if (dt > 0) {
				TimeUnit.NANOSECONDS.sleep(dt);
			}
			
			return image;
		} catch (InterruptedException e) {
			if (image != null)
				bufferQ.put(image);
			throw e;
		}		
	}

	@Override
	public synchronized void releaseImage(BufferedImage img)
			throws InterruptedException {
		if (img == null)
			return;

		bufferQ.put(img);
	}
}
