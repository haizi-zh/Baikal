/**
 * 
 */
package org.zephyre.baikal.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.BaikalCore.PrefConst;
import org.zephyre.baikal.NumberInputVerifier;

/**
 * 设置图像处理的各种参数
 * 
 * @author Zephyre
 * 
 */
public class BaikalProcParam extends JFrame {

	private boolean autoPreview_;

	public BaikalProcParam() {
		res_ = ResourceBundle.getBundle("org.zephyre.baikal.BaikalRes");
		autoPreview_ = true;
		initUI();
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
	}

	private ResourceBundle res_;

	/**
	 * 初始化UI
	 */
	private void initUI() {
		JPanel basicPanel = new JPanel();
		basicPanel.setLayout(new BoxLayout(basicPanel, BoxLayout.Y_AXIS));
		basicPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(basicPanel);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab(res_.getString("MarkerLoc"), createMarkerConfig());

		basicPanel.add(tabbedPane);

		final JCheckBox autoPreviewCheckbox = new JCheckBox(
				res_.getString("AutoPreview"), autoPreview_);
		autoPreviewCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				autoPreview_ = autoPreviewCheckbox.isSelected();
			}
		});
		basicPanel.add(autoPreviewCheckbox);
	}

	/**
	 * 图像处理模块
	 * 
	 * @return
	 */
	private JPanel createMarkerConfig() {
		final int DEFAULT_FIELD_WIDTH = 48;
		final BaikalCore core = BaikalCore.getInstance();
		// 阈值
		@SuppressWarnings("unchecked")
		ArrayList<Number> thrdList = (ArrayList<Number>) core.getEntry(PrefConst.THRESHOLD);
		int thrd = thrdList.get(0).intValue();
		JLabel thrdLabel = new JLabel(res_.getString("ThresholdParam"));
		final JFormattedTextField thrdField = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		thrdField.setValue(thrd);
		thrdField.setInputVerifier(new NumberInputVerifier(0, 256,
				Integer.class));
		thrdField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH, thrdField
				.getPreferredSize().height));

		// 平滑窗口大小
		@SuppressWarnings("unchecked")
		final ArrayList<Number> alWinSize = (ArrayList<Number>) core.getEntry(PrefConst.SMOOTH_WINDOW);
		int winsize = alWinSize.get(0).intValue();
		JLabel smoothWinSizeLabel = new JLabel(res_.getString("SmoothWinSize"));
		final JFormattedTextField smoothWinSizeField = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		smoothWinSizeField.setValue(winsize);
		smoothWinSizeField.setInputVerifier(new NumberInputVerifier(1,
				Integer.MAX_VALUE, Integer.class));
		smoothWinSizeField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				smoothWinSizeField.getPreferredSize().height));

		// Gaussian平滑参数
		@SuppressWarnings("unchecked")
		final ArrayList<Number> alParam = (ArrayList<Number>) core.getEntry(PrefConst.SMOOTH_PARAM);
		double smoothParam = alParam.get(0).doubleValue();
		JLabel smoothParamLabel = new JLabel(res_.getString("SmoothParam"));
		final JFormattedTextField smoothParamField = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		smoothParamField.setValue(smoothParam);
		smoothParamField.setInputVerifier(new NumberInputVerifier(0,
				Double.MAX_VALUE, Double.class));

		// Hough变换
		double houghDp = ((Number) core.getEntry(PrefConst.HOUGH_CIRCLE_DP))
				.doubleValue();
		JLabel houghDpLabel = new JLabel(res_.getString("HoughDp"));
		final JFormattedTextField dpField = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		dpField.setValue(houghDp);
		dpField.setInputVerifier(new NumberInputVerifier(0, Double.MAX_VALUE,
				Double.class));
		dpField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH, dpField
				.getPreferredSize().height));

		double houghMinDist = ((Number) core.getEntry(PrefConst.HOUGH_CIRCLE_MIN_DIST)).doubleValue();
		JLabel houghMinDistLabel = new JLabel(res_.getString("HoughMinDist"));
		final JFormattedTextField minDistField = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		minDistField.setValue(houghMinDist);
		minDistField.setInputVerifier(new NumberInputVerifier(0,
				Double.MAX_VALUE, Double.class));
		minDistField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				minDistField.getPreferredSize().height));

		double houghCanny = ((Number) core.getEntry(PrefConst.HOUGH_CIRCLE_HIGH_THRESHOLD)).doubleValue();
		JLabel houghCannyLabel = new JLabel(
				res_.getString("HoughCannyThreshold"));
		final JFormattedTextField cannyThrdField = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		cannyThrdField.setValue(houghCanny);
		cannyThrdField.setInputVerifier(new NumberInputVerifier(0,
				Double.MAX_VALUE, Double.class));
		cannyThrdField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				cannyThrdField.getPreferredSize().height));

		double houghAccThrd = ((Number) core.getEntry(PrefConst.HOUGH_CIRCLE_ACC_THRESHOLD)).doubleValue();
		JLabel houghAccThrdLabel = new JLabel(
				res_.getString("HoughAccThreshold"));
		final JFormattedTextField accThrdField = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		accThrdField.setValue(houghAccThrd);
		accThrdField.setInputVerifier(new NumberInputVerifier(0,
				Double.MAX_VALUE, Double.class));
		accThrdField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				accThrdField.getPreferredSize().height));

		int houghMinRadius = ((Number) core.getEntry(PrefConst.HOUGH_CIRCLE_MIN_RADIUS)).intValue();
		JLabel minRadiusLabel = new JLabel(res_.getString("HoughMinRadius"));
		final JFormattedTextField minRadiusField = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		minRadiusField.setValue(houghMinRadius);
		minRadiusField.setInputVerifier(new NumberInputVerifier(1,
				Integer.MAX_VALUE, Integer.class));
		minRadiusField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				minRadiusField.getPreferredSize().height));

		int houghMaxRadius = ((Number) core.getEntry(PrefConst.HOUGH_CIRCLE_MAX_RADIUS)).intValue();
		JLabel maxRadiusLabel = new JLabel(res_.getString("HoughMaxRadius"));
		final JFormattedTextField maxRadiusField = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		maxRadiusField.setValue(houghMaxRadius);
		maxRadiusField.setInputVerifier(new NumberInputVerifier(1,
				Integer.MAX_VALUE, Integer.class));
		maxRadiusField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				maxRadiusField.getPreferredSize().height));

		// 自动预览
		final JComponent[] controlArray = new JComponent[] { thrdField,
				smoothParamField, smoothWinSizeField, dpField, minDistField,
				cannyThrdField, accThrdField, minRadiusField, maxRadiusField };
		FocusAdapter focusAdapter = new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				int thrd = ((Number) thrdField.getValue()).intValue();
				int winsize = ((Number) smoothWinSizeField.getValue())
						.intValue();
				double smoothParam = ((Number) smoothParamField.getValue())
						.doubleValue();
				double dp = ((Number) dpField.getValue()).doubleValue();
				double minDist = ((Number) minDistField.getValue())
						.doubleValue();
				double cannyThrd = ((Number) cannyThrdField.getValue())
						.doubleValue();
				double accThrd = ((Number) accThrdField.getValue())
						.doubleValue();
				int minRadius = ((Number) minRadiusField.getValue()).intValue();
				int maxRadius = ((Number) maxRadiusField.getValue()).intValue();

				@SuppressWarnings("unchecked")
				ArrayList<Number> thrdList = (ArrayList<Number>) core.getEntry(PrefConst.THRESHOLD);
				// 默认改变的只有0,2两个通道的阈值
				thrdList.set(0, (double) thrd);
				thrdList.set(2, (double) thrd);
				core.putEntry(PrefConst.THRESHOLD, thrdList);
				ArrayList<Number> alWinSize = new ArrayList<Number>();
				alWinSize.add(winsize);
				alWinSize.add(winsize);
				core.putEntry(PrefConst.SMOOTH_WINDOW, alWinSize);
				ArrayList<Number> alParam = new ArrayList<Number>();
				alParam.add(smoothParam);
				alParam.add(smoothParam);
				core.putEntry(PrefConst.SMOOTH_PARAM, alParam);
				core.putEntry(PrefConst.HOUGH_CIRCLE_DP, dp);
				core.putEntry(PrefConst.HOUGH_CIRCLE_MIN_DIST, minDist);
				core.putEntry(PrefConst.HOUGH_CIRCLE_HIGH_THRESHOLD, cannyThrd);
				core.putEntry(PrefConst.HOUGH_CIRCLE_ACC_THRESHOLD, accThrd);
				core.putEntry(PrefConst.HOUGH_CIRCLE_MIN_RADIUS, minRadius);
				core.putEntry(PrefConst.HOUGH_CIRCLE_MAX_RADIUS, maxRadius);

				// 自动预览
				if (autoPreview_
						&& !BaikalMainFrame.getInstance().isPreviewing())
					BaikalMainFrame.getInstance().updateImage();
			}
		};
		ActionListener l = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// 如果为true，说明有的field格式错误，不会update。
				boolean formatError = false;
				for (JComponent c : controlArray) {
					NumberInputVerifier verifier = (NumberInputVerifier) ((JFormattedTextField) c)
							.getInputVerifier();
					if (!verifier.shouldYieldFocus(c)) {
						formatError = true;
						break;
					}
				}
				if (formatError)
					return;

				int thrd = ((Number) thrdField.getValue()).intValue();
				int winsize = ((Number) smoothWinSizeField.getValue())
						.intValue();
				double smoothParam = ((Number) smoothParamField.getValue())
						.doubleValue();
				double dp = ((Number) dpField.getValue()).doubleValue();
				double minDist = ((Number) minDistField.getValue())
						.doubleValue();
				double cannyThrd = ((Number) cannyThrdField.getValue())
						.doubleValue();
				double accThrd = ((Number) accThrdField.getValue())
						.doubleValue();
				int minRadius = ((Number) minRadiusField.getValue()).intValue();
				int maxRadius = ((Number) maxRadiusField.getValue()).intValue();

				@SuppressWarnings("unchecked")
				ArrayList<Number> thrdList = (ArrayList<Number>) core.getEntry(PrefConst.THRESHOLD);
				// 默认改变的只有0,2两个通道的阈值
				thrdList.set(0, (double) thrd);
				thrdList.set(2, (double) thrd);
				core.putEntry(PrefConst.THRESHOLD, thrdList);
				ArrayList<Number> alWinSize = new ArrayList<Number>();
				alWinSize.add(winsize);
				alWinSize.add(winsize);
				core.putEntry(PrefConst.SMOOTH_WINDOW, alWinSize);
				ArrayList<Number> alParam = new ArrayList<Number>();
				alParam.add(smoothParam);
				alParam.add(smoothParam);
				core.putEntry(PrefConst.SMOOTH_PARAM, alParam);
				core.putEntry(PrefConst.HOUGH_CIRCLE_DP, dp);
				core.putEntry(PrefConst.HOUGH_CIRCLE_MIN_DIST, minDist);
				core.putEntry(PrefConst.HOUGH_CIRCLE_HIGH_THRESHOLD, cannyThrd);
				core.putEntry(PrefConst.HOUGH_CIRCLE_ACC_THRESHOLD, accThrd);
				core.putEntry(PrefConst.HOUGH_CIRCLE_MIN_RADIUS, minRadius);
				core.putEntry(PrefConst.HOUGH_CIRCLE_MAX_RADIUS, maxRadius);

				// 自动预览
				if (autoPreview_
						&& !BaikalMainFrame.getInstance().isPreviewing())
					BaikalMainFrame.getInstance().updateImage();
			}
		};
		for (JComponent c : controlArray) {
			c.addFocusListener(focusAdapter);
			((JFormattedTextField) c).addActionListener(l);
		}

		// 布局
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.LINE_START;

		panel.add(thrdLabel, gbc);
		gbc.gridx++;
		panel.add(thrdField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		panel.add(smoothWinSizeLabel, gbc);
		gbc.gridx++;
		panel.add(smoothWinSizeField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		panel.add(smoothParamLabel, gbc);
		gbc.gridx++;
		panel.add(smoothParamField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		panel.add(houghDpLabel, gbc);
		gbc.gridx++;
		panel.add(dpField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		panel.add(houghMinDistLabel, gbc);
		gbc.gridx++;
		panel.add(minDistField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		panel.add(houghCannyLabel, gbc);
		gbc.gridx++;
		panel.add(cannyThrdField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		panel.add(houghAccThrdLabel, gbc);
		gbc.gridx++;
		panel.add(accThrdField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		panel.add(minRadiusLabel, gbc);
		gbc.gridx++;
		panel.add(minRadiusField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		panel.add(maxRadiusLabel, gbc);
		gbc.gridx++;
		panel.add(maxRadiusField, gbc);

		return panel;
	}
}
