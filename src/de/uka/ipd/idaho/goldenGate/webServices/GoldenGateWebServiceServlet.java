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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.easyIO.web.FormDataReceiver;
import de.uka.ipd.idaho.easyIO.web.FormDataReceiver.FieldValueInputStream;
import de.uka.ipd.idaho.easyIO.web.HtmlServlet;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.GenericGamtaXML;
import de.uka.ipd.idaho.gamta.util.feedback.html.AsynchronousRequestHandler;
import de.uka.ipd.idaho.gamta.util.feedback.html.AsynchronousRequestHandler.AsynchronousRequest;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost;

/**
 * Servlet that allows GoldenGATE document processors to run as web services.
 * GoldenGATE Web Services are resources that bundle a document processor with
 * input and output data formats; they can be created and maintained as part of
 * a GoldenGATE configuration. This servlet loads such a GoldenGATE
 * configuration on startup. There are three parameters that control which
 * configuration is loaded, and from where:
 * <ul>
 * <li><b>GgConfigName</b>: the name of the GoldenGATE configuration to load.</li>
 * <li><b>GgConfigPath</b>: the path to load the configuration from; this can be
 * (a) an absolute folder on the local computer (good for debugging), (b) a path
 * relative to the servlet's data folder (starting with './'), or (c) a URL
 * (starting with 'http://'); if this parameter is set, GgConfigHost is ignored,
 * and all update mechanisms are disabled; if it is not set, the configuration
 * is loaded from the 'Configurations' sub folder of the servlet's data folder,
 * updating from the configured host (see below), and from any zipped
 * configuration deposited in the 'Configurations' folder.</li>
 * <li><b>GgConfigHost</b>: the URL to update configurations from; if this
 * parameter is not set, updates via zip files still work.</li>
 * </ul>
 * There are an additional three parameters that control the servlet's runtime
 * behavior:
 * <ul>
 * <li><b>allowUserInteraction</b>: allow or disallow GoldenGATE web services to
 * ask users for input / feedback, e.g. for double-checking the result of an
 * automated classification or segmentation; if this parameter is not set, it
 * defaults to 'true', enabling request for user feedback; if it is set to
 * 'false' or 'no', user feedback is disabled, and all web services with
 * interactivity setting 'always' become unavailable.</li>
 * <li><b>maxParallelRequests</b>: the maximum number of requests to run in
 * parallel; if this parameter is not set, it defaults to 0, meaning 'no limit';
 * limiting the number of parallel requests is especially helpful to control
 * resource consumption for web services that are not interactive; when running
 * mostly interactive web services, the limit should be higher, as most web
 * services will likely spend a lot of time waiting on user input, so a low
 * limit would result in minimum resource use, and possibly in a blocked
 * pipeline.</li>
 * <li><b>maxParallelTokens</b>: the maximum number of tokens to process in
 * parallel; if this parameter is not set, it defaults to 0, meaning 'no limit';
 * this parameter has an effect similar to maxParallelRequest, but also factors
 * in the amount of data associated with each request.</li>
 * </ul>
 * If both maxParallelRequests and maxParallelTokens are 0, the servlet runs
 * requests as they arrive, directly from memory, and only the result is cached
 * on disc. If either of maxParallelRequests and maxParallelTokens is greater
 * than 0, all requests are cached on disc first, and then handed to the
 * scheduler that controls which request is processed when; the result is cached
 * on disc as well. If the caching of requests on disc is desired, but requests
 * should be processed right away anyway, simply set maxParallelRequests or
 * maxParallelTokens to a large number, imposing a limit that will practically
 * never prevent a request from being processed.
 * 
 * @author sautter
 */
public class GoldenGateWebServiceServlet extends HtmlServlet implements GoldenGateConstants, GoldenGateWebServiceConstants {
	
	private static final String REQUEST_ID_PARAMETER = "id";
	private static final String REQUEST_TIME_PARAMETER = "time";
	private static final String REQUEST_SIZE_PARAMETER = "size";
	private static final String REQUEST_NAME_PARAMETER = "name";
	private static final String HAS_ERROR_PARAMETER = "hasError";
	private static final String REQUEST_PARAM_GROPUP_PREFIX = "param";
	
	private static final String REQUEST_FILE_SUFFIX = ".request";
	private static final String RESULT_FILE_SUFFIX = ".result";
	private static final String DATA_FILE_SUFFIX = ".data";
	
	private static final int uploadMaxLength = (4 * 1024 * 1024); // 4MB for starters
	
	private File cacheRootFolder = null;
	private File requestCacheFolder = null;
	private File resultCacheFolder = null;
	
	private boolean allowUserInteraction = true;
	private Object goldenGateLock = new Object();
	private GoldenGATE goldenGate;
	private WebServiceManager webServiceManager;
	private String webServiceFileExtension;
	
	private int maxParallelRequests = 0;
	private int maxParallelTokens = 0;
	
	private boolean workDiscBased = false;
	private WaitingRequestQueue waitingRequestQueue = null;
	private WaitingRequestHandler waitingHandler = null;
	
	private RequestProxyMap finishedRequestIndex = new RequestProxyMap();
	private LinkedList finishedRequestQueue = new LinkedList();
	private FinishedRequestHandler finishedHandler = null;
	
