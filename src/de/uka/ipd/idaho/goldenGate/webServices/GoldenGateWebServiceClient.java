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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;

/**
 * Client object for GoldenGATE web service servlet.
 * 
 * @author sautter
 */
public class GoldenGateWebServiceClient implements GoldenGateWebServiceConstants {
	
	/**
	 * Descriptor object for a GoldenGATE web service
	 * 
	 * @author sautter
	 */
	public static class WebServiceDescriptor {
		
		private GoldenGateWebServiceClient wsc;
		
		/** the name of the web service, to be used for invokation */
		public final String name;
		
		/** the natural language label of the web service */
		public final String label;
		
		String description;
		
		String interactivity;
		boolean defaultInteractive = false;
		
		DataFormatDescriptor defaultInputFormat;
		ArrayList inputFormats = new ArrayList();
		DataFormatDescriptor defaultOutputFormat;
		ArrayList outputFormats = new ArrayList();
		
		WebServiceDescriptor(String name, String label, GoldenGateWebServiceClient wsc) {
			this.name = name;
			this.label = label;
			this.wsc = wsc;
		}
		
		/**
		 * @return a natural language description of the web service, may
		 *         contain HTML tags
		 */
		public String getDescription() {
			return this.description;
		}
		
		/**
		 * @return the interactivity of the web service, either of 'always',
		 *         'optional', or 'never'
		 */
		public String getInteractivity() {
			return this.interactivity;
		}
		
		/**
		 * @return true if the web service is interactive by default
		 */
		public boolean isDefaultInteractive() {
			return this.defaultInteractive;
		}
		
		/**
		 * @return the descriptor of the default input format of the web service
		 */
		public DataFormatDescriptor getDefaultInputFormat() {
			return this.defaultInputFormat;
		}
		
		/**
		 * @return the descriptors of the input formats available for the web service
		 */
		public DataFormatDescriptor[] getInputFormats() {
			return ((DataFormatDescriptor[]) this.inputFormats.toArray(new DataFormatDescriptor[this.inputFormats.size()]));
		}
		
		/**
		 * @return the the descriptor of the default output format of the web service
		 */
		public DataFormatDescriptor getDefaultOutputFormat() {
			return this.defaultOutputFormat;
		}
		
		/**
		 * @return the the descriptors of the output formats available for the web service
		 */
		public DataFormatDescriptor[] getOutputFormats() {
			return ((DataFormatDescriptor[]) this.outputFormats.toArray(new DataFormatDescriptor[this.outputFormats.size()]));
		}
		
		
		/**
		 * Invoke the web service, using its default input and output formats.
		 * @param interactivity the interactivity of the invokation, either of
		 *            'yes', 'no', or 'default', with null mapping to 'default'
		 * @param data the data to process
		 * @param dataIsUrl is the data String literal data, or an access or
		 *            callback URL for the data?
		 * @return an invokation object for further interaction
		 * @throws IOException
		 */
		public WebServiceInvokation invoke(String data, boolean dataIsUrl) throws IOException {
			return this.invoke(null, data, dataIsUrl, null, null);
		}
		
		/**
		 * Invoke the web service, using its default output format.
		 * @param interactivity the interactivity of the invokation, either of
		 *            'yes', 'no', or 'default', with null mapping to 'default'
		 * @param data the data to process, or the access/callback URL to load the
		 *            data from
		 * @param dataIsUrl is the data String literal data, or an access or
		 *            callback URL for the data?
		 * @param inputFormat the name of the input format to use, null uses the
		 *            default input format
		 * @return an invokation object for further interaction
		 * @throws IOException
		 */
		public WebServiceInvokation invoke(String data, boolean dataIsUrl, String inputFormat) throws IOException {
			return this.invoke(null, data, dataIsUrl, inputFormat, null);
		}
		
