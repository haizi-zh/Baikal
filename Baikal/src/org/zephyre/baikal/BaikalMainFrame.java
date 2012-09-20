package org.zephyre.baikal;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.io.Opener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultButtonModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
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
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.zephyre.baikal.camera.BaikalAbstractCamera;
import org.zephyre.baikal.camera.BaikalCameraException;
import org.zephyre.baikal.camera.BaikalSimCamera;
import org.zephyre.baikal.imagepanel.ImagePanel;
import org.zephyre.baikal.imagepanel.ImagePanelMouseEvent;
import org.zephyre.baikal.imagepanel.ImagePanelMouseMotionListener;

public class BaikalMainFrame extends JFrame {
	private BaikalCore core;

	private BaikalGridFrame gridFrame;
	private JButton toggleLiveButton;
	private JButton runTestButton;
	protected BaikalPrefDialog optionDlg;
	private JFormattedTextField mirrorHeightText;
	private JComboBox camListComboBox;
	private JComboBox lensListComboBox;
	private JButton camConnectButton;
	private JLabel statusLabel;

	private ImagePanel imagePanel;

	// Thread executor for previewing
	private ExecutorService previewExec;
	private volatile boolean isPreviewing;

	private static BaikalMainFrame instance;

	public BaikalCore getCore() {
		return core;
	}