	private GgWsRequestHandler requestHandler;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.HtmlServlet#doInit()
	 */
	protected void doInit() throws ServletException {
		super.doInit();
		
		//	read additional parameters
		String allowUserInteraction = this.getSetting("allowUserInteraction", "true");
		if ("no".equalsIgnoreCase(allowUserInteraction) || "false".equalsIgnoreCase(allowUserInteraction))
			this.allowUserInteraction = false;
		this.maxParallelRequests = Integer.parseInt(this.getSetting("maxParallelRequests", ("" + this.maxParallelRequests)));
		this.maxParallelTokens = Integer.parseInt(this.getSetting("maxParallelTokens", ("" + this.maxParallelTokens)));
		this.workDiscBased = ((this.maxParallelRequests > 0) || (this.maxParallelTokens > 0));
		
		//	create request handler
		this.requestHandler = new GgWsRequestHandler();
		
		//	set up caching
		String cacheRootPath = this.getSetting("cacheRootFolder", "cache");
		if (cacheRootPath.startsWith("/") || (cacheRootPath.indexOf(":\\") != -1) || (cacheRootPath.indexOf(":/") != -1))
			this.cacheRootFolder = new File(cacheRootPath);
		else this.cacheRootFolder = new File(this.dataFolder, cacheRootPath);
		this.cacheRootFolder.mkdirs();
		this.requestCacheFolder = new File(this.cacheRootFolder, "requests");
		this.requestCacheFolder.mkdir();
		this.resultCacheFolder = new File(this.cacheRootFolder, "results");
		this.resultCacheFolder.mkdir();
		
		//	load cached results from disc and put them in index
		File[] cachedResults = this.resultCacheFolder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				String fn = file.getName();
				return (file.isFile() && fn.endsWith(RESULT_FILE_SUFFIX));
			}
		});
		for (int r = 0; r < cachedResults.length; r++) {
			String[] crData = cachedResults[r].getName().split("\\.");
			String crId = crData[0];
			String crClientId = null;
			if (crData.length > 2) {
				StringBuffer cid = new StringBuffer(crData[1]);
				for (int d = 2; d < (crData.length - 1); d++) {
					cid.append('.');
					cid.append(crData[d]);
				}
				crClientId = cid.toString();
			}
			FinishedRequestProxy frp = new FinishedRequestProxy(crId, crClientId, cachedResults[r]);
			this.finishedRequestIndex.put(frp);
		}
		
		//	initialize finished request caching
		this.finishedHandler = new FinishedRequestHandler();
		this.finishedHandler.start();
		
		//	set up scheduling if working disc based
		if (this.workDiscBased) {
			this.waitingRequestQueue = new WaitingRequestQueue();
			this.waitingHandler = new WaitingRequestHandler();
			this.waitingHandler.start();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.HtmlServlet#reInit()
	 */
	protected void reInit() throws ServletException {
		super.reInit();
		
		//	check if any requests running
		if ((this.requestHandler != null) && (this.requestHandler.getRunningRequestCount() != 0))
			throw new ServletException("Unable to reload GoldenGATE, there are request running.");
		
		//	need to synchronize GG startup
		synchronized (this.goldenGateLock) {
			
			//	shut down GoldenGATE
			if (this.goldenGate != null) {
				this.goldenGate.exitShutdown();
				this.goldenGate = null;
				this.webServiceManager = null;
				this.webServiceFileExtension = null;
			}
			
			//	read how to access GoldenGATE config
			String ggConfigName = this.getSetting("GgConfigName");
			String ggConfigHost = this.getSetting("GgConfigHost");
			String ggConfigPath = this.getSetting("GgConfigPath");
			if (ggConfigName == null)
				throw new ServletException("Unable to access GoldenGATE Configuration.");
			
			//	read GG installation folder
			String ggRootPath = this.getSetting("GgRootPath");
			File ggRootFolder;
			if (ggRootPath == null)
				ggRootFolder = this.dataFolder;
			else if (ggRootPath.startsWith("/") || (ggRootPath.indexOf(":\\") != -1) || (ggRootPath.indexOf(":/") != -1))
				ggRootFolder = new File(ggRootPath);
			else ggRootFolder = new File(this.dataPath, ggRootPath);
			
			//	load GG configuration
			GoldenGateConfiguration ggConfig = null;
			
			//	load configuration
			try {
				ggRootFolder.mkdirs();
				ggConfig = ConfigurationUtils.getConfiguration(ggConfigName, ggConfigPath, ggConfigHost, ggRootFolder);
			}
			catch (IOException ioe) {
				throw new ServletException("Unable to access GoldenGATE Configuration.", ioe);
			}
			
			//	check if we got a configuration from somewhere
			if (ggConfig == null)
				throw new ServletException("Unable to access GoldenGATE Configuration.");
			
			//	start GG instance
			try {
				this.goldenGate = GoldenGATE.openGoldenGATE(ggConfig, false, false);
			}
			catch (IOException ioe) {
				throw new ServletException("Unable to load GoldenGATE instance.", ioe);
			}
			
			//	get services
			this.webServiceManager = ((WebServiceManager) this.goldenGate.getPlugin(WebServiceManager.class.getName()));
			if (this.webServiceManager == null)
				throw new ServletException("Unable to access GoldenGATE Web Service Manager.");
			this.webServiceFileExtension = this.webServiceManager.getFileExtension();
			
			//	wake up whoever is waiting on web service manager
			this.goldenGateLock.notify();
		}
	}
	
	private String[] getWebServiceNames() {
		WebServiceManager wsm;
		synchronized (this.goldenGateLock) {
			wsm = this.webServiceManager;
			while (wsm == null) try {
				this.goldenGateLock.wait(1000);
				wsm = this.webServiceManager;
			} catch (InterruptedException ie) {}
		}
		return wsm.getResourceNames();
	}
	
	private WebService getWebService(String name) {
		WebServiceManager wsm;
		synchronized (this.goldenGateLock) {
			wsm = this.webServiceManager;
			while (wsm == null) try {
				this.goldenGateLock.wait(1000);
				wsm = this.webServiceManager;
			} catch (InterruptedException ie) {}
		}
		return wsm.getWebService(name);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#exit()
	 */
	protected void exit() {
		
		//	shut down scheduler
		if (this.waitingHandler != null)
			this.waitingHandler.shutdown();
		
		//	shut down request handler
		this.requestHandler.shutdown();
		
		//	shut down GG instance
		this.goldenGate.exitShutdown();
		
		//	shut down result cache writer
		this.finishedHandler.shutdown();
	}
	
	private class GgWsRequestHandler extends AsynchronousRequestHandler {
		GgWsRequestHandler() {
			super(true);
		}
		public AsynchronousRequest buildAsynchronousRequest(HttpServletRequest request) throws IOException {
			return null; // we're creating the requests ourselves
		}
		public synchronized void enqueueRequest(AsynchronousRequest ar, String clientId) {
			this.runningRequestTokens += ((GgWsRequest) ar).data.size();
			super.enqueueRequest(ar, clientId);
		}
		protected synchronized AsynchronousRequest getRequest(String arId) {
			return super.getAsynchronousRequest(arId);
		}
		protected boolean retainAsynchronousRequest(AsynchronousRequest ar, int finishedArCount) {
			
			//	we're working disc based, result or error log are cached by now
			if (GoldenGateWebServiceServlet.this.workDiscBased)
				return false;
			
			/* client not yet notified that request is complete, we have to hold
			 * on to this one, unless last status update was more than 10 minutes
			 * ago, which indicates the client side is likely dead */
			if (!ar.isFinishedStatusSent())
				return (System.currentTimeMillis() < (ar.getLastAccessTime() + (1000 * 60 * 10)));
			
			//	result or error log not yet retrieved, hold on to it
			if (ar.hasError() ? !ar.isErrorLogSent() : !ar.isResultSent())
				return true;
			
			//	retain result or error log for at least 15 minutes
			if (System.currentTimeMillis() < (ar.getFinishTime() + (1000 * 60 * 30)))
				return true;
			
			//	retain at most 32 requests beyond the 15 minute limit
			return (finishedArCount <= 32);
		}
		public void asynchronousRequestDiscarded(AsynchronousRequest ar) {
			
			//	we're working disc based, result or error log are cached by now, or scheduled to be
			if (GoldenGateWebServiceServlet.this.workDiscBased)
				return;
			
//			//	this one's result or error log has been fetched, no use keeping it
//			if (ar.hasError() ? ar.isErrorLogSent() : ar.isResultSent())
//				return;
//			
			//	TODO_ne keep result on disc based cache for a while even if we work in memory
			//	if we work in memory, we likely have small requests whose result is fetched rather quickly
			//	BUT we can never be sure, so better keep it if the result has not been fetched yet
			this.cacheResult((GgWsRequest) ar);
		}
		public void asynchronousRequestFinished(AsynchronousRequest ar, boolean willBeRetained) {
			
			//	reduce number of processing tokens
			this.runningRequestTokens -= ((GgWsRequest) ar).data.size();
			
			//	store result right away only when working disc based
			if (GoldenGateWebServiceServlet.this.workDiscBased)
				this.cacheResult((GgWsRequest) ar);
		}
		private void cacheResult(GgWsRequest gwr) {
			
			//	wrap web service request
			FinishedRequestProxy frp = new FinishedRequestProxy(gwr);
			
			//	index finished request right away (no result or error log written as yet, so no removal from request handler ==> no gap in availability for status requests)
			GoldenGateWebServiceServlet.this.finishedRequestIndex.put(frp);
			
			//	schedule result / error log to be cached on disc
			synchronized (GoldenGateWebServiceServlet.this.finishedRequestQueue) {
				GoldenGateWebServiceServlet.this.finishedRequestQueue.addLast(frp);
				GoldenGateWebServiceServlet.this.finishedRequestQueue.notify();
			}
		}
		int getRunningRequestTokens() {
			return this.runningRequestTokens;
		}
		private int runningRequestTokens = 0;
		
		protected HtmlPageBuilderHost getPageBuilderHost() {
			return GoldenGateWebServiceServlet.this;
		}
		protected void sendHtmlPage(HtmlPageBuilder hpb) throws IOException {
			GoldenGateWebServiceServlet.this.sendHtmlPage(hpb);
		}
		protected void sendPopupHtmlPage(HtmlPageBuilder hpb) throws IOException {
			GoldenGateWebServiceServlet.this.sendPopupHtmlPage(hpb);
		}
	}
	
	private class GgWsRequest extends AsynchronousRequest {
		private String clientId;
		private WebService ws;
		private MutableAnnotation data;
		private Properties parameters;
		GgWsRequest(String docId, String clientId, WebService ws, MutableAnnotation data, Properties parameters) {
			super(docId, ("Invokation of " + ws.getName() + " on " + data.size() + " tokens of data, enqueued " + TIMESTAMP_DATE_FORMAT.format(new Date(System.currentTimeMillis()))));
			this.clientId = clientId;
			this.ws = ws;
			this.data = data;
			this.parameters = parameters;
		}
		GgWsRequest(String docId, String clientId, String name, WebService ws, MutableAnnotation data, Properties parameters) {
			super(docId, name);
			this.clientId = clientId;
			this.ws = ws;
			this.data = data;
			this.parameters = parameters;
		}
		protected void process() throws Exception {
			this.ws.process(this.data, this.parameters);
		}
		public boolean sendResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
			if (!this.isFinished())
				throw new IOException("Document still processing.");
			response.setCharacterEncoding(ENCODING);
			response.setContentType("text/xml");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), ENCODING));
			this.ws.writeResult(this.data, request.getParameter(DATA_FORMAT_PARAMETER), bw);
			bw.flush();
			this.resultSent();
			return true;
		}
		void writeResult(OutputStream out) throws IOException {
			this.data.setAttribute(FUNCTION_NAME_PARAMETER, this.ws.getName());
			GenericGamtaXML.storeDocument(this.data, out);
			this.resultSent();
		}
		void writeError(OutputStream out) throws IOException {
			String errorMessage = this.getErrorMessage();
			if (errorMessage == null)
				return;
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, ENCODING));
			bw.write(errorMessage);
			Throwable error = this.getError();
			if (error != null) {
				bw.newLine();
				bw.newLine();
				error.printStackTrace(new PrintWriter(bw));
			}
			bw.flush();
			this.errorReportSent();
		}
	}
	
	private void enqueueRequest(String requestId, String functionName, String clientId, MutableAnnotation data, Properties params) throws IOException {
		
		//	get time first
		long time = System.currentTimeMillis();
		
		//	collect parameters
		Settings request = new Settings();
		request.setSetting(FUNCTION_NAME_PARAMETER, functionName);
		request.setSetting(REQUEST_ID_PARAMETER, requestId);
		request.setSetting(REQUEST_TIME_PARAMETER, ("" + time));
		request.setSetting(REQUEST_SIZE_PARAMETER, ("" + data.size()));
		if (clientId != null)
			request.setSetting(CLIENT_ID_PARAMETER, clientId);
		request.getSubset(REQUEST_PARAM_GROPUP_PREFIX).setProperties(params);
		
		//	cache data
		File dataFile = new File(this.requestCacheFolder, (requestId + DATA_FILE_SUFFIX));
//		GenericGamtaXML.storeDocument(data, dataFile);
		FileOutputStream dataOut = new FileOutputStream(dataFile);
		GenericGamtaXML.storeDocument(data, dataOut);
		dataOut.flush();
		dataOut.close();
		
		//	cache parameters
		File paramFile = new File(this.requestCacheFolder, (requestId + REQUEST_FILE_SUFFIX));
		request.storeAsText(paramFile);
		
		//	create and enqueue proxy object
		WaitingRequestProxy wrp = new WaitingRequestProxy(requestId, clientId, time, data.size(), functionName, params, dataFile, paramFile);
		synchronized (this.waitingRequestQueue) {
			this.waitingRequestQueue.enqueue(wrp);
		}
	}
	
	private void sendStatus(String requestId, HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		//	check cached results first, likely the most frequent ones to be asked
		FinishedRequestProxy frp = ((FinishedRequestProxy) this.finishedRequestIndex.get(requestId));
		if (frp != null) {
			frp.sendStatus(request, response);
			return;
		}
		
		//	if we're not working disc based, we have nowhere else to look
		if (!this.workDiscBased) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Unknown Request ID: " + requestId));
			return;
		}
		
		//	check waiting requests
		WaitingRequestProxy wrp = this.waitingRequestQueue.get(requestId);
		if (wrp != null) {
			wrp.sendStatus(request, response, this.waitingRequestQueue.getPosition(requestId));
			return;
		}
		
		//	try working request handler once again (is checked very first thing in doGet() method itself), request might have transitioned away from us ...
		AsynchronousRequest ar = this.requestHandler.getRequest(requestId);
		if (ar != null) {
			ar.sendStatusUpdate(request, response);
			return;
		}
		
		//	check cached results again, request might have transitioned away from us again ...
		frp = ((FinishedRequestProxy) this.finishedRequestIndex.get(requestId));
		if (frp != null) {
			frp.sendStatus(request, response);
			return;
		}
		
		//	nothing we can do about this one
		response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Unknown Request ID: " + requestId));
	}
	
	private void cancelRequest(String requestId, HttpServletResponse response) throws IOException {
		
		//	if we're not working disc based, we have nothing to cancel
		if (!this.workDiscBased) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Unknown Request ID: " + requestId));
			return;
		}
		
		//	check waiting requests
		WaitingRequestProxy wrp = this.waitingRequestQueue.remove(requestId);
		if (wrp != null) {
			
			//	clean up cached data
			wrp.paramFile.delete();
			wrp.dataFile.delete();
			
			//	prepare output
			response.setContentType("text/xml");
			response.setCharacterEncoding(ENCODING);
			Writer out = new OutputStreamWriter(response.getOutputStream(), ENCODING);
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			//	write XML status message
			bw.writeLine("<status id=\"" + requestId + "\" state=\"" + AnnotationUtils.escapeForXml("Cancelled") + "\" stateDetail=\"" + AnnotationUtils.escapeForXml("Cancelled at client request.") + "\" percentFinished=\"" + 0 + "\"/>");
			
			//	send data
			bw.flush();
			out.flush();
			
			//	we're done here
			return;
		}
		
		//	nothing we can do about this one
		response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Unknown Request ID: " + requestId));
	}
	
	private void sendResult(String requestId, HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		//	check cached results
		FinishedRequestProxy frp = ((FinishedRequestProxy) this.finishedRequestIndex.get(requestId));
		if (frp != null) {
			frp.sendResult(request, response);
			return;
		}
		
		//	nothing we can do about this one
		response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Unknown Request ID: " + requestId));
	}
	
	private void sendErrorReport(String requestId, HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		//	check cached results first, likely the most frequent ones to be asked
		FinishedRequestProxy frp = ((FinishedRequestProxy) this.finishedRequestIndex.get(requestId));
		if (frp != null) {
			frp.sendErrorReport(request, response);
			return;
		}
		
		//	nothing we can do about this one
		response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Unknown Request ID: " + requestId));
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		//	check for status request, etc.
		if (this.requestHandler.handleRequest(request, response))
			return;
		
		//	other call
		String pathInfo = request.getPathInfo();
		String actionOrRequestId = LIST_FUNCTIONS_COMMAND;
		String clientIdOrFunctionNameOrAction = null;
		if ((pathInfo != null) && !"/".equals(pathInfo) && (pathInfo.length() != 0)) {
			while (pathInfo.startsWith("/"))
				pathInfo = pathInfo.substring(1);
			if (pathInfo.indexOf('/') == -1)
				actionOrRequestId = pathInfo;
			else {
				actionOrRequestId = pathInfo.substring(0, pathInfo.indexOf('/'));
				clientIdOrFunctionNameOrAction = pathInfo.substring(pathInfo.indexOf('/')+1);
			}
		}
		
		//	provide test form
		if ("test".equalsIgnoreCase(actionOrRequestId)) {
			this.sendTestForm(request, response);
			return;
		}
//		
//		//	list status info
//		//	TODO use this for trouble shooting
//		if ("status".equalsIgnoreCase(actionOrRequestId)) {
//			this.sendStatusOverview(response);
//			return;
//		}
		
		//	list available services
		if (LIST_FUNCTIONS_COMMAND.equals(actionOrRequestId))
			this.listServices(response);
		
		//	list requests of user
		else if (LIST_REQUESTS_COMMAND.equals(actionOrRequestId)) {
			
			//	get user name
			if (clientIdOrFunctionNameOrAction == null)
				clientIdOrFunctionNameOrAction = request.getParameter(CLIENT_ID_PARAMETER);
			if (clientIdOrFunctionNameOrAction == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			
			//	send list
			this.listRequests(clientIdOrFunctionNameOrAction, request, response);
		}
		
		//	Invocation with callback URL
		else if (INVOKE_FUNCTION_COMMAND.equals(actionOrRequestId)) {
			
			//	get service name
			if (clientIdOrFunctionNameOrAction == null)
				clientIdOrFunctionNameOrAction = request.getParameter(FUNCTION_NAME_PARAMETER);
			
			//	get service to use
			WebService service = this.getWebService(clientIdOrFunctionNameOrAction);
			if (service == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Unknown function: " + clientIdOrFunctionNameOrAction));
				return;
			}
			
			//	get document format
			String dataFormat = request.getParameter(DATA_FORMAT_PARAMETER);
			
			//	get document callback URL
			String dataUrl = request.getParameter(DATA_URL_PARAMETER);
			if (dataUrl == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Data URL missing");
				return;
			}
			
			//	get document
			InputStream dataIn = (new URL(dataUrl)).openStream();
			MutableAnnotation data = service.readDocument(dataFormat, dataIn);
			dataIn.close();
			
			//	document not found
			if (data == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, ("Could not load data from '" + dataUrl + "'"));
				return;
			}
			
			//	get document ID
			String docId = ((String) data.getAttribute(DocumentRoot.DOCUMENT_ID_ATTRIBUTE));
			if (docId == null) {
				docId = data.getAnnotationID();
				data.setAttribute(DocumentRoot.DOCUMENT_ID_ATTRIBUTE, docId);
			}
			if (data instanceof DocumentRoot)
				((DocumentRoot) data).setDocumentProperty(DocumentRoot.DOCUMENT_ID_ATTRIBUTE, docId);
			
			//	get client ID
			String clientId = request.getParameter(CLIENT_ID_PARAMETER);
			
			//	get remaining parameters
			Properties parameters = new Properties();
			Enumeration paramNames = request.getParameterNames();
			while (paramNames.hasMoreElements()) {
				String paramName = paramNames.nextElement().toString();
				String paramValue = request.getParameter(paramName);
				if (paramValue != null)
					parameters.setProperty(paramName, paramValue);
			}
			
			//	adjust interactivity if required
			if (!this.allowUserInteraction)
				parameters.setProperty(INTERACTIVE_PARAMETER, INTERACTIVE_NO);
			
			//	we're working disc based, store request on disc and leave starting it to scheduler
			if (this.workDiscBased) {
				this.enqueueRequest(docId, clientIdOrFunctionNameOrAction, clientId, data, parameters);
				this.sendStatus(docId, request, response);
			}
			
			//	we're working in memory, start request right away
			else {
				GgWsRequest gwr = new GgWsRequest(docId, clientId, service, data, parameters);
				this.requestHandler.enqueueRequest(gwr, gwr.clientId);
				gwr.sendStatusUpdate(request, response);
			}
		}
		
		//	status request for asynchronous request that is not currently running (waiting or finished)
		else if (AsynchronousRequestHandler.STATUS_UPDATE_ACTION.equals(clientIdOrFunctionNameOrAction))
			this.sendStatus(actionOrRequestId, request, response);
		
		//	status request for asynchronous request that is not currently running (waiting or finished)
		else if (AsynchronousRequestHandler.CANCEL_ACTION.equals(clientIdOrFunctionNameOrAction))
			this.cancelRequest(actionOrRequestId, response);
		
		//	status request for asynchronous request that is not currently running (waiting or finished)
		else if (AsynchronousRequestHandler.RESULT_ACTION.equals(clientIdOrFunctionNameOrAction))
			this.sendResult(actionOrRequestId, request, response);
		
		//	status request for asynchronous request that is not currently running (waiting or finished)
		else if (AsynchronousRequestHandler.ERRORS_ACTION.equals(clientIdOrFunctionNameOrAction))
			this.sendErrorReport(actionOrRequestId, request, response);
		
		//	nothing we can do about this one
		else response.sendError(HttpServletResponse.SC_BAD_REQUEST, ("Unknown action: " + actionOrRequestId));
	}
	
	private void sendTestForm(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String[] wsNames = this.getWebServiceNames();
		final ArrayList wsList = new ArrayList();
		for (int w = 0; w < wsNames.length; w++) {
			WebService ws = this.getWebService(wsNames[w]);
			if (ws != null)
				wsList.add(ws);
		}
		final WebService[] wss = ((WebService[]) wsList.toArray(new WebService[wsList.size()]));
		final TreeMap ifnMap = new TreeMap();
		for (int w = 0; w < wss.length; w++) {
			String[] ifns = wss[w].getInputFormatNames();
			for (int f = 0; f < ifns.length; f++) {
				String ifl = wss[w].getInputFormatLabel(ifns[f]);
				if (ifl != null)
					ifnMap.put(ifns[f], ifl);
			}
		}
		
		response.setContentType("text/html");
		response.setCharacterEncoding(ENCODING);
		this.sendHtmlPage("testFormPage.html", new HtmlPageBuilder(this, request, response) {
			protected void include(String type, String tag) throws IOException {
				if ("includeTestForm".equals(type))
					this.includeTestForm();
				else super.include(type, tag);
			}
			private void includeTestForm() throws IOException {
				
				//	catch lack of web services
				if (wss.length == 0) {
					this.writeLine("<p class=\"error\">");
					this.writeLine("No Web Services available, sorry!");
					this.writeLine("</p>");
					return;
				}
				
				//	open form
				this.writeLine("<form" +
						" id=\"testForm\"" +
						" method=\"POST\"" +
						" action=\"" + this.request.getContextPath() + this.request.getServletPath() + "/" + INVOKE_FUNCTION_COMMAND + "\"" +
						" onsubmit=\"return checkDataSource();\"" +
						" enctype=\"application/x-www-urlencoded\"" +
						" accept-charset=\"" + ENCODING + "\"" +
						" target=\"_blank\"" +
						">");
				
				//	add JavaScripts
				this.writeLine("<script type=\"text/javascript\">");
				this.writeLine("function checkDataSource() {");
				this.writeLine("  var dif = document.getElementById('dataInputField');");
				this.writeLine("  var dff = document.getElementById('dataFileField');");
				this.writeLine("  var duf = document.getElementById('dataUrlField');");
				this.writeLine("  if (dif.value != '') {");
				this.writeLine("    dif.name = 'data';");
				this.writeLine("    duf.value = '';");
				this.writeLine("    return true;");
				this.writeLine("  }");
				this.writeLine("  if (dff.value != '') {");
				this.writeLine("    dff.name = 'data';");
				this.writeLine("    duf.value = '';");
				this.writeLine("    document.getElementById('testForm').enctype = 'multipart/form-data';");
				this.writeLine("    return true;");
				this.writeLine("  }");
				this.writeLine("  if (duf.value != '')");
				this.writeLine("    return true;");
				this.writeLine("  alert('Please insert data in the text box, select a file to load, or specify a URL to retrieve data from.');");
				this.writeLine("  return false;");
				this.writeLine("}");
				this.writeLine("var dataFormats = new Object();");
				this.writeLine("function initWebServices() {");
				this.writeLine("  var dff = document.getElementById('dataFormatField');");
				this.writeLine("  while (dff.firstChild) {");
				this.writeLine("    dataFormats[dff.firstChild.value] = dff.firstChild;");
				this.writeLine("    dff.removeChild(dff.firstChild);");
				this.writeLine("  }");
				this.writeLine("  webServiceSelected();");
				this.writeLine("}");
				this.writeLine("function webServiceSelected() {");
				this.writeLine("  var fnf = document.getElementById('functionNameField');");
				this.writeLine("  var dff = document.getElementById('dataFormatField');");
				this.writeLine("  var iyf = document.getElementById('interactiveYesField');");
				this.writeLine("  var inf = document.getElementById('interactiveNoField');");
				this.writeLine("  while (dff.firstChild)");
				this.writeLine("    dff.removeChild(dff.firstChild);");
				for (int w = 0; w < wss.length; w++) {
					this.writeLine("  if (fnf.value == '" + wss[w].getName() + "') {");
					this.writeLine("    iyf.checked = '" + (wss[w].isDefaultInteractive() ? "true" : "false") + "'");
					this.writeLine("    inf.checked = '" + (wss[w].isDefaultInteractive() ? "false" : "true") + "'");
					this.writeLine("    iyf.disabled = '" + (WebService.INTERACTIVE_NEVER.equals(wss[w].getInteractivity()) ? "true" : "false") + "'");
					this.writeLine("    inf.disabled = '" + (WebService.INTERACTIVE_ALWAYS.equals(wss[w].getInteractivity()) ? "true" : "false") + "'");
					String[] ifns = wss[w].getInputFormatNames();
					for (int f = 0; f < ifns.length; f++)
						this.writeLine("    dff.appendChild(dataFormats['" + ifns[f] + "']);");
					this.writeLine("    dff.value = '" + wss[w].getDefaultInputFormatName() + "';");
					this.writeLine("  }");
				}
				this.writeLine("}");
				this.writeLine("</script>");
				
				//	data input field
				this.writeLine("<table class=\"ggWsTestFormTable\">");
				this.writeLine("<tr>");
				this.writeLine("<td class=\"ggWsTestFormTableCell\" colspan=\"2\">");
				this.writeLine("<textarea id=\"dataInputField\" name=\"dataField\" cols=\"80\" rows=\"10\"></textarea>");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	add upload URL field
				this.writeLine("<tr>");
				this.writeLine("<td class=\"ggWsTestFormTableLabelCell\">");
				this.writeLine("Data&nbsp;URL:");
				this.writeLine("</td>");
				this.writeLine("<td class=\"ggWsTestFormTableCell\">");
				this.writeLine("<input type=\"text\" id=\"dataUrlField\" name=\"dataUrl\">");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	add upload file selector field (file selected --> multipart/form-data, clear data field, data input in field --> application/x-www-urlencoded, clear file selector)
				this.writeLine("<tr>");
				this.writeLine("<td class=\"ggWsTestFormTableLabelCell\">");
				this.writeLine("Data&nbsp;File:");
				this.writeLine("</td>");
				this.writeLine("<td class=\"ggWsTestFormTableCell\">");
				this.writeLine("<input type=\"file\" id=\"dataFileField\" name=\"dataUrl\">");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	function name selector
				this.writeLine("<tr>");
				this.writeLine("<td class=\"ggWsTestFormTableLabelCell\">");
				this.writeLine("Function:");
				this.writeLine("</td>");
				this.writeLine("<td class=\"ggWsTestFormTableCell\">");
				this.writeLine("<select id=\"functionNameField\" name=\"functionName\" onchange=\"webServiceSelected();\">");
				for (int w = 0; w < wss.length; w++) {
					String wsName = wss[w].getName();
					StringBuffer wsLabel = new StringBuffer();
					int wsSuffixStart = wsName.lastIndexOf(webServiceFileExtension);
					for (int c = 0; c < wsSuffixStart; c++) {
						char ch = wsName.charAt(c);
						if ((c != 0) && Character.isUpperCase(ch))
							wsLabel.append(' ');
						wsLabel.append(ch);
					}
					this.writeLine("<option value=\"" + wsName + "\">" + html.escape(wsLabel.toString()) + "</option>");
				}
				this.writeLine("</select>");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	data format selector
				this.writeLine("<tr>");
				this.writeLine("<td class=\"ggWsTestFormTableLabelCell\">");
				this.writeLine("Input&nbsp;Format:");
				this.writeLine("</td>");
				this.writeLine("<td class=\"ggWsTestFormTableCell\">");
				this.writeLine("<select id=\"dataFormatField\" name=\"dataFormat\">");
				for (Iterator ifnit = ifnMap.keySet().iterator(); ifnit.hasNext();) {
					String ifn = ((String) ifnit.next());
					String ifl = ((String) ifnMap.get(ifn));
					this.writeLine("<option value=\"" + ifn + "\">" + html.escape(ifl) + "</option>");
				}
				this.writeLine("</select>");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	selector for interactivity
				this.writeLine("<tr>");
				this.writeLine("<td class=\"ggWsTestFormTableLabelCell\">");
				this.writeLine("Interactive:");
				this.writeLine("</td>");
				this.writeLine("<td class=\"ggWsTestFormTableCell\">");
				this.writeLine("Yes&nbsp;<input type=\"radio\" id=\"interactiveYesField\" name=\"INTERACTIVE\" value=\"yes\">");
				this.writeLine("&nbsp;&nbsp;");
				this.writeLine("No&nbsp;<input type=\"radio\" id=\"interactiveNoField\" name=\"INTERACTIVE\" value=\"no\">");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	submit button
				this.writeLine("<tr>");
				this.writeLine("<td class=\"ggWsTestFormTableButtonCell\" colspan=\"2\">");
				this.writeLine("<input type=\"submit\" value=\"Run Test\">");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	call initializer JavaScript function here
				this.writeLine("<script type=\"text/javascript\">");
				this.writeLine("  initWebServices();");
				this.writeLine("</script>");
				
				//	close table and form
				this.writeLine("</table>");
				this.writeLine("</form>");
			}
		});
	}