		/**
		 * Invoke the web service, using its default input and output formats.
		 * @param interactivity the interactivity of the invokation, either of
		 *            'yes', 'no', or 'default', with null mapping to 'default'
		 * @param data the data to process, or the access/callback URL to load the
		 *            data from
		 * @param dataIsUrl is the data String literal data, or an access or
		 *            callback URL for the data?
		 * @param inputFormat the name of the input format to use, null uses the
		 *            default input format
		 * @param outputFormat the name of the output format to use, null uses the
		 *            default output format
		 * @return an invokation object for further interaction
		 * @throws IOException
		 */
		public WebServiceInvokation invoke(String data, boolean dataIsUrl, String inputFormat, String outputFormat) throws IOException {
			return this.invoke(null, data, dataIsUrl, inputFormat, outputFormat);
		}
		
		/**
		 * Invoke the web service, using its default input and output formats.
		 * @param interactivity the interactivity of the invokation, either of
		 *            'yes', 'no', or 'default', with null mapping to 'default'
		 * @param data the data to process
		 * @param dataIsUrl is the data String literal data, or an access or
		 *            callback URL for the data?
		 * @return an invokation object for further interaction
		 * @throws IOException
		 */
		public WebServiceInvokation invoke(String interactivity, String data, boolean dataIsUrl) throws IOException {
			return this.invoke(interactivity, data, dataIsUrl, null, null);
		}
		
		/**
		 * Invoke the web service, using its default output format.
		 * @param interactivity the interactivity of the invokation, either of
		 *            'yes', 'no', or 'default', with null mapping to 'default'
		 * @param data the data to process, or the access/callback URL to load the
		 *            data from
		 * @param dataIsUrl is the data String literal data, or an access or
		 *            callback URL for the data?
		 * @param inputFormat the name of the input format to use, null uses the
		 *            default input format
		 * @return an invokation object for further interaction
		 * @throws IOException
		 */
		public WebServiceInvokation invoke(String interactivity, String data, boolean dataIsUrl, String inputFormat) throws IOException {
			return this.invoke(interactivity, data, dataIsUrl, inputFormat, null);
		}
		
		/**
		 * Invoke the web service, using its default input and output formats.
		 * @param interactivity the interactivity of the invokation, either of
		 *            'yes', 'no', or 'default', with null mapping to 'default'
		 * @param data the data to process, or the access/callback URL to load the
		 *            data from
		 * @param dataIsUrl is the data String literal data, or an access or
		 *            callback URL for the data?
		 * @param inputFormat the name of the input format to use, null uses the
		 *            default input format
		 * @param outputFormat the name of the output format to use, null uses the
		 *            default output format
		 * @return an invokation object for further interaction
		 * @throws IOException
		 */
		public WebServiceInvokation invoke(String interactivity, String data, boolean dataIsUrl, String inputFormat, String outputFormat) throws IOException {
			return this.wsc.invokeFunction(this.name, interactivity, data, dataIsUrl, inputFormat, outputFormat);
		}
	}
	
	/**
	 * Descriptor object for a data format used in a GoldenGATE web service
	 * 
	 * @author sautter
	 */
	public static class DataFormatDescriptor {
		
		/** the name of the data format, to be used for invokation */
		public final String name;
		
		/** the natural language label of the data format */
		public final String label;
		
		/**
		 * Constructor
		 * @param name the name of the data format
		 * @param label the natural language label of the data format
		 */
		DataFormatDescriptor(String name, String label) {
			this.name = name;
			this.label = label;
		}
	}
	
	/**
	 * Callback object for individual web sevrice invokations.
	 * 
	 * @author sautter
	 */
	public static class WebServiceInvokation {
		private static final Grammar htmlGrammar = new Html();
		private static final Parser htmlParser = new Parser(htmlGrammar);
		
		private GoldenGateWebServiceClient wsc;
		
		/** the invokation ID */
		public final String id;
		
		private boolean finished;
		
		private final String statusUrl;
		private String resultUrl;
		private String getFeedbackRequestUrl;
		private String skipFeedbackRequestUrl;
		private final String answerFeedbackRequestUrl;
		
		private final String outputFormat;
		
