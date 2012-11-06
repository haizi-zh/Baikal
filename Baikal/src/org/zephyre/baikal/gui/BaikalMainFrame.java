package org.zephyre.baikal.gui;

import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.process.ByteProcessor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultButtonModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.BaikalRes;
import org.zephyre.baikal.BaikalCore.PrefConst;
import org.zephyre.baikal.GridLineModel;
import org.zephyre.baikal.camera.BaikalAbstractCamera;
import org.zephyre.baikal.camera.BaikalCameraException;
import org.zephyre.baikal.camera.BaikalFileCamera;
import org.zephyre.baikal.camera.BaikalSimCamera;

import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.zephyre.baikal.edsdk.EDSDK_wrapJ;

import static com.googlecode.javacv.cpp.opencv_core.cvAdd;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BLUR;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;

public class BaikalMainFrame extends JFrame {
	private BaikalCore core_;

	private ResourceBundle res_;
	private BaikalGridFrame gridFrame_;
	protected BaikalPrefDialog optionDlg;
	private JFormattedTextField mirrorHeightText;
	private JButton camConnectButton_;
	private AbstractAction camConnectAction_;
	private JButton toggleLiveButton_;
	private JButton runTestButton_;
	private JButton snapshotButton_;
	private AbstractAction toggleLiveAction_;
	private AbstractAction runTestAction_;
	private AbstractAction setParamAction_;
	private JLabel statusLabel;

	private ResultImagePanel imagePanel_;
	private BaikalImagePanel profilePanel_;

	private Thread previewThread_;

	// 平面检测的线程管理
	private ExecutorService measureExec_;
	private volatile boolean isPreviewing_;
	/**
	 * Live view模式所需要的ExecutorService
	 */
	private ExecutorService liveViewExec_;

	private Future<?> previewTask;

	/**
	 * 相机返回的原始BufferedImage，经过复制以后，保存于此。
	 */
	private BufferedImage bufImage_;
	/**
	 * 相机返回的BufferedImage存储，和处理线程形成producer-consumer关系
	 */
	private ArrayBlockingQueue<BufferedImage> imageStorage_;
	private ArrayBlockingQueue<BufferedImage> imageProcQueque_;

	/**
	 * 图像处理模块的参数设置
	 */
	protected BaikalProcParam procParamDlg_;

	private AbstractAction snapshotAction_;

	private static BaikalMainFrame instance;

	public BaikalCore getCore() {
		return core_;
	}

	public BaikalMainFrame(BaikalCore core) {
		BaikalMainFrame.instance = this;
		res_ = ResourceBundle.getBundle(BaikalRes.class.getName());
		core_ = core;

		final int IMAGE_STORAGE_SIZE = 4;
		imageStorage_ = new ArrayBlockingQueue<BufferedImage>(
				IMAGE_STORAGE_SIZE);
		imageProcQueque_ = new ArrayBlockingQueue<BufferedImage>(
				IMAGE_STORAGE_SIZE);

		try {
			core_.loadAllDevices();
			initUI();
		} catch (BaikalCameraException e) {
			JOptionPane.showMessageDialog(this, "无法连接到相机，请检查硬件配置是否正常。", "错误",
					JOptionPane.ERROR_MESSAGE);
		} catch (JsonParseException e) {
		} catch (IOException e) {
		}

		// measureExec_ = Executors.newCachedThreadPool();
	}

	public static BaikalMainFrame getInstance() {
		return instance;
	}

