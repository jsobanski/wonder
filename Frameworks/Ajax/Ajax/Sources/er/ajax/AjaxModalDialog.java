package er.ajax;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.appserver.ERXWOContext;
import er.extensions.components._private.ERXWOForm;

/**
 * <p>AjaxModalDialog is a modal dialog window based on ModalBox (see below for link).  It differs from AjaxModalContainer
 * in that it handles submitting forms and updating the container contents.  It also looks more like an OS X modal
 * dialog if you consider that to be a benefit.</p>
 * 
 * <p>The AjaxModalDialog is not rendered where it is located in your page.  Because of this, it should not be physically 
 * nested in a form if it uses form input (needs a form), as it will be  rendered outside of the form.  If you want to have
 * such a dialog, place the AjaxModalDialog outside of the form and use an AjaxModalDialogOpener in the form.</p>
 * 
 * <p>The links shown to open the dialog can come from two sources:
 * <ul>
 * <li>the label binding</li>
 * <li>an in-line ERXWOTemplate named "link".  This allows for images etc to be used to open the dialog</li>
 * </ul></p>
 *
 * <p>The contents for the modal dialog can come from three sources:
 * <ul>
 * <li>in-line, between the open in close tags</li>
 * <li>externally (i.e. from another template), from an in-line WOComponent</li>
 * <li>externally (i.e. from another template), from an action method</li>
 * </ul></p>
 * 
 * <p>To cause the dialog to be closed in an Ajax action method, use this:
 * <code>
 * AjaxModalDialog.close(context());
 * </code>
 * </p>
 *  
 * <p>To cause the contents of the dialog to be updated in an Ajax action method, use this:
 * <code>
 * AjaxModalDialog.update(context());
 * </code>
 * </p>
 * 
 * <p>The modal dialog is opened by calling a JavaScript function.  While this is normally done from an onclick
 * handler, you can call it directly.  The function name is openAMD_<ID>():
 * <code>
 * openAMD_MyDialogId();
 * </code>
 * </p>
 * 
 * @binding action returns the contents of the dialog box
 * @binding label the text for the link that opens the dialog box
 * @binding title Title to be displayed in the ModalBox window header, also used as title attribute of link opening dialog
 * @binding linkTitle Title to be used as title attribute of link opening dialog, title is used if this is not present
 *
 * @binding width integer Width in pixels, use -1 for auto-width
 * @binding height integer Height in pixels, use -1 for auto-height. When set Modalbox will operate in 'fixed-height' mode. 
 * 
 * @binding open if true, the container is rendered already opened, the default is false
 * @binding showOpener if false, no HTML is generated for the link, button etc. to open this dialog, it can only be opened from
 * 			custom  JavaScript (see below).  The default is true
 * @binding enabled if false, nothing is rendered for this component.  This can be used instead of wrapping this in a WOConditional.
 *          The default is true.
 *
 * @binding onOpen server side method that runs before the dialog is opened, the return value is discarded
 * @binding onClose server side method that runs before the dialog is closed, the return value is discarded.
 *                  This will be executed if the page is reloaded, but not if the user navigates elsewhere.
 * @binding closeUpdateContainerID the update container to refresh when onClose is called
 * 
 * @binding id HTML id for the link activating the modal dialog
 * @binding class CSS class for the link activating the modal dialog
 * @binding style CSS style for the link activating the modal dialog
 *
 * @binding overlayClose true | false Close modal box by clicking on overlay. Default is true.
 * @binding method get | post. Method of passing variables to a server. Default is 'get'.
 * @binding params {} Collection of parameters to pass on AJAX request. Should be URL-encoded. See PassingFormValues for details.
 * 
 * @binding loadingString string The message to show during loading. Default is "Please wait. Loading...".
 * @binding closeString Defines title attribute for close window link. Default is "Close window".
 * @binding closeValue Defines the string for close link in the header. Default is '&times;'
 * 
 * @binding overlayOpacity Overlay opacity. Must be between 0-1. Default is .65.
 * @binding overlayDuration Overlay fade in/out duration in seconds.
 * @binding slideDownDuration Modalbox appear slide down effect in seconds.
 * @binding slideUpDuration Modalbox hiding slide up effect in seconds.
 * @binding resizeDuration Modalbox resize duration in seconds.
 * @binding inactiveFade true | false, Toggles Modalbox window fade on inactive state.
 * @binding transitions true | false, Toggles transition effects. Transitions are enabled by default.
 * @binding autoFocusing true | false, Toggles auto-focusing for form elements. Disable it for long text pages.  Add the class MB_notFocusable to
 *          any inputs you want excluded from focusing.
 *
 * @binding beforeLoad client side method, fires right before loading contents into the ModalBox. If the callback function returns false, content loading will skipped. This can be used for redirecting user to another MB-page for authorization purposes for example.
 * @binding afterLoad client side method, fires after loading content into the ModalBox (i.e. after showing or updating existing window).
 * @binding beforeHide client side method, fires right before removing elements from the DOM. Might be useful to get form values before hiding modalbox.
 * @binding afterHide client side method, fires after hiding ModalBox from the screen.
 * @binding afterResize client side method, fires after calling resize method.
 * @binding onShow client side method, fires on first appearing of ModalBox before the contents are being loaded.
 * @binding onUpdate client side method, fires on updating the content of ModalBox (on call of Modalbox.show method from active ModalBox instance).
 * 
 * @see AjaxModalDialogOpener
 * @see <a href="http://www.wildbit.com/labs/modalbox"/>Modalbox Page</a>
 * @see <a href="http://code.google.com/p/modalbox/">Google Group</a>
 * @author chill
 * 
 * TODO handle href to static content
 * TODO make dialog draggable
 * TODO lock dialog open unless closed by content
 * TODO add transitioning to other contents without closing dialog
 */
