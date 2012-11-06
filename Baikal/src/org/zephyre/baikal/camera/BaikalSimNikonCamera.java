/**
 * 
 */
package org.zephyre.baikal.camera;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

/**
 * @author Zephyre
 * 
 */
public class BaikalSimNikonCamera extends BaikalSimCamera {

	private static BaikalSimNikonCamera instance_;

	protected BaikalSimNikonCamera() throws BaikalCameraException {
	}

	public static BaikalSimNikonCamera getInstance()
			throws BaikalCameraException {
		if (instance_ == null)
			instance_ = new BaikalSimNikonCamera();
		return instance_;
	}

	static {
		ArrayList<Integer> dim = new ArrayList<Integer>(2);
		resMap_ = new HashMap<Integer, ArrayList<Integer>>();
		dim.add(4068);
		dim.add(3456);
		resMap_.put(1, dim);
	}

	/**
	 * 采集：从文件夹中读取相应的文件
	 */
	private int acqIndex_ = 0;

	@Override
	public synchronized BufferedImage snapshotAndWait()
			throws BaikalCameraException, InterruptedException {

		// 采集模式（1：只生成marker图像，2：循环生成栅格图像）
		String acqModeKey = "AcqMode";
		int acqMode = 1;
		HashMap<String, Object> optData = getOptData();
		if (optData.containsKey(acqModeKey))
			acqMode = ((Number) optData.get(acqModeKey)).intValue();
		else
			optData.put(acqModeKey, new Integer(acqMode));

		long tic = System.nanoTime();
		BufferedImage bi = null;
		if (acqMode == 1) {
			IplImage tmpImage = cvLoadImage("res/samples/sample-marker.jpg");
			bi = tmpImage.getBufferedImage();
			opencv_core.cvReleaseImage(tmpImage);
		} else if (acqMode == 2) {
			bi = cvLoadImage(
					String.format("res/samples/sample-%2d", acqIndex_ + 1))
					.getBufferedImage();
			if (acqIndex_ == 19)
				acqIndex_ = 0;
			else
				acqIndex_++;
		} else
			bi = null;
		long toc = System.nanoTime();
		long dt = 1000000L * exposureMs() - (toc - tic);
		if (dt > 0) {
			TimeUnit.NANOSECONDS.sleep(dt);
		}
		return bi;
	}

	/**
	 * 返回acqIndex_
	 * 
	 * @return
	 */
	public int getAcqIndex() {
		return acqIndex_;
	}

	public void setAcqIndex(int val) {
		acqIndex_ = val;
	}

	@Override
	public synchronized void releaseImage(BufferedImage img)
			throws InterruptedException {
		return;
	}
}
