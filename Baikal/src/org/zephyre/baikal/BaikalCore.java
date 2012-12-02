package org.zephyre.baikal;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.zephyre.baikal.camera.*;
import org.zephyre.baikal.gui.*;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvPoint3D32f;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

public class BaikalCore {
	public static class BaikalUserException extends Exception {
		public BaikalUserException() {
		}

		public BaikalUserException(Throwable e) {
			initCause(e);
		}
	}

	/**
	 * 处理后的图像
	 */
	private IplImage imgProcessed_;
	/**
	 * 图像的三个分量
	 */
	private IplImage[] imgChannels_;
	/**
	 * 绿色通道图像（即栅格图）的数据
	 */
	private byte[] gridImgBuf_;

	private static boolean isTick_ = true;
	private static long tic_;

	public static void tic_toc(String msg) {
		if (isTick_) {
			tic_ = System.nanoTime();
			isTick_ = false;
		} else {
			logger_.info(String.format("%s: %f", msg,
					(System.nanoTime() - tic_) / 1e6));
			isTick_ = true;
		}
	}

	public static void tic_toc() {
		tic_toc("");
	}

	/**
	 * 识别栅格图
	 * 
	 * @param inputbi
	 * @param horz
	 * @param markerList
	 * @param gridLineChannel
	 * @return
	 */
	public HashMap<String, Object> findGridLines(BufferedImage inputbi,
			int offset, boolean horz, double[][] markerList, int gridLineChannel) {
		HashMap<String, Object> opt = new HashMap<String, Object>();
		opt.put("ProcessedImage", imgProcessed_);
		opt.put("ImageChannels", imgChannels_);
		preProcess(inputbi, opt);
		imgProcessed_ = (IplImage) opt.get("ProcessedImage");
		imgChannels_ = (IplImage[]) opt.get("ImageChannels");
		IplImage imgGridLineChannel = imgChannels_[gridLineChannel];

		BufferedImage bufImage = imgProcessed_.getBufferedImage();
		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("ProcessedImage", bufImage);

		// 如果找到的marker数量小于9，则无法进行栅格图识别
		if (markerList.length < 9)
			return result;

		// 是否颠倒
		int horzUpSideDown = 1;
		int vertUpSideDown = -1;

		// 绿色通道数据
		ByteBuffer byteBuf = imgGridLineChannel.getByteBuffer();
		if (gridImgBuf_ == null || gridImgBuf_.length != byteBuf.capacity())
			gridImgBuf_ = new byte[byteBuf.capacity()];
		byteBuf.get(gridImgBuf_);
		// 线宽
		int lineWidth = ((Number) prefData_.get(PrefConst.LINE_WIDTH))
				.intValue();
		int widthStep = imgGridLineChannel.widthStep();
		// 确定纵横线的数量
		int segCount = ((Number) prefData_.get(PrefConst.SEGMENT_COUNT))
				.intValue();
		int nHorzGridLines = ((Number) prefData_.get(PrefConst.HORZ_DENSITY))
				.intValue() / segCount;
		int nVertGridLines = ((Number) prefData_.get(PrefConst.VERT_DENSITY))
				.intValue() / segCount;
		// 栅格的间距
		double horzInterval = (double) (markerList[7][1] - markerList[1][1])
				/ nHorzGridLines;
		double vertInterval = (double) (markerList[5][0] - markerList[3][0])
				/ nVertGridLines;
		// 最小栅格步进距离
		double dHStep = (double) (markerList[7][1] - markerList[1][1])
				/ ((Number) prefData_.get(PrefConst.HORZ_DENSITY)).intValue();
		double dVStep = (double) (markerList[5][0] - markerList[3][0])
				/ ((Number) prefData_.get(PrefConst.VERT_DENSITY)).intValue();

		// 确定起始扫描点：水平栅格：1号，垂直栅格：3号
		int[] currentPos = null;
		if (horz) {
			if (horzUpSideDown == 1)
				currentPos = new int[] { (int) Math.round(markerList[1][0]),
						(int) Math.round(markerList[1][1] + offset * dHStep) };
			else
				currentPos = new int[] { (int) Math.round(markerList[7][0]),
						(int) Math.round(markerList[7][1] - offset * dHStep) };
		} else {
			if (vertUpSideDown == 1)
				currentPos = new int[] {
						(int) Math.round(markerList[3][0] + offset * dVStep),
						(int) Math.round(markerList[3][1]) };
			else
				currentPos = new int[] {
						(int) Math.round(markerList[5][0] - offset * dVStep),
						(int) Math.round(markerList[5][1]) };
		}
		// 栅格线列表
		// LinkedList<double[][]> gridLineList = new LinkedList<double[][]>();
		ArrayList<GridLineModel> gridLineList = new ArrayList<GridLineModel>();

		// 如何判断一条栅格线已经扫描完毕？
		double gridLineThrd = ((Number) prefData_
				.get(PrefConst.GRID_LINE_THRESHOLD)).doubleValue();

		// 灰度重心法找栅格线的位置，需要沿着栅格方向按像素逐个计算。
		// 这两个变量描述了是往哪个方向走。（1,1表示沿着横向，往右走）
		int nGridLines = horz ? nHorzGridLines : nVertGridLines;
		for (int i = 0; i < nGridLines; i++) {
			// 灰度重心
			LinkedList<double[]> dataPoints = new LinkedList<double[]>();
			double stdSum = 0;
			// 标志：新扫描出来的点是添加在grid的头部还是尾部？
			boolean addAtHead = true;
			// 标志位：第一个扫描点
			boolean firstScanPoint = true;
			// 中间部分起始点的坐标
			int[] middlePoint = null;
			// 初始的扫描步长
			int scanStep = -1;
			do {
				double summary = 0;
				int count = 0;
				if (horz) {
					int top = currentPos[1] - lineWidth / 2;
					int bottom = currentPos[1] + lineWidth / 2;
					for (int j = top; j < bottom; j++) {
						int weight = 0xff & gridImgBuf_[widthStep * j
								+ currentPos[0]];
						summary += weight * j;
						count += weight;
					}
				} else {
					int left = currentPos[0] - lineWidth / 2;
					int right = currentPos[0] + lineWidth / 2;
					for (int j = left; j < right; j++) {
						int weight = 0xff & gridImgBuf_[widthStep
								* currentPos[1] + j];
						summary += weight * j;
						count += weight;
					}
				}
				// 如果是第一次循环，则建立标准的summary。
				// 如果之后的summary过小，则说明栅格线已经到头了，退出循环
				if (firstScanPoint) {
					firstScanPoint = false;
					if (horz)
						middlePoint = new int[] { currentPos[0],
								(int) Math.round((summary / count)) };
					else
						middlePoint = new int[] {
								(int) Math.round(summary / count),
								currentPos[1] };
					stdSum = summary;
					continue;
				} else {
					if (summary < gridLineThrd * stdSum) {
						if (scanStep == -1) {
							// 需要折返
							addAtHead = false;
							scanStep = 1;
							if (horz) {
								// 先往左走，再往右走，接下来就退出循环
								currentPos[0] = middlePoint[0] + scanStep;
								currentPos[1] = middlePoint[1];
							} else {
								// 先往上走，再往下走，接下来就退出循环
								currentPos[0] = middlePoint[0];
								currentPos[1] = middlePoint[1] + scanStep;
							}
						} else
							break;
					} else {
						// 找到有效的点，添加到grid里面
						double[] newPoint;
						if (horz)
							newPoint = new double[] { currentPos[0],
									summary / count };
						else
							newPoint = new double[] { summary / count,
									currentPos[1] };
						if (addAtHead)
							dataPoints.addFirst(newPoint);
						else
							dataPoints.addLast(newPoint);
						// 下一个扫描点
						if (horz) {
							currentPos[0] += scanStep;
							currentPos[1] = (int) Math.round(newPoint[1]);
						} else {
							currentPos[0] = (int) Math.round(newPoint[0]);
							currentPos[1] += scanStep;
						}
					}
				}
			} while (true);

			double[][] dataPointsArray = new double[dataPoints.size()][2];
			Iterator<double[]> it = dataPoints.iterator();
			int index = 0;
			while (it.hasNext()) {
				double[] pos = it.next();
				dataPointsArray[index][0] = pos[0];
				dataPointsArray[index][1] = pos[1];
				index++;
			}
			gridLineList.add(new GridLineModel(dataPointsArray,
					horz ? GridLineModel.GRID_LINE_HORIZONTAL
							: GridLineModel.GRID_LINE_VERTICAL));
			// 确定扫描下一条栅格线的起始点
			if (horz) {
				currentPos[0] = middlePoint[0];
				currentPos[1] = (int) (middlePoint[1] + horzUpSideDown
						* Math.round(horzInterval));
			} else {
				currentPos[0] = (int) (middlePoint[0] + vertUpSideDown
						* Math.round(vertInterval));
				currentPos[1] = middlePoint[1];
			}
		}
		result.put("GridLines", gridLineList);
		return result;
	}

