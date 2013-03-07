package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

public class WhiteLevelSlider extends BasicColorSlider
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
			setMaximum(ccm.getDataMax()); // left edge of slider widget
			// but don't allow slider handle to go past whiteLevel point (at least on Mac)
			setExtent(ccm.getBlackLevel()+1); // +1: don't go all the way to black value
			System.out.println("white extent = "+getExtent());
		}
	};
	
	public WhiteLevelSlider(int channelIndex, ImageColorModel imageColorModel) 
	{
		this.channelIndex = channelIndex;
		this.imageColorModel = imageColorModel;
		setInverted(true);
		imageColorModel.getColorModelChangedSignal().connect(updateSliderSlot);
	}

	@Override
	public void setValue(int value0) {
		int value = value0;
		if ( (imageColorModel != null) && (imageColorModel.getChannelCount() > channelIndex) ) 
		{
			ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
			value = ccm.getDataMax() - value; // slider is inverted
			value = Math.max(ccm.getBlackLevel()+1, value);
			ccm.setWhiteLevel(value);
		}
		super.setValue(value);
		System.out.println("White = " + value);
	}

}
