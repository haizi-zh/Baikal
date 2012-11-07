package org.zephyre.baikal.camera;

public class BaikalCameraException extends Exception {
	public enum BaikalCameraErrorDesc {
		CONNECTION_FAILURE, CAMERA_NOT_CONNECTED, CAMERA_BUSY, BUFFER_UNDER_FLOW, 
		INVALID_ARGUMENT, NOT_SUPPORTED, UNKNWON
	}

	public static BaikalCameraException create(BaikalCameraErrorDesc desc) {
		return new BaikalCameraException(desc);
	}

	public static BaikalCameraException create() {
		return new BaikalCameraException();
	}

	public static BaikalCameraException create(BaikalCameraErrorDesc desc,
			String message) {
		return new BaikalCameraException(desc, message);
	}

	public static BaikalCameraException create(BaikalCameraErrorDesc desc,
			String message, Throwable cause) {
		return new BaikalCameraException(desc, message, cause);
	}

	private BaikalCameraErrorDesc desc_;

	protected BaikalCameraException(BaikalCameraErrorDesc desc) {
		desc_ = desc;
	}

	protected BaikalCameraException(BaikalCameraErrorDesc desc, String message) {
		super(message);
		desc_ = desc;
	}

	protected BaikalCameraException(BaikalCameraErrorDesc desc, String message,
			Throwable cause) {
		super(message, cause);
		desc_ = desc;
	}

	protected BaikalCameraException() {
	}

	public BaikalCameraErrorDesc getDescription() {
		return desc_;
	}
}