//	
//	private void sendStatusOverview(HttpServletResponse response) throws IOException {
//		response.setCharacterEncoding(ENCODING);
//		response.setContentType("text/xml");
//		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), ENCODING));
//		bw.write("<status");
//		bw.write(" requests=\"" + this.requestHandler.getRequestCount() + "\"");
//		bw.write(" requestsRunning=\"" + this.requestHandler.getRunningRequestCount() + "\"");
//		bw.write(" requestsFeedback=\"" + this.requestHandler.getFeedbackAwaitingRequestCount() + "\"");
//		bw.write(" requestsWaiting=\"" + this.waitingRequestQueue.size() + "\"");
//		bw.write(" requestsFinishing=\"" + this.finishedRequestQueue.size() + "\"");
//		bw.write(" requestsFinished=\"" + this.finishedRequestIndex.size() + "\"");
//		bw.write(">");
//		bw.newLine();
//		for (Iterator rit = this.waitingRequestQueue.queue.iterator(); rit.hasNext();) {
//			WaitingRequestProxy wrp = ((WaitingRequestProxy) rit.next());
//			bw.write("<request");
//			bw.write(" id=\"" + wrp.id + "\"");
//			bw.write(" clientId=\"" + wrp.clientId + "\"");
//			bw.write(" name=\"" + wrp.name + "\"");
//			bw.write(" size=\"" + wrp.size + "\"");
//			bw.write(" functionName=\"" + wrp.functionName + "\"");
//			bw.write("/>");
//			bw.newLine();
//		}
//		bw.write("</status>");
//		bw.newLine();
//		bw.flush();
//	}
	
	private void listServices(HttpServletResponse response) throws IOException {
		response.setCharacterEncoding(ENCODING);
		response.setContentType("text/xml");
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), ENCODING));
		this.writeServiceList(bw);
	}
	
	private void writeServiceList(BufferedWriter bw) throws IOException {
		bw.write("<" + SERVICES_NODE_TYPE + ">");
		bw.newLine();
		String[] serviceNames = this.getWebServiceNames();
		for (int s = 0; s < serviceNames.length; s++) {
			WebService service = this.getWebService(serviceNames[s]);
			if (service == null)
				continue;
			if (INTERACTIVE_ALWAYS.equals(service.getInteractivity()) && !this.allowUserInteraction)
				continue;
			service.writeXmlDescription(bw);
		}
		bw.write("</" + SERVICES_NODE_TYPE + ">");
		bw.newLine();
		bw.flush();
	}
	
	private void listRequests(String clientId, HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		//	list requests of user
		LinkedHashSet userRequestIDs = new LinkedHashSet();
		if (this.waitingRequestQueue != null)
			userRequestIDs.addAll(Arrays.asList(this.waitingRequestQueue.getRequestIDs(clientId)));
		userRequestIDs.addAll(Arrays.asList(this.requestHandler.getRequestIDs(clientId)));
		userRequestIDs.addAll(Arrays.asList(this.finishedRequestIndex.getRequestIDs(clientId)));
		
		//	prepare output
		response.setCharacterEncoding(ENCODING);
		response.setContentType("text/xml");
		Writer out = new OutputStreamWriter(response.getOutputStream(), ENCODING);
		BufferedLineWriter bw = new BufferedLineWriter(out);
		
		//	send empty list
		if (userRequestIDs.size() == 0)
			bw.writeLine("<requests " + CLIENT_ID_PARAMETER + "=\"" + clientId + "\"/>");
		
		//	send list
		else {
			bw.writeLine("<requests " + CLIENT_ID_PARAMETER + "=\"" + clientId + "\">");
			for (Iterator urit = userRequestIDs.iterator(); urit.hasNext();) {
				String urid = ((String) urit.next());
				
				//	check waiting requests
				WaitingRequestProxy wrp = this.waitingRequestQueue.get(urid);
				if (wrp != null) {
					wrp.writeStatus(request, bw, this.waitingRequestQueue.getPosition(urid));
					continue;
				}
				
				//	try working request handler once again (is checked very first thing in doGet() method itself), request might have transitioned away from us ...
				AsynchronousRequest ar = this.requestHandler.getRequest(urid);
				if (ar != null) {
					ar.writeStatusXML(request, bw);
					continue;
				}
				
				//	check cached results again, request might have transitioned away from us again ...
				FinishedRequestProxy frp = ((FinishedRequestProxy) this.finishedRequestIndex.get(urid));
				if (frp != null) {
					frp.writeStatus(request, bw);
					continue;
				}
			}
			bw.writeLine("</requests>");
		}
		
		//	send data
		bw.flush();
		out.flush();
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//	feedback submission
		if (this.requestHandler.handleRequest(request, response))
			return;
		
		//	other call
		String pathInfo = request.getPathInfo();
		String action = INVOKE_FUNCTION_COMMAND;
		String functionName = null;
		if ((pathInfo != null) && !"/".equals(pathInfo) && (pathInfo.length() != 0)) {
			while (pathInfo.startsWith("/"))
				pathInfo = pathInfo.substring(1);
			if (pathInfo.indexOf('/') != -1) {
				action = pathInfo.substring(0, pathInfo.indexOf('/'));
				functionName = pathInfo.substring(pathInfo.indexOf('/')+1);
			}
		}
		
		//	document submission
		if (INVOKE_FUNCTION_COMMAND.equals(action)) {
			
			//	read request data
			FormDataReceiver reqData = FormDataReceiver.receive(request, uploadMaxLength, this.requestCacheFolder, 1024, new HashSet());
			
			//	get service name
			if (functionName == null)
				functionName = reqData.getFieldValue(FUNCTION_NAME_PARAMETER);
			
			//	get service to use
			WebService service = this.getWebService(functionName);
			if (service == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Unknown function: " + functionName));
				return;
			}
			
			//	get document format
			String dataFormat = reqData.getFieldValue(DATA_FORMAT_PARAMETER);
			
			//	get document
			MutableAnnotation data = null;
			String dataUrl = reqData.getFieldValue(DATA_URL_PARAMETER);
			InputStream dataIn = (((dataUrl == null) || (dataUrl.trim().length() == 0)) ? reqData.getFieldByteStream(DATA_STRING_PARAMETER) : (new URL(dataUrl)).openStream());
			
			//	load data
			if (dataIn != null) {
				
				//	try loading data from reader first
				try {
					data = service.readDocument(dataFormat, new InputStreamReader(dataIn, ((dataIn instanceof FieldValueInputStream) ? ((FieldValueInputStream) dataIn).getEncoding() : ENCODING)));
				}
				catch (IOException ioe) {
					
					//	load data from stream if we have a multipart request, might be binary file
					if ("multipart/form-data".equals(reqData.getContentType())) {
						dataIn.close();
						dataIn = (((dataUrl == null) || (dataUrl.trim().length() == 0)) ? reqData.getFieldByteStream(DATA_STRING_PARAMETER) : (new URL(dataUrl)).openStream());
						data = service.readDocument(dataFormat, dataIn);
					}
					
					//	give up otherwise
					else throw ioe;
				}
				
				//	clean up
				dataIn.close();
			}
			
			//	document not found
			if (data == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No data to process");
				return;
			}
			
			//	get document ID
			String docId = ((String) data.getAttribute(DocumentRoot.DOCUMENT_ID_ATTRIBUTE));
			if (docId == null) {
				docId = data.getAnnotationID();
				data.setAttribute(DocumentRoot.DOCUMENT_ID_ATTRIBUTE, docId);
			}
			if (data instanceof DocumentRoot)
				((DocumentRoot) data).setDocumentProperty(DocumentRoot.DOCUMENT_ID_ATTRIBUTE, docId);
			
			//	get remaining parameters
			Properties parameters = new Properties();
			String[] paramNames = reqData.getFieldNames();
			for (int p = 0; p < paramNames.length; p++) {
				if (DATA_STRING_PARAMETER.equals(paramNames[p]))
					continue;
				String paramValue = reqData.getFieldValue(paramNames[p]);
				if (paramValue != null)
					parameters.setProperty(paramNames[p], paramValue);
			}
			
			//	adjust interactivity if required
			if (!this.allowUserInteraction)
				parameters.setProperty(INTERACTIVE_PARAMETER, INTERACTIVE_NO);
			
			//	we're working disc based, store request on disc and leave starting it to scheduler
			if (this.workDiscBased) {
				this.enqueueRequest(docId, functionName, reqData.getFieldValue(CLIENT_ID_PARAMETER), data, parameters);
				this.sendStatus(docId, request, response);
			}
			
			//	we're working in memory, start request right away
			else {
				GgWsRequest gwr = new GgWsRequest(docId, reqData.getFieldValue(CLIENT_ID_PARAMETER), service, data, parameters);
				this.requestHandler.enqueueRequest(gwr, gwr.clientId);
				gwr.sendStatusUpdate(request, response);
			}
			
			//	clean up request data
			reqData.dispose();
		}
		
		//	nothing we can do about this one
		else response.sendError(HttpServletResponse.SC_BAD_REQUEST, ("Unknown action: " + action));
	}
	
	private boolean canRunWebServiceRequest(WaitingRequestProxy wrp) {
		boolean fitsRequestLimit = ((this.maxParallelRequests < 1) || (this.requestHandler.getRunningRequestCount() < this.maxParallelRequests));
		boolean fitsRequestSizeLimit = ((this.maxParallelTokens < 1) || (this.requestHandler.getRunningRequestTokens() < this.maxParallelTokens));
		return (fitsRequestLimit && fitsRequestSizeLimit);
		//	TODO also observe load per service name to make effort-based decisions
	}
	
	private static class RequestProxyMap {
		private HashMap requestsById = new HashMap();
		private HashMap requestsIDsByClientId = new HashMap();
		synchronized void put(RequestProxy rp) {
			this.requestsById.put(rp.id, rp);
			if (rp.clientId != null) {
				HashSet cidRequestIDs = ((HashSet) this.requestsIDsByClientId.get(rp.clientId));
				if (cidRequestIDs == null) {
					cidRequestIDs = new LinkedHashSet(2);
					this.requestsIDsByClientId.put(rp.clientId, cidRequestIDs);
				}
				cidRequestIDs.add(rp.id);
			}
		}
		synchronized RequestProxy get(String id) {
			return ((RequestProxy) this.requestsById.get(id));
		}
		synchronized RequestProxy remove(String id) {
			RequestProxy rp = ((RequestProxy) this.requestsById.remove(id));
			if (rp != null) {
				HashSet cidRequestIDs = ((HashSet) this.requestsIDsByClientId.get(rp.clientId));
				if (cidRequestIDs != null)
					cidRequestIDs.remove(rp.id);
			}
			return rp;
		}
		synchronized String[] getRequestIDs(String clientId) {
			if (clientId == null)
				return new String[0];
			HashSet cidRequestIDs = ((HashSet) this.requestsIDsByClientId.get(clientId));
			return ((cidRequestIDs == null) ? new String[0] : ((String[]) cidRequestIDs.toArray(new String[cidRequestIDs.size()])));
		}
		synchronized int size() {
			return this.requestsById.size();
		}
		synchronized boolean containsKey(Object key) {
			return this.requestsById.containsKey(key);
		}
		
	}
	
	private static final DateFormat TIMESTAMP_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
	
	private static abstract class RequestProxy {
		final String id;
		final String clientId;
		String name;
		RequestProxy(String id, String clientId) {
			this(id, clientId, null);
		}
		RequestProxy(String id, String clientId, String name) {
			this.id = id;
			this.clientId = clientId;
			this.name = name;
		}
	}
	
	private class WaitingRequestHandler extends Thread {
		private boolean run = true;
		public void run() {
			
			//	let starting thread continue
			synchronized (this) {
				this.notify();
			}
			
			//	load cached requests from disc and put them in queue (synchronized, so no newly arriving requests can be added before stored ones)
			synchronized (GoldenGateWebServiceServlet.this.waitingRequestQueue) {
				File[] waitingRequests = GoldenGateWebServiceServlet.this.requestCacheFolder.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return (file.isFile() && file.getName().endsWith(REQUEST_FILE_SUFFIX));
					}
				});
				Arrays.sort(waitingRequests, new Comparator() {
					public int compare(Object wrf1, Object wrf2) {
						long wrt1 = ((File) wrf1).lastModified();
						long wrt2 = ((File) wrf2).lastModified();
						if (wrt1 == wrt2) return 0;
						return ((wrt1 < wrt2) ? -1 : 1);
					}
				});
				for (int w = 0; w < waitingRequests.length; w++) {
					
					//	get reqiest data
					Settings request = Settings.loadSettings(waitingRequests[w]);
					String id = request.getSetting(REQUEST_ID_PARAMETER);
					if (id == null)
						continue;
					long time = Long.parseLong(request.getSetting(REQUEST_TIME_PARAMETER, "0"));
					if (time == 0)
						continue;
					int size = Integer.parseInt(request.getSetting(REQUEST_SIZE_PARAMETER, "0"));
					if (size == 0)
						continue;
					String functionName = request.getSetting(FUNCTION_NAME_PARAMETER);
					if (functionName == null)
						continue;
					String clientId = request.getSetting(CLIENT_ID_PARAMETER);
					Properties params = new Properties();
					params.putAll(request.getSubset(REQUEST_PARAM_GROPUP_PREFIX).toProperties());
					
					//	create and enqueue proxy object
					WaitingRequestProxy wrp = new WaitingRequestProxy(id, clientId, time, size, functionName, params, new File(GoldenGateWebServiceServlet.this.requestCacheFolder, (id + DATA_FILE_SUFFIX)), waitingRequests[w]);
					GoldenGateWebServiceServlet.this.waitingRequestQueue.enqueue(wrp);
				}
			}
			
			//	working off queue
			while (this.run) {
				
				//	check queue and wait
				WaitingRequestProxy wrp = null;
				synchronized (GoldenGateWebServiceServlet.this.waitingRequestQueue) {
					try {
						GoldenGateWebServiceServlet.this.waitingRequestQueue.wait(1000);
					} catch (InterruptedException ie) {}
					
					//	we're being shut down
					if (!this.run) {
						GoldenGateWebServiceServlet.this.waitingRequestQueue.notify();
						return;
					}
					
					//	try to get next waiting request
					if (GoldenGateWebServiceServlet.this.waitingRequestQueue.size() != 0)
						wrp = GoldenGateWebServiceServlet.this.waitingRequestQueue.getNext();
				}
				
				//	nothing to work with
				if (wrp == null)
					continue;
				System.out.println("WaitingRequestHandler: got request, ID is " + wrp.id);
				
				//	can we run this one?
				if (!GoldenGateWebServiceServlet.this.canRunWebServiceRequest(wrp)) {
					System.out.println(" ==> unrunnable");
					continue;
				}
				System.out.println(" - runnable");
				
				//	get service to use
				WebService service = GoldenGateWebServiceServlet.this.getWebService(wrp.functionName);
				if (service == null)
					System.out.println(" ==> function does not exist");
				else try {
					System.out.println(" - got function");
					
					//	read data
					FileInputStream dataIn = new FileInputStream(wrp.dataFile);
					MutableAnnotation data = GenericGamtaXML.readDocument(dataIn);
					dataIn.close();
					System.out.println(" - data loaded");
					
					//	create asynchronous request
					GgWsRequest gwr = new GgWsRequest(wrp.id, wrp.clientId, wrp.name, service, data, wrp.params);
					GoldenGateWebServiceServlet.this.requestHandler.enqueueRequest(gwr, wrp.clientId);
					System.out.println(" - request started");
				}
				catch (IOException ioe) {
					System.out.println("Error starting request " + wrp.id + ": " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
				
				//	remove request from waiting queue (running, or data lost, or service unavailable)
				synchronized (GoldenGateWebServiceServlet.this.waitingRequestQueue) {
					GoldenGateWebServiceServlet.this.waitingRequestQueue.dequeue();
					System.out.println(" - request removed from queue");
				}
			}
		}
		public void start() {
			synchronized (this) {
				super.start();
				try {
					this.wait();
				} catch (InterruptedException ie) {}
			}
		}
		public void shutdown() {
			synchronized (GoldenGateWebServiceServlet.this.waitingRequestQueue) {
				this.run = false;
				GoldenGateWebServiceServlet.this.waitingRequestQueue.notify();
				try {
					GoldenGateWebServiceServlet.this.waitingRequestQueue.wait();
				} catch (InterruptedException ie) {}
			}
		}
	}
	
	private static class WaitingRequestQueue {
		private RequestProxyMap index = new RequestProxyMap();
		private LinkedList queue = new LinkedList();
		int addCount = 0;
		int removeCount = 0;
		void enqueue(WaitingRequestProxy rp) {
			this.index.put(rp);
			this.queue.addLast(rp);
			rp.position = this.addCount++;
		}
		WaitingRequestProxy getNext() {
			WaitingRequestProxy wrp = ((WaitingRequestProxy) this.queue.getFirst());
			if (this.index.containsKey(wrp.id))
				return wrp;
			//	not in index ==> removed before, recurse
			this.dequeue();
			return this.getNext();
		}
		void dequeue() {
			WaitingRequestProxy wrp = ((WaitingRequestProxy) this.queue.removeFirst());
			if (this.index.remove(wrp.id) != null)
				this.removeCount++;
		}
		int size() {
			return this.index.size();
		}
//		boolean contains(String id) {
//			return this.index.containsKey(id);
//		}
		int getPosition(String id) {
			WaitingRequestProxy rp = this.get(id);
			return ((rp == null) ? -1 : (rp.position - this.removeCount));
		}
		WaitingRequestProxy get(String id) {
			return ((WaitingRequestProxy) this.index.get(id));
		}
		WaitingRequestProxy remove(String id) {
			WaitingRequestProxy rp = ((WaitingRequestProxy) this.index.remove(id));
			if (rp != null) // leave it in queue, to be sorted out by dequeue() ==> saves searching
				this.removeCount++;
			return rp;
		}
		String[] getRequestIDs(String clientId) {
			return this.index.getRequestIDs(clientId);
		}
	}
	
	private static class WaitingRequestProxy extends RequestProxy {
		long time;
		int size;
		String functionName;
		Properties params;
		File dataFile;
		File paramFile;
		int position;
		WaitingRequestProxy(String id, String clientId, long time, int size, String functionName, Properties params, File dataFile, File paramFile) {
			super(id, clientId);
			this.time = time;
			this.size = size;
			this.functionName = functionName;
			this.name = ("Invokation of " + this.functionName + " on " + this.size + " tokens of data, enqueued " + TIMESTAMP_DATE_FORMAT.format(new Date(this.time)));
			this.params = params;
			this.dataFile = dataFile;
			this.paramFile = paramFile;
		}
		void sendStatus(HttpServletRequest request, HttpServletResponse response, int position) throws IOException {
			
			//	prepare output
			response.setContentType("text/xml");
			response.setCharacterEncoding(ENCODING);
			Writer out = new OutputStreamWriter(response.getOutputStream(), ENCODING);
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			//	write XML status message
			this.writeStatus(request, bw, position);
			
			//	send data
			bw.flush();
			out.flush();
		}
		void writeStatus(HttpServletRequest request, BufferedLineWriter bw, int position) throws IOException {
			bw.writeLine("<status id=\"" + this.id + "\"" + ((this.name == null) ? "" : (" name=\"" + AnnotationUtils.escapeForXml(this.name, true) + "\"")) + " state=\"" + AnnotationUtils.escapeForXml("Waiting") + "\" stateDetail=\"" + AnnotationUtils.escapeForXml("Cached and waiting to be processed, on position " + position + " in queue.") + "\" percentFinished=\"" + 0 + "\">");
			bw.writeLine("<callback type=\"status\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + AsynchronousRequestHandler.STATUS_UPDATE_ACTION + "</callback>");
			bw.writeLine("<callback type=\"cancel\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + AsynchronousRequestHandler.CANCEL_ACTION + "</callback>");
			bw.writeLine("</status>");
		}
	}
	
	private class FinishedRequestHandler extends Thread {
		private boolean run = true;
		public void run() {
			
			//	let starting thread continue
			synchronized (this) {
				this.notify();
			}
			
			//	working off queue
			while (this.run) {
				
				//	check queue and wait
				FinishedRequestProxy frp = null;
				synchronized (GoldenGateWebServiceServlet.this.finishedRequestQueue) {
					try {
						GoldenGateWebServiceServlet.this.finishedRequestQueue.wait(1000);
					} catch (InterruptedException ie) {}
					
					//	we're being shut down
					if (!this.run) {
						GoldenGateWebServiceServlet.this.finishedRequestQueue.notify();
						return;
					}
					
					//	try to get next waiting request
					if (GoldenGateWebServiceServlet.this.finishedRequestQueue.size() != 0)
						frp = ((FinishedRequestProxy) GoldenGateWebServiceServlet.this.finishedRequestQueue.removeFirst());
				}
				
				//	nothing to work with
				if (frp == null)
					continue;
				
				//	store meta data
				Settings result = new Settings();
				result.setSetting(REQUEST_ID_PARAMETER, frp.id);
				if (frp.request.clientId != null)
					result.setSetting(CLIENT_ID_PARAMETER, frp.request.clientId);
				result.setSetting(FUNCTION_NAME_PARAMETER, frp.request.ws.getName());
				result.setSetting(REQUEST_TIME_PARAMETER, ("" + frp.request.getFinishTime()));
				result.setSetting(REQUEST_SIZE_PARAMETER, ("" + frp.request.data.size()));
				if (frp.request.name != null)
					result.setSetting(REQUEST_NAME_PARAMETER, frp.request.name);
				result.setSetting(HAS_ERROR_PARAMETER, (frp.request.hasError() ? "true" : "false"));
				try {
					result.storeAsText(frp.resultFile);
				}
				catch (IOException ioe) {
					System.out.println("Error writing result of request '" + frp.id + "' to disc cache:");
					ioe.printStackTrace(System.out);
					continue;
				}
				
				//	store error log or result
				try {
					FileOutputStream fos = new FileOutputStream(frp.dataFile);
					if (frp.hasError)
						frp.request.writeError(fos);
					else frp.request.writeResult(fos);
					fos.close();
				}
				catch (IOException ioe) {
					System.out.println("Error writing result data of request '" + frp.id + "' to disc cache:");
					ioe.printStackTrace(System.out);
					continue;
				}
				
				//	set actual request to null to facilitate garbage collection
				synchronized (GoldenGateWebServiceServlet.this.finishedRequestIndex) {
					frp.request = null;
				}
				
				//	clean up cached request data
				File dataFile = new File(GoldenGateWebServiceServlet.this.requestCacheFolder, (frp.id + DATA_FILE_SUFFIX));
				dataFile.delete();
				File paramFile = new File(GoldenGateWebServiceServlet.this.requestCacheFolder, (frp.id + REQUEST_FILE_SUFFIX));
				paramFile.delete();
			}
		}
		public void start() {
			synchronized (this) {
				super.start();
				try {
					this.wait();
				} catch (InterruptedException ie) {}
			}
		}
		public void shutdown() {
			synchronized (GoldenGateWebServiceServlet.this.finishedRequestQueue) {
				this.run = false;
				GoldenGateWebServiceServlet.this.finishedRequestQueue.notify();
				try {
					GoldenGateWebServiceServlet.this.finishedRequestQueue.wait();
				} catch (InterruptedException ie) {}
			}
		}
	}
	
	private class FinishedRequestProxy extends RequestProxy {
		File resultFile;
		File dataFile;
		boolean hasError;
		String functionName;
		GgWsRequest request;
		boolean resultDataLoaded;
		FinishedRequestProxy(String id, String clientId, File resultFile) {
			super(id, clientId);
			this.resultFile = resultFile;
			this.dataFile = new File(GoldenGateWebServiceServlet.this.resultCacheFolder, (id + DATA_FILE_SUFFIX));
			this.request = null;
			this.resultDataLoaded = false;
		}
		FinishedRequestProxy(GgWsRequest request) {
			super(request.id, request.clientId, request.name);
			this.resultFile = new File(GoldenGateWebServiceServlet.this.resultCacheFolder, (request.id + ((request.clientId == null) ? "" : ("." + request.clientId)) + RESULT_FILE_SUFFIX));
			this.dataFile = new File(GoldenGateWebServiceServlet.this.resultCacheFolder, (request.id + DATA_FILE_SUFFIX));
			this.hasError = request.hasError();
			this.functionName = request.ws.getName();
			this.request = request;
			this.resultDataLoaded = true;
		}
		synchronized void ensureResultDataLoaded() throws IOException {
			if (this.resultDataLoaded)
				return;
			Settings result = Settings.loadSettings(this.resultFile);
			this.name = result.getSetting(REQUEST_NAME_PARAMETER);
			this.hasError = "true".equals(result.getSetting(HAS_ERROR_PARAMETER));
			this.functionName = result.getSetting(FUNCTION_NAME_PARAMETER);
			this.resultDataLoaded = true;
		}
		void sendStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
			GgWsRequest gwr = this.request;
			if (gwr != null) {
				gwr.sendStatusUpdate(request, response);
				return;
			}
			
			//	prepare output
			response.setContentType("text/xml");
			response.setCharacterEncoding(ENCODING);
			Writer out = new OutputStreamWriter(response.getOutputStream(), ENCODING);
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			//	write XML status message
			this.writeStatus(request, bw);
			
			//	send data
			bw.flush();
			out.flush();
		}
		void writeStatus(HttpServletRequest request, BufferedLineWriter bw) throws IOException {
			this.ensureResultDataLoaded();
			bw.writeLine("<status id=\"" + this.id + "\"" + ((this.name == null) ? "" : (" name=\"" + AnnotationUtils.escapeForXml(this.name, true) + "\"")) + " state=\"" + AnnotationUtils.escapeForXml("Finished") + "\" stateDetail=\"" + AnnotationUtils.escapeForXml(this.hasError ? "Terminated with error" : "Processing completed succeccfully") + "\" percentFinished=\"" + 0 + "\">");
			bw.writeLine("<callback type=\"status\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + AsynchronousRequestHandler.STATUS_UPDATE_ACTION + "</callback>");
			if (this.hasError)
				bw.writeLine("<callback type=\"error\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + AsynchronousRequestHandler.ERRORS_ACTION + "</callback>");
			else bw.writeLine("<callback type=\"result\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + AsynchronousRequestHandler.RESULT_ACTION + "</callback>");
			bw.writeLine("</status>");
		}
		void sendResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
			GgWsRequest gwr = this.request;
			if (gwr != null) {
				if (!gwr.sendResult(request, response))
					response.sendError(HttpServletResponse.SC_NOT_FOUND, ("No result available for request " + this.id + ", see error report."));
				return;
			}
			
			//	load cached result data on demand
			this.ensureResultDataLoaded();
			
			//	we don't have a result
			if (this.hasError)
				response.sendError(HttpServletResponse.SC_NOT_FOUND, ("No result available for request " + this.id + ", see error report."));
			
			else {
				FileInputStream dataIn = new FileInputStream(this.dataFile);
				MutableAnnotation data = GenericGamtaXML.readDocument(dataIn);
				dataIn.close();
				WebService ws = ((this.functionName == null) ? null : GoldenGateWebServiceServlet.this.getWebService(this.functionName));
				response.setCharacterEncoding(ENCODING);
				response.setContentType("text/xml");
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), ENCODING));
				if (ws == null)
					AnnotationUtils.writeXML(data, bw);
				else ws.writeResult(data, request.getParameter(DATA_FORMAT_PARAMETER), bw);
				bw.flush();
			}
		}
		void sendErrorReport(HttpServletRequest request, HttpServletResponse response) throws IOException {
			GgWsRequest gwr = this.request;
			if (gwr != null) {
				if (!gwr.sendErrorReport(request, response))
					response.sendError(HttpServletResponse.SC_NOT_FOUND, ("No error report available for request " + this.id + ", get result."));
				return;
			}
			
			//	load cached result data on demand
			this.ensureResultDataLoaded();
			
			//	load error report from disc
			if (this.hasError) {
				response.setContentType("text/plain");
				response.setCharacterEncoding(ENCODING);
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(this.dataFile), ENCODING));
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), ENCODING));
				for (String el; ((el = br.readLine()) != null);) {
					bw.write(el);
					bw.newLine();
				}
				bw.flush();
				br.close();
			}
			
			//	no errors to report
			else response.sendError(HttpServletResponse.SC_NOT_FOUND, ("No error report available for request " + this.id + ", get result."));
		}
	}
}