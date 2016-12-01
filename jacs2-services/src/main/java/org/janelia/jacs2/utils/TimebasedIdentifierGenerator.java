package org.janelia.jacs2.utils;

import com.google.common.base.Preconditions;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class TimebasedIdentifierGenerator {

    private static final Long CURRENT_TIME_OFFSET = 921700000000L;
    private static final int MAX_DEPLOYMENT_CONTEXT = 15; // 4 bits only

    private final int ipComponent;
    private final int deploymentContext;
    private IDBlock lastIDBlock;

    private static final class IDBlock {
        private static final int BLOCK_SIZE = 1024;
        private long timeComponent;
        private long deploymentContext;
        private long ipComponent;
        private long currentIndex = 0;

        private synchronized boolean hasNext() {
            return currentIndex < BLOCK_SIZE;
        }

        private synchronized Number next() {
            BigInteger nextId = BigInteger.valueOf(timeComponent).shiftLeft(22)
                                    .add(BigInteger.valueOf(currentIndex << 12))
                                    .add(BigInteger.valueOf(deploymentContext << 8))
                                    .add(BigInteger.valueOf(ipComponent));
            currentIndex++;
            return nextId;
        }

    }

    public TimebasedIdentifierGenerator(int deploymentContext) {
        Preconditions.checkArgument(deploymentContext >= 0 && deploymentContext <= MAX_DEPLOYMENT_CONTEXT,
                "Deployment context value is out of range. It's current value is "
                        + deploymentContext + " and the allowed values are between 0 and " + MAX_DEPLOYMENT_CONTEXT);
        this.deploymentContext = deploymentContext;
        ipComponent = getIpAddrCompoment();
    }

    public List<Number> generateIdList(long n) {
        List<Number> idList = new ArrayList<>();
        long total = 0L;
        while (total < n) {
            IDBlock idBlock = getIDBlock();
            for (; total < n && idBlock.hasNext(); total++) {
                idList.add(idBlock.next());
            }
        }
        return idList;
    }

    public Number generateId() {
        IDBlock idBlock = getIDBlock();
        return idBlock.next();
    }

    private synchronized IDBlock getIDBlock() {
        if (lastIDBlock != null && lastIDBlock.hasNext()) {
            return lastIDBlock;
        }
        IDBlock idBlock = new IDBlock();
        idBlock.ipComponent = ipComponent;
        idBlock.deploymentContext = deploymentContext;
        idBlock.timeComponent = System.currentTimeMillis() - CURRENT_TIME_OFFSET;
        if (lastIDBlock != null && lastIDBlock.timeComponent == idBlock.timeComponent) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            idBlock.timeComponent = System.currentTimeMillis() - CURRENT_TIME_OFFSET;
        }
        lastIDBlock = idBlock;
        return idBlock;
    }

    private int getIpAddrCompoment() {
        try {
            byte[] ipAddress = InetAddress.getLocalHost().getAddress();
            return ((int)ipAddress[ipAddress.length - 1] & 0xFF);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

}
