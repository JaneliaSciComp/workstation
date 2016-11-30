package org.janelia.jacs2.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class TimebasedIdentifierGenerator {

    private static final Long CURRENT_TIME_OFFSET = 1480000000000L; // Nov-24-2016
    private static final int BLOCK_SIZE = 1024;

    private final long ipComponent;
    private long lastMillisecond;

    private static final class IDBlock {
        private long timeComponent;
        private long ipComponent;
        private int currentIndex = 0;

        private boolean hasNext() {
            return currentIndex < BLOCK_SIZE;
        }

        public Long next() {
            long nextId =
                    ((timeComponent << 18) & 0x7FFFFFFFL) +
                    ((ipComponent << 10) & 0x3FC00L) +
                    currentIndex;
            currentIndex++;
            return nextId;
        }
    }

    public TimebasedIdentifierGenerator() {
        ipComponent = getIpAddrCompoment();
        lastMillisecond = -1;
    }

    public List<Long> generateIdList(long n) {
        List<Long> idList = new ArrayList<>();
        long total = 0L;
        while (total < n) {
            IDBlock idBlock = generateBlock();
            for (long currentIndex = total; currentIndex < n && idBlock.hasNext(); currentIndex++) {
                idList.add(idBlock.next());
            }
            total += BLOCK_SIZE;
        }
        return idList;
    }

    public Long generateId() {
        IDBlock idBlock = generateBlock();
        return idBlock.next();
    }

    private synchronized IDBlock generateBlock() {
        IDBlock idBlock = new IDBlock();
        idBlock.ipComponent = ipComponent;
        idBlock.timeComponent = System.currentTimeMillis() - CURRENT_TIME_OFFSET;
        if (lastMillisecond != -1 && lastMillisecond == idBlock.timeComponent) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            idBlock.timeComponent = System.currentTimeMillis() - CURRENT_TIME_OFFSET;
        }
        lastMillisecond = idBlock.timeComponent;
        return idBlock;
    }

    private long getIpAddrCompoment() {
        try {
            byte[] ipAddress = InetAddress.getLocalHost().getAddress();
            return ((long)ipAddress[0] & 0xFF);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }
}
