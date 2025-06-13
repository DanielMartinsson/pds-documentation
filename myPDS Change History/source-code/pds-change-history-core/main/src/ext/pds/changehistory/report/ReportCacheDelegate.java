/*
 * Copyright 2018 PDSVision AB.
 * All rights reserved
 *
 * This software contains confidential proprietary information
 * belonging to PDSVision AB. No part of this information may be
 * used, reproduced, or stored without prior written consent
 * from PDSVision AB.
 */

package ext.pds.changehistory.report;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wt.util.WTException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Daniel Södling
 */
public class ReportCacheDelegate extends CacheLoader<String, File> implements RemovalListener<String, File> {

    private static final Logger logger = LoggerFactory.getLogger(ReportCacheDelegate.class);
    private final ReportProcessor processor;

    public ReportCacheDelegate(ReportProcessor processor) {
        this.processor = processor;
    }

    @Override
    public File load(String obid) throws WTException {
        if (logger.isTraceEnabled()) {
            logger.trace("load(String obid)");
            logger.trace("obid: {}", obid);
        }
        return processor.generateReport(obid);
    }

    /**
     * Deletes the file that corresponds to the removed cache entry
     * @param notification the remove notification
     */
    @Override
    public void onRemoval(RemovalNotification<String, File> notification) {
        if (logger.isTraceEnabled()) {
            logger.trace("onRemoval(RemovalNotification<String, File>");
            logger.trace("notification: {}", notification);
        }
        File file = notification.getValue();

        try {
            if (file != null && Files.deleteIfExists(file.toPath())) {
                logger.debug("Successfully cleared cached file {}", file.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to delete file {}", file.getAbsolutePath(), e);
        }
    }

}
