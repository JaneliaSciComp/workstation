package org.janelia.it.workstation.gui.browser.components.table;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.janelia.it.workstation.gui.browser.components.editor.DomainObjectAttribute;
import org.janelia.it.workstation.gui.browser.components.viewer.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectId;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A table viewer for domain objects. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectTableViewer extends TableViewer<DomainObject,DomainObjectId> implements AnnotatedDomainObjectListViewer {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectTableViewer.class);

    private static final String COLUMN_KEY_ANNOTATIONS = "annotations";
    
    private final DomainObjectAttribute annotationAttr = new DomainObjectAttribute(COLUMN_KEY_ANNOTATIONS,"Annotations",false,null);
    private final Map<String, DomainObjectAttribute> attributeMap = new HashMap<>();
    
    private AnnotatedDomainObjectList domainObjectList;
    
    @Override
    public void setSelectionModel(DomainObjectSelectionModel selectionModel) {
        super.setSelectionModel(selectionModel);
    }
    
    @Override
    public void showDomainObjects(AnnotatedDomainObjectList domainObjectList) {
        
        this.domainObjectList = domainObjectList;
        
        List<DomainObjectAttribute> searchAttrs = new ArrayList<>();
        searchAttrs.add(annotationAttr);
        
        for(DomainObject domainObject : domainObjectList.getDomainObjects()) {
            searchAttrs.addAll(getAttributes(domainObject));
            break; // for now we assume heterogenous lists, so we can quit after looking at the first object
        }
        
        Collections.sort(searchAttrs, new Comparator<DomainObjectAttribute>() {
            @Override
            public int compare(DomainObjectAttribute o1, DomainObjectAttribute o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        
        setAttributeColumns(searchAttrs);
                
        showObjects(domainObjectList.getDomainObjects());
    }
    
    private List<DomainObjectAttribute> getAttributes(DomainObject domainObject) {

        attributeMap.clear();
        
        List<DomainObjectAttribute> attrs = new ArrayList<>();
        Class<?> clazz = domainObject.getClass();
        
        for (Field field : ReflectionUtils.getAllFields(clazz)) {
            SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot!=null) {
                try {
                    Method getter = ReflectionHelper.getGetter(clazz, field.getName());
                    DomainObjectAttribute attr = new DomainObjectAttribute(searchAttributeAnnot.key(), searchAttributeAnnot.label(), searchAttributeAnnot.facet(), getter);
                    attrs.add(attr);
                    attributeMap.put(attr.getName(), attr);
                }
                catch (Exception e) {
                    log.warn("Error getting field " + field.getName() + " on object " + domainObject, e);
                }
            }
        }

        for (Method method : clazz.getMethods()) {
            SearchAttribute searchAttributeAnnot = method.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot!=null) {
                DomainObjectAttribute attr = new DomainObjectAttribute(searchAttributeAnnot.key(), searchAttributeAnnot.label(), searchAttributeAnnot.facet(), method);
                attrs.add(attr);
                attributeMap.put(attr.getName(), attr);
            }
        }

        return attrs;
    }
        
    @Override
    protected Object getValue(DomainObject object, String columnName) {
        try {
            if (COLUMN_KEY_ANNOTATIONS.equals(columnName)) {
                StringBuilder builder = new StringBuilder();
                for(Annotation annotation : domainObjectList.getAnnotations(object.getId())) {
                    if (builder.length()>0) builder.append(", ");
                    builder.append(annotation.getName());
                }
                return builder.toString();
            }
            else {
                DomainObjectAttribute attr = attributeMap.get(columnName);
                return attr.getGetter().invoke(object);
            }
        }
        catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.error("Error getting attribute value for column: "+columnName,e);
            return null;
        }
    }
    
    @Override
    public JPanel getViewerPanel() {
        return this;
    }
    
//    @Override
//    protected JPopupMenu getContextualPopupMenu() {
//        List<String> selectionIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
//        List<DomainObject> domainObjects = new ArrayList<>();
//        for (String entityId : selectionIds) {
//            Long id = new Long(entityId);
//            DomainObject imageObject = domainObjectList.getDomainObject(id);
//            if (imageObject == null) {
//                log.warn("Could not locate selected object with id {}", id);
//            }
//            else {
//                domainObjects.add(imageObject);
//            }
//        }
//        JPopupMenu popupMenu = new DomainObjectContextMenu(domainObjects);
//        ((DomainObjectContextMenu) popupMenu).addMenuItems();
//        return popupMenu;
//    }
    
}