public class AjaxModalDialog extends AjaxComponent {

	/** JavaScript to execute on the client to close the modal dialog */
	public static final String Close = "AMD.close();";

	private boolean _open;
	private WOComponent _actionResults;
	private AjaxModalDialog outerDialog;
	private boolean hasWarnedOnNesting = false;
	private WOComponent previousComponent;
	
	public static final Logger logger = Logger.getLogger(AjaxModalDialog.class);
	

	public AjaxModalDialog(WOContext context) {
		super(context);
	}

	public boolean synchronizesVariablesWithBindings() {
		return false;
	}

	public boolean isOpen() {
		return _open;
	}

	public void setOpen(boolean open) {
		_open = open;
	}
	
	/**
	 * Call this method to have a JavaScript response returned that opens the modal dialog.
	 * The title of the dialog will be what it was when rendered.
	 *
	 * @param context the current WOContext
	 * @param id the HTML ID of the AjaxModalDialog to open
	 */
	public static void open(WOContext context, String id) {
		AjaxUtils.javascriptResponse(openDialogFunctionName(id) + "();", context);
	}
	
	/**
	 * Call this method to have a JavaScript response returned that opens the modal dialog.
	 * The title of the dialog will be the passed title.  This is useful if the script to
	 * open this dialog was rendered without the title or with an incorrect title.
	 *
	 * @param context the current WOContext
	 * @param id the HTML ID of the AjaxModalDialog to open
	 * @param title the title for the AjaxModalDialog
	 */
	public static void open(WOContext context, String id, String title) {
		AjaxUtils.javascriptResponse(openDialogFunctionName(id) + "(" + AjaxValue.javaScriptEscaped(title) + ");", context);
	}
	
	/**
	 * Returns the JavaScript function name for the function to open the AjaxModalDialog with
	 * the specified ID.
	 *
	 * @param id the HTML ID of the AjaxModalDialog to open
	 * @return JavaScript function name for the function to open the AjaxModalDialog
	 */
	public static String openDialogFunctionName(String id) {
		return "openAMD_" + id;
	}
	
	/**
	 * Call this method to have a JavaScript response returned that closes the modal dialog.
	 *
	 * @param context the current WOContext
	 */
	public static void close(WOContext context) {
		AjaxUtils.javascriptResponse(AjaxModalDialog.Close, context);
	}

	/**
	 * Call this method to have a JavaScript response returned that updates the contents of the modal dialog.
	 * Note that this does not work with the action binding.  You need to manage your own AjaxUpdateContainer
	 * if you use an action method for the contents of the dialog.
	 *
	 * @param context the current WOContext
	 */
	public static void update(WOContext context) {
		AjaxUtils.javascriptResponse(currentDialog(context).updateContainerID() + "Update();", context);
	}

	/**
	 * Call this method to have a JavaScript response returned that updates the title of the modal dialog.
	 *
	 * @param context the current WOContext
	 * @param title the new title for the dialog window
	 */
	public static void setTitle(WOContext context, String title) {
		AjaxUtils.javascriptResponse("$wi('MB_caption').innerHTML=" + AjaxValue.javaScriptEscaped(title) + ";", context);
	}