	/**
	 * 识别marker
	 * 
	 * @param inputbi
	 *            输入的图像
	 * @param markerChannel
	 *            marker所在的channel编号
	 * @return
	 */
	public HashMap<String, Object> measureMarkers(BufferedImage inputbi,
			int markerChannel) {
		HashMap<String, Object> opt = new HashMap<String, Object>();
		opt.put("ProcessedImage", imgProcessed_);
		opt.put("ImageChannels", imgChannels_);
		preProcess(inputbi, opt);
		imgProcessed_ = (IplImage) opt.get("ProcessedImage");
		imgChannels_ = (IplImage[]) opt.get("ImageChannels");

		double[][] markerList = doHoughCircle(imgChannels_[markerChannel]);

		BufferedImage bufImage = imgProcessed_.getBufferedImage();
		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("ProcessedImage", bufImage);
		result.put("MarkerList", markerList);
		return result;
	}

	/**
	 * 图像的预处理，包括：平滑，降噪，阈值化，通道提取等。
	 * 
	 * @param inputbi
	 *            原始图像
	 * @param imgPlaceHolders
	 *            提供预先分配好的图像，包括： "ProcessedImage"：存放处理完成的图像。
	 *            "ImageChannels"：存放三个通道的图像。
	 * @return
	 */
	public void preProcess(BufferedImage inputbi,
			HashMap<String, Object> imgPlaceHolders) {
		// 准备图像
		IplImage inputIpl = IplImage.createFrom(inputbi);
		IplImage processedbi = (IplImage) imgPlaceHolders.get("ProcessedImage");
		if (processedbi == null || inputIpl.width() != processedbi.width()
				|| inputIpl.height() != processedbi.height()
				|| inputIpl.nChannels() != processedbi.nChannels()
				|| inputIpl.imageSize() != processedbi.imageSize()) {
			if (processedbi != null)
				processedbi.release();
			processedbi = IplImage.createCompatible(inputIpl);
			log("preProcessed: new processedbi created.");
		}

		IplImage[] imageChannels = (IplImage[]) imgPlaceHolders
				.get("ImageChannels");
		if (imageChannels == null)
			imageChannels = new IplImage[3];
		if (imageChannels[0] == null
				|| imageChannels[0].width() != inputIpl.width()
				|| imageChannels[0].height() != inputIpl.height()
				|| imageChannels[0].nChannels() != 1) {
			for (IplImage img : imageChannels) {
				if (img != null)
					img.release();
			}
			for (int i = 0; i < 3; i++)
				imageChannels[i] = IplImage.create(inputIpl.width(),
						inputIpl.height(), inputIpl.depth(), 1);
			log("preProcessed: imageChannels created.");
		}

		// 平滑和阈值化
		@SuppressWarnings("unchecked")
		ArrayList<Number> thrdList = (ArrayList<Number>) prefData_
				.get(PrefConst.THRESHOLD);
		@SuppressWarnings("unchecked")
		int winsize = ((ArrayList<Number>) prefData_
				.get(PrefConst.SMOOTH_WINDOW)).get(0).intValue();
		@SuppressWarnings("unchecked")
		double smoothParam = ((ArrayList<Number>) prefData_
				.get(PrefConst.SMOOTH_PARAM)).get(0).doubleValue();
		cvSmooth(inputIpl, processedbi, CV_BLUR, winsize, winsize, smoothParam,
				smoothParam);
		inputIpl.release();
		// 分离通道
		cvSplit(processedbi, imageChannels[0], imageChannels[1],
				imageChannels[2], null);
		for (int i = 0; i < 3; i++) {
			cvThreshold(imageChannels[i], imageChannels[i], thrdList.get(i)
					.doubleValue(), 0, CV_THRESH_TOZERO);
		}
		opencv_core.cvMerge(imageChannels[0], imageChannels[1],
				imageChannels[2], null, processedbi);

		// cvThreshold(processedbi, processedbi, thrd, 0, CV_THRESH_TOZERO);

		imgPlaceHolders.put("ProcessedImage", processedbi);
		imgPlaceHolders.put("ImageChannels", imageChannels);
		return;
	}

