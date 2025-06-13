/*
 * Copyright 2018 PDSVision AB.
 * All rights reserved
 *
 * This software contains confidential proprietary information
 * belonging to PDSVision AB. No part of this information may be
 * used, reproduced, or stored without prior written consent
 * from PDSVision AB.
 */

package ext.pds.changehistory.wf;

import ext.pds.changehistory.ChangeHistoryProcessor;
import ext.pds.changehistory.utils.LogHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Daniel Martinsson
 */
@Slf4j
public class WorkflowAPI {

    private WorkflowAPI() {
    // Hides implicit no-args constructor
    }

    public static boolean setChangeHistory(Object pbo) {
        log.trace("setChangeHistory(Object pbo={})", LogHelper.toString(pbo));
        return ChangeHistoryProcessor.newProcessor().setChangeHistory(pbo);
    }

}
