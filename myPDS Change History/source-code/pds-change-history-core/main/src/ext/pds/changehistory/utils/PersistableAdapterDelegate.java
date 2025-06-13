/*
 * Copyright 2025 PDSVISION.
 * All rights reserved
 *
 * This software contains confidential proprietary information
 * belonging to PDSVISION. No part of this information may be
 * used, reproduced, or stored without prior written consent
 * from PDSVISION.
 */
package ext.pds.changehistory.utils;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.CreateOperationIdentifier;
import com.ptc.core.meta.common.OperationIdentifier;
import com.ptc.core.meta.common.TypeInstanceIdentifier;
import com.ptc.core.meta.common.UpdateOperationIdentifier;
import com.ptc.core.meta.container.common.AttributeTypeSummary;
import com.ptc.core.meta.descriptor.common.DefinitionDescriptor;
import wt.fc.Persistable;
import wt.session.SessionHelper;
import wt.util.WTException;

import java.util.Collection;

/**
 * @author Daniel Martinsson
 */
public class PersistableAdapterDelegate {
    private final PersistableAdapter adapter;

    public static PersistableAdapterDelegate newRetrieveDelegate(Persistable p) throws WTException {
        return newDelegate(p, (OperationIdentifier) null);
    }

    public static PersistableAdapterDelegate newCreateDelegate(String type) throws WTException {
        PersistableAdapter adapter = new PersistableAdapter(type, SessionHelper.getLocale(), new CreateOperationIdentifier());
        return new PersistableAdapterDelegate(adapter);
    }

    public static PersistableAdapterDelegate newUpdateDelegate(Persistable p) throws WTException {
        return newDelegate(p, new UpdateOperationIdentifier());
    }

    public static PersistableAdapterDelegate newDelegate(Persistable p, OperationIdentifier op) throws WTException {
        PersistableAdapter adapter = new PersistableAdapter(p, (String) null, SessionHelper.getLocale(), op);
        return new PersistableAdapterDelegate(adapter);
    }

    private PersistableAdapterDelegate(PersistableAdapter adapter) {
        this.adapter = adapter;
    }

    public void load(String... attributes) throws WTException {
        this.adapter.load(attributes);
    }

    public void load(Collection<String> attributes) throws WTException {
        this.adapter.load(attributes);
    }

    public Object get(String attribute) throws WTException {
        return this.adapter.get(attribute);
    }

    public Object set(String attribute, Object value) throws WTException {
        return this.adapter.set(attribute, value);
    }

    public String getAsString(String attribute) throws WTException {
        return (String) this.adapter.getAsString(attribute);
    }

    public DefinitionDescriptor getTypeDescriptor() throws WTException {
        return this.adapter.getTypeDescriptor();
    }

    public AttributeTypeSummary getAttributeDescriptor(String attribute) throws WTException {
        return this.adapter.getAttributeDescriptor(attribute);
    }

    public TypeInstanceIdentifier persist() throws WTException {
        return this.adapter.persist();
    }

    public Persistable apply() throws WTException {
        return this.adapter.apply();
    }
}