	// /**
	// * LiveView模式下的图像处理函数。
	// *
	// * @param bi
	// * 输入的原始图像
	// * @returns
	// */
	// public HashMap<String, Object> liveViewProcessing(BufferedImage bi) {
	//
	// HashMap<String, Object> opt = new HashMap<String, Object>();
	// opt.put("ProcessedImage", imgProcessed_);
	// opt.put("ImageChannels", imgChannels_);
	// preProcess(bi, opt);
	// imgProcessed_ = (IplImage) opt.get("ProcessedImage");
	// imgChannels_ = (IplImage[]) opt.get("ImageChannels");
	//
	// double[][] markerList = doHoughCircle();
	//
	// // findGrids(markerList);
	//
	// BufferedImage bufImage = imgProcessed_.getBufferedImage();
	//
	// HashMap<String, Object> hm = new HashMap<String, Object>();
	// hm.put("ProcessedImage", bufImage);
	// hm.put("MarkerList", markerList);
	// return hm;
	// }

	/**
	 * Hough Circle变换，寻找圆。
	 * 
	 * @param image
	 */
	private double[][] doHoughCircle(IplImage channelImage) {
		double dp = ((Number) prefData_.get(PrefConst.HOUGH_CIRCLE_DP))
				.doubleValue();
		double minDist = ((Number) prefData_
				.get(PrefConst.HOUGH_CIRCLE_MIN_DIST)).doubleValue();
		double cannyThrd = ((Number) prefData_
				.get(PrefConst.HOUGH_CIRCLE_HIGH_THRESHOLD)).doubleValue();
		double accThrd = ((Number) prefData_
				.get(PrefConst.HOUGH_CIRCLE_ACC_THRESHOLD)).doubleValue();
		int minRadius = ((Number) prefData_
				.get(PrefConst.HOUGH_CIRCLE_MIN_RADIUS)).intValue();
		int maxRadius = ((Number) prefData_
				.get(PrefConst.HOUGH_CIRCLE_MAX_RADIUS)).intValue();

		// 提取圆心
		CvSeq circles = cvHoughCircles(channelImage, cvStorage_,
				CV_HOUGH_GRADIENT, dp, minDist, cannyThrd, accThrd, minRadius,
				maxRadius);

		CvPoint3D32f[] clist = new CvPoint3D32f[circles.total()];
		for (int i = 0; i < clist.length; i++) {
			clist[i] = new CvPoint3D32f(cvGetSeqElem(circles, i));
		}

		// 如果识别到的marker个数大于或者等于9，说明全部marker都已被找到，可以进行排序
		final int NUM_MARKERS = 9;
		if (clist.length >= NUM_MARKERS) {
			// 先按照y进行排序
			Arrays.sort(clist, new Comparator<CvPoint3D32f>() {
				@Override
				public int compare(CvPoint3D32f o1, CvPoint3D32f o2) {
					return (o1.y() > o2.y() ? 1 : -1);
				}
			});

			// 每一排再按照x进行排序
			for (int i = 0; i < 3; i++) {
				Arrays.sort(clist, i * 3, (i + 1) * 3,
						new Comparator<CvPoint3D32f>() {
							@Override
							public int compare(CvPoint3D32f o1, CvPoint3D32f o2) {
								return (o1.x() > o2.x() ? 1 : -1);
							}
						});
			}
		}

		double[][] result = new double[clist.length][3];
		for (int i = 0; i < result.length; i++) {
			result[i][0] = clist[i].x();
			result[i][1] = clist[i].y();
			result[i][2] = clist[i].z();
		}
		return result;
	}

	private static BaikalCore instance_;

	/**
	 * 返回BaikalCore的实例。
	 * 
	 * @return
	 */
	public static BaikalCore getInstance() {
		return instance_;
	}

