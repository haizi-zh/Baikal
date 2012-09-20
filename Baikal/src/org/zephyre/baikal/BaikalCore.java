package org.zephyre.baikal;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.zephyre.baikal.camera.BaikalAbstractCamera;
import org.zephyre.baikal.camera.BaikalCameraException;
import org.zephyre.baikal.camera.BaikalSimCamera;

public class BaikalCore {
	private static void setUIFont() {
		Font myFont = new Font("微软雅黑", Font.PLAIN, 12);
		javax.swing.plaf.FontUIResource fontRes = new javax.swing.plaf.FontUIResource(
				myFont);
		java.util.Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof javax.swing.plaf.FontUIResource) {
				UIManager.put(key, fontRes);
			}
		}
	}

	public static void main(String[] args) {
		// System.setProperty("swing.plaf.metal.controlFont", "微软雅黑");
		setUIFont();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				LoginDialog dlg = new LoginDialog();
				dlg.setVisible(true);
				if (dlg.selectedUserName == null) {
					System.exit(0);
				}

				BaikalMainFrame frame = new BaikalMainFrame(
						dlg.selectedUserName);
				frame.setVisible(true);
			}
		});
	}

	private JSONObject userData;
	private JSONObject devicesList;
	private BaikalAbstractCamera cam;

	/**
	 * Initialize the BaikalCore object with the configuration file.
	 * 
	 * @param fileName
	 *            Name of the configuration file.
	 * @throws IOException
	 */
	public BaikalCore(String fileName) throws IOException {
		// Load the user
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			userData = (JSONObject) JSONValue.parse(reader);
			if (userData == null)
				throw new IOException();
		} finally {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		}

		// Check the user data
		HashMap<String, Object> defaultPairs = new HashMap<String, Object>();
		defaultPairs.put("MirrorId", 1);
		defaultPairs.put("Name", "Unknown");
		defaultPairs.put("UUID", System.currentTimeMillis());
		defaultPairs.put("MirrorWidth", 1000.0);
		defaultPairs.put("MirrorHeight", 1000.0);
		defaultPairs.put("CameraModel", "");
		defaultPairs.put("LensModel", "");
		defaultPairs.put("Shutter", (Integer) 50);
		defaultPairs.put("Aperture", 1);
		defaultPairs.put("DensityX", 100);
		defaultPairs.put("DensityY", 100);
		defaultPairs.put("MarkerRadius", 16);
		defaultPairs.put("GridSegmentCount", 10);
		defaultPairs.put("ISO", 100);
		defaultPairs.put("Resolution", 1);

		// If userData is updated, it needs to be serialized to disk.
		for (String key : defaultPairs.keySet()) {
			if (!userData.containsKey(key)) {
				userData.put(key, defaultPairs.get(key));
			}
		}
		serialize();

		try {
			reader = new BufferedReader(new FileReader(fileName));
			userData = (JSONObject) JSONValue.parse(reader);
			if (userData == null)
				throw new IOException();
		} finally {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		}

		// Load the devices information.
		try {
			reader = new BufferedReader(new FileReader("Devices.dat"));
			devicesList = (JSONObject) JSONValue.parse(reader);
		} finally {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		}

		// Initialize devices
		cam = new BaikalSimCamera();
	}

	/**
	 * Serialize the user data to disk
	 * 
	 * @throws IOException
	 */
	public void serialize() throws IOException {
		String fileName = (String) userData.get("Name") + ".ini";

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(fileName));
			writer.write(userData.toJSONString());
			writer.close();
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	public JSONObject getUserData() {
		return userData;
	}

	public JSONObject getSupportedDevices() {
		return devicesList;
	}

	/*
	 * Initialize and load all the devices
	 */
	public void loadAllDevices() throws BaikalCameraException {
		connectCamera();
		setExposureTime(((Number) getUserData().get("Shutter")).intValue());
		setResolution(((Number) getUserData().get("Resolution")).intValue());
	}

	/*
	 * Connect to the default camrea.
	 */
	public void connectCamera() throws BaikalCameraException {
		cam.connect();
	}

	/*
	 * Set the exposure time in millisecond.
	 */
	public void setExposureTime(int val) throws BaikalCameraException {
		cam.setExposureTime(val);
	}

	/*
	 * Get the exposure time in millisecond.
	 */
	public int exposureTime() throws BaikalCameraException {
		return cam.exposureTime();
	}

	/*
	 * Set the resolution type.
	 */
	public void setResolution(int res) throws BaikalCameraException {
		cam.setResolutioin(res);
	}

	/*
	 * Get the camera object.
	 */
	public BaikalAbstractCamera getCamera() {
		return cam;
	}
}