	/**
	 * @param context the current WOContext
	 * @return the AjaxModalDialog currently being processed
	 * @throws RuntimeException if no AjaxModalDialog is currently being processed
	 */
	public static AjaxModalDialog currentDialog(WOContext context) {
		AjaxModalDialog currentDialog = (AjaxModalDialog) ERXWOContext.contextDictionary().objectForKey(AjaxModalDialog.class.getName());
		if (currentDialog == null) {
			throw new RuntimeException("Attempted to get current AjaxModalDialog when none active.  Check your page structure.");
		}
		return currentDialog;
	}

	/**
	 * Start of R-R loop.  awakes the components from action if action is bound.
	 *
	 * @see com.webobjects.appserver.WOComponent#awake()
	 */
	public void awake() {
		super.awake();
		if (_actionResults != null) {
			_actionResults._awakeInContext(context());
		}
	}

	/**
	 * Only handle this phase if the modal box is open.  Also includes result returned by action binding if bound.
	 *
	 * @see com.webobjects.appserver.WOComponent#takeValuesFromRequest(com.webobjects.appserver.WORequest, com.webobjects.appserver.WOContext)
	 */
	public void takeValuesFromRequest(WORequest request, WOContext context) {
		if (isOpen()) {
			try {
				pushDialog();
				if (_actionResults != null) {
					pushActionResultsIntoContext(context);
					try {
						_actionResults.takeValuesFromRequest(request, context);
					}
					finally {
						popActionResultsFromContext(context);
					}
				}
				else {
					super.takeValuesFromRequest(request, context);
				}
			}
			finally {
				popDialog();
			}

		}
	}

	/**
	 * Only handle this phase if the modal box is open or it is our action (opening the box).  
	 * Overridden to include result returned by action binding if bound.
	 *
	 * @see #close(WOContext)
	 * @see #update(WOContext)
	 * @see com.webobjects.appserver.WOComponent#takeValuesFromRequest(com.webobjects.appserver.WORequest, com.webobjects.appserver.WOContext)
	 */
	public WOActionResults invokeAction(WORequest request, WOContext context) {
		pushDialog();		
		try {
			WOActionResults result = null;
			if (AjaxUtils.shouldHandleRequest(request, context, _containerID(context))) {
				result = super.invokeAction(request, context);
			}
			else if (isOpen()) {
				if (_actionResults != null) {
					pushActionResultsIntoContext(context);
					try {
						result = _actionResults.invokeAction(request, context);
					}
					finally {
						popActionResultsFromContext(context);
					}
				}
				else {
					result = super.invokeAction(request, context);
				}
			}

			return result;
		}
		finally {
			popDialog();
		}
	}

	/**
	 * Handles the open and close dialog actions.
	 *
	 * @see er.ajax.AjaxComponent#handleRequest(com.webobjects.appserver.WORequest, com.webobjects.appserver.WOContext)
	 *
	 * @return null or dialog contents
	 */
	public WOActionResults handleRequest(WORequest request, WOContext context) {
		WOActionResults response = null;
		String modalBoxAction = request.stringFormValueForKey("modalBoxAction");

		if ("close".equals(modalBoxAction) && isOpen()) {
			closeDialog();
			String closeUpdateContainerID = AjaxUpdateContainer.updateContainerID((String) valueForBinding("closeUpdateContainerID"));
			if (closeUpdateContainerID != null) {
				AjaxUpdateContainer.setUpdateContainerID(request, closeUpdateContainerID);
			}
		}
		else if ("open".equals(modalBoxAction) && !isOpen()) {
			openDialog();

			// If there is an action binding, we need to cache the result of calling that so that
			// the awake, takeValues, etc. messages can get passed onto it
			if (hasBinding("action")) {
				_actionResults = (WOComponent) valueForBinding("action");
				_actionResults._awakeInContext(context);
			}
		}

		if (isOpen()) {
			response = AjaxUtils.createResponse(request, context);
			
			// Register the id of this component on the page in the request so that when 
			// it comes time to cache the context, it knows that this area is an Ajax updating area
			AjaxUtils.setPageReplacementCacheKey(context, _containerID(context));

			appendOpenAjaxUpdateDiv((WOResponse) response, context);
			if (_actionResults != null) {
				pushActionResultsIntoContext(context);
				try {
					_actionResults.appendToResponse((WOResponse) response, context);
				}
				finally {
					popActionResultsFromContext(context);
				}
			}
			else {
				// This loads the content from the default ERWOTemplate (our component contents that are not
				// in the "link" template.
				super.appendToResponse((WOResponse) response, context);
			}
			appendCloseAjaxUpdateDiv((WOResponse) response, context);
		}

		return response;
	}

