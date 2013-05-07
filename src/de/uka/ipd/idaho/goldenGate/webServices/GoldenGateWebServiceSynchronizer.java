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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uka.ipd.idaho.easyIO.web.WebServlet;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * Synchronous web frontend for GoldenGATE web services.
 * 
 * @author sautter
 */
public class GoldenGateWebServiceSynchronizer extends WebServlet implements GoldenGateWebServiceConstants {
	
	private String webServiceAddress = null;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#doInit()
	 */
	protected void doInit() throws ServletException {
		
		//	get URL of actual web service servlet
		String webServiceAddress = this.getSetting("webServiceAddress");
		if (webServiceAddress == null)
			throw new ServletException("URL of web service servlet missing.");
		try {
			new URL(webServiceAddress);
		}
		catch (MalformedURLException mue) {
			throw new ServletException("URL of web service servlet invalid: " + webServiceAddress);
		}
		this.webServiceAddress = webServiceAddress;
	}
	
	private Html html = new Html();
	private Parser htmlParser = new Parser(this.html);
	
	private StandardGrammar xml = new StandardGrammar();
	private Parser xmlParser = new Parser(this.xml);
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//	check what client wants
		String pathInfo = request.getPathInfo();
		String actionOrRequestId = LIST_FUNCTIONS_COMMAND;
		if ((pathInfo != null) && !"/".equals(pathInfo) && (pathInfo.length() != 0)) {
			while (pathInfo.startsWith("/"))
				pathInfo = pathInfo.substring(1);
			if (pathInfo.indexOf('/') == -1)
				actionOrRequestId = pathInfo;
			else actionOrRequestId = pathInfo.substring(0, pathInfo.indexOf('/'));
		}
		
		//	catch data URL callback invokations
		if (INVOKE_FUNCTION_COMMAND.equals(actionOrRequestId))
			this.doWsRequest(request, response, "GET");
		
		//	TODO alter URL of test form to point to this servlet
		else if ("test".equals(actionOrRequestId)) {
			this.doTestForm(request, response, "GET");
		}
		
