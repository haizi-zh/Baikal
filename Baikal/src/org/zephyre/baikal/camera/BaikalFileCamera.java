package org.zephyre.baikal.camera;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.camera.BaikalCameraException.BaikalCameraErrorDesc;

/**
 * 模拟相机的一种，从硬盘上读取数据
 * 
 * @author Zephyre
 * 
 */
public class BaikalFileCamera extends BaikalAbstractCamera {
	private static BaikalFileCamera instance_;
	private ArrayList<String> imageFiles_ = new ArrayList<String>();
	private Iterator<String> imageItr_;
	private boolean isConnected_ = false;
	private int width_;
	private int height_;
	private int expMs_ = 100;
	private int pos_;

	private BaikalFileCamera() {
	}

	public static BaikalFileCamera getInstance() {
		if (instance_ == null)
			instance_ = new BaikalFileCamera();
		return instance_;
	}

	@Override
	public void connect() throws BaikalCameraException {
		isConnected_ = true;
	}

	public void setImageFiles(String[] files) {
		imageFiles_.clear();
		for (String f : files)
			imageFiles_.add(f);
		resetSeries();
	}

	@Override
	public void disconnect() {
		isConnected_ = false;
	}

	@Override
	public int getImageByteSize() throws BaikalCameraException {
		return getWidth() * getHeight() * getBitDepth() / 8;
	}

	@Override
	public int getBitDepth() throws BaikalCameraException {
		return 24;
	}

	public int getPos() {
		return pos_;
	}

	public void setPos(int pos) {
		resetSeries();
		for (int i = 0; i < pos; i++)
			imageItr_.next();
	}

	@Override
	public BufferedImage snapshotAndWait() throws BaikalCameraException,
			InterruptedException {
		if (imageFiles_.size() == 0)
			throw new BaikalCameraException(BaikalCameraErrorDesc.UNKNWON);

		if (!imageItr_.hasNext())
			resetSeries();

		try {
			String name = imageItr_.next();
			pos_++;
			BufferedImage image = ImageIO.read(new File(name));
			BaikalCore.log(name);
			return image;
		} catch (IOException e) {
			e.printStackTrace();
			BaikalCameraException exception = new BaikalCameraException(
					BaikalCameraErrorDesc.UNKNWON);
			exception.initCause(e);
		}
		return null;
	}

	@Override
	public void releaseImage(BufferedImage img) {
	}

	@Override
	public boolean isConnected() {
		return isConnected_;
	}

	@Override
	public void setResolution(Object resType) throws BaikalCameraException {
		throw BaikalCameraException.create(BaikalCameraErrorDesc.NOT_SUPPORTED);
	}

	public void resetSeries() {
		imageItr_ = imageFiles_.iterator();
		pos_ = 0;
	}

	@Override
	public int getWidth() throws BaikalCameraException {
		return width_;
	}

	@Override
	public int getHeight() throws BaikalCameraException {
		return height_;
	}

	@Override
	public void setExposureTime(int val) throws BaikalCameraException {
		expMs_ = val;
	}

	@Override
	public int getExposureMs() throws BaikalCameraException {
		return expMs_;
	}

	@Override
	public HashMap<Object, ArrayList<Integer>> getResolutionList()
			throws BaikalCameraException {
		// TODO Auto-generated method stub
		return null;
	}
}
