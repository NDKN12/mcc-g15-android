/**
 * Copyright (C) 2009 Michael A. MacDonald
 */
package androidVNC;

import android.widget.ImageView.ScaleType;
import fi.aalto.openoranges.project1.mcc.R;
/**
 * @author Michael A. MacDonald
 */
class FitToScreenScaling extends AbstractScaling {


	FitToScreenScaling() {
		super(R.id.itemFitToScreen, ScaleType.FIT_CENTER);
	}

	/* (non-Javadoc)
	 * @see android.androidVNC.AbstractScaling#isAbleToPan()
	 */
	@Override
	boolean isAbleToPan() {
		return false;
	}

	/* (non-Javadoc)
	 * @see android.androidVNC.AbstractScaling#isValidInputMode(int)
	 */
	@Override
	boolean isValidInputMode(int mode) {
		return mode == fi.aalto.openoranges.project1.mcc.R.id.itemInputFitToScreen;
	}

	/* (non-Javadoc)
	 * @see android.androidVNC.AbstractScaling#getDefaultHandlerId()
	 */
	@Override
	int getDefaultHandlerId() {
		return fi.aalto.openoranges.project1.mcc.R.id.itemInputFitToScreen;
	}

	/* (non-Javadoc)
	 * @see android.androidVNC.AbstractScaling#setCanvasScaleType(android.androidVNC.VncCanvas)
	 */
	@Override
	void setScaleTypeForActivity(VncCanvasActivity activity) {
		super.setScaleTypeForActivity(activity);
		activity.vncCanvas.absoluteXPosition = activity.vncCanvas.absoluteYPosition = 0;
		activity.vncCanvas.scrollTo(0, 0);
	}

}
