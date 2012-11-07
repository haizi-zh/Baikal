package org.zephyre.baikal.camera;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BaikalAbstractCamera {
	public abstract void connect() throws BaikalCameraException;

	public abstract void disconnect() throws BaikalCameraException;

	/*
	 * Get the size in bytes which is needed during a capture.
	 */
	public abstract int getImageByteSize() throws BaikalCameraException;

	/*
	 * The bit depth
	 */
	public abstract int getBitDepth() throws BaikalCameraException;

	/**
	 * 单张拍照（阻塞模式）。返回的BufferedImage使用完毕以后需要归还。
	 * 
	 * @return 图像
	 * @throws BaikalCameraException
	 * @throws InterruptedException
	 */
	public abstract BufferedImage snapshotAndWait()
			throws BaikalCameraException, InterruptedException;

	/**
	 * 将获取到的BufferedImage归还到相机的内部缓冲区序列中。
	 * 
	 * @param img
	 * @throws BaikalCameraException
	 */
	public abstract void releaseImage(BufferedImage img)
			throws BaikalCameraException;

	/*
	 * If the camera has been connected to PC
	 */
	public abstract boolean isConnected();

	/**
	 * Set the image size
	 * 
	 * @param resType
	 *            The resolution id, starts from 1
	 */
	public abstract void setResolution(Object resType)
			throws BaikalCameraException;

	/**
	 * Get supported resolutions
	 * 
	 * @return
	 * @throws BaikalCameraException
	 */
	public abstract HashMap<Object, ArrayList<Integer>> getResolutionList()
			throws BaikalCameraException;

	/*
	 * The image width.
	 */
	public abstract int getWidth() throws BaikalCameraException;

	/*
	 * The image height.
	 */
	public abstract int getHeight() throws BaikalCameraException;

	/*
	 * Set the exposure time in millisecond.
	 */
	public abstract void setExposureTime(int val) throws BaikalCameraException;

	/*
	 * Get the exposure time in millisecond.
	 */
	public abstract int getExposureMs() throws BaikalCameraException;

//	/**
//	 * misc数据
//	 */
//	protected HashMap<String, Object> optData_ = new HashMap<String, Object>();

//	public HashMap<String, Object> getOptData() {
//		HashMap<String, Object> data = new HashMap<String, Object>();
//		for (String key : optData_.keySet())
//			data.put(key, optData_.get(key));
//		return data;
//	}
}