	/**
	 * 初始化BaikalCore
	 * 
	 * @param userName
	 * @return
	 * @throws JsonParseException
	 *             配置文件解析错误。
	 * @throws BaikalCameraException
	 *             相机初始化错误。
	 * @throws IOException
	 *             IO错误。
	 */
	public static BaikalCore createInstance_(String userName)
			throws JsonParseException, BaikalCameraException, IOException {
		if (instance_ == null)
			instance_ = new BaikalCore(userName);
		return instance_;
	}

	/**
	 * 获得用户名列表。
	 * 
	 * @return
	 */
	public static String[] getUserNames() {
		File path = new File(System.getProperty("user.home"), "Baikal");
		path.mkdirs();
		File[] files = path.listFiles();
		if (files == null) {
			return new String[0];
		}
		ArrayList<String> userNames = new ArrayList<String>();
		String ext = ".ini";
		for (File f : files) {
			String name = f.getName();
			int index = name.lastIndexOf(".");
			if (index != -1 && name.substring(index).equals(ext))
				userNames.add(name.substring(0, index));
		}
		return userNames.toArray(new String[0]);
	}

	/**
	 * 添加新用户。
	 * 
	 * @param userName
	 *            用户名
	 * @throws BaikalUserException
	 *             该用户已存在。
	 * @throws IOException
	 *             添加用户失败（IO错误）。
	 */
	public static void addUser(String userName) throws BaikalUserException,
			IOException {
		if (isUserExist(userName))
			throw new BaikalUserException();

		HashMap<String, Object> pref = new HashMap<String, Object>();
		checkAndFillInDefaults(pref, userName);
		writePref(pref);
	}

	/**
	 * 查询某个用户是否存在
	 * 
	 * @param userName
	 * @return true 如果用户存在，false 如果用户不存在。
	 */
	public static boolean isUserExist(String userName) {
		File path = new File(System.getProperty("user.home"), "Baikal");
		File file = new File(path, userName + ".ini");
		return (file.isFile() && file.exists());
	}

	/**
	 * 删除用户
	 * 
	 * @param userName
	 *            用户名
	 * @throws BaikalUserException
	 *             删除用户失败
	 */
	public static void removeUser(String userName) throws BaikalUserException {
		File path = new File(System.getProperty("user.home"), "Baikal");
		File file = new File(path, userName + ".ini");
		if (!file.delete())
			throw new BaikalUserException();
	}

	private BaikalAbstractCamera cam_;
	private Map<String, Object> prefData_;
	private HashMap<String, String[]> devList_;
	private static Logger logger_;

