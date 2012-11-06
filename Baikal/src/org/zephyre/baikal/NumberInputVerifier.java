/**
 * 
 */
package org.zephyre.baikal;

import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;

/**
 * 有效输入：
 * 
 * @author Zephyre
 * 
 */
public class NumberInputVerifier extends InputVerifier {

	private Number min_;
	private Number max_;
	private Class<? extends Number> cls_;
	private NumberFormat formatter_;

	public NumberInputVerifier(Number min, Number max,
			Class<? extends Number> cls) {
		min_ = min;
		max_ = max;
		cls_ = cls;

		if (cls_ == Integer.class)
			formatter_ = NumberFormat.getIntegerInstance();
		else if (cls == Double.class)
			formatter_ = NumberFormat.getNumberInstance();
		else
			formatter_ = NumberFormat.getInstance();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.InputVerifier#verify(javax.swing.JComponent)
	 */
	@Override
	public boolean verify(JComponent input) {
		JTextField field = (JTextField) input;
		try {
			Number val = formatter_.parse(field.getText());
			if (val == null || val.doubleValue() < min_.doubleValue()
					|| val.doubleValue() >= max_.doubleValue())
				return false;
		} catch (ParseException e) {
			return false;
		}
		return true;
	}

	@Override
	public boolean shouldYieldFocus(JComponent input) {
		JFormattedTextField field = (JFormattedTextField) input;
		boolean ret = verify(field);
		if (ret) {
			// 重新格式化字符串
			try {
				// field.setText(formatter_.format(formatter_.parse(field
				// .getText())));
				field.commitEdit();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else
			field.selectAll();
		return ret;
	}
}
