package org.janelia.jacs2.asyncservice.common;

import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DefaultServiceErrorChecker implements ServiceErrorChecker {

    private final Logger logger;

    public DefaultServiceErrorChecker(Logger logger) {
        this.logger = logger;
    }

    public List<String> collectErrors(JacsServiceData jacsServiceData) {
        List<String> errors = new ArrayList<>();
        InputStream outputStream = null;
        InputStream errorStream = null;
        try {
            if (StringUtils.isNotBlank(jacsServiceData.getOutputPath()) && new File(jacsServiceData.getOutputPath()).exists()) {
                outputStream = new FileInputStream(jacsServiceData.getOutputPath());
                streamHandler(outputStream, s -> {
                    if (hasErrors(s)) {
                        logger.error(s);
                        errors.add(s);
                    }
                    if (StringUtils.isNotBlank(s)) logger.debug(s);
                });
            }
            if (StringUtils.isNotBlank(jacsServiceData.getErrorPath()) && new File(jacsServiceData.getErrorPath()).exists()) {
                errorStream = new FileInputStream(jacsServiceData.getErrorPath());
                streamHandler(errorStream, s -> {
                    if (hasErrors(s)) {
                        errors.add(s);
                    }
                    if (StringUtils.isNotBlank(s)) logger.error(s);
                });
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.warn("Output stream close error", e);
                }
            }
            if (errorStream != null) {
                try {
                    errorStream.close();
                } catch (IOException e) {
                    logger.warn("Error stream close error", e);
                }
            }
        }
        return errors;
    }

    private void streamHandler(InputStream outStream, Consumer<String> lineConsumer) {
        BufferedReader outputReader = new BufferedReader(new InputStreamReader(outStream));
        for (;;) {
            try {
                String l = outputReader.readLine();
                if (l == null) break;
                lineConsumer.accept(l);
            } catch (IOException e) {
                logger.warn("Error stream close error", e);
                break;
            }
        }
    }

    protected boolean hasErrors(String l) {
        return StringUtils.isNotBlank(l) && l.matches("(?i:.*(error|exception).*)");
    }
}
