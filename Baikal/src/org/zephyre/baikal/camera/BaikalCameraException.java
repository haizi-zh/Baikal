package org.zephyre.baikal.camera;

public class BaikalCameraException extends Exception {
	public enum BaikalCameraErrorDesc {
		CONNECTION_FAILURE, CAMERA_NOT_CONNECTED, CAMERA_BUSY, INVALID_ARGUMENT, UNKNWON
	}

	private BaikalCameraErrorDesc desc;

	public BaikalCameraException() {
		super();
	}

	public BaikalCameraException(String message) {
		super(message);
	}

	public BaikalCameraException(String message, Throwable cause) {
		super(message, cause);
	}

	public BaikalCameraException(Throwable cause) {
		super(cause);
	}

	public void setDesc(BaikalCameraErrorDesc val) {
		desc = val;
	}

	public BaikalCameraErrorDesc desc() {
		return desc;
	}
}