	private void initUI() throws JsonIOException, JsonSyntaxException,
			IOException {
		initActions();
		initMenu();
		initFocusManager();

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exitApplication();
			}
		});

		setLayout(new BorderLayout());
		JPanel basicPanel = new JPanel();
		basicPanel.setLayout(new BoxLayout(basicPanel, BoxLayout.Y_AXIS));
		basicPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Status bar
		statusLabel = new JLabel("Status");
		statusLabel.setBorder(BorderFactory
				.createBevelBorder(BevelBorder.LOWERED));
		add(statusLabel, BorderLayout.SOUTH);
		getContentPane().add(basicPanel);

		// Panel for batch information
		basicPanel.add(createInfoPanel());
		basicPanel.add(Box.createRigidArea(new Dimension(0, 20)));

		// The main panel for controllers
		basicPanel.add(createControllerPanel());

		setTitle("Baikal光学镜面检测系统 - "
				+ (String) core_.getEntry(PrefConst.NAME));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		pack();
		this.setMinimumSize(this.getPreferredSize());
		setLocationRelativeTo(null);
	}

	@SuppressWarnings({ "serial" })
	private void initActions() {
		camConnectAction_ = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					core_.connectCamera();
				} catch (BaikalCameraException e1) {
					JOptionPane.showMessageDialog(BaikalMainFrame.this,
							"无法连接到相机，请检查硬件配置是否正常。", "",
							JOptionPane.ERROR_MESSAGE);
				}

			}
		};
		camConnectAction_.putValue(AbstractAction.NAME, "连接相机");

		snapshotAction_ = new AbstractAction() {
			private int cnt_;

			@Override
			public void actionPerformed(ActionEvent evt) {
				BaikalAbstractCamera cam = core_.getCamera();
				int segCount = ((Number) core_
						.getEntry(PrefConst.SEGMENT_COUNT)).intValue();

				BufferedImage bi = null;
				BufferedImage bi2 = null;
				try {
					int index = (int) (cnt_ % (2 * segCount + 1));
					HashMap<String, Object> optData = cam.getOptData();
					if (index == 0) {
						// 显示markers
						optData.put("DrawMarkers", true);
						optData.put("DrawHorzGridLines", false);
						optData.put("DrawVertGridLines", false);
					} else if (index <= segCount) {
						// 显示水平栅格
						optData.put("DrawMarkers", false);
						optData.put("DrawHorzGridLines", true);
						optData.put("DrawVertGridLines", false);
						optData.put("HorzIndex", index - 1);
					} else {
						// 显示垂直栅格
						optData.put("DrawMarkers", false);
						optData.put("DrawHorzGridLines", false);
						optData.put("DrawVertGridLines", true);
						optData.put("VertIndex", index - segCount - 1);
					}
					bi = cam.snapshotAndWait();
					bi2 = copyImage(bi, imageStorage_.poll());
					cnt_++;
				} catch (BaikalCameraException e) {
					JOptionPane.showMessageDialog(BaikalMainFrame.this,
							res_.getObject("CameraError"), "",
							JOptionPane.ERROR_MESSAGE);
				} catch (InterruptedException e) {
				} finally {
					if (bi != null) {
						try {
							cam.releaseImage(bi);
						} catch (InterruptedException e) {
						}
					}
				}
				if (bi2 != null) {
					if (bufImage_ != null)
						imageStorage_.offer(bufImage_);
					bufImage_ = bi2;
					updateImage();
				}
			}
		};
		snapshotAction_.putValue(AbstractAction.NAME,
				res_.getObject("SnapShotAction"));

		toggleLiveAction_ = new AbstractAction() {
			private boolean isLive_ = false;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (isLive_) {
					try {
						stopPreview();
						isLive_ = false;
						putValue(AbstractAction.NAME, "预览");
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				} else {
					if (gridFrame_ == null) {
						gridFrame_ = new BaikalGridFrame();
					}
					gridFrame_.setDrawMarkers(true);
					gridFrame_.drawHorzGridLines(true);
					gridFrame_.drawVertGridLines(true);
					gridFrame_.displayFullGrids(true);
					gridFrame_.setVisible(true);
					gridFrame_.repaint();
					startPreview();
					isLive_ = true;
					putValue(AbstractAction.NAME, "停止");
				}
			}

			{
				putValue(AbstractAction.NAME, "预览");
			}
		};

		runTestAction_ = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// 显示栅格窗口
				if (gridFrame_ == null)
					gridFrame_ = new BaikalGridFrame();
				gridFrame_.setVisible(true);

				// 开启检测线程
				ThreadFactory factory = new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setDaemon(true);
						return t;
					}
				};
				measureExec_ = Executors.newCachedThreadPool(factory);
				measureExec_.execute(new Runnable() {
					@Override
					public void run() {
						measurementProc();
					}
				});
			}
		};
		runTestAction_.putValue(AbstractAction.NAME, res_.getString("RunTest"));

		setParamAction_ = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (procParamDlg_ == null) {
					procParamDlg_ = new BaikalProcParam();
				}
				procParamDlg_.setVisible(true);
			}
		};
		setParamAction_.putValue(AbstractAction.NAME,
				res_.getString("SetParam"));
	}

	private JPanel createControllerPanel() {
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(BorderFactory.createEtchedBorder());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;

		HashMap<String, String[]> devList = core_.getDeviceList();
		JLabel camLabel = new JLabel("相机型号：");
		final JComboBox<String> camListComboBox = new JComboBox<String>(
				devList.get(PrefConst.CAMERA_LIST));
		camListComboBox.setSelectedItem(null);
		camListComboBox.setSelectedItem(core_
				.getEntry(PrefConst.CAMERA_MODEL));
		camListComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				core_.putEntry(PrefConst.CAMERA_MODEL,
						camListComboBox.getSelectedItem());
			}
		});

		JLabel lenseLabel = new JLabel("镜头型号：");
		final JComboBox<String> lensListComboBox = new JComboBox<String>(
				devList.get(PrefConst.LENS_LIST));
		lensListComboBox.setSelectedItem(null);
		lensListComboBox.setSelectedItem(core_
				.getEntry(PrefConst.LENS_MODEL));
		lensListComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				core_.putEntry(PrefConst.LENS_MODEL,
						lensListComboBox.getSelectedItem());
			}
		});

		JLabel capacityLabel1 = new JLabel("空间容量：");
		JLabel capacityLabel2 = new JLabel("N/A");

		JLabel camStatusLabel1 = new JLabel("相机状态：");
		JLabel camStatusLabel2 = new JLabel("未连接");
		camConnectButton_ = new JButton(camConnectAction_);
		JPanel camConnectionPanel = new JPanel();
		camConnectionPanel.setLayout(new BoxLayout(camConnectionPanel,
				BoxLayout.X_AXIS));
		camConnectionPanel.add(camStatusLabel2);
		camConnectionPanel.add(Box.createHorizontalStrut(16));
		camConnectionPanel.add(Box.createHorizontalGlue());
		camConnectionPanel.add(camConnectButton_);

		int shutter = ((Number) core_.getEntry(PrefConst.SHUTTER))
				.intValue();
		final int maxShutter = 1000;
		JLabel shutterLabel = new JLabel("快门时间（毫秒）：");
		final JSlider shutterSlider = new JSlider(1, maxShutter);
		final JFormattedTextField shutterText = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		shutterText.setValue(shutter);
		shutterText.setPreferredSize(new Dimension(48, shutterText
				.getPreferredSize().height));
		shutterText.setInputVerifier(new IntegerInputVerifier(1, maxShutter,
				new Runnable() {
					@Override
					public void run() {
						shutterSlider.setValue(((Number) shutterText.getValue())
								.intValue());
					}
				}));

		shutterSlider.setValue(shutter);
		shutterSlider.setSnapToTicks(true);
		shutterSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int val = shutterSlider.getValue();
				try {
					core_.getCamera().setExposureTime(val);
					shutterText.setValue(val);
					core_.putEntry(PrefConst.SHUTTER, val);
				} catch (BaikalCameraException e) {
					JOptionPane.showMessageDialog(BaikalMainFrame.this, "相机异常",
							"错误", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JLabel apertureLabel = new JLabel("光圈：f/");
		// int aperture = 0;
		// {
		// Object obj = core_.getPrefData().get("Aperture");
		// if (obj instanceof Long)
		// aperture = ((Long) obj).intValue();
		// else if (obj instanceof Integer)
		// aperture = (Integer) obj;
		// }

		JTextField apertureText = new JTextField("1.4");
		apertureText.setPreferredSize(new Dimension(32, apertureText
				.getPreferredSize().height));
		JSlider apertureSlider = new JSlider(0, 9);
		apertureSlider.setValue(0);
		apertureSlider.setSnapToTicks(true);

		JLabel gridCountLabel = new JLabel("栅格图分割数目：");
		final JFormattedTextField gridCountText = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		gridCountText.setInputVerifier(new IntegerInputVerifier(1, 100,
				new Runnable() {
					@Override
					public void run() {
						core_.putEntry(PrefConst.SEGMENT_COUNT,
								((Number) gridCountText.getValue()).intValue());
					}
				}));
		gridCountText.setValue(((Number) core_
				.getEntry(PrefConst.SEGMENT_COUNT)).intValue());
		gridCountText.setPreferredSize(new Dimension(32, gridCountText
				.getPreferredSize().height));

		toggleLiveButton_ = new JButton(toggleLiveAction_);
		runTestButton_ = new JButton(runTestAction_);
		snapshotButton_ = new JButton(snapshotAction_);

		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.LINE_START;

		mainPanel.add(camLabel, gbc);
		gbc.gridx = 1;
		mainPanel.add(camListComboBox, gbc);
		gbc.gridx = 0;
		gbc.gridy++;
		mainPanel.add(lenseLabel, gbc);
		gbc.gridx = 1;
		mainPanel.add(lensListComboBox, gbc);
		gbc.gridx = 0;
		gbc.gridy++;
		mainPanel.add(capacityLabel1, gbc);
		gbc.gridx = 1;
		mainPanel.add(capacityLabel2, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		mainPanel.add(camStatusLabel1, gbc);
		gbc.gridx = 1;
		mainPanel.add(camConnectionPanel, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		mainPanel.add(shutterLabel, gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.NONE;
		mainPanel.add(shutterText, gbc);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		mainPanel.add(shutterSlider, gbc);
		gbc.gridwidth = 1;
		gbc.gridy++;
		gbc.gridx = 0;
		mainPanel.add(apertureLabel, gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.NONE;
		mainPanel.add(apertureText, gbc);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		mainPanel.add(apertureSlider, gbc);
		gbc.gridwidth = 1;

		gbc.gridy++;
		gbc.gridx = 0;
		mainPanel.add(gridCountLabel, gbc);
		gbc.gridx++;
		gbc.fill = GridBagConstraints.NONE;
		mainPanel.add(gridCountText, gbc);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JPanel buttonBox = new JPanel();
		buttonBox.setLayout(new BoxLayout(buttonBox, BoxLayout.X_AXIS));
		buttonBox.add(snapshotButton_);
		buttonBox.add(Box.createHorizontalStrut(24));
		buttonBox.add(toggleLiveButton_);
		buttonBox.add(Box.createHorizontalStrut(24));
		buttonBox.add(runTestButton_);
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		mainPanel.add(buttonBox, gbc);
		gbc.gridwidth = 1;

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.weighty = 1;
		mainPanel.add(Box.createVerticalGlue(), gbc);

		JTabbedPane tabbedPane = createTabbedImagePanel();

		gbc.gridwidth = 1;
		gbc.weighty = 0;
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridheight = 11;
		mainPanel.add(tabbedPane, gbc);

		return mainPanel;
	}

	/**
	 * 创立一个JTabbedPane，里面保存拍照的结果，以及profiler图像。
	 * 
	 * @return
	 */
	private JTabbedPane createTabbedImagePanel() {
		JTabbedPane mainTabbedPanel = new JTabbedPane();

		// 拍照图像的结果包括图像显示区域和一个通道选择区域
		// The painting area
		imagePanel_ = new ResultImagePanel();
		imagePanel_.setPreferredSize(new Dimension(640, 640 * 2 / 3));
		imagePanel_.setBorder(BorderFactory.createLineBorder(Color.PINK, 12));
		imagePanel_.setAlignmentX(LEFT_ALIGNMENT);
		imagePanel_.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				double[] imageCoord = imagePanel_.screenToImage(e.getX(),
						e.getY());
				if (imageCoord == null)
					return;

				int imgX = (int) Math.round(imageCoord[0]);
				int imgY = (int) Math.round(imageCoord[1]);
				byte[] pixVal = imagePanel_.getPixelValue(imgX, imgY);
				statusLabel.setText(String.format(
						"Screen: %d, %d; Image: %d, %d, Value: (%d, %d, %d)",
						e.getX(), e.getY(), imgX, imgY, pixVal[0] & 0xff,
						pixVal[1] & 0xff, pixVal[2] & 0xff));
			}
		});

		// 通道选择区域
		JPanel channelSelPanel = new JPanel();
		channelSelPanel.setLayout(new BoxLayout(channelSelPanel,
				BoxLayout.X_AXIS));
		channelSelPanel.setAlignmentX(LEFT_ALIGNMENT);
		// 全选通道
		final JCheckBox toggleAll = new JCheckBox();
		final JCheckBox[] toggleChannels = new JCheckBox[3];
		for (int i = 0; i < 3; i++)
			toggleChannels[i] = new JCheckBox();

		@SuppressWarnings("serial")
		AbstractAction toggleAllAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (JCheckBox cb : toggleChannels)
					cb.setSelected(toggleAll.isSelected());
			}
		};
		toggleAllAction.putValue(AbstractAction.NAME,
				res_.getObject("ToggleAllAction"));
		toggleAll.setAction(toggleAllAction);

		for (int i = 0; i < 3; i++) {
			@SuppressWarnings("serial")
			AbstractAction toggleChannelAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					boolean allSel = true;
					for (int j = 0; j < 3; j++) {
						if (!toggleChannels[j].isSelected()) {
							allSel = false;
							break;
						}
					}
					toggleAll.setSelected(allSel);
				}
			};
			toggleChannelAction.putValue(AbstractAction.NAME,
					res_.getObject(String.format("Toggle%d", i + 1)));
			toggleChannels[i].setAction(toggleChannelAction);
		}
		channelSelPanel.add(toggleAll);
		for (JCheckBox cb : toggleChannels) {
			channelSelPanel.add(cb);
		}

		JPanel imgTabbedPanel = new JPanel();
		imgTabbedPanel
				.setLayout(new BoxLayout(imgTabbedPanel, BoxLayout.Y_AXIS));
		imgTabbedPanel.add(imagePanel_);
		imgTabbedPanel.add(channelSelPanel);
		mainTabbedPanel.add("Captured image", imgTabbedPanel);

		profilePanel_ = new BaikalImagePanel();
		profilePanel_.setPreferredSize(new Dimension(640, 640 * 2 / 3));
		profilePanel_.setBackground(Color.BLACK);

		mainTabbedPanel.add("Profile", profilePanel_);

		return mainTabbedPanel;
	}

	/**
	 * 将src复制到(byte-by-byte)dst中。如果dst为null，或者格式不兼容，则新建一个BufferedImage，并返回。
	 * 
	 * @param src
	 *            源
	 * @param dst
	 *            目标
	 * @return dst。如果新建了一个BufferedImage，则为该新建的图像。
	 */
	private BufferedImage copyImage(BufferedImage src, BufferedImage dst) {
		if (dst == null
				|| dst.getRaster().getDataBuffer().getSize() != src.getRaster()
						.getDataBuffer().getSize()) {
			// 不一致，需要重建bufImage_
			BaikalCore.log(String.format("%s: %s",
					"copyImage: new BufferedImage created", dst));
			dst = new BufferedImage(src.getColorModel(),
					Raster.createWritableRaster(src.getSampleModel(), null),
					src.isAlphaPremultiplied(), null);
		}

		byte[] srcData = ((DataBufferByte) src.getRaster().getDataBuffer())
				.getData();
		System.arraycopy(srcData, 0, ((DataBufferByte) dst.getRaster()
				.getDataBuffer()).getData(), 0, srcData.length);

		return dst;
	}

	protected void startPreview() {
		if (isPreviewing_)
			return;

		// 相机的采集线程
		Runnable camTask = new Runnable() {
			@Override
			public void run() {
				BaikalAbstractCamera cam = core_.getCamera();

				// 确定栅格线的数量
				// 确定纵横线的数量
				int horzDensity = ((Number) core_
						.getEntry(PrefConst.HORZ_DENSITY)).intValue();
				int vertDensity = ((Number) core_
						.getEntry(PrefConst.VERT_DENSITY)).intValue();
				int segCount = ((Number) core_
						.getEntry(PrefConst.SEGMENT_COUNT)).intValue();
				int nHGrids = horzDensity / segCount;
				int nVGrids = vertDensity / segCount;

				long cnt = 0;
				while (!Thread.interrupted()) {
					BufferedImage bi = null;
					BufferedImage bi2 = null;
					try {
						// int index = (int) (cnt % (nHGrids + nVGrids + 1));
						int index = (int) (cnt % (2 * segCount + 1));
						HashMap<String, Object> optData = cam.getOptData();
						if (index == 0) {
							// 显示markers
							optData.put("DrawMarkers", true);
							optData.put("DrawHorzGridLines", false);
							optData.put("DrawVertGridLines", false);
						} else if (index <= segCount) {
							// 显示水平栅格
							optData.put("DrawMarkers", false);
							optData.put("DrawHorzGridLines", true);
							optData.put("DrawVertGridLines", false);
							optData.put("HorzIndex", index - 1);
						} else {
							// 显示垂直栅格
							optData.put("DrawMarkers", false);
							optData.put("DrawHorzGridLines", false);
							optData.put("DrawVertGridLines", true);
							optData.put("VertIndex", index - segCount - 1);
						}
						bi = cam.snapshotAndWait();
						bi2 = copyImage(bi, imageStorage_.poll());
						// BaikalCore.log(String.format("camTask: acquired %d",
						// cnt));
						cnt++;
					} catch (BaikalCameraException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						if (bi != null) {
							boolean bv = Thread.interrupted();
							// ReleaseImage的时候，需要先把interrupted状态临时清空
							try {
								cam.releaseImage(bi);
							} catch (InterruptedException e) {
							}
							if (bv)
								Thread.currentThread().interrupt();
						}
					}
					if (bi2 != null) {
						try {
							imageProcQueque_.put(bi2);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
				}
				BaikalCore.log("Camera live-view thread existed.");
			}
		};

		// 图像处理及显示线程
		Runnable procTask = new Runnable() {
			@Override
			public void run() {
				// 计算FPS
				LinkedList<Long> fpsQueue = new LinkedList<Long>();
				final int QUEUE_SIZE = 100;
				int frameCnt = 0;
				while (!Thread.interrupted()) {
					try {
						if (bufImage_ != null)
							imageStorage_.offer(bufImage_);
						bufImage_ = imageProcQueque_.take();
						updateImage();
						frameCnt++;

						if (fpsQueue.size() >= QUEUE_SIZE)
							fpsQueue.removeLast();
						fpsQueue.addFirst(System.nanoTime());
						if (fpsQueue.size() >= 10) {
							double fps = (double) (fpsQueue.size() - 1)
									/ ((fpsQueue.getFirst().longValue() - fpsQueue
											.getLast().longValue()) / 1.0e9);
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		};

		ThreadFactory factory = new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		};
		liveViewExec_ = Executors.newFixedThreadPool(2, factory);
		liveViewExec_.execute(camTask);
		liveViewExec_.execute(procTask);

		isPreviewing_ = true;
	}

	protected void stopPreview() throws InterruptedException {
		if (!isPreviewing_)
			return;

		liveViewExec_.shutdownNow();
		// previewThread_.interrupt();
		isPreviewing_ = false;
		imagePanel_.repaint();
	}

	/**
	 * 将图像处理，再显示出来。
	 */
	public synchronized void updateImage() {
		if (bufImage_ == null)
			return;

		imagePanel_.drawImage(copyImage(bufImage_,null), null, null, null);
//		HashMap<String, Object> hm = core_.measureMarkers(bufImage_, 2);
//		BufferedImage bufProcImage = (BufferedImage) hm.get("ProcessedImage");
//		double[][] markerList = (double[][]) hm.get("MarkerList");
//		imagePanel_.drawImage(bufProcImage, markerList, null, null);
	}

	/**
	 * 是否正在预览
	 * 
	 * @return
	 */
	public boolean isPreviewing() {
		return isPreviewing_;
	}

	/**
	 * 检测程序的线程函数
	 */
	private void measurementProc() {
		final int[] dlgReturn = new int[1];
		// 表示采集是否成功
		final boolean[] acqSucceeded = new boolean[] { false };
		// 是否终止测试
		final boolean[] stopAcq = new boolean[] { false };
		gridFrame_.displayFullGrids(false);

		// 询问是否接受采集的图像
		Runnable acceptAcq = new Runnable() {
			@Override
			public void run() {
				acqSucceeded[0] = true;
				stopAcq[0] = false;
				if (true)
					return;
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							dlgReturn[0] = JOptionPane.showConfirmDialog(
									BaikalMainFrame.getInstance(),
									res_.getString("AcqSucceeded"), "",
									JOptionPane.YES_NO_CANCEL_OPTION);

						}
					});
					switch (dlgReturn[0]) {
					case JOptionPane.YES_OPTION:
						BaikalCore.log("Opt test approved.");
						acqSucceeded[0] = true;
						break;
					case JOptionPane.NO_OPTION:
						BaikalCore.log("Opt test retried.");
						acqSucceeded[0] = false;
						break;
					case JOptionPane.CANCEL_OPTION:
						BaikalCore.log("Opt test cancelled.");
						stopAcq[0] = true;
					}
				} catch (InvocationTargetException e) {
					BaikalCore.log(e.toString());
					stopAcq[0] = true;
				} catch (InterruptedException e) {
					BaikalCore.log("Opt test cancelled.");
					stopAcq[0] = true;
				}
			}
		};

		Runnable acqTask = new Runnable() {
			@Override
			public void run() {
				BaikalAbstractCamera cam = core_.getCamera();
				BufferedImage bi = null;
				try {
					bi = cam.snapshotAndWait();
					if (bufImage_ != null)
						imageStorage_.offer(bufImage_);
					bufImage_ = copyImage(bi, imageStorage_.poll());
				} catch (BaikalCameraException e1) {
					BaikalCore.log("measurementProc: camera error.");
					int opt = JOptionPane.showConfirmDialog(
							BaikalMainFrame.this, res_.getString("AcqError"),
							"", JOptionPane.YES_NO_OPTION,
							JOptionPane.ERROR_MESSAGE);
					if (opt == JOptionPane.YES_OPTION)
						acqSucceeded[0] = false;
					else
						stopAcq[0] = true;
				} catch (InterruptedException e1) {
					BaikalCore.log("measurementProc: interrupted.");
					stopAcq[0] = true;
				} finally {
					if (bi != null) {
						boolean bv = Thread.interrupted();
						// ReleaseImage的时候，需要先把interrupted状态临时清空
						try {
							cam.releaseImage(bi);
						} catch (InterruptedException e) {
						}
						if (bv)
							Thread.currentThread().interrupt();
					}
				}
			}
		};

		// 拍照的提示对话框
		Runnable goShotTask = new Runnable() {
			@Override
			public void run() {
				if (true)
					return;
				int opt = JOptionPane.showConfirmDialog(BaikalMainFrame.this,
						res_.getString("ReadyToShot"), "",
						JOptionPane.OK_CANCEL_OPTION);
				if (opt == JOptionPane.CANCEL_OPTION)
					stopAcq[0] = true;
			}
		};

		long frameCnt = 1;
		double[][] markerList = null;
		// 最后叠加的总图
		IplImage sumImage = null;

		// 画marker
		gridFrame_.setDrawMarkers(true);
		gridFrame_.drawHorzGridLines(false);
		gridFrame_.drawVertGridLines(false);
		gridFrame_.repaint();
		goShotTask.run();
		if (stopAcq[0])
			return;
		do {
			BaikalAbstractCamera cam = core_.getCamera();
			HashMap<String, Object> optData = cam.getOptData();
			optData.put("DrawMarkers", true);
			optData.put("DrawHorzGridLines", false);
			optData.put("DrawVertGridLines", false);

			if (cam instanceof BaikalFileCamera) {
				BaikalFileCamera fc = (BaikalFileCamera) cam;
				fc.resetSeries();
			}
			acqTask.run();

			HashMap<String, Object> hm = core_.measureMarkers(bufImage_, 2);
			BufferedImage bufProcImage = (BufferedImage) hm
					.get("ProcessedImage");
			markerList = (double[][]) hm.get("MarkerList");

			frameCnt++;
			imagePanel_.drawImage(bufProcImage, markerList, null, null);

			acceptAcq.run();
			if (stopAcq[0])
				return;
			if (acqSucceeded[0]) {
				sumImage = IplImage.createFrom(bufProcImage);
				break;
			}
		} while (true);

		int segCount = ((Number) core_.getEntry(PrefConst.SEGMENT_COUNT))
				.intValue();

		// 栅格线
		ArrayList<GridLineModel> horzGridLines = new ArrayList<GridLineModel>();
		ArrayList<GridLineModel> vertGridLines = new ArrayList<GridLineModel>();
		ArrayList<GridLineModel> totGridLines = new ArrayList<GridLineModel>();

		// 画横向栅格线
		gridFrame_.setDrawMarkers(false);
		gridFrame_.drawHorzGridLines(true);
		gridFrame_.drawVertGridLines(false);
		for (int i = 0; i < segCount; i++) {
			gridFrame_.setGridLineOffset(0, i);
			gridFrame_.repaint();
			goShotTask.run();
			if (stopAcq[0])
				return;

			do {
				BaikalAbstractCamera cam = core_.getCamera();
				HashMap<String, Object> optData = cam.getOptData();
				optData.put("DrawMarkers", false);
				optData.put("DrawHorzGridLines", true);
				optData.put("DrawVertGridLines", false);
				optData.put("HorzIndex", i);

				int pos = 0;
				if (cam instanceof BaikalFileCamera) {
					BaikalFileCamera fc = (BaikalFileCamera) cam;
					pos = fc.getPos();
				}
				acqTask.run();

				HashMap<String, Object> hm = core_.findGridLines(bufImage_, i,
						true, markerList, 1);
				BufferedImage bufProcImage = (BufferedImage) hm
						.get("ProcessedImage");
				Collection<? extends GridLineModel> lines = (Collection<? extends GridLineModel>) hm
						.get("GridLines");

				frameCnt++;
				imagePanel_.drawImage(bufProcImage, null, lines, null);

				acceptAcq.run();
				if (stopAcq[0])
					return;
				if (acqSucceeded[0]) {
					IplImage tmpImage = IplImage.createFrom(bufProcImage);
					cvAdd(sumImage, tmpImage, sumImage, null);
					tmpImage.release();
					horzGridLines.addAll(lines);
					totGridLines.addAll(lines);
					break;
				} else {
					if (cam instanceof BaikalFileCamera) {
						BaikalFileCamera fc = (BaikalFileCamera) cam;
						fc.setPos(pos);
					}
				}

			} while (true);
		}

		// 画纵向栅格线
		gridFrame_.setDrawMarkers(false);
		gridFrame_.drawHorzGridLines(false);
		gridFrame_.drawVertGridLines(true);
		for (int i = 0; i < segCount; i++) {
			gridFrame_.setGridLineOffset(i, 0);
			gridFrame_.repaint();
			goShotTask.run();
			if (stopAcq[0])
				return;

			do {
				BaikalAbstractCamera cam = core_.getCamera();
				HashMap<String, Object> optData = cam.getOptData();
				optData.put("DrawMarkers", false);
				optData.put("DrawHorzGridLines", false);
				optData.put("DrawVertGridLines", true);
				optData.put("VertIndex", i);

				int pos = 0;
				if (cam instanceof BaikalFileCamera) {
					BaikalFileCamera fc = (BaikalFileCamera) cam;
					pos = fc.getPos();
				}
				acqTask.run();

				HashMap<String, Object> hm = core_.findGridLines(bufImage_, i,
						false, markerList, 1);
				BufferedImage bufProcImage = (BufferedImage) hm
						.get("ProcessedImage");
				Collection<? extends GridLineModel> lines = (Collection<? extends GridLineModel>) hm
						.get("GridLines");

				frameCnt++;
				imagePanel_.drawImage(bufProcImage, null, lines, null);

				acceptAcq.run();
				if (stopAcq[0])
					return;
				if (acqSucceeded[0]) {
					IplImage tmpImage = IplImage.createFrom(bufProcImage);
					cvAdd(sumImage, tmpImage, sumImage, null);
					tmpImage.release();
					vertGridLines.addAll(lines);
					totGridLines.addAll(lines);
					break;
				} else {
					if (cam instanceof BaikalFileCamera) {
						BaikalFileCamera fc = (BaikalFileCamera) cam;
						fc.setPos(pos);
					}
				}
			} while (true);
		}

		// 排序
		Comparator<GridLineModel> comparator = new Comparator<GridLineModel>() {
			@Override
			public int compare(GridLineModel o1, GridLineModel o2) {
				if (o1.getType() == GridLineModel.GRID_LINE_HORIZONTAL) {
					if (o1.getDataPoints()[0][1] > o2.getDataPoints()[0][1])
						return 1;
					else if (o1.getDataPoints()[0][1] < o2.getDataPoints()[0][1])
						return -1;
					else
						return 0;
				} else {
					if (o1.getDataPoints()[0][0] > o2.getDataPoints()[0][0])
						return 1;
					else if (o1.getDataPoints()[0][0] < o2.getDataPoints()[0][0])
						return -1;
					else
						return 0;
				}
			}
		};
		Collections.sort(horzGridLines, comparator);
		Collections.sort(vertGridLines, comparator);

		// 找到交点
		double[][] intersections = core_.findIntersections(horzGridLines,
				vertGridLines);

		// 画总图
		imagePanel_.drawImage(sumImage.getBufferedImage(), null, null,
				intersections);

		// 生成形貌图
		renderProfile(vertGridLines.size(), horzGridLines.size(), intersections);
	}

	/**
	 * 将交点渲染成形貌图
	 * 
	 * @param width
	 * @param height
	 * @param intersections
	 */
	private void renderProfile(int width, int height, double[][] intersections) {
		// 计算距离
		double[] profileD = new double[width * height];
		double minDist = Double.MAX_VALUE;
		double maxDist = Double.MIN_VALUE;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int cnt = 0;
				double sum = 0;
				double[] pt = intersections[width * i + j];
				// 距离四个近邻的平均距离
				if (j > 0) {
					sum += getDistance(pt, intersections[width * i + j - 1]);
					cnt++;
				}
				if (j < width - 1) {
					sum += getDistance(pt, intersections[width * i + j + 1]);
					cnt++;
				}
				if (i > 0) {
					sum += getDistance(pt, intersections[width * (i - 1) + j]);
					cnt++;
				}
				if (i < height - 1) {
					sum += getDistance(pt, intersections[width * (i + 1) + j]);
					cnt++;
				}
				double avgDist = sum / cnt;
				if (minDist > avgDist)
					minDist = avgDist;
				if (maxDist < avgDist)
					maxDist = avgDist;
				profileD[width * i + j] = avgDist;
			}
		}

		byte[] profileBuf = new byte[width * height];
		for (int i = 0; i < profileBuf.length; i++)
			profileBuf[i] = (byte) Math.round((profileD[i] - minDist)
					/ (maxDist - minDist) * 255);

		// ImagePlus imgPlus = new ImagePlus("Profile", new ByteProcessor(width,
		// height, profileBuf));
		// imgPlus.show();
		// FileSaver fs = new FileSaver(imgPlus);
		// fs.saveAsBmp("profile.bmp");

		int bytespp = 1;
		int scanline = width * bytespp;
		PixelInterleavedSampleModel sm = new PixelInterleavedSampleModel(
				DataBuffer.TYPE_BYTE, width, height, bytespp, scanline,
				new int[] { 0 });
		ColorModel cm = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
				ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);

		// 初始化BufferedImage
		DataBufferByte db = new DataBufferByte(profileBuf, profileBuf.length);
		WritableRaster raster = Raster.createWritableRaster(sm, db, null);
		BufferedImage bi = new BufferedImage(cm, raster, false, null);
		try {
			ImageIO.write(bi, "bmp", new File("profile_BI.bmp"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		IplImage img1 = IplImage.createFrom(bi);
		cvSmooth(img1, img1, CV_GAUSSIAN, 3, 3, 2, 2);

		profilePanel_.drawImage(img1.getBufferedImage());
	}

	/**
	 * 计算两个点之间的距离
	 * 
	 * @param pt1
	 * @param pt2
	 * @return
	 */
	private double getDistance(double[] pt1, double[] pt2) {
		return Math.sqrt(Math.pow(pt2[0] - pt1[0], 2)
				+ Math.pow(pt2[1] - pt1[1], 2));
	}

	/**
	 * @return
	 */
	private JPanel createInfoPanel() {
		JPanel infoPanel = new JPanel(new GridBagLayout());
		infoPanel.setBorder(BorderFactory.createEtchedBorder());

		JLabel operatorLabel = new JLabel("操作员：");
		JTextField operatorText = new JTextField(
				(String) core_.getEntry(PrefConst.NAME));
		operatorText.setEditable(false);
		operatorText.setFocusable(false);
		operatorText.setMaximumSize(new Dimension(256, operatorText
				.getMaximumSize().height));
		operatorText.setPreferredSize(new Dimension(96, operatorText
				.getPreferredSize().height));

		JLabel mirrorIdLabel = new JLabel("镜面编号：");
		JFormattedTextField mirrorIdText = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		mirrorIdText.setValue(((Number) core_
				.getEntry(PrefConst.MIRROR_COUNT)).intValue() + 1);
		mirrorIdText.setEditable(false);
		mirrorIdText.setFocusable(false);
		mirrorIdText.setPreferredSize(new Dimension(64, mirrorIdText
				.getPreferredSize().height));

		JLabel batchTimeLabel = new JLabel("时间：");
		final JFormattedTextField batchTimeText = new JFormattedTextField(
				DateFormat.getDateTimeInstance());
		batchTimeText.setValue(new Date());
		batchTimeText.setEditable(false);
		batchTimeText.setFocusable(false);
		// Update the time text field once per second.
		Timer timer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				batchTimeText.setValue(new Date());
			}
		});
		timer.start();

		JLabel mirrorWidthLabel = new JLabel("镜面宽度（毫米）：");
		JLabel mirrorHeightLabel = new JLabel("镜面高度（毫米）：");
		final JFormattedTextField mirrorWidthText = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		mirrorWidthText.setInputVerifier(new DoubleInputVerifier(1,
				Double.MAX_VALUE));
		final JFormattedTextField mirrorHeightText = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		mirrorHeightText.setInputVerifier(new DoubleInputVerifier(1,
				Double.MAX_VALUE));

		PropertyChangeListener mirrorDimensionListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				// TODO Auto-generated method stub
				Number width = (Number) mirrorWidthText.getValue();
				Number height = (Number) mirrorHeightText.getValue();
				core_.putEntry(PrefConst.MIRROR_WIDTH, width.doubleValue());
				core_.putEntry(PrefConst.MIRROR_HEIGHT, height.doubleValue());
			}
		};

		mirrorWidthText.setValue(((Number) (core_
				.getEntry(PrefConst.MIRROR_WIDTH))).doubleValue());
		mirrorWidthText.addPropertyChangeListener("value",
				mirrorDimensionListener);

		mirrorHeightText.setValue(((Number) (core_
				.getEntry(PrefConst.MIRROR_HEIGHT))).doubleValue());
		mirrorHeightText.addPropertyChangeListener("value",
				mirrorDimensionListener);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.LINE_START;

		infoPanel.add(operatorLabel, c);
		c.gridx = 1;
		infoPanel.add(operatorText, c);
		c.gridx = 2;
		infoPanel.add(mirrorIdLabel, c);
		c.gridx = 3;
		infoPanel.add(mirrorIdText, c);
		c.gridx = 4;
		infoPanel.add(batchTimeLabel, c);
		c.gridx = 5;
		infoPanel.add(batchTimeText, c);

		c.gridx = 0;
		c.gridy = 1;
		infoPanel.add(mirrorWidthLabel, c);
		c.gridx = 1;
		infoPanel.add(mirrorWidthText, c);
		c.gridx = 2;
		infoPanel.add(mirrorHeightLabel, c);
		c.gridx = 3;
		infoPanel.add(mirrorHeightText, c);

		c.gridy = 0;
		c.gridx = 6;
		c.gridheight = 2;
		c.weightx = 1;
		infoPanel.add(Box.createHorizontalGlue(), c);

		return infoPanel;
	}

	private void initFocusManager() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.addPropertyChangeListener("permanentFocusOwner",
						new PropertyChangeListener() {
							public void propertyChange(
									final PropertyChangeEvent e) {
								if (e.getNewValue() instanceof JTextField) {
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											JTextField textField = (JTextField) e
													.getNewValue();
											textField.selectAll();
										}
									});

								}
							}
						});
	}

	@Override
	protected JRootPane createRootPane() {
		JRootPane rootPane = super.createRootPane();
		rootPane.registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				exitApplication();
			}
		}, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
		return rootPane;
	}

	/*
	 * Exit the application
	 */
	public void exitApplication() {
		if (JOptionPane.showConfirmDialog(this, "是否退出镜面光学检测系统？", "退出",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			try {
				core_.writePref();
			} catch (IOException e) {
			}
			System.exit(0);
		}
	}

	private void initMenu() {
		JMenuBar bar = new JMenuBar();

		JMenu fileMenu = new JMenu("文件");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		JMenuItem exitItem = new JMenuItem("退出");
		exitItem.setToolTipText("退出系统");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				exitApplication();
			}
		});
		fileMenu.add(exitItem);
		bar.add(fileMenu);

		JMenu toolMenu = new JMenu("工具");
		toolMenu.setMnemonic(KeyEvent.VK_T);
		JMenuItem optionItem = new JMenuItem("选项");
		optionItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (optionDlg == null)
					optionDlg = new BaikalPrefDialog();
				optionDlg.setVisible(true);
			}
		});
		toolMenu.add(optionItem);
		bar.add(toolMenu);

		JMenu helpMenu = new JMenu("帮助");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		JMenuItem aboutItem = new JMenuItem("关于Baikal光学镜面监测系统");
		helpMenu.add(aboutItem);
		bar.add(helpMenu);

		setJMenuBar(bar);
	}

	/**
	 * 设置NikonSim相机的拍照序列号码
	 * 
	 * @param cam
	 */
	private void setNikonCamIndx(int indx) {
		BaikalAbstractCamera cam = core_.getCamera();
		// if (!(cam instanceof BaikalNikonCamera))
		// return;

		try {
			Method method = cam.getClass().getMethod("setImageNo",
					new Class[] { int.class });
			method.invoke(cam, 0);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 把所有字体设置为微软雅黑。
	 */
	private static void setUIFont() {
		Font myFont = new Font("微软雅黑", Font.PLAIN, 12);
		javax.swing.plaf.FontUIResource fontRes = new javax.swing.plaf.FontUIResource(
				myFont);
		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof javax.swing.plaf.FontUIResource) {
				UIManager.put(key, fontRes);
			}
		}
	}

	static {
		System.loadLibrary("EDSDK_wrapJ");
	}

	public static void main(String[] args) {

		if (test())
			return;

		setUIFont();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// BaikalLoginDialog dlg = new BaikalLoginDialog();
				// dlg.setVisible(true);
				// if (dlg.selectedUserName == null) {
				// System.exit(0);
				// }
				//
				// String user = dlg.selectedUserName;
				String user = "Zephyre";
				try {
					BaikalCore core = BaikalCore.createInstance_(user);
					BaikalMainFrame frame = new BaikalMainFrame(core);
					frame.setVisible(true);
				} catch (BaikalCameraException e) {
					JOptionPane.showMessageDialog(null, "连接相机失败", "错误",
							JOptionPane.ERROR_MESSAGE);
					System.exit(0);
				} catch (JsonParseException e) {
					JOptionPane.showMessageDialog(null, "读取用户信息失败。", "",
							JOptionPane.ERROR_MESSAGE);
					System.exit(0);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "读取用户信息失败。", "",
							JOptionPane.ERROR_MESSAGE);
					System.exit(0);
				}
			}
		});
	}

	/**
	 * 
	 */
	private static boolean test() {
		return false;
	}

	/**
	 * 设置BaikalCore对象
	 * 
	 * @param core
	 */
	private void setCore(BaikalCore core) {
		core_ = core;
	}
}