	public BaikalMainFrame(String userName) {
		String iniFileName = userName + ".ini";
		try {
			core = new BaikalCore(iniFileName);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "加载用户失败", "错误",
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		BaikalMainFrame.instance = this;

		try {
			core.loadAllDevices();
		} catch (BaikalCameraException e) {
			JOptionPane.showMessageDialog(this, "无法连接到相机，请检查硬件配置是否正常。", "错误",
					JOptionPane.ERROR_MESSAGE);
		}
		initUI();
	}

	public static BaikalMainFrame getInstance() {
		return instance;
	}

	private void initUI() {
		initMenu();
		initFocusManager();

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exitApplication();
			}
		});

		this.setLayout(new BorderLayout());
		JPanel basicPanel = new JPanel();
		basicPanel.setLayout(new BoxLayout(basicPanel, BoxLayout.Y_AXIS));
		basicPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Status bar
		statusLabel = new JLabel("Status");
		statusLabel.setBorder(BorderFactory
				.createBevelBorder(BevelBorder.LOWERED));
		this.add(statusLabel, BorderLayout.SOUTH);
		getContentPane().add(basicPanel);

		// Panel for batch information
		basicPanel.add(createInfoPanel());
		basicPanel.add(Box.createRigidArea(new Dimension(0, 20)));

		// The main panel for controllers
		basicPanel.add(createControllerPanel());

		setTitle("Baikal光学镜面检测系统 - " + core.getUserData().get("name"));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		pack();
		this.setMinimumSize(this.getPreferredSize());
		setLocationRelativeTo(null);
	}

	private JPanel createControllerPanel() {
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(BorderFactory.createEtchedBorder());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;

		JSONObject spDevices = core.getSupportedDevices();
		JLabel camLabel = new JLabel("相机型号：");
		JSONArray spCameras = (JSONArray) spDevices.get("SupportedCameras");
		String[] spCamerasStr = new String[spCameras.size()];
		String camModel = (String) core.getUserData().get("CameraModel");
		int camModelIndex = -1;
		for (int i = 0; i < spCamerasStr.length; i++) {
			spCamerasStr[i] = (String) spCameras.get(i);
			if (camModel.equals(spCamerasStr[i]))
				camModelIndex = i;
		}
		camListComboBox = new JComboBox(spCamerasStr);
		camListComboBox.setSelectedIndex(camModelIndex);
		camListComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				core.getUserData().put("CameraModel",
						(String) camListComboBox.getSelectedItem());
			}
		});

		JLabel lenseLabel = new JLabel("镜头型号：");
		JSONArray spLenses = (JSONArray) spDevices.get("SupportedLenses");
		String[] spLensesStr = new String[spLenses.size()];
		String lensModel = (String) core.getUserData().get("LensModel");
		int lensModelIndex = -1;
		for (int i = 0; i < spLensesStr.length; i++) {
			spLensesStr[i] = (String) spLenses.get(i);
			if (lensModel.equals(spLensesStr[i]))
				lensModelIndex = i;
		}
		lensListComboBox = new JComboBox(spLensesStr);
		lensListComboBox.setSelectedIndex(lensModelIndex);
		lensListComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				core.getUserData().put("LensModel",
						(String) lensListComboBox.getSelectedItem());
			}
		});

		JLabel capacityLabel1 = new JLabel("空间容量：");
		JLabel capacityLabel2 = new JLabel("N/A");

		JLabel camStatusLabel1 = new JLabel("相机状态：");
		JLabel camStatusLabel2 = new JLabel("未连接");
		camConnectButton = new JButton("重新连接");
		camConnectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					core.connectCamera();
				} catch (BaikalCameraException e1) {
					JOptionPane.showMessageDialog(BaikalMainFrame.this,
							"无法连接到相机，请检查硬件配置是否正常。", "错误",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		JPanel camConnectionPanel = new JPanel();
		camConnectionPanel.setLayout(new BoxLayout(camConnectionPanel,
				BoxLayout.X_AXIS));
		camConnectionPanel.add(camStatusLabel2);
		camConnectionPanel.add(Box.createHorizontalStrut(16));
		camConnectionPanel.add(Box.createHorizontalGlue());
		camConnectionPanel.add(camConnectButton);

		JLabel shutterLabel = new JLabel("快门时间（毫秒）：");
		int shutter = ((Long) core.getUserData().get("Shutter")).intValue();
		final JFormattedTextField shutterText = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		shutterText.setValue(shutter);
		shutterText.setPreferredSize(new Dimension(48, shutterText
				.getPreferredSize().height));
		shutterText.setInputVerifier(new FormattedTextInputVerifier());

		final JSlider shutterSlider = new JSlider(1, 1000);
		shutterSlider.setValue(shutter);
		shutterSlider.setSnapToTicks(true);
		shutterSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int val = shutterSlider.getValue();
				try {
					core.getCamera().setExposureTime(val);
					shutterText.setValue(val);
					core.getUserData().put("Shutter", val);
				} catch (BaikalCameraException e) {
					JOptionPane.showMessageDialog(BaikalMainFrame.this, "相机异常",
							"错误", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		shutterText.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int val = ((Number) shutterText.getValue()).intValue();
				shutterSlider.setValue(val);
			}
		});
		shutterText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				shutterSlider.setValue(((Number) shutterText.getValue())
						.intValue());
			}
		});

		JLabel apertureLabel = new JLabel("光圈：f/");
		int aperture = 0;
		{
			Object obj = core.getUserData().get("Aperture");
			if (obj instanceof Long)
				aperture = ((Long) obj).intValue();
			else if (obj instanceof Integer)
				aperture = (Integer) obj;
		}
		JTextField apertureText = new JTextField("1.4");
		apertureText.setPreferredSize(new Dimension(32, apertureText
				.getPreferredSize().height));
		JSlider apertureSlider = new JSlider(0, 9);
		apertureSlider.setValue(0);
		apertureSlider.setSnapToTicks(true);

		JLabel gridCountLabel = new JLabel("栅格图分割数目：");
		NumberFormat intFormat;
		intFormat = NumberFormat.getIntegerInstance();
		final JFormattedTextField gridCountText = new JFormattedTextField(
				intFormat);
		gridCountText.setValue((Long) core.getUserData()
				.get("GridSegmentCount"));
		gridCountText.setPreferredSize(new Dimension(32, gridCountText
				.getPreferredSize().height));
		gridCountText.addPropertyChangeListener(new PropertyChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String prop = evt.getPropertyName();
				if (prop.equals("value")) {
					core.getUserData().put("GridSegmentCount",
							evt.getNewValue());
				}
			}
		});

		toggleLiveButton = new JButton("预览");
		toggleLiveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				DefaultButtonModel model = (DefaultButtonModel) toggleLiveButton
						.getModel();
				if (model.isSelected()) {
					stopPreview();
					model.setSelected(false);
					toggleLiveButton.setText("预览");
				} else {
					if (gridFrame == null) {
						gridFrame = new BaikalGridFrame();
					}
					gridFrame.setDrawMarkers(true);
					gridFrame.setDrawXGridLines(true);
					gridFrame.setDrawYGridLines(true);
					gridFrame.setSegCount(1);
					gridFrame.setXYOffset(0, 0);
					gridFrame.setVisible(true);
					startPreview();

					model.setSelected(true);
					toggleLiveButton.setText("停止");
				}
			}
		});

		runTestButton = new JButton("开始测试");
		runTestButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				performMeasurement();
				Runnable task = new Runnable() {
					@Override
					public void run() {
						try {
							SwingUtilities.invokeAndWait(new Runnable() {
								@Override
								public void run() {
									if (gridFrame == null)
										gridFrame = new BaikalGridFrame();
									gridFrame.setVisible(true);
								}
							});
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}

						int cnt = ((Long) core.getUserData().get(
								"GridSegmentCount")).intValue();
						gridFrame.setSegCount(cnt);
						gridFrame.setDrawXGridLines(false);
						gridFrame.setDrawYGridLines(false);
						gridFrame.setDrawMarkers(true);
						try {
							SwingUtilities.invokeAndWait(new Runnable() {
								@Override
								public void run() {
									gridFrame.repaint();
								}
							});
							Thread.sleep(500);
						} catch (InterruptedException e2) {
							e2.printStackTrace();
						} catch (InvocationTargetException e2) {
							e2.printStackTrace();
						}

						gridFrame.setDrawMarkers(false);
						for (int i = 0; i < 2 * cnt; i++) {
							gridFrame.setDrawYGridLines(i < cnt);
							gridFrame.setDrawXGridLines(i >= cnt);
							gridFrame.setXYOffset(i / 2, i / 2);
							try {
								SwingUtilities.invokeAndWait(new Runnable() {
									@Override
									public void run() {
										gridFrame.repaint();
									}
								});
								Thread.sleep(100);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (InvocationTargetException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}

						gridFrame.setDrawXGridLines(true);
						gridFrame.setDrawYGridLines(true);
						gridFrame.setDrawMarkers(true);
						gridFrame.setXYOffset(0, 0);
						gridFrame.setSegCount(1);
						try {
							SwingUtilities.invokeAndWait(new Runnable() {
								@Override
								public void run() {
									gridFrame.repaint();

									try {
										Thread.sleep(800);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									Opener opener = new Opener();
									ImagePlus img = opener
											.openImage("res/sample.jpg");
									// System.out.println(BaikalMainFrame.this
									// .getClass()
									// .getResource("sample.jpg")
									// .toString());
									img.setTitle("镜面检测结果 - 形貌图");
									img.show();
								}
							});
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				};
				// (new Thread(task)).start();
			}
		});

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
		buttonBox.add(toggleLiveButton);
		buttonBox.add(Box.createHorizontalStrut(24));
		buttonBox.add(runTestButton);
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

		// The painting area
		imagePanel = new ImagePanel();
		imagePanel.setPreferredSize(new Dimension(800, 800 * 2 / 3));
		imagePanel.setBackground(Color.BLACK);
		imagePanel
				.addImagePanelMouseMotionListener(new ImagePanelMouseMotionListener() {
					@Override
					public void mouseDragged(MouseEvent e) {
					}

					@Override
					public void mouseMoved(MouseEvent e) {
						ImagePanelMouseEvent evt = (ImagePanelMouseEvent) e;
						statusLabel.setText(String.format(
								"Original: %d, %d; Image: %d, %d", evt.getX(),
								evt.getY(), evt.getImageX(), evt.getImageY()));
					}
				});
		imagePanel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				double x = e.getPoint().getX();
				double y = e.getPoint().getY();
				statusLabel.setText(String.format("%f,  %f", x, y));
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}

		});
		gbc.gridwidth = 1;
		gbc.weighty = 0;
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridheight = 11;
		// imagePanel.drawImage();
		mainPanel.add(imagePanel, gbc);

		return mainPanel;
	}

	protected void startPreview() {
		previewExec = Executors.newCachedThreadPool();
		isPreviewing = true;
		previewExec.execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				BaikalAbstractCamera cam = core.getCamera();

				ReentrantLock lock = imagePanel.lock;
				byte[] buffer = null;
				while (!Thread.interrupted() && isPreviewing) {
					try {
						lock.lockInterruptibly();
						imagePanel.initImage(cam.getWidth(), cam.getHeight(),
								cam.getBitDepth());
						buffer = imagePanel.getInternalBuffer();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					} catch (BaikalCameraException e) {
						JOptionPane.showMessageDialog(BaikalMainFrame.this,
								"相机异常", "错误", JOptionPane.ERROR_MESSAGE);
					} finally {
						if (lock.isHeldByCurrentThread())
							lock.unlock();
					}

					try {
						cam.snapshotAndWait(buffer, lock);
					} catch (BaikalCameraException e) {
						JOptionPane.showMessageDialog(BaikalMainFrame.this,
								"相机异常", "错误", JOptionPane.ERROR_MESSAGE);
					}

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							imagePanel.repaint();
						}
					});
				}
			}
		});
	}

	protected void stopPreview() {
		// TODO Auto-generated method stub
		if (previewExec == null)
			return;

		isPreviewing = false;
		previewExec.shutdown();
		previewExec = null;
	}

	/*
	 * Start to measure the surface
	 */
	public void performMeasurement() {
		// TODO Auto-generated method stub
		BaikalAbstractCamera cam = core.getCamera();

		ReentrantLock lock = imagePanel.lock;
		byte[] buffer = null;
		try {
			lock.lockInterruptibly();
			imagePanel.initImage(cam.getWidth(), cam.getHeight(),
					cam.getBitDepth());
			buffer = imagePanel.getInternalBuffer();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (BaikalCameraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}

		try {
			cam.snapshotAndWait(buffer, lock);
		} catch (BaikalCameraException e) {
			JOptionPane.showMessageDialog(BaikalMainFrame.this, "相机异常", "错误",
					JOptionPane.ERROR_MESSAGE);
		}

		imagePanel.repaint();
	}

	/**
	 * @return
	 */
	private JPanel createInfoPanel() {
		JSONObject userData = core.getUserData();
		JPanel infoPanel = new JPanel(new GridBagLayout());
		infoPanel.setBorder(BorderFactory.createEtchedBorder());

		JLabel operatorLabel = new JLabel("操作员：");
		JTextField operatorText = new JTextField((String) userData.get("Name"));
		operatorText.setEditable(false);
		operatorText.setFocusable(false);
		operatorText.setMaximumSize(new Dimension(256, operatorText
				.getMaximumSize().height));
		operatorText.setPreferredSize(new Dimension(96, operatorText
				.getPreferredSize().height));

		JLabel mirrorIdLabel = new JLabel("镜面编号：");
		JTextField mirrorIdText = new JTextField(
				((Long) userData.get("MirrorId")).toString());
		mirrorIdText.setEditable(false);
		mirrorIdText.setFocusable(false);
		mirrorIdText.setPreferredSize(new Dimension(64, mirrorIdText
				.getPreferredSize().height));

		JLabel batchTimeLabel = new JLabel("时间：");
		final JTextField batchTimeText = new JTextField((new Date()).toString());
		batchTimeText.setEditable(false);
		batchTimeText.setFocusable(false);
		// Update the time text field once per second.
		javax.swing.Timer timer = new javax.swing.Timer(1000,
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								batchTimeText.setText((new Date()).toString());
							}
						});
					}
				});
		timer.start();

		JLabel mirrorWidthLabel = new JLabel("镜面宽度（毫米）：");
		JLabel mirrorHeightLabel = new JLabel("镜面高度（毫米）：");
		final JFormattedTextField mirrorWidthText = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		final JFormattedTextField mirrorHeightText = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		PropertyChangeListener mirrorDimensionListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				// TODO Auto-generated method stub
				Number width = (Number) mirrorWidthText.getValue();
				Number height = (Number) mirrorHeightText.getValue();
				core.getUserData().put("MirrorWidth", width.doubleValue());
				core.getUserData().put("MirrorHeight", height.doubleValue());
			}
		};

		mirrorWidthText.setInputVerifier(new FormattedTextInputVerifier());
		mirrorWidthText.setValue((Double) (core.getUserData()
				.get("MirrorWidth")));
		mirrorWidthText.addPropertyChangeListener("value",
				mirrorDimensionListener);

		mirrorHeightText.setInputVerifier(new FormattedTextInputVerifier());
		mirrorHeightText.setValue((Double) (core.getUserData()
				.get("MirrorHeight")));
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
				core.serialize();
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

}

class FormattedTextInputVerifier extends InputVerifier {
	@Override
	public boolean verify(JComponent input) {
		return ((JFormattedTextField) input).isEditValid();
	}

}