		private long lastStatusLookup = System.currentTimeMillis();
		
		private WebServiceFeedbackRequest wsfr;
		
		private IOException exception;
		
		WebServiceInvokation(GoldenGateWebServiceClient wsc, Reader dataReader, String outputFormat) throws IOException {
			String id = this.readStatus(dataReader);
			dataReader.close();
			if (id == null)
				throw new IOException("Could not get invokation ID");
			this.id = id;
			this.wsc = wsc;
			this.statusUrl = (this.wsc.webServiceUrl  + "/" + CALLBACK_COMMAND + "/" + this.id + "/" + STATUS_ACTION);
			this.answerFeedbackRequestUrl = (this.wsc.webServiceUrl  + "/" + ANSWER_FEEDBACK_REQUEST_COMMAND);
			this.outputFormat = outputFormat;
		}
		
		/**
		 * @return the exception from the (last) status update, if any
		 */
		public IOException getException() {
			return this.exception;
		}
		
		/**
		 * @return true if the invokation is finished, false otherwise
		 */
		public boolean isFinished() {
			this.updateStatus();
			return (this.finished && (this.resultUrl != null));
		}
		
		/**
		 * @return true if a feedback request is pending, false otherwise
		 */
		public boolean isFeedbackRequestPending() {
			this.updateStatus();
			return (this.getFeedbackRequestUrl != null);
		}
		