	/**
	 * 读取JSON格式的设置数据。
	 * 
	 * @param userName
	 *            用户名。设置数据文件名为：[userName].ini
	 * @return
	 * @throws BaikalUserException
	 *             用户数据读取异常（文件不存在，文件读取错误，JSON解析错误等）
	 * @throws IOException
	 *             文件IO异常。
	 * @throws JsonIOException
	 * @throws JsonSyntaxException
	 *             JSON解析错误
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> loadPref(String userName)
			throws IOException, JsonIOException, JsonSyntaxException {
		BufferedReader reader = null;
		HashMap<String, Object> pref = null;
		try {
			File path = new File(System.getProperty("user.home"), "Baikal");
			File file = new File(path, userName + ".ini");
			log(String.format("Loading user data: %s", file.getAbsolutePath()));
			reader = new BufferedReader(new FileReader(file));
			Gson gson = new Gson();
			Type typeOfT = new TypeToken<HashMap<String, Object>>() {
			}.getType();
			pref = gson.fromJson(reader, typeOfT);
		} finally {
			if (reader != null)
				reader.close();
		}
		checkAndFillInDefaults(pref, userName);
		return pref;
	}

	/**
	 * 如果pref中缺少某些字段，则使用默认值将其填充。
	 * 
	 * @param pref
	 * @param userName
	 */
	private static void checkAndFillInDefaults(HashMap<String, Object> pref,
			String userName) {
		if (!pref.containsKey(PrefConst.NAME))
			pref.put(PrefConst.NAME, userName);
		if (!pref.containsKey(PrefConst.MIRROR_COUNT))
			pref.put(PrefConst.MIRROR_COUNT, 0);
		if (!pref.containsKey(PrefConst.MIRROR_WIDTH))
			pref.put(PrefConst.MIRROR_WIDTH, 1000.0);
		if (!pref.containsKey(PrefConst.MIRROR_HEIGHT))
			pref.put(PrefConst.MIRROR_HEIGHT, 1000.0);
		if (!pref.containsKey(PrefConst.CAMERA_MODEL))
			pref.put(PrefConst.CAMERA_MODEL, "");
		if (!pref.containsKey(PrefConst.LENS_MODEL))
			pref.put(PrefConst.LENS_MODEL, "");
		if (!pref.containsKey(PrefConst.SHUTTER))
			pref.put(PrefConst.SHUTTER, 100);
		if (!pref.containsKey(PrefConst.APERTURE))
			pref.put(PrefConst.APERTURE, 1);
		if (!pref.containsKey(PrefConst.HORZ_DENSITY))
			pref.put(PrefConst.HORZ_DENSITY, 100);
		if (!pref.containsKey(PrefConst.VERT_DENSITY))
			pref.put(PrefConst.VERT_DENSITY, 100);
		if (!pref.containsKey(PrefConst.ISO_SPEED))
			pref.put(PrefConst.ISO_SPEED, 100);
		if (!pref.containsKey(PrefConst.RESOLUTION))
			pref.put(PrefConst.RESOLUTION, Integer.valueOf(1));
		if (!pref.containsKey(PrefConst.MARKER_RADIUS))
			pref.put(PrefConst.MARKER_RADIUS, 15);
		if (!pref.containsKey(PrefConst.SEGMENT_COUNT))
			pref.put(PrefConst.SEGMENT_COUNT, 10);
		if (!pref.containsKey(PrefConst.THRESHOLD)) {
			ArrayList<Double> al = new ArrayList<Double>();
			for (int i = 0; i < 3; i++)
				al.add(0.0);
			pref.put(PrefConst.THRESHOLD, al);
		}
		if (!pref.containsKey(PrefConst.SMOOTH_WINDOW)) {
			ArrayList<Integer> al = new ArrayList<Integer>();
			al.add(3);
			al.add(3);
			pref.put(PrefConst.SMOOTH_WINDOW, al);
		}
		if (!pref.containsKey(PrefConst.SMOOTH_PARAM)) {
			ArrayList<Double> al = new ArrayList<Double>();
			al.add(0.0);
			al.add(0.0);
			pref.put(PrefConst.SMOOTH_PARAM, al);
		}
		if (!pref.containsKey(PrefConst.HOUGH_CIRCLE_DP))
			pref.put(PrefConst.HOUGH_CIRCLE_DP, 1.0);
		if (!pref.containsKey(PrefConst.HOUGH_CIRCLE_MIN_DIST))
			pref.put(PrefConst.HOUGH_CIRCLE_MIN_DIST, 800);
		if (!pref.containsKey(PrefConst.HOUGH_CIRCLE_HIGH_THRESHOLD))
			pref.put(PrefConst.HOUGH_CIRCLE_HIGH_THRESHOLD, 200);
		if (!pref.containsKey(PrefConst.HOUGH_CIRCLE_ACC_THRESHOLD))
			pref.put(PrefConst.HOUGH_CIRCLE_ACC_THRESHOLD, 4.5);
		if (!pref.containsKey(PrefConst.HOUGH_CIRCLE_MIN_RADIUS))
			pref.put(PrefConst.HOUGH_CIRCLE_MIN_RADIUS, 50);
		if (!pref.containsKey(PrefConst.HOUGH_CIRCLE_MAX_RADIUS))
			pref.put(PrefConst.HOUGH_CIRCLE_MAX_RADIUS, 100);
		if (!pref.containsKey(PrefConst.LINE_WIDTH))
			pref.put(PrefConst.LINE_WIDTH, 10);
		if (!pref.containsKey(PrefConst.MARKER_MARGIN))
			pref.put(PrefConst.MARKER_MARGIN, 24);
		if (!pref.containsKey(PrefConst.THREAD_COUNT))
			pref.put(PrefConst.THREAD_COUNT, 2);
		if (!pref.containsKey(PrefConst.POLYFIT_SEG_COUNT))
			pref.put(PrefConst.POLYFIT_SEG_COUNT, 20);
		if (!pref.containsKey(PrefConst.GRID_LINE_THRESHOLD))
			pref.put(PrefConst.GRID_LINE_THRESHOLD, 0.1);
		if (!pref.containsKey(PrefConst.GRID_FRAME_POS_SIZE)) {
			Dimension scrDim = Toolkit.getDefaultToolkit().getScreenSize();
			int width = 600;
			int height = 480;
			int x = (scrDim.width - width) / 2;
			int y = (scrDim.height - height) / 2;
			int[] vals = new int[] { x, y, width, height };
			ArrayList<Integer> posSize = new ArrayList<Integer>();
			for (int v : vals)
				posSize.add(Integer.valueOf(v));
			pref.put(PrefConst.GRID_FRAME_POS_SIZE, posSize);
		}
	}