		//	simply loop through the rest
		else this.doProxyRequest(request, response, "GET");
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doWsRequest(request, response, "POST");
	}
	
	private void doTestForm(final HttpServletRequest request, HttpServletResponse response, String method) throws ServletException, IOException {
		
		//	build target address & connect
		String pathInfo = request.getPathInfo();
		String queryString = request.getQueryString();
		URL wsUrl = new URL(this.webServiceAddress + ((pathInfo == null) ? "" : pathInfo) + ((queryString == null) ? "" : ("?" + queryString)));
		HttpURLConnection wsCon = ((HttpURLConnection) wsUrl.openConnection());
		wsCon.setDoOutput(true);
		wsCon.setDoInput(true);
		
		//	loop through request headers
		wsCon.setRequestMethod(method);
		for (Enumeration hns = request.getHeaderNames(); hns.hasMoreElements();) {
			String hn = ((String) hns.nextElement());
			wsCon.setRequestProperty(hn, request.getHeader(hn));
		}
		
		//	request response
		InputStream wsIn = wsCon.getInputStream();
		
		//	loop through reponse headers
		Map wsResHeaders = wsCon.getHeaderFields();
		for (Iterator hns = wsResHeaders.keySet().iterator(); hns.hasNext();) {
			String hn = ((String) hns.next());
			response.setHeader(hn, wsCon.getHeaderField(hn));
		}
		response.setCharacterEncoding(wsCon.getContentEncoding());
		response.setContentType(wsCon.getContentType());
		
		//	send test from page
		Reader wsR = new BufferedReader(new InputStreamReader(wsIn, ENCODING));
		final Writer respW = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), ENCODING));
		this.htmlParser.stream(wsR, new TokenReceiver() {
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token) && "form".equalsIgnoreCase(html.getType(token)) && !html.isEndTag(token)) {
					TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
					tnas.setAttribute("action", (request.getContextPath() + request.getServletPath() + "/" + INVOKE_FUNCTION_COMMAND));
					token = ("<form " + tnas.getAttributeValueString(html) + ">");
				}
				respW.write(token);
			}
		});
		
		//	we're done here
		respW.flush();
		wsR.close();
	}
	
	private void doProxyRequest(HttpServletRequest request, HttpServletResponse response, String method) throws ServletException, IOException {
		
		//	build target address & connect
		String pathInfo = request.getPathInfo();
		String queryString = request.getQueryString();
		URL wsUrl = new URL(this.webServiceAddress + ((pathInfo == null) ? "" : pathInfo) + ((queryString == null) ? "" : ("?" + queryString)));
		HttpURLConnection wsCon = ((HttpURLConnection) wsUrl.openConnection());
		wsCon.setDoOutput(true);
		wsCon.setDoInput(true);
		
		//	loop through request headers
		wsCon.setRequestMethod(method);
		for (Enumeration hns = request.getHeaderNames(); hns.hasMoreElements();) {
			String hn = ((String) hns.nextElement());
			wsCon.setRequestProperty(hn, request.getHeader(hn));
		}
		
		//	request response
		InputStream wsIn = wsCon.getInputStream();
		
		//	loop through reponse headers
		Map wsResHeaders = wsCon.getHeaderFields();
		for (Iterator hns = wsResHeaders.keySet().iterator(); hns.hasNext();) {
			String hn = ((String) hns.next());
			response.setHeader(hn, wsCon.getHeaderField(hn));
		}
		response.setCharacterEncoding(wsCon.getContentEncoding());
		response.setContentType(wsCon.getContentType());
		
		//	loop through response content
		OutputStream respOut = response.getOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = wsIn.read(buffer, 0, buffer.length)) != -1)
			respOut.write(buffer, 0, read);
		
		//	we're done here
		respOut.flush();
		wsIn.close();
	}
	
	private void doWsRequest(HttpServletRequest request, HttpServletResponse response, String method) throws ServletException, IOException {
		
		//	build target address & connect
		String pathInfo = request.getPathInfo();
		String queryString = request.getQueryString();
		URL wsUrl = new URL(this.webServiceAddress + ((pathInfo == null) ? "" : pathInfo) + ((queryString == null) ? "" : ("?" + queryString)));
		HttpURLConnection wsCon = ((HttpURLConnection) wsUrl.openConnection());
		wsCon.setDoOutput(true);
		wsCon.setDoInput(true);
		
		//	loop through request headers
		wsCon.setRequestMethod(method);
		for (Enumeration hns = request.getHeaderNames(); hns.hasMoreElements();) {
			String hn = ((String) hns.nextElement());
			wsCon.setRequestProperty(hn, request.getHeader(hn));
		}
		
		//	loop through data
		OutputStream wsOut = new BufferedOutputStream(wsCon.getOutputStream());
		InputStream reqIn = request.getInputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = reqIn.read(buffer, 0, buffer.length)) != -1)
			wsOut.write(buffer, 0, read);
		wsOut.flush();
		wsOut.close();
		
		//	keep polling until there is a result or error
		BufferedReader wsIn = new BufferedReader(new InputStreamReader(wsCon.getInputStream(), ENCODING));
		final String[] status = {null};
		final String[] feedback = {null};
		final String[] cancelFeedback = {null};
		final String[] result = {null};
		final String[] error = {null};
		while (true) {
			
			//	connect to last stautus update URL if not reading initial invokation response
			if (wsIn == null)
				wsIn = new BufferedReader(new InputStreamReader((new URL(wsUrl.getProtocol(), wsUrl.getHost(), wsUrl.getPort(), status[0])).openStream(), ENCODING));
			
			//	clear registers
			status[0] = null;
			feedback[0] = null;
			cancelFeedback[0] = null;
			result[0] = null;
			error[0] = null;
			
			//	read status update
			this.xmlParser.stream(wsIn, new TokenReceiver() {
				private String type = null;
				public void close() throws IOException {}
				public void storeToken(String token, int treeDepth) throws IOException {
					if (xml.isTag(token)) {
						if (!"callback".equals(xml.getType(token)))
							return;
						if (xml.isEndTag(token))
							this.type = null;
						else {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
							this.type = tnas.getAttribute("type");
						}
					}
					else if (this.type != null) {
						if ("status".equals(this.type))
							status[0] = token.trim();
						else if ("result".equals(this.type))
							result[0] = token.trim();
						else if ("error".equals(this.type))
							error[0] = token.trim();
						else if ("cancelFeedback".equals(this.type))
							cancelFeedback[0] = token.trim();
						else if (this.type.indexOf("Feedback") != -1)
							feedback[0] = token.trim();
					}
				}
			});
			
			//	we're done with this response
			wsIn.close();
			wsIn = null;
			
			//	we have a result ==> send it to client
			if (result[0] != null) {
				
				//	connect to get headers and loop them through
				URL wsResUrl = new URL(wsUrl.getProtocol(), wsUrl.getHost(), wsUrl.getPort(), result[0]);
				HttpURLConnection wsResCon = ((HttpURLConnection) wsResUrl.openConnection());
				wsResCon.setRequestMethod("GET");
				wsResCon.setDoInput(true);
				InputStream wsResIn = wsResCon.getInputStream();
				Map wsResHeaders = wsResCon.getHeaderFields();
				for (Iterator hns = wsResHeaders.keySet().iterator(); hns.hasNext();) {
					String hn = ((String) hns.next());
					response.setHeader(hn, wsResCon.getHeaderField(hn));
				}
				response.setCharacterEncoding(wsResCon.getContentEncoding());
				response.setContentType(wsResCon.getContentType());
				
				//	loop through content
				OutputStream respOut = response.getOutputStream();
				while ((read = wsResIn.read(buffer, 0, buffer.length)) != -1)
					respOut.write(buffer, 0, read);
				respOut.flush();
				respOut.close();
				
				//	we're done here
				wsResIn.close();
				return;
			}
			
			//	we have an error ==> notify client
			if (error[0] != null) {
				
				//	connect to get headers and loop them through
				URL wsErrUrl = new URL(wsUrl.getProtocol(), wsUrl.getHost(), wsUrl.getPort(), error[0]);
				HttpURLConnection wsErrCon = ((HttpURLConnection) wsErrUrl.openConnection());
				wsErrCon.setRequestMethod("GET");
				wsCon.setDoInput(true);
				InputStream wsErrIn = wsErrCon.getInputStream();
				Map wsErrHeaders = wsErrCon.getHeaderFields();
				for (Iterator hns = wsErrHeaders.keySet().iterator(); hns.hasNext();) {
					String hn = ((String) hns.next());
					response.setHeader(hn, wsErrCon.getHeaderField(hn));
				}
				response.setCharacterEncoding(wsErrCon.getContentEncoding());
				response.setContentType(wsErrCon.getContentType());
				
				//	loop through content
				OutputStream respOut = response.getOutputStream();
				while ((read = wsErrIn.read(buffer, 0, buffer.length)) != -1)
					respOut.write(buffer, 0, read);
				respOut.flush();
				respOut.close();
				
				//	we're done here
				wsErrIn.close();
				return;
			}
			
			//	we have a pending feedback request ==> cancel it, and throw error
			if (feedback[0] != null) {
				
				//	cancel feedback request if possible
				if (cancelFeedback[0] != null) {
					URL wsCfbUrl = new URL(wsUrl.getProtocol(), wsUrl.getHost(), wsUrl.getPort(), result[0]);
					HttpURLConnection wsCfbCon = ((HttpURLConnection) wsCfbUrl.openConnection());
					wsCon.setDoInput(true);
					wsCfbCon.setRequestMethod("GET");
					InputStream wsCfbIn = wsCfbCon.getInputStream();
					while ((read = wsCfbIn.read(buffer, 0, buffer.length)) != -1) {}
					wsCfbIn.close();
				}
				
				//	send error
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot work interactively in synchronous mode");
				return;
			}
			
			//	we don't have a callback for the status ==> something is utterly wrong, little we can do
			if (status[0] == null) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lost contact to processing engine");
				return;
			}
			
			//	wait a little
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ie) {}
		}
	}
}