	/**
	 * This has two modes.  One is to generate the link that opens the dialog.  The other is to return the contents
	 * of the dialog (the result returned by action binding is handled in handleRequest, not here).
	 *
	 * @see er.ajax.AjaxComponent#appendToResponse(com.webobjects.appserver.WOResponse, com.webobjects.appserver.WOContext)
	 */
	public void appendToResponse(WOResponse response, WOContext context) {
		if (context.isInForm()) {
			logger.warn("The AjaxModalDialog should not be used inside of a WOForm (" + ERXWOForm.formName(context, "- not specified -") +
					") if it contains any form inputs or buttons.  Remove this AMD from this form, add a form of its own. Replace it with " +
					"an AjaxModalDialogOpener with a dialogID that matches the ID of this dialog.");
					log.warn("    page: " + context.page());
					log.warn("    component: " + context.component());
		}
		
		if( ! booleanValueForBinding("enabled", true)) {
			return;
		}

		// If this is not an Ajax request, the page has been reloaded.  Try to recover state
		if (isOpen() && ! AjaxRequestHandler.AjaxRequestHandlerKey.equals(context().request().requestHandlerKey())) {
			closeDialog();
		}

		if (isOpen()) {
			if (_actionResults != null) {
				throw new RuntimeException("Unexpected call to appendToResponse");
			}
			try {
				pushDialog();
				super.appendToResponse(response, context);
			}
			finally {
				popDialog();
			}

		}
		else {
			boolean showOpener = booleanValueForBinding("showOpener", true);
			
			if (showOpener) {
				response.appendContentString("<a href=\"javascript:void(0)\"");
				appendTagAttributeToResponse(response, "id", id());
				appendTagAttributeToResponse(response, "class", valueForBinding("class", null));
				appendTagAttributeToResponse(response, "style", valueForBinding("style", null));
				if (hasBinding("linkTitle")) {
					appendTagAttributeToResponse(response, "title", valueForBinding("linkTitle", null));
				} else {
					appendTagAttributeToResponse(response, "title", valueForBinding("title", null));
				}
				
				// onclick calls the script below
				response.appendContentString(" onclick=\"");
				response.appendContentString(openDialogFunctionName(id()));
				response.appendContentString("(); return false;\" >");	
				
				if (hasBinding("label")) {
					response.appendContentString((String) valueForBinding("label"));
				} else {
					// This will append the contents of the ERXWOTemplate named "link"
					super.appendToResponse(response, context);
				}
				response.appendContentString("</a>");
			}
			
			// This script can also be called directly by other code to show the modal dialog
			response.appendContentString("<script>\n");
			response.appendContentString(openDialogFunctionName(id()));
			response.appendContentString(" = function(titleBarText) {\n");
			appendOpenModalDialogFunction(response, context);
			response.appendContentString("}\n");
			
			// Auto-open
			if (booleanValueForBinding("open", false)) {
				response.appendContentString(openDialogFunctionName(id()));
				response.appendContentString("();\n");
			}
			response.appendContentString("</script>");
			
			// normally this would be done in super, but we're not always calling super here
			addRequiredWebResources(response);
		}
	}

	/**
	 * Appends function body to open the modal dialog window.
	 * 
	 * @see AjaxModalDialog#openDialogFunctionName(String)
	 * 
	 * @param response WOResponse to append to
	 * @param context WOContext of response
	 */
	protected void appendOpenModalDialogFunction(WOResponse response, WOContext context) {
		response.appendContentString("    options = ");
		AjaxOptions.appendToResponse(createModalBoxOptions(), response, context);
		response.appendContentString(";\n");
		response.appendContentString("    if (titleBarText) options.title = titleBarText;\n");
		response.appendContentString("    Modalbox.show('");
		response.appendContentString(AjaxUtils.ajaxComponentActionUrl(context));
		response.appendContentString("?modalBoxAction=open', options);\n");
	}
	
