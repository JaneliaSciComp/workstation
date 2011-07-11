/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/15/11
 * Time: 12:32 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.keybind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OntologyElementAction;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.OntologyRoot;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;

/**
 * Maintains a set of key bindings for the user. Maps KeyboardShortcuts to Actions. Enforces a one-to-one mapping,
 * i.e. a single shortcut for each action, and a single action for each shortcut.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class KeyBindings {

	private static final String CATEGORY_KEYBINDS_GENERAL = "Keybind:General";
	private static final String CATEGORY_KEYBINDS_ONTOLOGY = "Keybind:Ontology:";

    private Map<KeyboardShortcut, Action>generalBindings;
    private Map<KeyboardShortcut, Action>ontologyBindings;

    public KeyBindings() {
        this.generalBindings = new HashMap<KeyboardShortcut, Action>();
        this.ontologyBindings = new HashMap<KeyboardShortcut, Action>();
    }
  
	public Action getConflict(KeyboardShortcut shortcut) {
        return ontologyBindings.get(shortcut);
    }
	
	private KeyboardShortcut getBinding(Map<KeyboardShortcut, Action>bindings, Action action) {
        for(Map.Entry<KeyboardShortcut, Action> entry : bindings.entrySet()) {
            if (action.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
	
	public KeyboardShortcut getBinding(Action action) {
		KeyboardShortcut ontologyAction = getBinding(ontologyBindings, action);
        if (ontologyAction != null) {
            return ontologyAction;
        }
        return getBinding(generalBindings, action);
    }

    private void setBinding(Map<KeyboardShortcut, Action>bindings, KeyboardShortcut shortcut, Action action) {
        // First remove all existing shortcuts for the action
        ArrayList<KeyboardShortcut> binds = new ArrayList<KeyboardShortcut>();
        for(Map.Entry<KeyboardShortcut, Action> entry : bindings.entrySet()) {
            if (action.equals(entry.getValue())) {
                binds.add(entry.getKey());
            }
        }
        for(KeyboardShortcut bind : binds) {
        	bindings.remove(bind);
        }
        // Now we can add the new shortcut
        bindings.put(shortcut, action);
    }

    public void setBinding(KeyboardShortcut shortcut, Action action) {
    	if (action instanceof OntologyElementAction) {
    		setBinding(ontologyBindings, shortcut, action);
    	}
    	else {
    		setBinding(generalBindings, shortcut, action);	
    	}
		
    }
    
    public void executeBinding(KeyboardShortcut shortcut) {
        Action action = ontologyBindings.get(shortcut);
        if (action == null) {
        	action = generalBindings.get(shortcut);
        }
        action.doAction();
    }
    
    public void loadGeneralKeybinds() {
    	// TODO: complete implementation of general keybinds
    	throw new UnsupportedOperationException();
    }
    
    /**
     * Load the key binding preferences for a given ontology. 
     * @param ontologyRoot
     */
    public void loadOntologyKeybinds(OntologyRoot root, Map<OntologyElement, Action> ontologyActionMap) {

    	System.out.println("Loading key bindings for ontology "+root.getName());
    	
    	ontologyBindings.clear();
    	
    	try {
    		String category = CATEGORY_KEYBINDS_ONTOLOGY+root.getId();
        	User user = EJBFactory.getRemoteComputeBean().getUserByName(System.getenv("USER"));
        	Map<String,UserPreference> prefs = user.getCategoryPreferences(category);
        	
        	for(UserPreference pref : prefs.values()) {
        		KeyboardShortcut shortcut = KeyboardShortcut.fromString(pref.getName());
        		
        		try {
            		long entityId = Long.parseLong(pref.getValue());
            		OntologyElement element = root.getElementById(entityId);
            		if (element == null) {
            			// This means that the element was deleted but the keybind remains. It's ok because 
            			// it will get deleted the next time the ontology is saved.
            		}
            		else {
            			Action action = ontologyActionMap.get(element);
            			if (action == null) {
            				System.out.println("Ontology does not have an action for element "+entityId);
            			}
            			else {
            				ontologyBindings.put(shortcut, action);	
            			}
            		}
        		}
        		catch (Exception e) {
        			System.out.println("Could not load key binding from user preference '"+pref.getValue()+"'.");
        			e.printStackTrace();
        		}
        	}
    	}
    	catch (Exception e) {
    		System.out.println("Could not load user's key binding preferences");
    		e.printStackTrace();
    	}
    }
    
    /**
     * Save the key binding preferences for a given ontology.
     */
    public void saveOntologyKeybinds(OntologyRoot root) {

    	System.out.println("Saving key bindings");
    	
    	try {
    		String category = CATEGORY_KEYBINDS_ONTOLOGY+root.getId();
        	User user = EJBFactory.getRemoteComputeBean().getUserByName(System.getenv("USER"));

        	// Delete all keybinds first, to maintain one key per entity
        	for(String key : user.getCategoryPreferences(category).keySet()) {
        		user.getPreferenceMap().remove(category+":"+key);
        	}
        	        	
            for(Map.Entry<KeyboardShortcut, Action> entry : ontologyBindings.entrySet()) {
                if (entry.getValue() instanceof OntologyElementAction) {
                	KeyboardShortcut shortcut = entry.getKey();
                	OntologyElementAction action = (OntologyElementAction)entry.getValue();
                	user.setPreference(new UserPreference(shortcut.toString(), category, action.getOntologyElement().getId().toString()));  
                }
            }

            EJBFactory.getRemoteComputeBean().genericSave(user);
    	}
    	catch (Exception e) {
    		System.out.println("Could not save user's key binding preferences");
    		e.printStackTrace();
    	}
    }
    
    /**
     * Remove all key binds referencing the given ontology.
     */
    public void removeOntologyKeybinds(OntologyRoot root) {

    	try {
            EJBFactory.getRemoteComputeBean().removePreferenceCategory(CATEGORY_KEYBINDS_ONTOLOGY+root.getId());
    	}
    	catch (Exception e) {
    		System.out.println("Could not delete key binding preferences for defunct ontology "+root.getName());
    		e.printStackTrace();
    	}
    }
}