		/**
		 * @return the currently pending feedback request, if any
		 */
		public WebServiceFeedbackRequest getFeedbackRequest() {
			if (this.wsfr != null)
				return this.wsfr;
			if (this.isFinished())
				return null;
			if (this.getFeedbackRequestUrl == null)
				return null;
			try {
				URL getFrUrl = new URL(this.getFeedbackRequestUrl);
				BufferedReader getFrReader = new BufferedReader(new InputStreamReader(getFrUrl.openStream(), ENCODING));
				final WebServiceFeedbackRequest wsfr = new WebServiceFeedbackRequest(this);
				htmlParser.stream(getFrReader, new TokenReceiver() {
					private StringBuffer head;
					private StringBuffer form;
					public void close() throws IOException {}
					public void storeToken(String token, int treeDepth) throws IOException {
						if (htmlGrammar.isTag(token)) {
							String type = htmlGrammar.getType(token);
							if ("head".equalsIgnoreCase(type)) {
								if (htmlGrammar.isEndTag(token)) {
									if (this.head != null)
										wsfr.headerContent = this.head.toString();
									this.head = null;
								}
								else this.head = new StringBuffer();
							}
							else if ("body".equalsIgnoreCase(type)) {
								if (htmlGrammar.isEndTag(token))
									return;
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, htmlGrammar);
								wsfr.onloadCalls = tnas.getAttribute("onload");
								wsfr.onunloadCalls = tnas.getAttribute("onunload");
							}
							else if (this.head != null) {
								//	TODO fully qualify URLs
								this.head.append(token);
							}
							else if ("form".equalsIgnoreCase(type)) {
								if (htmlGrammar.isEndTag(token)) {
									if (this.form != null) {
										this.form.append(token);
										wsfr.feedbackForm = this.form.toString();
									}
									this.form = null;
									return;
								}
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, htmlGrammar);
								if ("feedbackForm".equals(tnas.getAttribute("id")))
									this.form = new StringBuffer(token);
							}
							else if (this.form != null) {
								//	TODO fully qualify URLs
								this.form.append(token);
							}
						}
						else if (this.head != null)
							this.head.append(token);
						else if (this.form != null)
							this.form.append(token);
					}
				});
				getFrReader.close();
				this.exception = null;
				if (wsfr.feedbackForm != null)
					this.wsfr = wsfr;
			}
			catch (IOException ioe) {
				this.exception = ioe;
			}
			return this.wsfr;
		}
		
		/**
		 * Skip a pending feedback request
		 */
		public void skipFeedbackRequest() {
			if (this.skipFeedbackRequestUrl == null)
				return;
			try {
				URL skipFrUrl = new URL(this.skipFeedbackRequestUrl);
				BufferedReader statusReader = new BufferedReader(new InputStreamReader(skipFrUrl.openStream(), ENCODING));
				this.readStatus(statusReader);
				this.wsfr = null;
				statusReader.close();
				this.exception = null;
			}
			catch (IOException ioe) {
				this.exception = ioe;
			}
		}
		
		/**
		 * Answer a feedback request from a raw data stream.
		 * @param answer a reader providing the answer data
		 */
		public void answerFeedbackRequest(Reader answer) {
			this.wsfr = null;
			try {
				URL answerUrl = new URL(this.answerFeedbackRequestUrl);
				HttpURLConnection answerCon = ((HttpURLConnection) answerUrl.openConnection());
				answerCon.setDoOutput(true);
				answerCon.setDoInput(true);
				answerCon.setRequestMethod("POST");
				BufferedWriter answerWriter = new BufferedWriter(new OutputStreamWriter(answerCon.getOutputStream(), ENCODING));
				char[] buffer = new char[1024];
				int read;
				while ((read = answer.read(buffer, 0, buffer.length)) != -1)
					answerWriter.write(buffer, 0, read);
				answerWriter.flush();
				BufferedReader statusReader = new BufferedReader(new InputStreamReader(answerCon.getInputStream(), ENCODING));
				this.readStatus(statusReader);
				answerWriter.close();
				statusReader.close();
				this.exception = null;
			}
			catch (IOException ioe) {
				this.exception = ioe;
			}
		}
		
		/**
		 * Answer a feedback request using pre-parsed parameters.
		 * @param answer a Properties object holding the answer parameters
		 */
		public void answerFeedbackRequest(Properties answer) {
			this.wsfr = null;
			try {
				URL answerUrl = new URL(this.answerFeedbackRequestUrl);
				HttpURLConnection answerCon = ((HttpURLConnection) answerUrl.openConnection());
				answerCon.setDoOutput(true);
				answerCon.setDoInput(true);
				answerCon.setRequestMethod("POST");
				BufferedWriter answerWriter = new BufferedWriter(new OutputStreamWriter(answerCon.getOutputStream(), ENCODING));
				answerWriter.write(DOCUMENT_ID_ATTRIBUTE + "=" + URLEncoder.encode(this.id, ENCODING));
				for (Iterator pnit = answer.keySet().iterator(); pnit.hasNext();) {
					String paramName = ((String) pnit.next());
					String paramValue = answer.getProperty(paramName);
					answerWriter.write("&" + paramName + "=" + URLEncoder.encode(paramValue, ENCODING));
				}
				answerWriter.flush();
				BufferedReader statusReader = new BufferedReader(new InputStreamReader(answerCon.getInputStream(), ENCODING));
				this.readStatus(statusReader);
				answerWriter.close();
				statusReader.close();
				this.exception = null;
			}
			catch (IOException ioe) {
				this.exception = ioe;
			}
		}
		
		/**
		 * @return a reader to read the invokation result from, if already available
		 */
		public Reader getResultReader() {
			return this.getResultReader(this.outputFormat);
		}
		
		/**
		 * Retrieve a reader to read the invokation result from, in a custom
		 * format.
		 * @param outputFormat the name of the output format to use, null uses
		 *            the default input format
		 * @return a reader to read the invokation result from, if already
		 *         available
		 */
		public Reader getResultReader(String outputFormat) {
			if (this.isFinished()) try {
				if (outputFormat == null)
					outputFormat = this.outputFormat;
				URL resultUrl = new URL(this.resultUrl + ((outputFormat == null) ? "" : ("?" + DATA_FORMAT_PARAMETER + "=" + URLEncoder.encode(outputFormat, ENCODING))));
				return new BufferedReader(new InputStreamReader(resultUrl.openStream(), ENCODING));
			}
			catch (IOException ioe) {
				this.exception = ioe;
			}
			return null;
		}
		
		private void updateStatus() {
			if ((this.lastStatusLookup + 1000) < System.currentTimeMillis()) try {
				URL statusUrl = new URL(this.statusUrl);
				BufferedReader statusReader = new BufferedReader(new InputStreamReader(statusUrl.openStream(), ENCODING));
				this.readStatus(statusReader);
				statusReader.close();
				this.exception = null;
			}
			catch (IOException ioe) {
				this.exception = ioe;
			}
		}
		
		private String readStatus(Reader statusReader) throws IOException {
			final String[] id = {null};
			this.resultUrl = null;
			this.getFeedbackRequestUrl = null;
			this.skipFeedbackRequestUrl = null;
			xmlParser.stream(statusReader, new TokenReceiver() {
				private String callbackType = null;
				public void close() throws IOException {}
				public void storeToken(String token, int treeDepth) throws IOException {
					if (xmlGrammar.isTag(token)) {
						if (xmlGrammar.isEndTag(token)) {
							this.callbackType = null;
							return;
						}
						String type = xmlGrammar.getType(token);
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xmlGrammar);
						if (STATUS_NODE_TYPE.equals(type)) {
							id[0] = tnas.getAttribute(DOCUMENT_ID_ATTRIBUTE);
							finished = FINISHED_STATE.equals(tnas.getAttribute(STATE_ATTRIBUTE));
						}
						else if (CALLBACK_NODE_TYPE.equals(type))
							this.callbackType = tnas.getAttribute(CALLBACK_TYPE_ATTRIBUTE);
					}
					else if (RESULT_CALLBACK_TYPE.equals(this.callbackType))
						resultUrl = (wsc.webServiceHost + xmlGrammar.unescape(token.trim()));
					else if (FEEDBACK_SHOW_CALLBACK_TYPE.equals(this.callbackType))
						getFeedbackRequestUrl = (wsc.webServiceHost + xmlGrammar.unescape(token.trim()));
					else if (FEEDBACK_SKIP_CALLBACK_TYPE.equals(this.callbackType))
						skipFeedbackRequestUrl = (wsc.webServiceHost + xmlGrammar.unescape(token.trim()));
				}
			});
			this.lastStatusLookup = System.currentTimeMillis();
			return id[0];
		}
	}
	
	/**
	 * Wrapper for an HTML feedback request, consisting of the required HTML
	 * header content (scripts, styles), the feedback form, and the onload and
	 * onunload function calls of the HTML body tag.
	 * 
	 * @author sautter
	 */
	public static class WebServiceFeedbackRequest {
		private WebServiceInvokation wsi;
		
		String headerContent;
		String feedbackForm;
		
		String onloadCalls;
		String onunloadCalls;
		
		WebServiceFeedbackRequest(WebServiceInvokation wsi) {
			this.wsi = wsi;
		}
		
		/**
		 * @return the onload function calls to include in the body tag of a
		 *         feedback page
		 */
		public String getOnloadCalls() {
			return this.onloadCalls;
		}
		
		/**
		 * @return the onunload function calls to include in the body tag of a
		 *         feedback page
		 */
		public String getOnunloadCalls() {
			return this.onunloadCalls;
		}
		
		/**
		 * Output the content of the HTML header (scripts, styles) to a writer.
		 * @param w the writer to write to
		 * @throws IOException
		 */
		public void writeHeaderContent(Writer w) throws IOException {
			w.write(this.headerContent);
		}
		
		/**
		 * Output the feedback form to a writer.
		 * @param w the writer to write to
		 * @throws IOException
		 */
		public void writeFeedbackForm(Writer w) throws IOException {
			w.write(this.feedbackForm);
		}
		
		/**
		 * Skip/cancel the feedback request.
		 */
		public void skip() {
			this.wsi.skipFeedbackRequest();
		}
		
		/**
		 * Answer the feedback request from a raw data stream.
		 * @param answer a reader providing the answer data
		 */
		public void answer(Reader answer) {
			this.wsi.answerFeedbackRequest(answer);
		}
		
		/**
		 * Answer the feedback request using pre-parsed parameters.
		 * @param answer a Properties object holding the answer parameters
		 */
		public void answer(Properties answer) {
			this.wsi.answerFeedbackRequest(answer);
		}
	}
	
	private String webServiceUrl;
	private String webServiceHost;
	
	/**
	 * Constructor
	 * @param webServiceUrl the URL of the backing web service servlet
	 */
	public GoldenGateWebServiceClient(String webServiceUrl) {
		while (webServiceUrl.endsWith("/"))
			webServiceUrl = webServiceUrl.substring(0, (webServiceUrl.length()-1));
		this.webServiceUrl = webServiceUrl;
		int hostStart = this.webServiceUrl.indexOf("://");
		if (hostStart == -1)
			hostStart = 0;
		else hostStart += "://".length();
		int hostEnd = this.webServiceUrl.indexOf('/', hostStart);
		this.webServiceHost = ((hostEnd == -1) ? this.webServiceUrl : this.webServiceUrl.substring(0, hostEnd));
	}
	
	/**
	 * Obtain the descriptor of the web services available from the backing
	 * servlet.
	 * @return the descriptors of the web services available from the backing
	 *         servlet
	 * @throws IOException
	 */
	public WebServiceDescriptor[] getFunctions() throws IOException {
		URL functionsUrl = new URL(this.webServiceUrl + "/" + LIST_FUNCTIONS_COMMAND);
		BufferedReader functionsReader = new BufferedReader(new InputStreamReader(functionsUrl.openStream(), ENCODING));
		final ArrayList functions = new ArrayList();
		xmlParser.stream(functionsReader, new TokenReceiver() {
			private WebServiceDescriptor wsd;
			private boolean nextIsDescription = false;
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (xmlGrammar.isTag(token)) {
					String type = xmlGrammar.getType(token);
					if (SERVICE_NODE_TYPE.equals(type)) {
						if (xmlGrammar.isEndTag(token)) {
							if ((this.wsd != null) && (this.wsd.defaultInputFormat != null) && (this.wsd.defaultOutputFormat != null))
								functions.add(wsd);
							this.wsd = null;
							this.nextIsDescription = false;
						}
						else {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xmlGrammar);
							String name = tnas.getAttribute(NAME_ATTRIBUTE);
							String label = tnas.getAttribute(LABEL_ATTRIBUTE);
							if ((name == null) || (label == null))
								return;
							this.wsd = new WebServiceDescriptor(name, label, GoldenGateWebServiceClient.this);
							this.wsd.interactivity = tnas.getAttribute(INTERACTIVE_ATTRIBUTE, INTERACTIVE_OPTIONAL);
							this.wsd.defaultInteractive = INTERACTIVE_YES.equalsIgnoreCase(tnas.getAttribute(DEFAULT_INTERACTIVE_ATTRIBUTE, INTERACTIVE_NO));
						}
					}
					else if (this.wsd == null)
						return;
					else if (INPUT_FORMAT_NODE_TYPE.equals(type)) {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xmlGrammar);
						String name = tnas.getAttribute(NAME_ATTRIBUTE);
						String label = tnas.getAttribute(LABEL_ATTRIBUTE);
						if ((name == null) || (label == null))
							return;
						DataFormatDescriptor dfd = new DataFormatDescriptor(name, label);
						this.wsd.inputFormats.add(dfd);
						if ("true".equals(tnas.getAttribute(IS_DEFAULT_DATA_FORMAT_ATTRIBUTE)))
							this.wsd.defaultInputFormat = dfd;
					}
					else if (OUTPUT_FORMAT_NODE_TYPE.equals(type)) {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xmlGrammar);
						String name = tnas.getAttribute(NAME_ATTRIBUTE);
						String label = tnas.getAttribute(LABEL_ATTRIBUTE);
						if ((name == null) || (label == null))
							return;
						DataFormatDescriptor dfd = new DataFormatDescriptor(name, label);
						this.wsd.outputFormats.add(dfd);
						if ("true".equals(tnas.getAttribute(IS_DEFAULT_DATA_FORMAT_ATTRIBUTE)))
							this.wsd.defaultOutputFormat = dfd;
					}
					else if (DESCRIPTION_NODE_TYPE.equals(type))
						this.nextIsDescription = !xmlGrammar.isEndTag(token);
				}
				else if ((this.wsd != null) && this.nextIsDescription)
					this.wsd.description = xmlGrammar.unescape(token);
			}
		});
		functionsReader.close();
		return ((WebServiceDescriptor[]) functions.toArray(new WebServiceDescriptor[functions.size()]));
	}
	
	/**
	 * Invoke a web service, using its default interactivity and input and
	 * output formats.
	 * @param functionName the name of the web service
	 * @param data the data to process
	 * @param dataIsUrl is the data String literal data, or an access or
	 *            callback URL for the data?
	 * @return an invokation object for further interaction
	 * @throws IOException
	 */
	public WebServiceInvokation invokeFunction(String functionName, String data, boolean dataIsUrl) throws IOException {
		return this.invokeFunction(functionName, null, data, dataIsUrl, null, null);
	}
	
	/**
	 * Invoke a web service, using its default interactivity and output format.
	 * @param functionName the name of the web service
	 * @param data the data to process, or the access/callback URL to load the
	 *            data from
	 * @param dataIsUrl is the data String literal data, or an access or
	 *            callback URL for the data?
	 * @param inputFormat the name of the input format to use, null uses the
	 *            default input format
	 * @return an invokation object for further interaction
	 * @throws IOException
	 */
	public WebServiceInvokation invokeFunction(String functionName, String data, boolean dataIsUrl, String inputFormat) throws IOException {
		return this.invokeFunction(functionName, null, data, dataIsUrl, inputFormat, null);
	}
	
	/**
	 * Invoke a web service, using its default interactivity.
	 * @param functionName the name of the web service
	 * @param data the data to process, or the access/callback URL to load the
	 *            data from
	 * @param dataIsUrl is the data String literal data, or an access or
	 *            callback URL for the data?
	 * @param inputFormat the name of the input format to use, null uses the
	 *            default input format
	 * @param outputFormat the name of the output format to use, null uses the
	 *            default output format
	 * @return an invokation object for further interaction
	 * @throws IOException
	 */
	public WebServiceInvokation invokeFunction(String functionName, String data, boolean dataIsUrl, String inputFormat, String outputFormat) throws IOException {
		return this.invokeFunction(functionName, null, data, dataIsUrl, inputFormat, outputFormat);
	}
	
	/**
	 * Invoke a web service, using its default input and output formats.
	 * @param functionName the name of the web service
	 * @param interactivity the interactivity of the invokation, either of
	 *            'yes', 'no', or 'default', with null mapping to 'default'
	 * @param data the data to process
	 * @param dataIsUrl is the data String literal data, or an access or
	 *            callback URL for the data?
	 * @return an invokation object for further interaction
	 * @throws IOException
	 */
	public WebServiceInvokation invokeFunction(String functionName, String interactivity, String data, boolean dataIsUrl) throws IOException {
		return this.invokeFunction(functionName, interactivity, data, dataIsUrl, null, null);
	}
	
	/**
	 * Invoke a web service, using its default output format.
	 * @param functionName the name of the web service
	 * @param interactivity the interactivity of the invokation, either of
	 *            'yes', 'no', or 'default', with null mapping to 'default'
	 * @param data the data to process, or the access/callback URL to load the
	 *            data from
	 * @param dataIsUrl is the data String literal data, or an access or
	 *            callback URL for the data?
	 * @param inputFormat the name of the input format to use, null uses the
	 *            default input format
	 * @return an invokation object for further interaction
	 * @throws IOException
	 */
	public WebServiceInvokation invokeFunction(String functionName, String interactivity, String data, boolean dataIsUrl, String inputFormat) throws IOException {
		return this.invokeFunction(functionName, interactivity, data, dataIsUrl, inputFormat, null);
	}
	
	/**
	 * Invoke a web service.
	 * @param functionName the name of the web service
	 * @param interactivity the interactivity of the invokation, either of
	 *            'yes', 'no', or 'default', with null mapping to 'default'
	 * @param data the data to process, or the access/callback URL to load the
	 *            data from
	 * @param dataIsUrl is the data String literal data, or an access or
	 *            callback URL for the data?
	 * @param inputFormat the name of the input format to use, null uses the
	 *            default input format
	 * @param outputFormat the name of the output format to use, null uses the
	 *            default output format
	 * @return an invokation object for further interaction
	 * @throws IOException
	 */
	public WebServiceInvokation invokeFunction(String functionName, String interactivity, String data, boolean dataIsUrl, String inputFormat, String outputFormat) throws IOException {
		URL invokeUrl = new URL(this.webServiceUrl + "/" + INVOKE_FUNCTION_COMMAND);
		HttpURLConnection invokeCon = ((HttpURLConnection) invokeUrl.openConnection());
		invokeCon.setDoOutput(true);
		invokeCon.setDoInput(true);
		invokeCon.setRequestMethod("POST");
		BufferedWriter invokeWriter = new BufferedWriter(new OutputStreamWriter(invokeCon.getOutputStream(), ENCODING));
		invokeWriter.write(FUNCTION_NAME_PARAMETER + "=" + URLEncoder.encode(functionName, ENCODING));
		if (inputFormat != null) {
			invokeWriter.write("&");
			invokeWriter.write(DATA_FORMAT_PARAMETER + "=" + URLEncoder.encode(inputFormat, ENCODING));
		}
		if (interactivity != null) {
			invokeWriter.write("&");
			invokeWriter.write(INTERACTIVE_PARAMETER + "=" + URLEncoder.encode(interactivity, ENCODING));
		}
		invokeWriter.write("&");
		invokeWriter.write((dataIsUrl ? DATA_URL_PARAMETER : DATA_STRING_PARAMETER) + "=" + URLEncoder.encode(data, ENCODING));
		invokeWriter.flush();
		BufferedReader statusReader = new BufferedReader(new InputStreamReader(invokeCon.getInputStream(), ENCODING));
		return new WebServiceInvokation(this, statusReader, outputFormat);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		GoldenGateWebServiceClient wsc = new GoldenGateWebServiceClient("http://localhost:8080/GgWS/");
		WebServiceDescriptor[] wsds = wsc.getFunctions();
		for (int f = 0; f < wsds.length; f++) {
			System.out.println(wsds[f].name + " - " + wsds[f].label);
		}
		
//		WebServiceInvokation wsi = wsc.invokeFunction("GeoCoordinateTagger2.webService", "Test at 12.0815°N, 32.123°W, at full throttle ...");
		WebServiceInvokation wsi = wsc.invokeFunction("BibRefParser.webService", "Sautter, G., K. Böhm, and D. Agosti. 2006. A combining approach to find all taxon names (FAT) in legacy biosisystematics literature. Biodiversity Informatics 3, 41-53.", false);
		System.out.println("WS invoked");
		while (!wsi.isFinished()) {
			try {
				System.out.println("waiting for WS");
				Thread.sleep(1000);
			} catch (Exception e) {}
			
			if (wsi.getException() != null)
				throw wsi.getException();
			
			if (!wsi.isFeedbackRequestPending())
				continue;
			
			WebServiceFeedbackRequest wsfr = wsi.getFeedbackRequest();
			if (wsfr == null)
				continue;
			Writer ffw = new PrintWriter(System.out);
			wsfr.writeFeedbackForm(ffw);
			ffw.flush();
			wsfr.skip();
		}
		System.out.println("WS finished");
		
		Reader result = wsi.getResultReader();
		int ch;
		while ((ch = result.read()) != -1)
			System.out.print((char) ch);
	}
}