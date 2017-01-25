package org.janelia.jacs2.asyncservice.imageservices;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FijiUtils {

    public static List<FijiColor> getColorSpec(String channelColors, String chanSpec) {
        if (StringUtils.isBlank(channelColors)) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(chanSpec)) {
            return Collections.emptyList();
        }

        Iterator<String> colorsIterator = Splitter.on(',').trimResults().splitToList(channelColors).iterator();
        Iterator<Integer> chanIterator = chanSpec.chars().iterator();
        List<FijiColor> colors = new ArrayList<>();
        for (; colorsIterator.hasNext() && chanIterator.hasNext();) {
            String color = colorsIterator.next();
            int chanValue = chanIterator.next();
            char chan = (char) chanValue;
            FijiColor fijiColor = getColorCode(color, chan);
            colors.add(fijiColor);
        }
        return colors;
    }

    /**
     * Fiji defines color channels as follows: (R)ed, (G)reen, (B)lue, grey(1), (C)yan, (M)agenta, (Y)ellow
     * We also control a divisor (inverse brightness, where 1 is brightest) that can be used to control the
     * color when it is used for a reference channel.
     * @param hexColor
     * @param channelType
     * @return
     */
    public static FijiColor getColorCode(String hexColor, char channelType) {
        if ("#ff0000".equals(hexColor)) {
            return new FijiColor('R',channelType=='r' ? 3 : 1); // Red
        } else if ("#00ff00".equals(hexColor)) {
            return new FijiColor('G',channelType=='r' ? 2 : 1); // Green
        } else if ("#0000ff".equals(hexColor)) {
            return new FijiColor('B',channelType=='r' ? 1 : 1); // Blue
        } else if ("#ffffff".equals(hexColor)) {
            return new FijiColor('1',channelType=='r' ? 2 : 1); // Grey
        } else if ("#0000ff".equals(hexColor)) {
            return new FijiColor('C',channelType=='r' ? 2 : 1); // Cyan
        } else if ("#ff00ff".equals(hexColor)) {
            return new FijiColor('M',channelType=='r' ? 2 : 1); // Magenta
        } else if ("#ffff00".equals(hexColor)) {
            return new FijiColor('Y',channelType=='r' ? 2 : 1); // Yellow
        } else if ("#7e5200".equals(hexColor)) {
            return new FijiColor('Y',channelType=='r' ? 3 : 2); // Brown
        }
        return new FijiColor('?',1);
    }

    public static List<FijiColor> getDefaultColorSpec(String chanSpec, String signalColors, char referenceColor) {
        Preconditions.checkArgument(StringUtils.isNotBlank(chanSpec));

        Iterator<Integer> signalColorsIterator = signalColors.chars().iterator();
        Iterator<Integer> chanIterator = chanSpec.chars().iterator();
        List<FijiColor> colors = new ArrayList<>();
        for (; chanIterator.hasNext();) {

            int chanValue = chanIterator.next();
            char chan = (char) chanValue;
            if (chan == 'r') {
                colors.add(new FijiColor(referenceColor, 2));
            } else {
                if (signalColorsIterator.hasNext()) {
                    int colorValue = signalColorsIterator.next();
                    char color = (char) colorValue;
                    colors.add(new FijiColor(color, 1));
                } else {
                    colors.add(new FijiColor('?', 1));
                }
            }
        }
        return colors;
    }
}
