/* GoldenGATE Web Services, the web based text analysis engine for everyone.
 * Copyright (C) 2011-2013 ViBRANT (FP7/2007-2013, GA 261532), by G. Sautter
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package de.uka.ipd.idaho.goldenGate.webServices;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * Constant bearer for GoldenGATE web services.
 * 
 * @author sautter
 */
public interface GoldenGateWebServiceConstants {
	
	public static final Grammar xmlGrammar = new StandardGrammar();
	public static final Parser xmlParser = new Parser(xmlGrammar);
	
	/** the encoding to use for all interactions, namely UTF-8 */
	public static final String ENCODING = "UTF-8";
	
	
	/** interactivity descriptor for a web service, indicating required interactivity */
	public static final String INTERACTIVE_ALWAYS = "always";
	
	/** interactivity descriptor for a web service, indicating optional interactivity */
	public static final String INTERACTIVE_OPTIONAL = "optional";
	
	/** interactivity descriptor for a web service, indicating no possible interactivity */
	public static final String INTERACTIVE_NEVER = "never";
	
	
	/** interactivity setting for a web service invocation, indicating interactivity */
	public static final String INTERACTIVE_YES = "yes";
	
	/** interactivity setting for a web service invocation, indicating default interactivity, i.e., the default of the web service */
	public static final String INTERACTIVE_DEFAULT = "default";
	
	/** interactivity setting for a web service invocation, indicating no interactivity */
	public static final String INTERACTIVE_NO = "no";
	
	
	/** the XML node type for the root of a list of web service descriptions */
	public static final String SERVICES_NODE_TYPE = "services";
	
	/** the XML node type for the root of a web service description */
	public static final String SERVICE_NODE_TYPE = "service";
	
	/** the XML node type for a node describing an input format for a web service */
	public static final String INPUT_FORMAT_NODE_TYPE = "inputFormat";
	
	/** the XML node type for a node describing an output format of a web service */
	public static final String OUTPUT_FORMAT_NODE_TYPE = "outputFormat";
	
	/** the XML node type for a node wrapping the textual description of a web service */
	public static final String DESCRIPTION_NODE_TYPE = "description";
	
	
	/** the XML attribute holding the name of a web service or one of its input or output formats */
	public static final String NAME_ATTRIBUTE = "name";
	
	/** the XML attribute holding a short natural language label of a web service or one of its input or output formats */
	public static final String LABEL_ATTRIBUTE = "label";
	
	/** the XML attribute holding the interactivity of a web service, either of 'always', 'never', or 'optional' */
	public static final String INTERACTIVE_ATTRIBUTE = "interactive";
	
	/** the XML attribute holding the default interactivity of a web service, either of 'yes' or 'no' */
	public static final String DEFAULT_INTERACTIVE_ATTRIBUTE = "defaultInteractive";
	
	/** the XML attribute indicating that an input or output format is the default for a web service, its value is always 'true' */
	public static final String IS_DEFAULT_DATA_FORMAT_ATTRIBUTE = "default";
	
	
	/** the XML node type for a node wrapping the status of a web service invokation */
	public static final String STATUS_NODE_TYPE = "status";
	
	/** the XML node type for a node holding a specific callback in the status of a web service invokation */
	public static final String CALLBACK_NODE_TYPE = "callback";
	
	/** the XML attribute holding the document/invokation ID in the status of a web service invokation */
	public static final String DOCUMENT_ID_ATTRIBUTE = "docId";
	
	/** the XML attribute holding the state in the status of a web service invokation, its value is either of 'processing' or 'finished' */
	public static final String STATE_ATTRIBUTE = "state";
	
	/** the value to the state type attribute indicating that a web service invokation is still processing */
	public static final String PROCESSING_STATE = "processing";
	
	/** the value to the state type attribute indicating that a web service invokation is finished */
	public static final String FINISHED_STATE = "finished";
	
	/** the XML attribute holding the type of a callback in the status of a web service invokation */
	public static final String CALLBACK_TYPE_ATTRIBUTE = "type";
	
	/** the value to the callback type attribute indicating that the URL it contains returns the status of a web service invokation */
	public static final String STATUS_CALLBACK_TYPE = "status";
	
	/** the value to the callback type attribute indicating the that URL it contains returns the result of a web service invokation */
	public static final String RESULT_CALLBACK_TYPE = "result";
	
	/** the value to the callback type attribute indicating that the URL it contains returns the currently pending feedback request of a web service invokation */
	public static final String FEEDBACK_SHOW_CALLBACK_TYPE = "feedbackShow";
	
	/** the value to the callback type attribute indicating that the URL it contains skips the currently pending feedback request of a web service invokation, and any subsequent ones */
	public static final String FEEDBACK_SKIP_CALLBACK_TYPE = "feedbackSkip";
	
	
	/** the invokation path suffix causing the web service servlet to return XML formatted descriptions of the web services it provides, to use with HTTP GET */
	public static final String LIST_FUNCTIONS_COMMAND = "listFunctions";
	
	/** the invokation path suffix causing the web service servlet to return XML formatted descriptions of the web service invokations of a given client (user or other) that follows in the invokation path right after this suffix, separated with a slash, to use with HTTP GET */
	public static final String LIST_REQUESTS_COMMAND = "listRequests";
	
	/** the invokation path suffix for invoking a web service, to be used with HTTP POST */
	public static final String INVOKE_FUNCTION_COMMAND = "invokeFunction";
	
	/** the invokation path suffix for interacting with the web service servlet on a running invokation, to be suffixed by the invokation ID and the desired action; if action is omitted, it defaults to 'status', to be used with HTTP GET */
	public static final String CALLBACK_COMMAND = "callback";
	
	/** the invokation path suffix for submitting the answer to a feedback request, to be used with HTTP POST */
	public static final String ANSWER_FEEDBACK_REQUEST_COMMAND = "answerFeedbackRequest";
	
	/** the suffix for the callback for obtaining the status of an invokation */
	public static final String STATUS_ACTION = "status";
	
	/** the suffix for the callback for obtaining the result of an invokation, soon as it becomes available */
	public static final String RESULT_ACTION = "result";
	
	/** the suffix for the callback for obtaining the currently pending feedback request of an invokation, if any */
	public static final String SHOW_FEEDBACK_REQUEST_ACTION = "showFeedbackRequest";
	
	/** the suffix for the callback for skipping the currently pending feedback request of an invokation, if any */
	public static final String SKIP_FEEDBACK_REQUEST_ACTION = "skipFeedbackRequest";
	
	/** the HTTP parameter specifying the web service to invoke in a respective call */
	public static final String FUNCTION_NAME_PARAMETER = "functionName";
	
	/** the HTTP parameter specifying the identifier of the client invoking a web service, e.g. a user name */
	public static final String CLIENT_ID_PARAMETER = "clientId";
	
	/** the HTTP parameter specifying the web service to invoke in a respective call */
	public static final String INTERACTIVE_PARAMETER = "INTERACTIVE"; // copy from GoldenGATE Resource interface, to prevent dependencies
	
	/** the HTTP parameter specifying the data format to use in the invokation of a web service and in obtaining an invokation result; defaults to the web service default settings if omitted */
	public static final String DATA_FORMAT_PARAMETER = "dataFormat";
	
	/** the HTTP parameter specifying the URL to obtain the to-process data from in the invokation of a web service; the data string is used if omitted */
	public static final String DATA_URL_PARAMETER = "dataUrl";
	
	/** the HTTP parameter specifying the data to process in the invokation of a web service; the data URL takes precedence if specified */
	public static final String DATA_STRING_PARAMETER = "data";
}