	/**
	 * Appends opening div that will function as an AjaxUpdateContainer.
	 * 
	 * @param response WOResponse to append to
	 * @param context WOContext of response
	 */
	protected void appendOpenAjaxUpdateDiv(WOResponse response, WOContext context) {
		System.out.println("PUTPUT");
		response.appendContentString("<div id=\"");
		response.appendContentString(updateContainerID());
		response.appendContentString("\" updateUrl=\"");
		response.appendContentString(AjaxUtils.ajaxComponentActionUrl(context));
		response.appendContentString("\">");
	}

	/**
	 * Appends closing div that will function as an AjaxUpdateContainer and the JavaScript to register it.
	 * 
	 * @param response WOResponse to append to
	 * @param context WOContext of response
	 */
	protected void appendCloseAjaxUpdateDiv(WOResponse response, WOContext context) {
		response.appendContentString("</div>");
		response.appendContentString("<script>AUC.register('");
		response.appendContentString(updateContainerID());
		response.appendContentString("', {onComplete:function() { Modalbox.resizeToContent(); }});</script>");
	}

	/**
	 * End of R-R loop.  Puts the components from action to sleep if action is bound.
	 *
	 * @see com.webobjects.appserver.WOComponent#sleep()
	 */
	public void sleep() {
		super.sleep();
		if (_actionResults != null) {
			_actionResults._sleepInContext(context());
		}
	}

	/**
	 * Calls the method bound to onOpen (if any), and marks the dialog state as open.
	 */
	public void openDialog() {
		if (hasBinding("onOpen")) {
			valueForBinding("onOpen");
		}

		setOpen(true);
	}

	/**
	 * Calls the method bound to onClose (if any), and marks the dialog state as closed.  This can get called if the page
	 * gets reloaded.  Be careful modifying the response if 
	 * <code>! AjaxRequestHandler.AjaxRequestHandlerKey.equals(context().request().requestHandlerKey())</code>
	 */
	public void closeDialog() {
		if (hasBinding("onClose")) {
			valueForBinding("onClose");
		}

		setOpen(false);
		_actionResults = null;
	}

	/**
	 * @see er.ajax.AjaxComponent#_containerID(com.webobjects.appserver.WOContext)
	 *
	 * @return id()
	 */
	protected String _containerID(WOContext context) {
		return id();
	}

	/**
	 * @return the value bound to id or an manufactured string if id is not bound
	 */
	public String id() {
		return hasBinding("id") ? (String) valueForBinding("id") : ERXWOContext.safeIdentifierName(context(), false);
	}

	/**
	 * Returns the ID of the AjaxUpdateContainer that wraps the in-line contents of this dialog.
	 * 
	 * @return  id() + "Updater"
	 */
	public String updateContainerID() {
		return id() + "Updater";
	}
	
	/**
	 * Returns the template name for the ERXWOComponentContent: null to show the dialog (default) contents
	 * and "link" to show the link contents
	 * 
	 * @return null or "link"
	 */
	public String templateName() {
		return !isOpen() && !hasBinding("label") ? "link" : null;
	}

