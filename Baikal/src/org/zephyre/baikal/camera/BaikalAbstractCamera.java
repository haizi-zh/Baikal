package org.zephyre.baikal.camera;

import java.util.concurrent.locks.ReentrantLock;

public abstract class BaikalAbstractCamera {
	public abstract void connect() throws BaikalCameraException;

	public abstract void disconnect();

	/*
	 * Take a single shot, fill the image into the buffer, and then call back
	 */
	public abstract void snapshot(byte[] buffer, Runnable callback)
			throws BaikalCameraException;

	/*
	 * Get the size in bytes which is needed during a capture.
	 */
	public abstract int getImageByteSize() throws BaikalCameraException;

	/*
	 * The bit depth
	 */
	public abstract int getBitDepth() throws BaikalCameraException;

	/*
	 * Take a single shot and wait for return.
	 */
	public abstract void snapshotAndWait(byte[] buffer, ReentrantLock lock)
			throws BaikalCameraException;

	/*
	 * If the camera has been connected to PC
	 */
	public abstract boolean isConnected();

	/*
	 * Set the image size
	 * 
	 * @param resType The resolution id, starts from 1
	 */
	public abstract void setResolutioin(int resType)
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
	public abstract int exposureTime() throws BaikalCameraException;

}