class FormattedTextInputVerifier extends InputVerifier {
	@Override
	public boolean verify(JComponent input) {
		return ((JFormattedTextField) input).isEditValid();
	}

}

/**
 * 有效输入：正整数
 * 
 * @author Zephyre
 * 
 */
class IntegerInputVerifier extends InputVerifier {
	private NumberFormat formatter_;
	private int min_;
	private int max_;
	private Runnable callback_;

	public IntegerInputVerifier(int min, int max) {
		this(min, max, null);
	}

	public IntegerInputVerifier(int min, int max, Runnable callback) {
		formatter_ = NumberFormat.getIntegerInstance();
		min_ = min;
		max_ = max;
		callback_ = callback;
	}

	@Override
	public boolean verify(JComponent input) {
		JTextField c = (JTextField) input;
		try {
			Number val = formatter_.parse(c.getText());
			if (val == null || val.intValue() >= max_ || val.intValue() < min_)
				return false;
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	@Override
	public boolean shouldYieldFocus(JComponent input) {
		JTextField c = (JTextField) input;
		boolean ret = verify(input);
		boolean inputOK;
		if (ret) {
			try {
				Number val;
				if (c instanceof JFormattedTextField) {
					JFormattedTextField c1 = (JFormattedTextField) c;
					c1.commitEdit();
					val = (Number) c1.getValue();
				} else {
					val = formatter_.parse(c.getText());
					c.setText(formatter_.format(val.intValue()));
				}
				if (val == null || val.intValue() >= max_
						|| val.intValue() < min_)
					inputOK = false;
				else
					inputOK = true;
			} catch (ParseException e) {
				inputOK = false;
			}
		} else {
			inputOK = false;
		}
		if (!inputOK) {
			c.selectAll();
			JOptionPane.showMessageDialog(input, "无效的输入！", "错误",
					JOptionPane.ERROR_MESSAGE);
		} else {
			if (callback_ != null)
				callback_.run();
		}
		return inputOK;
	}
}

/**
 * 有效输入：有范围的实数
 * 
 * @author Zephyre
 * 
 */
class DoubleInputVerifier extends InputVerifier {
	private NumberFormat formatter_;
	private double min_;
	private double max_;

	public DoubleInputVerifier(double min, double max) {
		formatter_ = NumberFormat.getNumberInstance();
		min_ = min;
		max_ = max;
	}

	@Override
	public boolean verify(JComponent input) {
		JTextField c = (JTextField) input;
		try {
			Number val = formatter_.parse(c.getText());
			if (val == null || val.doubleValue() >= max_
					|| val.doubleValue() < min_)
				return false;
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	@Override
	public boolean shouldYieldFocus(JComponent input) {
		JTextField c = (JTextField) input;
		boolean ret = verify(input);
		boolean inputOK;
		if (ret) {
			try {
				Number val = formatter_.parse(c.getText());
				c.setText(formatter_.format(val.doubleValue()));
				inputOK = true;
			} catch (ParseException e) {
				inputOK = false;
			}
		} else {
			inputOK = false;
		}
		if (!inputOK) {
			c.selectAll();
			JOptionPane.showMessageDialog(input, "无效的输入！", "错误",
					JOptionPane.ERROR_MESSAGE);
		}
		return inputOK;
	}
}