	/**
	 * 保存JSON格式的设置数据。
	 * 
	 * @param pref
	 *            TODO
	 * 
	 * @throws IOException
	 *             写文件IO异常。
	 */
	public static void writePref(Map<String, Object> pref) throws IOException {
		String userName = (String) pref.get(PrefConst.NAME);
		BufferedWriter writer = null;
		try {
			File path = new File(System.getProperty("user.home"), "Baikal");
			path.mkdirs();
			File file = new File(path, userName + ".ini");
			log(String.format("Writing user data: %s", file.getAbsolutePath()));
			writer = new BufferedWriter(new FileWriter(file));
			Gson gson = new Gson();
			writer.write(gson.toJson(pref));
		} catch (IOException e) {
			throw e;
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	public void writePref() throws IOException {
		writePref(prefData_);
	}

	public static class PrefConst {
		public static final String CAMERA_LIST = "CameraList";
		public static final String LENS_LIST = "LensList";
		public final static String NAME = "Name";
		public final static String MIRROR_COUNT = "MirrorCount";
		public final static String UUID = "UUID";
		public final static String MIRROR_WIDTH = "MirrorWidth";
		public final static String MIRROR_HEIGHT = "MirrorHeight";
		public final static String CAMERA_MODEL = "CameraModel";
		public final static String LENS_MODEL = "LensModel";
		public final static String SHUTTER = "Shutter";
		public final static String APERTURE = "Aperture";
		public final static String HORZ_DENSITY = "DensityX";
		public final static String VERT_DENSITY = "DensityY";
		public final static String MARKER_RADIUS = "MarkerRadius";
		public final static String SEGMENT_COUNT = "SegmentCount";
		public final static String ISO_SPEED = "ISOSpeed";
		public final static String RESOLUTION = "Resolution";
		public final static String THRESHOLD = "Threshold";
		public final static String SMOOTH_WINDOW = "SmoothWindow";
		public final static String SMOOTH_PARAM = "SmoothParam";
		public final static String HOUGH_CIRCLE_DP = "HoughCircle_dp";
		public final static String HOUGH_CIRCLE_MIN_DIST = "HoughCircle_minDist";
		public final static String HOUGH_CIRCLE_HIGH_THRESHOLD = "HoughCircle_highThreshold";
		public final static String HOUGH_CIRCLE_ACC_THRESHOLD = "HoughCircle_accThreshold";
		public final static String HOUGH_CIRCLE_MIN_RADIUS = "HoughCircle_minRadius";
		public final static String HOUGH_CIRCLE_MAX_RADIUS = "HoughCircle_maxRadius";
		public static final String SUPPORTED_CAMERAS = "SupportedCameras";
		public static final String SUPPORTED_LENSES = "SupportedLenses";
		public static final String LINE_WIDTH = "LineWidth";
		public static final String MARKER_MARGIN = "MarkerMargin";
		// 处理图像时的最大线程个数，和CPU核心的个数有关
		public static final String THREAD_COUNT = "ThreadCount";
		public static final String POLYFIT_SEG_COUNT = "PolyFitSegCount";
		public static final String GRID_LINE_THRESHOLD = "GridLineThreshold";
		// Grid窗口的位置和大小
		public static final String GRID_FRAME_POS_SIZE = "GridFramePosSize";
	}

	/**
	 * 日志
	 * 
	 * @param msg
	 */
	public static void log(String msg) {
		logger_.info(msg);
	}

	/**
	 * 日志（Level）
	 * 
	 * @param level
	 * @param msg
	 */
	public static void log(Level level, String msg) {
		logger_.log(level, msg);
	}

	/**
	 * 返回logger
	 * 
	 * @return
	 */
	public static Logger getLogger() {
		return logger_;
	}

	static {
		// Logging
		logger_ = Logger.getLogger("org.zephyre.baikal");
		logger_.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		logger_.addHandler(handler);
	}

	/**
	 * Initialize the BaikalCore object with the configuration file.
	 * 
	 * @param userName
	 *            Name of the configuration file.
	 * @throws IOException
	 * @throws BaikalCameraException
	 * @throws JsonParseException
	 */
	private BaikalCore(String userName) throws BaikalCameraException,
			JsonParseException, IOException {

		prefData_ = Collections.synchronizedMap(loadPref(userName));
		devList_ = loadSupportedDevices();

		// Initialize devices

		CanonEOSCamera cam = CanonEOSCamera.getInstance();
		CanonEOSCamera.isSimulated_ = true;
		String[] images = new String[9];
		for (int i = 0; i < images.length; i++)
			images[i] = String.format("res\\samples\\sample-5d-%d.JPG", i + 1);
		cam.setImageFiles(images);
		cam_ = cam;

		// cam_ = BaikalSimCamera.getInstance();

		// OpenCV相关
		cvStorage_ = cvCreateMemStorage(0);
		imagesList = new IplImage[4];

		instance_ = this;
	}

	/**
	 * @throws BaikalCameraException
	 */
	private void loadSimCamera() throws BaikalCameraException {
		cam_.connect();
		if (cam_ instanceof BaikalSimCamera) {
			BaikalSimCamera cam = (BaikalSimCamera) cam_;
			cam.connect();
			cam.setDrawMarkers(false);
			cam.setDrawHorzGridLines(false);
			cam.setDrawVertGridLines(false);
			cam.setHorzGridLineIndex(0);
			cam.setVertGridLineIndex(0);
			cam.setExposureTime(((Number) prefData_.get(PrefConst.SHUTTER))
					.intValue());
			cam.setMarkerMargin(((Number) prefData_
					.get(PrefConst.MARKER_MARGIN)).intValue());
			cam.setMarkerRadius(((Number) prefData_
					.get(PrefConst.MARKER_RADIUS)).intValue());
			cam.setGridLineDensity(
					((Number) prefData_.get(PrefConst.HORZ_DENSITY)).intValue(),
					((Number) prefData_.get(PrefConst.VERT_DENSITY)).intValue());
			cam.setSegmentCount(((Number) prefData_
					.get(PrefConst.SEGMENT_COUNT)).intValue());
			int res = ((Number) prefData_.get(PrefConst.RESOLUTION)).intValue();
			cam.setResolution(Integer.valueOf(res));
		}
	}

	/**
	 * 读取generalData信息，比如：支持的相机列表等等。
	 * 
	 * @return
	 */
	private HashMap<String, Object> loadGeneralData() {
		HashMap<String, Object> gd = new HashMap<String, Object>();

		// 支持的相机列表
		String[] supportedCam = new String[] { "Canon EOS 5D",
				"Canon EOS 5D Mark II", "Canon EOS 5D Mark III" };
		// 支持的镜头列表
		String[] supportedLenses = new String[] { "Canon EF 50mm f/1.4 USM",
				"Canon EF 100mm f/2.8L IS USM", "Canon EF 35mm f/2" };

		gd.put(PrefConst.SUPPORTED_CAMERAS, supportedCam);
		gd.put(PrefConst.SUPPORTED_LENSES, supportedLenses);
		return gd;
	}

	// /**
	// * Get the user-specific configuration.
	// */
	// public Map<String, Object> getPrefData() {
	// return prefData_;
	// }

	public Object getEntry(String key) {
		return prefData_.get(key);
	}

	public void putEntry(String key, Object value) {
		prefData_.put(key, value);
	}

	/**
	 * 返回支持设备列表。
	 * 
	 * @return
	 */
	public HashMap<String, String[]> getDeviceList() {
		return devList_;
	}

	/**
	 * Initialize and load all the devices
	 */
	public void loadAllDevices() throws BaikalCameraException {
		loadSimCamera();
	}

	/**
	 * Connect to the default camrea.
	 */
	public void connectCamera() throws BaikalCameraException {
		cam_.connect();
	}

	/**
	 * Set the exposure time in millisecond.
	 */
	public void setExposureTime(int val) throws BaikalCameraException {
		cam_.setExposureTime(val);
	}

	/**
	 * Get the exposure time in millisecond.
	 */
	public int exposureTime() throws BaikalCameraException {
		return cam_.getExposureMs();
	}

	/**
	 * Set the resolution type.
	 */
	public void setResolution(int res) throws BaikalCameraException {
		cam_.setResolution(res);
	}

	/**
	 * Get the camera object.
	 */
	public BaikalAbstractCamera getCamera() {
		return cam_;
	}

	// private IplImage imageAll;
	// Layout: 0, 1, 2, All
	private IplImage[] imagesList;
	// private IplImage imageRed;
	// private IplImage imageGreen;
	// private IplImage imageBlue;
	private CvMemStorage cvStorage_;
	private double[][] markerPos;

	/**
	 * OpenCV处理：寻找marker中心
	 * 
	 * @param channel
	 *            TODO
	 * @param threshold
	 *            TODO
	 * @param data
	 * @param width
	 * @param height
	 * @param dep
	 * @param numChannels
	 * @return int[#circles][3]，第一个指标为circle个数，第二个指标为：[x, y, radius]
	 */
	public double[][] findCircles(int channel, int dp, double minDist,
			double highThreshold, double accThreshold, int minRadius,
			int maxRadius, int threshold) {
		IplImage image = imagesList[channel];

		// 平滑，降噪。
		cvSmooth(image, image, CV_GAUSSIAN, 9, 9, 2, 2);
		cvThreshold(image, image, threshold, 0, CV_THRESH_TOZERO);

		// 提取圆心
		CvSeq circles = cvHoughCircles(image, cvStorage_, CV_HOUGH_GRADIENT,
				dp, minDist, highThreshold, accThreshold, minRadius, maxRadius);

		double[][] result = new double[circles.total()][3];
		for (int i = 0; i < circles.total(); i++) {
			CvPoint3D32f pt = new CvPoint3D32f(cvGetSeqElem(circles, i));
			CvPoint2D32f center = new CvPoint2D32f(pt.x(), pt.y());
			float radius = pt.z();
			result[i][0] = center.x();
			result[i][1] = center.y();
			result[i][2] = radius;
		}

		System.out.println(String.format("Found: %d circles", result.length));
		for (int i = 0; i < result.length; i++) {
			System.out.println(String.format("Circle #%d: %f, %f, %f", i,
					result[i][0], result[i][1], result[i][2]));
		}

		// 给9个marker排序，从左到右，然后从上到下
		// 所有的坐标排序，同时生成双向字典，可以从x查对应的y，也可以从y查对应的x
		double[] toutx = new double[9];
		double[] touty = new double[9];
		HashMap<Double, Double> x2y = new HashMap<Double, Double>();
		HashMap<Double, Double> y2x = new HashMap<Double, Double>();
		for (int i = 0; i < 9; i++) {
			toutx[i] = result[i][0];
			touty[i] = result[i][1];
			x2y.put(result[i][0], result[i][1]);
			y2x.put(result[i][1], result[i][0]);
		}
		Arrays.sort(touty);

		double[][] rt = new double[result.length][3];
		for (int i = 0; i < 3; i++) {
			// 考察第i排
			double[] temp = new double[3];
			for (int j = 0; j < 3; j++) {
				temp[j] = y2x.get(touty[i * 3 + j]);
			}
			Arrays.sort(temp);
			for (int j = 0; j < 3; j++) {
				double x = temp[j];
				double y = x2y.get(temp[j]);
				rt[i * 3 + j] = new double[] { x, y, 0 };
				// 查找对应的半径
				for (int k = 0; k < result.length; k++) {
					if (result[k][0] == x && result[k][1] == y) {
						rt[i * 3 + j][2] = result[k][2];
						break;
					}
				}
			}
		}

		markerPos = rt;
		return rt;
	}

	/**
	 * 设置定位完成的marker坐标及半径。
	 * 
	 * @param pos
	 *            marker的坐标及半径。
	 */
	public void setMarkerPos(double[][] pos) {
		markerPos = pos;
	}

	/**
	 * 初始化IplImageAll及其三个通道
	 * 
	 * @param width
	 * @param height
	 */
	public void initIplImages(byte[] data, int channel, int width, int height) {
		IplImage imageAll = imagesList[3];
		if (imageAll == null || imageAll.width() != width
				|| imageAll.height() != height
				|| imageAll.nChannels() != channel) {
			imagesList[3] = cvCreateImageHeader(new CvSize(width, height),
					IPL_DEPTH_8U, 3);
			imageAll = imagesList[3];
		}
		if (data != null) {
			imagesList[3].imageData(new BytePointer(ByteBuffer.wrap(data)));
		}
		for (int i = 0; i < 3; i++) {
			// 初始化分通道IplImage
			IplImage image = imagesList[i];
			if (image == null || image.width() != width
					|| image.height() != height) {
				imagesList[i] = cvCreateImage(new CvSize(width, height),
						IPL_DEPTH_8U, 1);
			}
		}
		cvSplit(imageAll, imagesList[0], imagesList[1], imagesList[2], null);
	}

	/**
	 * 获得支持设备列表。
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, String[]> loadSupportedDevices() throws IOException,
			JsonIOException, JsonSyntaxException {
		File path = new File(System.getProperty("user.home"), "Baikal");
		File file = new File(path, "devicelist.cfg");
		Gson gson = new Gson();
		BufferedReader reader = null;
		HashMap<String, String[]> dl = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			Type typeOfT = new TypeToken<HashMap<String, String[]>>() {
			}.getType();
			dl = gson.fromJson(reader, typeOfT);
		} catch (FileNotFoundException e) {
			// 默认支持设备
			dl = new HashMap<String, String[]>();
			dl.put(PrefConst.CAMERA_LIST, new String[0]);
			dl.put(PrefConst.LENS_LIST, new String[0]);
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(file));
				writer.write(gson.toJson(dl));
			} catch (IOException e1) {
				throw e1;
			} finally {
				if (writer != null)
					try {
						writer.close();
					} catch (IOException e1) {
						throw e1;
					}
			}
		} finally {
			if (reader != null)
				reader.close();
		}
		return dl;
	}

	/**
	 * 确定交叉点的坐标
	 * 
	 * @param horzGrindLines
	 * @param vertGrindLines
	 */
	public double[][] findIntersections(
			ArrayList<? extends GridLineModel> horzGrindLines,
			ArrayList<? extends GridLineModel> vertGrindLines) {
		int width = vertGrindLines.size();
		int height = horzGrindLines.size();

		// 拟合
		Iterator<? extends GridLineModel> it = horzGrindLines.iterator();
		while (it.hasNext()) {
			polyFitSegments(it.next());
		}
		it = vertGrindLines.iterator();
		while (it.hasNext()) {
			polyFitSegments(it.next());
		}

		int segCount = ((Number) prefData_.get(PrefConst.POLYFIT_SEG_COUNT))
				.intValue();
		// 交点
		double[][] intersections = new double[height * width][2];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				// 栅格线
				GridLineModel hline = horzGrindLines.get(i);
				GridLineModel vline = vertGrindLines.get(j);
				double[] hStartPt = hline.getDataPoints()[0];
				double[] vStartPt = vline.getDataPoints()[0];
				int xoffset = (int) Math.round(hStartPt[0]);
				int yoffset = (int) Math.round(vStartPt[1]);

				// 找到合适的分段号，在该区间内寻找交点
				int horzSeg = (int) ((double) j / width * segCount);
				int vertSeg = (int) ((double) i / height * segCount);

				double x = 0;
				double y = 0;
				do {
					// 交点所在的区间
					int[] xEnds = new int[2];
					System.arraycopy(hline.getPolyFitSegments().get(horzSeg),
							0, xEnds, 0, 2);
					int[] yEnds = new int[2];
					System.arraycopy(vline.getPolyFitSegments().get(vertSeg),
							0, yEnds, 0, 2);
					for (int k = 0; k < 2; k++) {
						xEnds[k] += xoffset;
						yEnds[k] += yoffset;
					}
					// 得到系数
					double[] hc = hline.getPolyFitCoeffs().get(horzSeg);
					double[] vc = vline.getPolyFitCoeffs().get(vertSeg);
					x = (hc[0] * vc[1] + vc[0]) / (1 - hc[1] * vc[1]);
					y = (vc[0] * hc[1] + hc[0]) / (1 - hc[1] * vc[1]);

					// 检查区间
					if (x >= xEnds[0] && x <= xEnds[1] && y >= yEnds[0]
							&& y <= yEnds[1])
						break;
					if (x < xEnds[0])
						horzSeg--;
					else if (x > xEnds[1])
						horzSeg++;
					else if (y < yEnds[0])
						vertSeg--;
					else if (y > yEnds[1])
						vertSeg++;
					if (horzSeg < 0 || horzSeg >= segCount || vertSeg < 0
							|| vertSeg >= segCount)
						break;
				} while (true);
				intersections[width * i + j] = new double[] { x, y };
			}
		}
		return intersections;
	}

