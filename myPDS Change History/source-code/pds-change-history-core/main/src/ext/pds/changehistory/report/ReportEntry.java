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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Daniel Södling
 */
public class ReportEntry {

    protected final Map<String, Object> attributes;

    public ReportEntry(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    public Object get(String attributeName) {
        return attributes.get(attributeName);
    }

    public Object set(String attributeName, Object value) {
        return attributes.put(attributeName, value);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.attributes);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ReportEntry other = (ReportEntry) obj;
        return Objects.equals(this.attributes, other.attributes);
    }

}
