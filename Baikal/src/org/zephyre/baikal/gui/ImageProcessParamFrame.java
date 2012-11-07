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
public class ImageProcessParamFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 580982715732151077L;
	private boolean autoPreview_;

	public ImageProcessParamFrame() {
		res_ = ResourceBundle.getBundle("org.zephyre.baikal.BaikalRes");
		autoPreview_ = true;
		initUI();
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
	}

	private ResourceBundle res_;
	private ImageProcessParamPanel imageProcessParamPanel_;

	/**
	 * 初始化UI
	 */
	private void initUI() {
		JPanel basicPanel = new JPanel();
		basicPanel.setLayout(new BoxLayout(basicPanel, BoxLayout.Y_AXIS));
		basicPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(basicPanel);

		JTabbedPane tabbedPane = new JTabbedPane();
		imageProcessParamPanel_ = new ImageProcessParamPanel();
		imageProcessParamPanel_.setAutoPreview(autoPreview_);
		tabbedPane.addTab(res_.getString("MarkerLoc"), imageProcessParamPanel_);

		basicPanel.add(tabbedPane);

		final JCheckBox autoPreviewCheckbox = new JCheckBox(
				res_.getString("AutoPreview"), autoPreview_);
		autoPreviewCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				autoPreview_ = autoPreviewCheckbox.isSelected();
				imageProcessParamPanel_.setAutoPreview(autoPreview_);
			}
		});
		basicPanel.add(autoPreviewCheckbox);
	}
}
