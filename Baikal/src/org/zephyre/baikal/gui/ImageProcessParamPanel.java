/**
 * 
 */
package org.zephyre.baikal.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.NumberInputVerifier;
import org.zephyre.baikal.BaikalCore.PrefConst;

/**
 * 设置图像处理方面的参数设置界面
 * 
 * @author Zephyre
 * 
 */
final class ImageProcessParamPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -266687107932234187L;
	private boolean autoPreview_;

	boolean getAutoPreview() {
		return autoPreview_;
	}

	void setAutoPreview(boolean val) {
		autoPreview_ = val;
	}

	ImageProcessParamPanel() {
		autoPreview_ = false;
		initUI();
	}

	private void initUI() {
		final int DEFAULT_FIELD_WIDTH = 48;
		final BaikalCore core = BaikalCore.getInstance();
		ResourceBundle res_ = ResourceBundle
				.getBundle("org.zephyre.baikal.BaikalRes");

		// 阈值
		@SuppressWarnings("unchecked")
		ArrayList<Number> thrdList = (ArrayList<Number>) core
				.getEntry(PrefConst.THRESHOLD);
		JLabel thrdLabel = new JLabel(res_.getString("ThresholdParam"));
		final JFormattedTextField[] thrdFields = new JFormattedTextField[3];
		for (int i = 0; i < thrdFields.length; i++) {
			JFormattedTextField thrdField = new JFormattedTextField(
					NumberFormat.getIntegerInstance());
			thrdField.setValue(thrdList.get(i).intValue());
			thrdField.setInputVerifier(new NumberInputVerifier(0, 256,
					Integer.class));
			thrdField.setPreferredSize(new Dimension(28, thrdField
					.getPreferredSize().height));
			thrdFields[i] = thrdField;
		}

		// 平滑窗口大小
		@SuppressWarnings("unchecked")
		final ArrayList<Number> alWinSize = (ArrayList<Number>) core
				.getEntry(PrefConst.SMOOTH_WINDOW);
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
		final ArrayList<Number> alParam = (ArrayList<Number>) core
				.getEntry(PrefConst.SMOOTH_PARAM);
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

		double houghMinDist = ((Number) core
				.getEntry(PrefConst.HOUGH_CIRCLE_MIN_DIST)).doubleValue();
		JLabel houghMinDistLabel = new JLabel(res_.getString("HoughMinDist"));
		final JFormattedTextField minDistField = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		minDistField.setValue(houghMinDist);
		minDistField.setInputVerifier(new NumberInputVerifier(0,
				Double.MAX_VALUE, Double.class));
		minDistField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				minDistField.getPreferredSize().height));

		double houghCanny = ((Number) core
				.getEntry(PrefConst.HOUGH_CIRCLE_HIGH_THRESHOLD)).doubleValue();
		JLabel houghCannyLabel = new JLabel(
				res_.getString("HoughCannyThreshold"));
		final JFormattedTextField cannyThrdField = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		cannyThrdField.setValue(houghCanny);
		cannyThrdField.setInputVerifier(new NumberInputVerifier(0,
				Double.MAX_VALUE, Double.class));
		cannyThrdField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				cannyThrdField.getPreferredSize().height));

		double houghAccThrd = ((Number) core
				.getEntry(PrefConst.HOUGH_CIRCLE_ACC_THRESHOLD)).doubleValue();
		JLabel houghAccThrdLabel = new JLabel(
				res_.getString("HoughAccThreshold"));
		final JFormattedTextField accThrdField = new JFormattedTextField(
				NumberFormat.getNumberInstance());
		accThrdField.setValue(houghAccThrd);
		accThrdField.setInputVerifier(new NumberInputVerifier(0,
				Double.MAX_VALUE, Double.class));
		accThrdField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				accThrdField.getPreferredSize().height));

		int houghMinRadius = ((Number) core
				.getEntry(PrefConst.HOUGH_CIRCLE_MIN_RADIUS)).intValue();
		JLabel minRadiusLabel = new JLabel(res_.getString("HoughMinRadius"));
		final JFormattedTextField minRadiusField = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		minRadiusField.setValue(houghMinRadius);
		minRadiusField.setInputVerifier(new NumberInputVerifier(1,
				Integer.MAX_VALUE, Integer.class));
		minRadiusField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				minRadiusField.getPreferredSize().height));

		int houghMaxRadius = ((Number) core
				.getEntry(PrefConst.HOUGH_CIRCLE_MAX_RADIUS)).intValue();
		JLabel maxRadiusLabel = new JLabel(res_.getString("HoughMaxRadius"));
		final JFormattedTextField maxRadiusField = new JFormattedTextField(
				NumberFormat.getIntegerInstance());
		maxRadiusField.setValue(houghMaxRadius);
		maxRadiusField.setInputVerifier(new NumberInputVerifier(1,
				Integer.MAX_VALUE, Integer.class));
		maxRadiusField.setPreferredSize(new Dimension(DEFAULT_FIELD_WIDTH,
				maxRadiusField.getPreferredSize().height));

		// 自动预览
		final ArrayList<JComponent> fieldList = new ArrayList<JComponent>();
		fieldList.addAll(Arrays.asList(new JComponent[] { smoothParamField,
				smoothWinSizeField, dpField, minDistField, cannyThrdField,
				accThrdField, minRadiusField, maxRadiusField }));
		fieldList.addAll(Arrays.asList(thrdFields));

		final Runnable refreshUI = new Runnable() {
			@Override
			public void run() {
				ArrayList<Integer> thrds = new ArrayList<Integer>();
				for (JFormattedTextField field : thrdFields)
					thrds.add(Integer.valueOf(((Number) field.getValue())
							.intValue()));

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

				core.putEntry(PrefConst.THRESHOLD, new ArrayList<Number>(thrds));
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

		FocusAdapter focusAdapter = new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				refreshUI.run();
			}
		};
		ActionListener l = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshUI.run();
			}
		};
		for (JComponent ctrl : fieldList) {
			ctrl.addFocusListener(focusAdapter);
			((JFormattedTextField) ctrl).addActionListener(l);
		}

		// 布局
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.LINE_START;

		add(thrdLabel, gbc);
		for (JFormattedTextField field : thrdFields) {
			gbc.gridx++;
			add(field, gbc);
		}

		gbc.gridx = 0;
		gbc.gridy++;
		add(smoothWinSizeLabel, gbc);
		gbc.gridx++;
		gbc.gridwidth = 3;
		add(smoothWinSizeField, gbc);

		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy++;
		add(smoothParamLabel, gbc);
		gbc.gridx++;
		gbc.gridwidth = 3;
		add(smoothParamField, gbc);

		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy++;
		add(houghDpLabel, gbc);
		gbc.gridx++;
		gbc.gridwidth = 3;
		add(dpField, gbc);

		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy++;
		add(houghMinDistLabel, gbc);
		gbc.gridx++;
		gbc.gridwidth = 3;
		add(minDistField, gbc);

		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy++;
		add(houghCannyLabel, gbc);
		gbc.gridx++;
		gbc.gridwidth = 3;
		add(cannyThrdField, gbc);

		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy++;
		add(houghAccThrdLabel, gbc);
		gbc.gridx++;
		gbc.gridwidth = 3;
		add(accThrdField, gbc);

		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy++;
		add(minRadiusLabel, gbc);
		gbc.gridx++;
		gbc.gridwidth = 3;
		add(minRadiusField, gbc);

		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy++;
		add(maxRadiusLabel, gbc);
		gbc.gridx++;
		gbc.gridwidth = 3;
		add(maxRadiusField, gbc);
	}
}
