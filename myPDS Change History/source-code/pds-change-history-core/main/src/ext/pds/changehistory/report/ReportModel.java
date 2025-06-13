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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Martinsson
 */
public class ReportModel {

    public final List<ReportEntry> parts = new ArrayList<>();
    public ReportEntry pn;

    public ReportModel(ReportEntry pn) {
        this.pn = pn;
    }

    public void addPart(ReportEntry entry) {
        parts.add(entry);
    }

}
