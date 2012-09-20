/**
 * 
 */
package org.zephyre.baikal.camera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.zephyre.baikal.camera.BaikalCameraException.BaikalCameraErrorDesc;

/**
 * A simulated camera
 * 
 * @author Zephyre
 * 
 */
public class BaikalSimCamera extends BaikalAbstractCamera {

	private static final int MAX_EXPOSURE = 60000;
	private boolean isConnected;
	private int resolution;
	private int exposure = 10;
	private ExecutorService exec;
	private int offset;
	// Various resolutions
	private static HashMap<Integer, ArrayList<Integer>> resMap;

	static {
		ArrayList<Integer> dim = new ArrayList<Integer>(2);
		resMap = new HashMap<Integer, ArrayList<Integer>>();
		dim.add(1200);
		dim.add(800);
		resMap.put(1, dim);
		dim = new ArrayList<Integer>(2);
		dim.add(1800);
		dim.add(1200);
		resMap.put(2, dim);
		dim = new ArrayList<Integer>(2);
		dim.add(3600);
		dim.add(2400);
		resMap.put(3, dim);
	}

	/**
	 * 
	 */
	public BaikalSimCamera() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.zephyre.baikal.BaikalAbstractCamera#connect()
	 */
	@Override
	public void connect() throws BaikalCameraException {
		isConnected = true;
		exec = Executors.newCachedThreadPool();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.zephyre.baikal.BaikalAbstractCamera#disconnect()
	 */
	@Override
	public void disconnect() {
		isConnected = false;
		exec.shutdownNow();
	}

	@Override
	public boolean isConnected() {
		return isConnected;
	}

	@Override
	protected void finalize() throws Throwable {
		if (isConnected) {
			System.err.println("Camera not closed!");
			disconnect();
		}
	}

	@Override
	public void setResolutioin(int resType) throws BaikalCameraException {
		if (!resMap.containsKey(resType)) {
			BaikalCameraException e = new BaikalCameraException();
			e.setDesc(BaikalCameraErrorDesc.INVALID_ARGUMENT);
			throw e;
		}
		resolution = resType;
	}

	@Override
	public int getWidth() throws BaikalCameraException {
		return resMap.get(resolution).get(0).intValue();
	}

	@Override
	public int getHeight() throws BaikalCameraException {
		return resMap.get(resolution).get(1).intValue();
	}

	@Override
	public void setExposureTime(int val) throws BaikalCameraException {
		if (val <= 0 || val > MAX_EXPOSURE) {
			BaikalCameraException e = new BaikalCameraException();
			e.setDesc(BaikalCameraErrorDesc.INVALID_ARGUMENT);
			throw e;
		}
		exposure = val;
	}

	@Override
	public int exposureTime() throws BaikalCameraException {
		return exposure;
	}

	@Override
	public void snapshot(byte[] buffer, Runnable callback)
			throws BaikalCameraException {
		// TODO Auto-generated method stub
	}

	private byte[] internalBuffer;

	@Override
	public void snapshotAndWait(byte[] buffer, ReentrantLock lock)
			throws BaikalCameraException {
		if (!isConnected)
			throw createException(BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED);

		if (internalBuffer == null
				|| internalBuffer.length != getImageByteSize()) {
			// Initialize internalBuffer
			internalBuffer = new byte[getImageByteSize()];
		}

		long tic = System.nanoTime();
		int width = getWidth();
		int height = getHeight();
		int bytesPerPixel = getBitDepth() / 8;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				internalBuffer[bytesPerPixel * (i * width + j)] = (byte) (i + offset);
				internalBuffer[bytesPerPixel * (i * width + j) + 1] = (byte) (i
						+ offset + 85);
				internalBuffer[bytesPerPixel * (i * width + j) + 2] = (byte) (i
						+ offset + 170);
			}
		}
		offset++;

		try {
			if (lock != null)
				lock.lockInterruptibly();
			System.arraycopy(internalBuffer, 0, buffer, 0,
					internalBuffer.length);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} finally {
			if (lock != null && lock.isHeldByCurrentThread())
				lock.unlock();
		}

		long toc = System.nanoTime();
		// Time cost: [exposure] ms
		long dt = 1000000L * (long) exposure - (toc - tic);
		if (dt > 0) {
			try {
				TimeUnit.NANOSECONDS.sleep(dt);
			} catch (InterruptedException e) {
			}
		}
	}

	private BaikalCameraException createException(BaikalCameraErrorDesc desc) {
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
		return 24;
	}
}
