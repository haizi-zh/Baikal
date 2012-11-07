/**
 * 
 */
package org.zephyre.baikal.camera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.zephyre.baikal.camera.BaikalCameraException.BaikalCameraErrorDesc;

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
	private int resolution_;
	private int exposureMs_;
	// 维护一个借出图像的列表
	private Set<BufferedImage> takenOutImages_;
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
	}

	public static BaikalSimCamera getInstance() throws BaikalCameraException {
		if (instance_ == null)
			instance_ = new BaikalSimCamera();
		return instance_;
	}

	/**
	 * 根据当前的设置，初始化相应数量的BufferedImage，填充于bufferQ中。同时初始化takenOutImages，
	 * 记录借出的BufferedImage。
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
		int bytespp = dep / 8;
		int scanline = width * bytespp;
		PixelInterleavedSampleModel sm = new PixelInterleavedSampleModel(
				DataBuffer.TYPE_BYTE, width, height, bytespp, scanline,
				bandOffsets);
		ColorModel cm = new ComponentColorModel(

		ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
				ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);

		// 初始化BufferedImage
		bufferQ = new ArrayBlockingQueue<BufferedImage>(BUFFER_SIZE);
		for (int i = 0; i < BUFFER_SIZE; i++) {
			WritableRaster raster = Raster.createWritableRaster(sm, null);
			bufferQ.add(new BufferedImage(cm, raster, false, null));
		}

		takenOutImages_ = Collections
				.synchronizedSet(new HashSet<BufferedImage>());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.zephyre.baikal.BaikalAbstractCamera#connect()
	 */
	@Override
	public synchronized void connect() throws BaikalCameraException {
		isConnected_ = true;
		// 默认值
		drawMarkers_ = true;
		drawHorzGridLines_ = true;
		drawVertGridLines_ = true;
		horzGridLineIndex_ = 0;
		vertGridLineIndex_ = 0;
		horzGridLineDensity_ = 100;
		vertGridLineDensity_ = 100;
		segmentCount_ = 10;
		markerMargin_ = 48;
		markerRadius_ = 16;
		bitDepth_ = 24;
		resolution_ = 1;
		exposureMs_ = 10;
		maxWaitingMs_ = Long.MAX_VALUE;

		fillBufferQ();
	}

	public void setMaxWaitingMs(int val) {
		maxWaitingMs_ = val;
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
	public synchronized void setResolution(Object resType)
			throws BaikalCameraException {
		if (!isConnected())
			throw BaikalCameraException
					.create(BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED);
		if ((!resMap_.containsKey(resType)) || !(resType instanceof Integer))
			throw createException(BaikalCameraErrorDesc.INVALID_ARGUMENT);
		resolution_ = (Integer) resType;
	}

	@Override
	public synchronized int getWidth() throws BaikalCameraException {
		if (!isConnected())
			throw BaikalCameraException
					.create(BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED);
		return resMap_.get(resolution_).get(0).intValue();
	}

	@Override
	public synchronized int getHeight() throws BaikalCameraException {
		if (!isConnected())
			throw BaikalCameraException
					.create(BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED);
		return resMap_.get(resolution_).get(1).intValue();
	}

	@Override
	public synchronized void setExposureTime(int val)
			throws BaikalCameraException {
		if (val <= 0 || val > MAX_EXPOSURE)
			throw createException(BaikalCameraErrorDesc.INVALID_ARGUMENT);
		exposureMs_ = val;
	}

	@Override
	public synchronized int getExposureMs() throws BaikalCameraException {
		if (!isConnected())
			throw new BaikalCameraException(
					BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED);
		return exposureMs_;
	}

	// 生成一个BufferedImage对象，供测试使用
	protected BufferedImage internalImage;
	private int bitDepth_;

	// 是否画marker
	private boolean drawMarkers_;
	// 是否画水平栅格线
	private boolean drawHorzGridLines_;
	// 是否画垂直栅格线
	private boolean drawVertGridLines_;
	// 水平栅格线的偏移量
	private int horzGridLineIndex_;
	// 垂直栅格线的偏移量
	private int vertGridLineIndex_;
	// Marker的边距
	private int markerMargin_;
	// Marker的半径
	private int markerRadius_;
	// 水平栅格线密度
	private int horzGridLineDensity_;
	// 数值栅格线密度
	private int vertGridLineDensity_;
	// 分段的数量
	private int segmentCount_;
	private long maxWaitingMs_;

	protected BaikalCameraException createException(BaikalCameraErrorDesc desc) {
		BaikalCameraException e = new BaikalCameraException(desc);
		return e;
	}

	@Override
	public synchronized int getImageByteSize() throws BaikalCameraException {
		return getWidth() * getHeight() * getBitDepth() / 8;
	}

	@Override
	public synchronized int getBitDepth() throws BaikalCameraException {
		if (!isConnected())
			throw BaikalCameraException
					.create(BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED);
		return bitDepth_;
	}

	public void setDrawMarkers(boolean val) {
		drawMarkers_ = val;
	}

	public void setDrawHorzGridLines(boolean val) {
		drawHorzGridLines_ = val;
	}

	public void setDrawVertGridLines(boolean val) {
		drawVertGridLines_ = val;
	}

	public void setHorzGridLineIndex(int index) {
		horzGridLineIndex_ = index;
	}

	public void setVertGridLineIndex(int index) {
		vertGridLineIndex_ = index;
	}

	public void setMarkerMargin(int val) {
		markerMargin_ = val;
	}

	public void setMarkerRadius(int val) {
		markerRadius_ = val;
	}

	public synchronized void setGridLineDensity(int horz, int vert) {
		horzGridLineDensity_ = horz;
		vertGridLineDensity_ = vert;
	}

	public void setSegmentCount(int val) {
		segmentCount_ = val;
	}

	@Override
	public synchronized BufferedImage snapshotAndWait()
			throws BaikalCameraException, InterruptedException {
		if (!isConnected())
			throw BaikalCameraException
					.create(BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED);

		BufferedImage image = null;
		try {
			long tic = System.nanoTime();
			int width = getWidth();
			int height = getHeight();
			image = bufferQ.poll(maxWaitingMs_, TimeUnit.MILLISECONDS);
			if (image == null)
				throw BaikalCameraException
						.create(BaikalCameraErrorDesc.BUFFER_UNDER_FLOW);
			if (!takenOutImages_.add(image))
				// 按理说，takenOutImages_里面应该没有image才对。
				throw BaikalCameraException
						.create(BaikalCameraErrorDesc.UNKNWON);

			// 绘制图形
			Graphics2D g2d = null;
			g2d = (Graphics2D) image.getGraphics();
			g2d.setBackground(Color.DARK_GRAY);
			g2d.clearRect(0, 0, width, height);

			int diameter = 2 * markerRadius_;
			int left = markerMargin_;
			int right = getWidth() - markerMargin_ - diameter;
			int top = markerMargin_;
			int bottom = getHeight() - markerMargin_ - diameter;
			int center_x = (left + right) / 2;
			int center_y = (top + bottom) / 2;

			// 绘制栅格线
			g2d.setStroke(new BasicStroke(2));
			g2d.setColor(Color.GREEN);
			// 确定纵横线的数量
			int nHGrids = horzGridLineDensity_ / segmentCount_;
			int nVGrids = vertGridLineDensity_ / segmentCount_;
			// 栅格间距
			double dHGrid = (double) (bottom - top) / horzGridLineDensity_;
			double dVGrid = (double) (right - left) / vertGridLineDensity_;
			// 绘制横线
			if (drawHorzGridLines_) {
				double dy = (bottom - top) / (double) nHGrids;
				for (int i = 0; i < nHGrids; i++) {
					int y = (int) (Math.round(top + 0.5 * diameter + dy * i
							+ horzGridLineIndex_ * dHGrid));
					g2d.drawLine((int) (Math.round((left + 0.5 * diameter))),
							y, (int) (Math.round((right + 0.5 * diameter))), y);
				}
			}
			// 绘制竖线
			if (drawVertGridLines_) {
				double dx = (right - left) / (double) nVGrids;
				for (int i = 0; i < nVGrids; i++) {
					int x = (int) (Math.round(left + 0.5 * diameter + dx * i
							+ vertGridLineIndex_ * dVGrid));
					g2d.drawLine(x, (int) (Math.round((top + 0.5 * diameter))),
							x, (int) (Math.round((bottom + 0.5 * diameter))));
				}
			}

			// 绘制marker
			if (drawMarkers_) {
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
			long dt = 1000000L * (long) exposureMs_ - (toc - tic);
			if (dt > 0) {
				TimeUnit.NANOSECONDS.sleep(dt);
			}

			return image;
		} catch (InterruptedException e) {
			if (image != null) {
				bufferQ.put(image);
				if (!takenOutImages_.remove(image))
					throw BaikalCameraException
							.create(BaikalCameraErrorDesc.UNKNWON);
			}
			throw e;
		}
	}

	@Override
	public synchronized void releaseImage(BufferedImage img)
			throws BaikalCameraException {
		if (!isConnected())
			throw BaikalCameraException
					.create(BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED);
		if (img == null)
			return;

		// img需要在takenOutImages_中注册过
		if (!takenOutImages_.contains(img))
			throw BaikalCameraException
					.create(BaikalCameraErrorDesc.INVALID_ARGUMENT);

		takenOutImages_.remove(img);
		if (!bufferQ.offer(img))
			throw BaikalCameraException.create(BaikalCameraErrorDesc.UNKNWON);
	}

	@Override
	public synchronized HashMap<Object, ArrayList<Integer>> getResolutionList()
			throws BaikalCameraException {
		if (!isConnected())
			throw BaikalCameraException
					.create(BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED);

		Iterator<Integer> it = resMap_.keySet().iterator();
		HashMap<Object, ArrayList<Integer>> result = new HashMap<Object, ArrayList<Integer>>();
		while (it.hasNext()) {
			Integer key = it.next();
			result.put(key, resMap_.get(key));
		}
		return result;
	}
}
