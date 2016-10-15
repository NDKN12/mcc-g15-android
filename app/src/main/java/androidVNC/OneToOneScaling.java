/**
 * Copyright (C) 2009 Michael A. MacDonald
 */
package androidVNC;

import android.widget.ImageView.ScaleType;

import fi.aalto.openoranges.project1.mcc.R;

/**
 * @author Michael A. MacDonald
 */
class OneToOneScaling extends AbstractScaling {

	/**
	 * @param id
	 * @param scaleType
	 */
	public OneToOneScaling() {
		super(fi.aalto.openoranges.project1.mcc.R.id.itemOneToOne,ScaleType.CENTER);
	}

	/* (non-Javadoc)
	 * @see android.androidVNC.AbstractScaling#getDefaultHandlerId()
	 */
	@Override
	int getDefaultHandlerId() {
		return fi.aalto.openoranges.project1.mcc.R.id.itemInputTouchPanTrackballMouse;
	}

	/* (non-Javadoc)
	 * @see android.androidVNC.AbstractScaling#isAbleToPan()
	 */
	@Override
	boolean isAbleToPan() {
		return true;
	}

	/* (non-Javadoc)
	 * @see android.androidVNC.AbstractScaling#isValidInputMode(int)
	 */
	@Override
	boolean isValidInputMode(int mode) {
		return mode != fi.aalto.openoranges.project1.mcc.R.id.itemInputFitToScreen;
	}

	/* (non-Javadoc)
	 * @see android.androidVNC.AbstractScaling#setScaleTypeForActivity(android.androidVNC.VncCanvasActivity)
	 */
	@Override
	void setScaleTypeForActivity(VncCanvasActivity activity) {
		super.setScaleTypeForActivity(activity);
		activity.vncCanvas.scrollToAbsolute();
		activity.vncCanvas.pan(0,0);
	}

}