	/**
	 * 将栅格线分段拟合
	 * 
	 * @param next
	 */
	private void polyFitSegments(GridLineModel line) {
		// 分段
		int segCount = ((Number) prefData_.get(PrefConst.POLYFIT_SEG_COUNT))
				.intValue();
		double segLength = line.getLength() / (double) segCount;
		// 分段的端点（左闭右开区间）
		ArrayList<int[]> segment = new ArrayList<int[]>();
		// 栅格线的起点
		double[][] pts = line.getDataPoints();
		for (int i = 0; i < segCount; i++) {
			int[] ends = new int[2];
			ends[0] = (int) Math.round(i * segLength);
			if (i < segCount - 1)
				ends[1] = (int) Math.round((i + 1) * segLength);
			else
				// 最后一个区间
				ends[1] = line.getLength();
			segment.add(ends);
		}
		line.setPolyFitSegments(segment);

		// 分段拟合
		ArrayList<double[]> coeffList = new ArrayList<double[]>();
		for (int i = 0; i < segCount; i++) {
			int[] ends = segment.get(i);
			int len = ends[1] - ends[0];
			double Sx = 0;
			double Sy = 0;
			double Sxx = 0;
			double Sxy = 0;

			if (line.getType() == GridLineModel.GRID_LINE_HORIZONTAL) {
				for (int j = ends[0]; j < ends[1]; j++) {
					Sx += pts[j][0];
					Sy += pts[j][1];
					Sxx += Math.pow(pts[j][0], 2);
					Sxy += pts[j][0] * pts[j][1];
				}
			} else {
				for (int j = ends[0]; j < ends[1]; j++) {
					Sx += pts[j][1];
					Sy += pts[j][0];
					Sxx += Math.pow(pts[j][1], 2);
					Sxy += pts[j][0] * pts[j][1];
				}
			}
			double delta = len * Sxx - Math.pow(Sx, 2);
			coeffList.add(new double[] { (Sxx * Sy - Sx * Sxy) / delta,
					(len * Sxy - Sx * Sy) / delta });
		}
		line.setPolyFitCoeffs(coeffList);
	}
}
