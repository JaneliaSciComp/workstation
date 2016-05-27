package org.janelia.it.workstation.gui.browser.gui.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around a DomainObject which provides a facade with dynamic access to its properties by UI label. This is
 * useful for user-driven user interface configuration.
 *
 * TODO: move to model
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicDomainObjectProxy implements Map<String,Object> {

    private static final Logger log = LoggerFactory.getLogger(DynamicDomainObjectProxy.class);

    private DomainObject domainObject;
    private HashMap<String,DomainObjectAttribute> attrs;

    public DynamicDomainObjectProxy(DomainObject domainObject) {
        this.domainObject = domainObject;
        this.attrs = new HashMap<>();
        for (DomainObjectAttribute attr : DomainUtils.getSearchAttributes(domainObject.getClass())) {
            log.trace("  {} -> {}.{}",attr.getLabel(), domainObject.getClass().getSimpleName(), attr.getName());
            attrs.put(attr.getLabel(), attr);
        }
    }

    @Override
    public int size() {
        return attrs.size();
    }

    @Override
    public boolean isEmpty() {
        return attrs.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return attrs.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("This method is not implemented on the proxy object");
    }

    @Override
    public Object get(Object key) {
        DomainObjectAttribute attr = attrs.get(key);
        if (attr==null) {
            log.trace("No such attribute: "+key);
        }
        try {
            return attr.getGetter().invoke(domainObject);
        }
        catch (Exception e) {
            log.error("Error getting sample attribute value for: "+key,e);
        }
        return null;
    }

    @Override
    public Object put(String key, Object value) {
        DomainObjectAttribute attr = attrs.get(key);
        if (attr==null) {
            log.trace("No such attribute: "+key);
        }
        try {
            return attr.getSetter().invoke(domainObject, value);
        }
        catch (Exception e) {
            log.error("Error setting sample attribute value for: "+key,e);
        }
        return null;
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("This method is not implemented on the proxy object");
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("This method is not implemented on the proxy object");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This method is not implemented on the proxy object");
    }

    @Override
    public Set<String> keySet() {
        return attrs.keySet();
    }

    @Override
    public Collection<Object> values() {
        throw new UnsupportedOperationException("This method is not implemented on the proxy object");
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException("This method is not implemented on the proxy object");
    }
}
