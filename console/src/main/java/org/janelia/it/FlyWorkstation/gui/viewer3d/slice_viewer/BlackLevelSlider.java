package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;


public class BlackLevelSlider extends BasicColorSlider 
{
	private static final long serialVersionUID = 1L;

	private Slot updateSliderSlot = new Slot() {
		@Override
		public void execute() {
			if (imageColorModel == null)
				return;
			if (imageColorModel.getChannelCount() <= channelIndex)
				return;
			ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
			int valueRange = ccm.getDataMax();
			int pixelRange = getWidth() - leftMarginPixels - rightMarginPixels - 2*handleWidthPixels;
			float pixelsPerValue = pixelRange / (float)valueRange;
			handleWidthValue = (int)Math.round(handleWidthPixels / pixelsPerValue);
			setMaximum(ccm.getDataMax()); // right edge of slider widget
			// but don't allow slider handle to go past whiteLevel point (at least on Mac)
			setExtent(getMaximum() - ccm.getWhiteLevel() + 1); // +1: don't go all the way to white value
			System.out.println("black extent = "+getExtent());
		}
	};
	
	public BlackLevelSlider(int channelIndex, ImageColorModel imageColorModel) {
		this.channelIndex = channelIndex;
		this.imageColorModel = imageColorModel;
		imageColorModel.getColorModelChangedSignal().connect(updateSliderSlot);
	}

	@Override
	public void setValue(int value0) {
		int value = value0;
		if ( (imageColorModel != null) && (imageColorModel.getChannelCount() > channelIndex) ) 
		{
			ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
			value = Math.min(ccm.getWhiteLevel()-1, value);
			ccm.setBlackLevel(value);
		}
		super.setValue(value);
		System.out.println("Black = " + value);
	}

}