	/**
	 * @return binding values converted into Ajax options for ModalBox
	 */
	protected NSMutableDictionary createModalBoxOptions() {
		NSMutableArray ajaxOptionsArray = new NSMutableArray();
		ajaxOptionsArray.addObject(new AjaxOption("title", AjaxOption.STRING));
		ajaxOptionsArray.addObject(new AjaxOption("width", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("overlayClose", AjaxOption.BOOLEAN));
		ajaxOptionsArray.addObject(new AjaxOption("height", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("method", AjaxOption.STRING));
		ajaxOptionsArray.addObject(new AjaxOption("params", AjaxOption.DICTIONARY));
		ajaxOptionsArray.addObject(new AjaxOption("loadingString", AjaxOption.STRING));
		ajaxOptionsArray.addObject(new AjaxOption("closeString", AjaxOption.STRING));
		ajaxOptionsArray.addObject(new AjaxOption("closeValue", AjaxOption.STRING));
		ajaxOptionsArray.addObject(new AjaxOption("overlayOpacity", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("overlayDuration", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("slideDownDuration", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("slideUpDuration", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("resizeDuration", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("inactiveFade", AjaxOption.BOOLEAN));
		ajaxOptionsArray.addObject(new AjaxOption("transitions", AjaxOption.BOOLEAN));
		ajaxOptionsArray.addObject(new AjaxOption("autoFocusing", AjaxOption.BOOLEAN));

		// IMPORTANT NOTICE. Each callback gets removed from options of the ModalBox after execution
		ajaxOptionsArray.addObject(new AjaxOption("beforeLoad", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("afterLoad", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("beforeHide", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("afterResize", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("onShow", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("onUpdate", AjaxOption.SCRIPT));

		// JS to notify server when the dialog box is closed.  This needs to be added to anything
		// bound to afterHide
		String closeUpdateContainerID = AjaxUpdateContainer.updateContainerID((String) valueForBinding("closeUpdateContainerID"));
		String serverUpdate;
		if (closeUpdateContainerID == null) {
			serverUpdate = " AUL.request('" + AjaxUtils.ajaxComponentActionUrl(context()) + "', null, null, 'modalBoxAction=close');";
		}
		else {
			serverUpdate = " AUL._update('" + closeUpdateContainerID + "', '" + AjaxUtils.ajaxComponentActionUrl(context()) + "', null, null, 'modalBoxAction=close');";
		}

		if (hasBinding("afterHide")) {
			String afterHide = (String) valueForBinding("afterHide");
			int openingBraceIndex = afterHide.indexOf('{');
			if (openingBraceIndex > -1) {
				serverUpdate = "function() {" + serverUpdate + " " + afterHide.substring(openingBraceIndex + 1);
			}
			else
				throw new RuntimeException("Don't know how to handle afterHide value '" + afterHide + "', did you forget to wrap it in function() { ...}?");
		}
		else {
			serverUpdate = "function(v) { " + serverUpdate + '}';
		}
		ajaxOptionsArray.addObject(new AjaxOption("afterHide", serverUpdate, AjaxOption.SCRIPT));

		NSMutableDictionary options = AjaxOption.createAjaxOptionsDictionary(ajaxOptionsArray, this);

		return options;
	}

	/**
	 * @see er.ajax.AjaxComponent#addRequiredWebResources(com.webobjects.appserver.WOResponse)
	 */
	protected void addRequiredWebResources(WOResponse response) {
		addScriptResourceInHead(response, "prototype.js");
		addScriptResourceInHead(response, "wonder.js");
		addScriptResourceInHead(response, "effects.js");
		addScriptResourceInHead(response, "modalbox.js");
		addStylesheetResourceInHead(response, "modalbox.css");
	}
	
	/**
	 * Stash this dialog instance in the context so we can access it from the static methods.  If there is one AMD 
	 * next in another (a rather dubious thing to do that we warn about but it may have its uses), we need to remember 
	 * the outer one while processing this inner one
	 * @see AjaxModalDialog#popDialog()
	 */
	protected void pushDialog() {
		// 
		// 
		outerDialog = (AjaxModalDialog) ERXWOContext.contextDictionary().objectForKey(AjaxModalDialog.class.getName());
		ERXWOContext.contextDictionary().setObjectForKey(this, AjaxModalDialog.class.getName());
		if ( ! hasWarnedOnNesting && outerDialog != null) {
			hasWarnedOnNesting = true;
			logger.warn("AjaxModalDialog " + id() + " is nested inside of " + outerDialog.id() + ". Are you sure you want to do this?");
		}		

	}
	
	/**
	 * Remove this dialog instance from the context, replacing the previous one if any.
	 * @see #pushDialog()
	 */
	protected void popDialog() {
		if (outerDialog != null) {
			ERXWOContext.contextDictionary().setObjectForKey(outerDialog, AjaxModalDialog.class.getName());
		}
		else {
			ERXWOContext.contextDictionary().removeObjectForKey(AjaxModalDialog.class.getName());
		}
	}
	
	/**
	 * Make _actionResults (result of the action binding) the current component in context for WO processing.
	 * Remembers the current component so that it can be restored.
	 * @param context WOContext to push _cationResults into
	 * @see #popActionResultsFromContext(WOContext)
	 */
	protected void pushActionResultsIntoContext(WOContext context) {
		previousComponent = context.component();
		context._setCurrentComponent(_actionResults);
	}
	
	/**
	 * Sets the current component in context to the one there before pushActionResultsIntoContext
	 * was called.
	 * @param context WOContext to restore previous component in
	 * @see #pushActionResultsIntoContext(WOContext)
	 */
	protected void popActionResultsFromContext(WOContext context) {
		context._setCurrentComponent(previousComponent);
	}
		
	
}