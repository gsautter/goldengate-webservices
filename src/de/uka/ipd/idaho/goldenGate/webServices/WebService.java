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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;

/**
 * @author sautter
 */
public class WebService implements GoldenGateWebServiceConstants, Resource {
	
	private String name;
	
	private TreeMap inputFormats = new TreeMap();
	private String defaultInputFormat = null;
	
	private TreeMap outputFormats = new TreeMap();
	private String defaultOutputFormat = null;
	
	private DocumentProcessor[] processors;
	
	private String label;
	private String description;
	
	private String interactivity;
	private boolean defaultInteractive;
	
	WebService(String name, DocumentProcessor[] processors, String interactivity, boolean defaultInteractive, Map inputFormats, String defaultInputFormat, Map outputFormats, String defaultOutputFormat, String label, String description) {
		this.name = name;
		this.processors = processors;
		this.interactivity = interactivity;
		this.defaultInteractive = (this.isDefaultInteractive() || defaultInteractive);
		this.inputFormats.putAll(inputFormats);
		this.defaultInputFormat = defaultInputFormat;
		this.outputFormats.putAll(outputFormats);
		this.defaultOutputFormat = defaultOutputFormat;
		this.label = label;
		this.description = description;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
	 */
	public String getName() {
		return this.name;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
	 */
	public String getProviderClassName() {
		return WebServiceManager.class.getName();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getTypeLabel()
	 */
	public String getTypeLabel() {
		return "Web Service";
	}
	
	/**
	 * Run the web service on a document, more specifically, the embedded
	 * document processor.
	 * @param data the MutableAnnotation to process
	 * @param parameters gives processing parameters
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	check interactivity
		String interactivityParam = parameters.getProperty(GoldenGateWebServiceConstants.INTERACTIVE_PARAMETER);
		if ((interactivityParam == null) || INTERACTIVE_DEFAULT.equals(interactivityParam)) {
			Properties params = new Properties();
			params.putAll(parameters);
			if (this.isDefaultInteractive())
				params.setProperty(Resource.INTERACTIVE_PARAMETER, Resource.INTERACTIVE_PARAMETER);
			else params.remove(Resource.INTERACTIVE_PARAMETER);
			parameters = params;
		}
		else if (INTERACTIVE_YES.equals(interactivityParam)) {
			if (INTERACTIVE_NEVER.equals(this.interactivity))
				throw new RuntimeException("Invalid interactivity setting, " + this.name + " is never interactive.");
			else {
				Properties params = new Properties();
				params.putAll(parameters);
				params.setProperty(Resource.INTERACTIVE_PARAMETER, Resource.INTERACTIVE_PARAMETER);
				parameters = params;
			}
		}
		else if (INTERACTIVE_NO.equals(interactivityParam)) {
			if (INTERACTIVE_ALWAYS.equals(this.interactivity))
				throw new RuntimeException("Invalid interactivity setting, " + this.name + " is always interactive.");
			else {
				Properties params = new Properties();
				params.putAll(parameters);
				params.remove(Resource.INTERACTIVE_PARAMETER);
				parameters = params;
			}
		}
		else throw new RuntimeException("Invalid interactivity setting, specify 'yes' or 'no', or nothing at all.");
		
		//	process data
		for (int p = 0; p < this.processors.length; p++)
			this.processors[p].process(data, parameters);
	}
	
	/**
	 * Write an XML description of the web service to some writer.
	 * @param out the WRiter to write to
	 */
	public void writeXmlDescription(Writer out) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		bw.write("<" + SERVICE_NODE_TYPE + " " + NAME_ATTRIBUTE + "=\"" + this.name + "\" " + LABEL_ATTRIBUTE + "=\"" + AnnotationUtils.escapeForXml(this.label, true) + "\" " + INTERACTIVE_ATTRIBUTE + "=\"" + this.interactivity + "\" " + DEFAULT_INTERACTIVE_ATTRIBUTE + "=\"" + (this.isDefaultInteractive() ? INTERACTIVE_YES : INTERACTIVE_NO) + "\">");
		bw.newLine();
		for (Iterator idfit = this.inputFormats.keySet().iterator(); idfit.hasNext();) {
			String idfa = ((String) idfit.next());
			DocumentFormat idf = ((DocumentFormat) this.inputFormats.get(idfa));
			bw.write("<" + INPUT_FORMAT_NODE_TYPE + " " + NAME_ATTRIBUTE + "=\"" + AnnotationUtils.escapeForXml(idfa) + "\" " + LABEL_ATTRIBUTE + "=\"" + AnnotationUtils.escapeForXml(idf.getDescription()) + "\"" + (idfa.equals(this.defaultInputFormat) ? " " + IS_DEFAULT_DATA_FORMAT_ATTRIBUTE + "=\"true\"" : "") + "/>");
			bw.newLine();
		}
		for (Iterator odfit = this.outputFormats.keySet().iterator(); odfit.hasNext();) {
			String odfa = ((String) odfit.next());
			DocumentFormat odf = ((DocumentFormat) this.outputFormats.get(odfa));
			bw.write("<" + OUTPUT_FORMAT_NODE_TYPE + " " + NAME_ATTRIBUTE + "=\"" + AnnotationUtils.escapeForXml(odfa) + "\" " + LABEL_ATTRIBUTE + "=\"" + AnnotationUtils.escapeForXml(odf.getDescription()) + "\"" + (odfa.equals(this.defaultOutputFormat) ? " " + IS_DEFAULT_DATA_FORMAT_ATTRIBUTE + "=\"true\"" : "") + "/>");
			bw.newLine();
		}
		bw.write("<" + DESCRIPTION_NODE_TYPE + ">" + AnnotationUtils.escapeForXml(this.description, true) + "</" + DESCRIPTION_NODE_TYPE + ">");
		bw.newLine();
		bw.write("</" + SERVICE_NODE_TYPE + ">");
		bw.newLine();
		if (bw != out)
			bw.flush();
	}

	/**
	 * @return the interactivity of the web service, one of 'always',
	 *         'optional', and 'never'
	 */
	public String getInteractivity() {
		return this.interactivity;
	}

	/**
	 * @return true if the web service is interactive unless specified
	 *         otherwise, false in the opposite case
	 */
	public boolean isDefaultInteractive() {
		if (INTERACTIVE_NEVER.equals(this.interactivity))
			return false;
		else if (INTERACTIVE_ALWAYS.equals(this.interactivity))
			return true;
		else return this.defaultInteractive;
	}
	
	/**
	 * @return the name of the default input format for the web service
	 */
	public String getDefaultInputFormatName() {
		return this.defaultInputFormat;
	}
	
	/**
	 * @return an array holding the names of the input formats the web service
	 *         unterstands
	 */
	public String[] getInputFormatNames() {
		return ((String[]) this.inputFormats.keySet().toArray(new String[this.inputFormats.size()]));
	}
	
	/**
	 * @param format the name of the input format to obtain a label for
	 * @return a label (nice name) for the input format with the argument name
	 */
	public String getInputFormatLabel(String format) {
		try {
			return this.getDocumentFormat(format, true).getDescription();
		}
		catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * @return the name of the default output format for the web service
	 */
	public String getDefaultOutputFormatName() {
		return this.defaultOutputFormat;
	}
	
	/**
	 * @return an array holding the names of the output formats the web service
	 *         can return its results in
	 */
	public String[] getOutputFormatNames() {
		return ((String[]) this.outputFormats.keySet().toArray(new String[this.outputFormats.size()]));
	}
	
	/**
	 * @param format the name of the input format to obtain a label for
	 * @return a label (nice name) for the input format with the argument name
	 */
	public String getOutputFormatLabel(String format) {
		try {
			return this.getDocumentFormat(format, false).getDescription();
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private DocumentFormat getDocumentFormat(String format, boolean input) throws IOException {
		if (format == null)
			format = (input ? this.defaultInputFormat : this.defaultOutputFormat);
		DocumentFormat df = ((DocumentFormat) (input ? this.inputFormats.get(format) : this.outputFormats.get(format)));
		if (df == null)
			throw new IOException("Invalid format: " + format);
		return df;
	}
	
	/**
	 * Instantiate a document from some input stream, using a specific document
	 * format for decoding it. The specified format name has to be one of the
	 * ones returned by the getInputFormatNames() method. Specifying null
	 * results in the default format being used.
	 * @param format the name of the document format to use
	 * @param in the input stream to read from
	 * @return a document representing the data from the specified input stream
	 * @throws IOException
	 */
	public MutableAnnotation readDocument(String format, InputStream in) throws IOException {
		DocumentFormat df = this.getDocumentFormat(format, true);
		return df.loadDocument(in);
	}
	
	/**
	 * Instantiate a document from some reader, using a specific document format
	 * for decoding it. The specified format name has to be one of the ones
	 * returned by the getInputFormatNames() method. Specifying null results in
	 * the default format being used.
	 * @param format the name of the document format to use
	 * @param in the reader to read from
	 * @return a document representing the data from the specified reader
	 * @throws IOException
	 */
	public MutableAnnotation readDocument(String format, Reader in) throws IOException {
		DocumentFormat df = this.getDocumentFormat(format, true);
		return df.loadDocument(in);
	}
	
	/**
	 * Write the result of a web service invokation to some output stream, using
	 * a specific document format. The specified format name has to be one of
	 * the ones returned by the getOutputFormatNames() method. Specifying null
	 * results in the default format being used.
	 * @param data the document to write
	 * @param format the name of the document format to use
	 * @param out the output stream to write to
	 * @return true if the document was written successfully, false otherwise
	 * @throws IOException
	 */
	public boolean writeResult(MutableAnnotation data, String format, OutputStream out) throws IOException {
		DocumentFormat df = this.getDocumentFormat(format, false);
		return df.saveDocument(data, out);
	}
	
	/**
	 * Write the result of a web service invokation to some writer, using a
	 * specific document format. The specified format name has to be one of the
	 * ones returned by the getOutputFormatNames() method. Specifying null
	 * results in the default format being used.
	 * @param data the document to write
	 * @param format the name of the document format to use
	 * @param out the writer to write to
	 * @return true if the document was written successfully, false otherwise
	 * @throws IOException
	 */
	public boolean writeResult(MutableAnnotation data, String format, Writer out) throws IOException {
		DocumentFormat df = this.getDocumentFormat(format, false);
		return df.saveDocument(data, out);
	}
}