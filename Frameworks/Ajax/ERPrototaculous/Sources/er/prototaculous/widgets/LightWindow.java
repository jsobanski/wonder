package er.prototaculous.widgets;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;

import er.extensions.appserver.ERXResponseRewriter;
import er.extensions.foundation.ERXProperties;

/**
 * Abstract superclass that encapsulates http://www.stickmanlabs.com/lightwindow 2.0
 * 
 * @property er.prototaculous.useUnobtrusively For Unobtrusive JavaScript programming
 * @property er.prototaculous.widgets.LightWindow.type
 * 
 * @author mendis
 */
public abstract class LightWindow extends WOComponent {
	private static boolean useUnobtrusively = ERXProperties.booleanForKeyWithDefault("er.prototaculous.useUnobtrusively", true);

	public LightWindow(WOContext context) {
		super(context);
	}

	public final static String type = ERXProperties.stringForKeyWithDefault("er.prototaculous.widgets.LightWindow.type", "external");		// Default is to treat all Window content as external

    /*
     * API or bindings common to light window subcomponents
     */
    public static interface Bindings {
    	public static final String directActionName = "directActionName";
    	public static final String action = "action";
    	public static final String queryDictionary = "queryDictionary";
    	public static final String formID = "formID";
    	public static final String type = "type";
    	public static final String height = "height";
    	public static final String width = "width";
    	public static final String top = "top";
    	public static final String left = "left";
		public static final String title = "title";
    }
    
    // accessors
    public String formID() {
    	return (String) valueForBinding(Bindings.formID);
    }
    
    // R/R
    @Override
	public void appendToResponse(WOResponse response, WOContext context) {
    	super.appendToResponse(response, context);
    	if (!useUnobtrusively) {
    		ERXResponseRewriter.addScriptResourceInHead(response, context, "Ajax", "prototype.js");
    		ERXResponseRewriter.addScriptResourceInHead(response, context, "Ajax", "scriptaculous.js");
    		ERXResponseRewriter.addScriptResourceInHead(response, context, "Ajax", "effects.js");
    		ERXResponseRewriter.addScriptResourceInHead(response, context, "ERPrototaculous", "lightwindow.js");
    		ERXResponseRewriter.addStylesheetResourceInHead(response, context, "ERPrototaculous", "lightwindow.css");
    	}
    }
}
