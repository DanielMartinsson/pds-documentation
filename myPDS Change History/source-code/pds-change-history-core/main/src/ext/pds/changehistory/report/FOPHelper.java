/*
 * Copyright 2017 PDSVision AB.
 * All rights reserved
 *
 * This software contains confidential proprietary information
 * belonging to PDSVision AB. No part of this information may be
 * used, reproduced, or stored without prior written consent
 * from PDSVision AB.
 */

package ext.pds.changehistory.report;

import org.apache.fop.apps.FopFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * @author Daniel Södling
 */
public class FOPHelper {

    private static final Logger logger = LoggerFactory.getLogger(FOPHelper.class);

    public static FOPHelper newInstance() {
        return new FOPHelper();
    }

    /**
     * Creates a new FOPFactory based on the given style sheet.
     * <p>
     * Basic configuration is read from the configuration file defined by the
     * wt property ext.fopConfig;
     *
     * @param baseURL path to config folder
     * @return a new FopFactory instance
     */
    public FopFactory getFOPFactory(String baseURL) {
        if (logger.isTraceEnabled()) {
            logger.trace("getFOPFactory(File stylesheet)");
            logger.trace("baseURL: {}", baseURL);
        }
        return FopFactory.newInstance(URI.create(baseURL));
    }
}
