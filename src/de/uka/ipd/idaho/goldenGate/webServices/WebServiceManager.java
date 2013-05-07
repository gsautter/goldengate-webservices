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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceManager;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class WebServiceManager extends AbstractResourceManager {
	
	private static final String FILE_EXTENSION = ".webService";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Web Service";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Web Services";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		if (!this.dataProvider.isDataEditable())
			return new JMenuItem[0];
		
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Create");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createWebService();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editWebServices();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		StringVector names = new StringVector();
		names.addContentIgnoreDuplicates(super.getDataNamesForResource(name));
		
		WebServiceInternal webService = this.getWebServiceInternal(name);
		if (webService == null) return names.toStringArray();
		
		DocumentProcessor[] parts = webService.getProcessors();
		for (int p = 0; p < parts.length; p++) {
			DocumentProcessorManager dpm = this.parent.getDocumentProcessorProvider(parts[p].getProviderClassName());
			if (dpm != null)
				names.addContentIgnoreDuplicates(dpm.getDataNamesForResource(parts[p].getName()));
		}
		
		for (Iterator idfit = webService.inputFormats.values().iterator(); idfit.hasNext();) {
			DocumentFormat idf = ((DocumentFormat) idfit.next());
			DocumentFormatProvider dfp = this.parent.getDocumentFormatProvider(idf.getProviderClassName());
			if (dfp != null)
				names.addContentIgnoreDuplicates(dfp.getDataNamesForResource(idf.getName()));
		}
		
		for (Iterator odfit = webService.outputFormats.values().iterator(); odfit.hasNext();) {
			DocumentFormat odf = ((DocumentFormat) odfit.next());
			DocumentFormatProvider dfp = this.parent.getDocumentFormatProvider(odf.getProviderClassName());
			if (dfp != null)
				names.addContentIgnoreDuplicates(dfp.getDataNamesForResource(odf.getName()));
		}
		
		return names.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		WebServiceInternal webService = this.getWebServiceInternal(name);
		if (webService == null) return new String[0];
		
		StringVector nameCollector = new StringVector();
		
		DocumentProcessor[] processors = webService.getProcessors();
		for (int p = 0; p < processors.length; p++)
			nameCollector.addElementIgnoreDuplicates(processors[p].getName() + "@" + processors[p].getProviderClassName());
		
		for (Iterator idfit = webService.inputFormats.values().iterator(); idfit.hasNext();) {
			DocumentFormat idf = ((DocumentFormat) idfit.next());
			nameCollector.addElementIgnoreDuplicates(idf.getName() + "@" + idf.getProviderClassName());
		}
		
		for (Iterator odfit = webService.outputFormats.values().iterator(); odfit.hasNext();) {
			DocumentFormat odf = ((DocumentFormat) odfit.next());
			nameCollector.addElementIgnoreDuplicates(odf.getName() + "@" + odf.getProviderClassName());
		}
		
		int nameIndex = 0;
		while (recourse && (nameIndex < nameCollector.size())) {
			String resName = nameCollector.get(nameIndex);
			int split = resName.indexOf('@');
			if (split != -1) {
				String plainResName = resName.substring(0, split);
				String resProviderClassName = resName.substring(split + 1);
				
				ResourceManager rm = this.parent.getResourceProvider(resProviderClassName);
				if (rm != null)
					nameCollector.addContentIgnoreDuplicates(rm.getRequiredResourceNames(plainResName, recourse));
			}
			nameIndex++;
		}
		
		return nameCollector.toStringArray();
	}
	
	private class WebServiceInternal {
		static final String DEFAULT_INTERACTIVE_ATTRIBUTE = "DEFAULT_INTERACTIVE";
		static final String PROCESSOR_SUBSET_PREFIX = "PROCESSOR";
		static final String INPUT_FORMAT_SUBSET_PREFIX = "INPUT_FORMAT";
		static final String OUTPUT_FORMAT_SUBSET_PREFIX = "OUTPUT_FORMAT";
		static final String DEFAULT_INPUT_FORMAT_NAME_ATTRIBUTE = "DEFAULT_INPUT_FORMAT";
		static final String DEFAULT_OUTPUT_FORMAT_NAME_ATTRIBUTE = "DEFAULT_OUTPUT_FORMAT";
		static final String LABEL_ATTRIBUTE = "LABEL";
		static final String DESCRIPTION_ATTRIBUTE = "DESCRIPTION";
		
		private Vector processors = new Vector();
		
		String interactivity = WebService.INTERACTIVE_OPTIONAL;
		boolean defaultInteractive = true;
		
		TreeMap inputFormats = new TreeMap();
		String defaultInputFormat = null;
		
		TreeMap outputFormats = new TreeMap();
		String defaultOutputFormat = null;
		
		String label;
		String description;
		
		WebServiceInternal(String interactivity, boolean defaultInteractive, String defaultInputFormat, String defaultOutputFormat, String label, String description) {
			this.interactivity = interactivity;
			this.defaultInteractive = defaultInteractive;
			this.defaultInputFormat = defaultInputFormat;
			this.defaultOutputFormat = defaultOutputFormat;
			this.label = label;
			this.description = description;
		}
		
		DocumentProcessor[] getProcessors() {
			return ((DocumentProcessor[]) this.processors.toArray(new DocumentProcessor[this.processors.size()]));
		}
		
		void addProcessor(DocumentProcessor processor) {
			this.processors.add(processor);
		}
		
		void addInputFormat(String alias, DocumentFormat format) {
			this.inputFormats.put(alias, format);
		}
		
		void addOutputFormat(String alias, DocumentFormat format) {
			this.outputFormats.put(alias, format);
		}
	}
	
	/**
	 * Retrieve a web service by its name.
	 * @param name the name of the web service
	 * @return the web service with the specified name, or null, if there is
	 *         no such web service
	 */
	public WebService getWebService(String name) {
		WebServiceInternal webService = this.getWebServiceInternal(name);
		if (webService == null)
			return null;
		return new WebService(
				name,
				webService.getProcessors(),
				webService.interactivity,
				webService.defaultInteractive,
				webService.inputFormats,
				webService.defaultInputFormat,
				webService.outputFormats,
				webService.defaultOutputFormat,
				webService.label,
				webService.description
			);
	}
	
	private String escapeDescription(String description) {
		if (description == null)
			return null;
		StringBuffer escaped = new StringBuffer();
		for (int c = 0; c < description.length(); c++) {
			char ch = description.charAt(c);
			if (ch == '\n')
				escaped.append("\\n");
			else if (ch == '\r')
				escaped.append("\\r");
			else escaped.append(ch);
		}
		return escaped.toString();
	}
	
	private String unescapeDescription(String description) {
		if (description == null)
			return null;
		StringBuffer unescaped = new StringBuffer();
		for (int c = 0; c < description.length(); c++) {
			char ch = description.charAt(c);
			if ((ch == '\\') && ((c+1) < description.length())) {
				char nCh = description.charAt(c+1);
				c++;
				if (nCh == 'n')
					unescaped.append('\n');
				else if (nCh == 'r')
					unescaped.append('\r');
				else c--;
			}
			else unescaped.append(ch);
		}
		return unescaped.toString();
	}
	
	private WebServiceInternal getWebServiceInternal(String name) {
		if (name == null)
			return null;
		return this.getWebService(this.loadSettingsResource(name));
	}
	
	private WebServiceInternal getWebService(Settings settings) {
		if (settings == null)
			return null;
		try {
			String defaultInteractive = settings.getSetting(WebServiceInternal.DEFAULT_INTERACTIVE_ATTRIBUTE);
			WebServiceInternal webService = new WebServiceInternal(
						settings.getSetting(Resource.INTERACTIVE_PARAMETER, WebService.INTERACTIVE_OPTIONAL),
						(defaultInteractive != null),
						settings.getSetting(WebServiceInternal.DEFAULT_INPUT_FORMAT_NAME_ATTRIBUTE),
						settings.getSetting(WebServiceInternal.DEFAULT_OUTPUT_FORMAT_NAME_ATTRIBUTE),
						settings.getSetting(WebServiceInternal.LABEL_ATTRIBUTE),
						this.unescapeDescription(settings.getSetting(WebServiceInternal.DESCRIPTION_ATTRIBUTE))
					);
			Settings processorSettings = settings.getSubset(WebServiceInternal.PROCESSOR_SUBSET_PREFIX);
			for (int p = 0; p < processorSettings.size(); p++) {
				String processorName = processorSettings.getSetting("P" + p);
				DocumentProcessor dp = this.parent.getDocumentProcessorForName(processorName);
				if (dp != null)
					webService.addProcessor(dp);
			}
			Settings inputFormatSettings = settings.getSubset(WebServiceInternal.INPUT_FORMAT_SUBSET_PREFIX);
			String[] inputFormatNames = inputFormatSettings.getLocalKeys();
			for (int f = 0; f < inputFormatNames.length; f++) {
				DocumentFormat inputFormat = this.parent.getDocumentFormatForName(inputFormatSettings.getSetting(inputFormatNames[f]));
				if (inputFormat != null)
					webService.addInputFormat(inputFormatNames[f], inputFormat);
			}
			Settings outputFormatSettings = settings.getSubset(WebServiceInternal.OUTPUT_FORMAT_SUBSET_PREFIX);
			String[] outputFormatNames = outputFormatSettings.getLocalKeys();
			for (int f = 0; f < outputFormatNames.length; f++) {
				DocumentFormat outputFormat = this.parent.getDocumentFormatForName(outputFormatSettings.getSetting(outputFormatNames[f]));
				if (outputFormat != null)
					webService.addOutputFormat(outputFormatNames[f], outputFormat);
			}
			return webService;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private boolean createWebService() {
		return (this.createWebService(new Settings(), null) != null);
	}
	
	private boolean cloneWebService() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createWebService();
		else {
			String name = "New " + selectedName;
			return (this.createWebService(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private DocumentFormat[] getDocumentFormats(boolean inputFormats) {
		ArrayList dfList = new ArrayList();
		DocumentFormatProvider[] dfps = this.parent.getDocumentFormatProviders();
		for (int p = 0; p < dfps.length; p++) {
			String[] dfns = (inputFormats ? dfps[p].getLoadFormatNames() : dfps[p].getSaveFormatNames());
			for (int f = 0; f < dfns.length; f++) {
				DocumentFormat df = dfps[p].getFormatForName(dfns[f]);
				if (df != null)
					dfList.add(df);
			}
		}
		return ((DocumentFormat[]) dfList.toArray(new DocumentFormat[dfList.size()]));
	}
	
	private String createWebService(Settings modelWebService, String name) {
		DocumentFormat[] inputFormats = this.getDocumentFormats(true);
		DocumentFormat[] outputFormats = this.getDocumentFormats(false);
		CreateWebServiceDialog cpd = new CreateWebServiceDialog(this.getWebService(modelWebService), name, inputFormats, outputFormats);
		cpd.setVisible(true);
		
		if (cpd.isCommitted()) {
			Settings webService = cpd.getWebService();
			String webServiceName = cpd.getWebServiceName();
			if (!webServiceName.endsWith(FILE_EXTENSION)) webServiceName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(webServiceName, webService)) {
					this.resourceNameList.refresh();
					return webServiceName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editWebServices() {
		final DocumentFormat[] inputFormats = this.getDocumentFormats(true);
		final DocumentFormat[] outputFormats = this.getDocumentFormats(false);
		final WebServiceEditorPanel[] editor = new WebServiceEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit WebServices", true);
		editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		editDialog.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				this.closeDialog();
			}
			public void windowClosing(WindowEvent we) {
				this.closeDialog();
			}
			private void closeDialog() {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].name, editor[0].getSettings());
					} catch (IOException ioe) {}
				}
				if (editDialog.isVisible()) editDialog.dispose();
			}
		});
		
		editDialog.setLayout(new BorderLayout());
		
		JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button;
		button = new JButton("Create");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createWebService();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cloneWebService();
			}
		});
		editButtons.add(button);
		button = new JButton("Delete");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (deleteResource(resourceNameList.getSelectedName()))
					resourceNameList.refresh();
			}
		});
		editButtons.add(button);
		
		editDialog.add(editButtons, BorderLayout.NORTH);
		
		final JPanel editorPanel = new JPanel(new BorderLayout());
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			Settings set = this.loadSettingsResource(selectedName);
			if (set == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new WebServiceEditorPanel(selectedName, this.getWebService(set), inputFormats, outputFormats);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].name, editor[0].getSettings());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].name + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].name);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					Settings set = loadSettingsResource(dataName);
					if (set == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new WebServiceEditorPanel(dataName, getWebService(set), inputFormats, outputFormats);
						editorPanel.add(editor[0], BorderLayout.CENTER);
					}
				}
				editorPanel.validate();
			}
		};
		this.resourceNameList.addDataListListener(dll);
		
		editDialog.setSize(DEFAULT_EDIT_DIALOG_SIZE);
		editDialog.setLocationRelativeTo(editDialog.getOwner());
		editDialog.setVisible(true);
		
		this.resourceNameList.removeDataListListener(dll);
	}
	
	private class CreateWebServiceDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private WebServiceEditorPanel editor;
		private String webServiceName = null;
		
		CreateWebServiceDialog(WebServiceInternal webService, String name, DocumentFormat[] inputFormats, DocumentFormat[] outputFormats) {
			super("Create WebService", true);
			
			this.nameField = new JTextField((name == null) ? "New WebService" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (editor.isDataValid()) {
						webServiceName = nameField.getText();
						dispose();
					}
					else JOptionPane.showMessageDialog(CreateWebServiceDialog.this, editor.getDataError(), "Cannot Create Web Service", JOptionPane.ERROR_MESSAGE);
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					webServiceName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new WebServiceEditorPanel(name, webService, inputFormats, outputFormats);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.nameField, BorderLayout.NORTH);
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 600));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.webServiceName != null);
		}
		
		Settings getWebService() {
			return this.editor.getSettings();
		}
		
		String getWebServiceName() {
			return this.webServiceName;
		}
	}
	
	private class WebServiceEditorPanel extends JPanel {
		
		private String name;
		
		private Vector processors = new Vector();
		private JList processorList;
		
		private JComboBox interactivity = new JComboBox();
		private JCheckBox defaultInteractive = new JCheckBox("Default");
		
		private boolean dirty = false;
		
		private DocumentFormatAliasPanel inputFormatPanel;
		private DocumentFormatAliasPanel outputFormatPanel;
		
		private JTextField label = new JTextField();
		private JTextArea description = new JTextArea();
		
		WebServiceEditorPanel(String name, WebServiceInternal webService, DocumentFormat[] inputFormats, DocumentFormat[] outputFormats) {
			super(new BorderLayout(), true);
			this.name = name;
			
			if (webService != null) {
				DocumentProcessor[] processors = webService.getProcessors();
				for (int p = 0; p < processors.length; p++)
					this.processors.add(processors[p]);
			}
			
			JPanel processorPanel = new JPanel(new BorderLayout(), true);
			this.processorList = new JList(new WebServicePartListModel(this.processors));
			JScrollPane partListBox = new JScrollPane(this.processorList);
			processorPanel.add(partListBox, BorderLayout.CENTER);
			
			JButton upButton = new JButton("Up");
			upButton.setBorder(BorderFactory.createRaisedBevelBorder());
			upButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					moveUp();
				}
			});
			JButton editButton = new JButton("Edit");
			editButton.setBorder(BorderFactory.createRaisedBevelBorder());
			editButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editProcessor();
				}
			});
			JButton downButton = new JButton("Down");
			downButton.setBorder(BorderFactory.createRaisedBevelBorder());
			downButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					moveDown();
				}
			});
			JPanel reorderButtonPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 3;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			
			gbc.gridy = 0;
			reorderButtonPanel.add(upButton, gbc.clone());
			gbc.gridy = 1;
			reorderButtonPanel.add(editButton, gbc.clone());
			gbc.gridy = 2;
			reorderButtonPanel.add(downButton, gbc.clone());
			
			processorPanel.add(reorderButtonPanel, BorderLayout.WEST);
			
			JButton removePartButton = new JButton("Remove");
			removePartButton.setBorder(BorderFactory.createRaisedBevelBorder());
			removePartButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					removeProcessor();
				}
			});
			
			this.defaultInteractive.setSelected((webService == null) ? true : webService.defaultInteractive);
			this.defaultInteractive.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			this.interactivity.addItem(WebService.INTERACTIVE_ALWAYS);
			this.interactivity.addItem(WebService.INTERACTIVE_OPTIONAL);
			this.interactivity.addItem(WebService.INTERACTIVE_NEVER);
			this.interactivity.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
					String interactivity = ((String) WebServiceEditorPanel.this.interactivity.getSelectedItem());
					if (WebService.INTERACTIVE_ALWAYS.equals(interactivity))
						WebServiceEditorPanel.this.defaultInteractive.setSelected(true);
					else if (WebService.INTERACTIVE_NEVER.equals(interactivity))
						WebServiceEditorPanel.this.defaultInteractive.setSelected(false);
					WebServiceEditorPanel.this.defaultInteractive.setEnabled(WebService.INTERACTIVE_OPTIONAL.equals(interactivity));
				}
			});
			this.interactivity.setSelectedItem(WebService.INTERACTIVE_NEVER);
			if (webService == null)
				this.interactivity.setSelectedItem(WebService.INTERACTIVE_OPTIONAL);
			else this.interactivity.setSelectedItem(webService.interactivity);
			this.dirty = false;
			
			JPanel managerButtonPanel = new JPanel(new GridBagLayout());
			gbc.insets.top = 3;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.weighty = 0;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			managerButtonPanel.add(removePartButton, gbc.clone());
			
			gbc.insets.top = 2;
			DocumentProcessorManager[] dpms = parent.getDocumentProcessorProviders();
			JButton button;
			for (int a = 0; a < dpms.length; a++) {
				final String className = dpms[a].getClass().getName();
				gbc.gridy++;
				
				button = new JButton("Add " + dpms[a].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						addProcessor(className);
					}
				});
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				gbc.gridwidth = 2;
				managerButtonPanel.add(button, gbc.clone());
				
				button = new JButton("Create " + dpms[a].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						createProcessor(className);
					}
				});
				
				gbc.gridx = 2;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				managerButtonPanel.add(button, gbc.clone());
			}
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			managerButtonPanel.add(new JLabel("Interactive", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			managerButtonPanel.add(this.interactivity, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 1;
			gbc.gridwidth = 1;
			managerButtonPanel.add(this.defaultInteractive, gbc.clone());
			
			processorPanel.add(managerButtonPanel, BorderLayout.SOUTH);
			
			this.inputFormatPanel = new DocumentFormatAliasPanel(true, inputFormats, webService.defaultInputFormat, webService.inputFormats);
			this.outputFormatPanel = new DocumentFormatAliasPanel(false, outputFormats, webService.defaultOutputFormat, webService.outputFormats);
			
			if (webService != null)
				this.label.setText(webService.label);
			this.label.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent ke) {
					dirty = true;
				}
			});
			this.label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(this.getBackground(), 3), BorderFactory.createLoweredBevelBorder()));
			
			this.description.setLineWrap(true);
			this.description.setWrapStyleWord(true);
			if (webService != null)
				this.description.setText(webService.description);
			this.description.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent ke) {
					dirty = true;
				}
			});
			JScrollPane descriptionBox = new JScrollPane(this.description);
			descriptionBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			descriptionBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			descriptionBox.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(this.getBackground(), 3), BorderFactory.createLoweredBevelBorder()));
			
			JPanel ldPanel = new JPanel(new BorderLayout(), true);
			ldPanel.add(this.label, BorderLayout.NORTH);
			ldPanel.add(descriptionBox, BorderLayout.CENTER);
			
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Processor", processorPanel);
			tabs.addTab("Input Formats", this.inputFormatPanel);
			tabs.addTab("Output Formats", this.outputFormatPanel);
			tabs.addTab("Label/Description", ldPanel);
			
			this.add(tabs, BorderLayout.CENTER);
		}
		
		void addProcessor(String providerClassName) {
			DocumentProcessorManager dpmp = parent.getDocumentProcessorProvider(providerClassName);
			if (dpmp != null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(dpmp, ("Select " + dpmp.getResourceTypeLabel()), "Select");
				rd.setLocationRelativeTo(DialogPanel.getTopWindow());
				rd.setVisible(true);
				
				//	get annotator
				DocumentProcessor dp = dpmp.getDocumentProcessor(rd.getSelectedResourceName());
				if (dp != null) {
					this.processors.add(dp);
					this.processorList.setModel(new WebServicePartListModel(this.processors));
					this.dirty = true;
				}
			}
		}
		
		void createProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				
				String dpName = dpm.createDocumentProcessor();
				
				//	get annotator
				DocumentProcessor dp = dpm.getDocumentProcessor(dpName);
				if (dp != null) {
					this.processors.add(dp);
					this.processorList.setModel(new WebServicePartListModel(this.processors));
					this.dirty = true;
				}
			}
		}
		
		boolean isDirty() {
			return (this.dirty || this.inputFormatPanel.isDirty() || this.outputFormatPanel.isDirty());
		}
		
		boolean isDataValid() {
			return ((this.processors.size() != 0) && (this.description.getText().trim().length() != 0) && this.inputFormatPanel.isDataValid() && this.outputFormatPanel.isDataValid());
		}
		
		String getDataError() {
			if (this.isDataValid())
				return null;
			String processorError = (this.processors.isEmpty() ? "Please select at least one document processor." : null);
			String inputFormatError = this.inputFormatPanel.getDataError();
			String outputFormatError = this.outputFormatPanel.getDataError();
			String labelError = ((this.label.getText().trim().length() == 0) ? "Please enter a meaningful label." : null);
			String descriptionError = ((this.description.getText().trim().length() == 0) ? "Please enter a textual description." : null);
			return ("The data for this web service is incomplete:"
					+ ((processorError == null) ? "" : ("\n- " + processorError))
					+ ((inputFormatError == null) ? "" : ("\n- " + inputFormatError))
					+ ((outputFormatError == null) ? "" : ("\n- " + outputFormatError))
					+ ((labelError == null) ? "" : ("\n- " + labelError))
					+ ((descriptionError == null) ? "" : ("\n- " + descriptionError))
				);
		}
		
		void removeProcessor() {
			int index = this.processorList.getSelectedIndex();
			if (index != -1) {
				this.processors.remove(index);
				this.processorList.setModel(new WebServicePartListModel(this.processors));
				this.dirty = true;
			}
		}
		
		void moveUp() {
			int index = this.processorList.getSelectedIndex();
			if (index > 0) {
				this.processors.insertElementAt(this.processors.remove(index - 1), index);
				this.processorList.setModel(new WebServicePartListModel(this.processors));
				this.processorList.setSelectedIndex(index - 1);
				this.dirty = true;
			}
		}
		
		void editProcessor() {
			int index = this.processorList.getSelectedIndex();
			if (index > -1) {
				DocumentProcessor dp = ((DocumentProcessor) this.processors.get(index));
				DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(dp.getProviderClassName());
				if (dpm != null)
					dpm.editDocumentProcessor(dp.getName());
			}
		}
		
		void moveDown() {
			int index = this.processorList.getSelectedIndex();
			if ((index != -1) && ((index + 1) != this.processors.size())) {
				this.processors.insertElementAt(this.processors.remove(index), (index + 1));
				this.processorList.setModel(new WebServicePartListModel(this.processors));
				this.processorList.setSelectedIndex(index + 1);
				this.dirty = true;
			}
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			
			if (this.defaultInteractive.isSelected())
				set.setSetting(WebServiceInternal.DEFAULT_INTERACTIVE_ATTRIBUTE, Resource.INTERACTIVE_PARAMETER);
			set.setSetting(Resource.INTERACTIVE_PARAMETER, ((String) this.interactivity.getSelectedItem()));
			String defaultInputFormatAlias = this.inputFormatPanel.getDefaultFormatAlias();
			if (defaultInputFormatAlias != null)
				set.setSetting(WebServiceInternal.DEFAULT_INPUT_FORMAT_NAME_ATTRIBUTE, defaultInputFormatAlias);
			String defaultOutputFormatAlias = this.outputFormatPanel.getDefaultFormatAlias();
			if (defaultOutputFormatAlias != null)
				set.setSetting(WebServiceInternal.DEFAULT_OUTPUT_FORMAT_NAME_ATTRIBUTE, defaultOutputFormatAlias);
			String label = this.label.getText();
			if (label != null)
				set.setSetting(WebServiceInternal.LABEL_ATTRIBUTE, label);
			String description = this.description.getText();
			if (description != null)
				set.setSetting(WebServiceInternal.DESCRIPTION_ATTRIBUTE, escapeDescription(description));
			
			Settings pSet = set.getSubset(WebServiceInternal.PROCESSOR_SUBSET_PREFIX);
			for (int p = 0; p < this.processors.size(); p++) {
				DocumentProcessor dp = ((DocumentProcessor) this.processors.get(p));
				pSet.setSetting(("P" + p), (dp.getName() + "@" + dp.getProviderClassName()));
			}
			
			this.inputFormatPanel.addSettings(set.getSubset(WebServiceInternal.INPUT_FORMAT_SUBSET_PREFIX));
			this.outputFormatPanel.addSettings(set.getSubset(WebServiceInternal.OUTPUT_FORMAT_SUBSET_PREFIX));
			
			return set;
		}
		
		private class WebServicePartListModel implements ListModel {
			private Vector data;
			WebServicePartListModel(Vector data) {
				this.data = data;
			}
			public Object getElementAt(int index) {
				DocumentProcessor dp = ((DocumentProcessor) this.data.get(index));
				return (dp.getTypeLabel() + ": " + dp.getName());
			}
			public int getSize() {
				return this.data.size();
			}
			public void addListDataListener(ListDataListener ldl) {}
			public void removeListDataListener(ListDataListener ldl) {}
		}
	}
	
	private class DocumentFormatAliasPanel extends JPanel {
		private TreeMap formatAliases = new TreeMap();
		private String selectedFormatAlias = null;
		
		private boolean dirty = false;
		
		private JLabel defaultLabel = new JLabel("Is Default", JLabel.CENTER);
		private JLabel aliasLabel = new JLabel("Format Alias", JLabel.CENTER);
		private JLabel formatLabel = new JLabel("Format", JLabel.CENTER);
		private JPanel linePanelSpacer = new JPanel();
		private JPanel linePanel = new JPanel(new GridBagLayout());
		
		private boolean inputFormats = false;
		private DocumentFormat[] formats;
		
		private String defaultFormatAlias = null;
		private ButtonGroup defaultButtonGroup = new ButtonGroup();
		
		DocumentFormatAliasPanel(boolean inputFormats, DocumentFormat[] formats, String defaultFormatAlias, TreeMap formatAliases) {
			super(new BorderLayout(), true);
			this.inputFormats = inputFormats;
			this.formats = formats;
			this.defaultFormatAlias = defaultFormatAlias;
			
			this.defaultLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.defaultLabel.getBackground(), 2)));
			this.defaultLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectDocumentFormatAlias(null);
					linePanel.requestFocusInWindow();
				}
			});
			
			this.aliasLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.aliasLabel.getBackground(), 2)));
			this.aliasLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectDocumentFormatAlias(null);
					linePanel.requestFocusInWindow();
				}
			});
			
			this.formatLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.formatLabel.getBackground(), 2)));
			this.formatLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectDocumentFormatAlias(null);
					linePanel.requestFocusInWindow();
				}
			});
			
			this.linePanelSpacer.setBackground(Color.WHITE);
			this.linePanelSpacer.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectDocumentFormatAlias(null);
					linePanel.requestFocusInWindow();
				}
			});
			this.linePanel.setBorder(BorderFactory.createLineBorder(this.getBackground(), 3));
			this.linePanel.setFocusable(true);
			this.linePanel.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent fe) {
					System.out.println("focusGained");
				}
				public void focusLost(FocusEvent fe) {
					System.out.println("focusLost");
				}
			});
			
			JScrollPane linePanelBox = new JScrollPane(this.linePanel);
			
			JButton addDocumentFormatAliasButton = new JButton("Add Document Format Alias");
			addDocumentFormatAliasButton.setBorder(BorderFactory.createRaisedBevelBorder());
			addDocumentFormatAliasButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					addDocumentFormatAlias();
				}
			});
			
			JButton removeDocumentFormatAliasButton = new JButton("Remove Document Format Alias");
			removeDocumentFormatAliasButton.setBorder(BorderFactory.createRaisedBevelBorder());
			removeDocumentFormatAliasButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					removeDocumentFormatAlias();
				}
			});
			
			JPanel buttonPanel = new JPanel(new GridBagLayout(), true);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 3;
			gbc.insets.bottom = 3;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.weightx = 1;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 0;
			gbc.gridx = 0;
			buttonPanel.add(addDocumentFormatAliasButton, gbc.clone());
			gbc.gridx = 1;
			buttonPanel.add(removeDocumentFormatAliasButton, gbc.clone());
			
			this.add(linePanelBox, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			
			for (Iterator fait = formatAliases.keySet().iterator(); fait.hasNext();) {
				String formatAlias = ((String) fait.next());
				DocumentFormat format = ((DocumentFormat) formatAliases.get(formatAlias));
				this.addDocumentFormatAlias(formatAlias, (format.getName() + "@" + format.getProviderClassName()), defaultFormatAlias.equals(formatAlias), true);
			}
			this.layoutDocumentFormatAliases();
		}
		
		void layoutDocumentFormatAliases() {
			this.linePanel.removeAll();
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 0;
			gbc.insets.bottom = 0;
			gbc.insets.left = 0;
			gbc.insets.right = 0;
			gbc.weighty = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.linePanel.add(this.defaultLabel, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 0;
			this.linePanel.add(this.aliasLabel, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			this.linePanel.add(this.formatLabel, gbc.clone());
			gbc.gridy++;
			
			for (Iterator lit = this.formatAliases.values().iterator(); lit.hasNext();) {
				DocumentFormatAliasLine line = ((DocumentFormatAliasLine) lit.next());
				gbc.gridx = 0;
				gbc.weightx = 0;
				this.linePanel.add(line.isDefault, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 0;
				this.linePanel.add(line.formatAliasDisplay, gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 0;
				this.linePanel.add(line.formatSelector, gbc.clone());
				gbc.gridy++;
			}
			
			gbc.gridx = 0;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridwidth = 3;
			this.linePanel.add(this.linePanelSpacer, gbc.clone());
			
			this.validate();
			this.repaint();
		}
		
		void addDocumentFormatAlias() {
			String formatAlias = JOptionPane.showInputDialog(this, "Please enter the new format alias.\nThe document format it maps to can be selected later.", "Enter Format Alias", JOptionPane.PLAIN_MESSAGE);
			if (formatAlias == null)
				return;
			if (this.formatAliases.containsKey(formatAlias)) {
				JOptionPane.showMessageDialog(this, "The format alias you have entered already exists.", "Invalid Format Alias", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!formatAlias.matches("[A-Za-z\\-\\_0-9]++")) {
				JOptionPane.showMessageDialog(this, "The format alias you have entered is invalid.\nPlease use only letters, digits, dashes, or underscores.", "Invalid Format Alias", JOptionPane.ERROR_MESSAGE);
				return;
			}
			String formatName = null;
			for (int f = 0; f < this.formats.length; f++) {
				if (formatAlias.equals(this.formats[f].getDefaultSaveFileExtension()))
					formatName = (this.formats[f].getName() + "@" + this.formats[f].getProviderClassName());
				else if (this.formats[f].accept("XYZ." + formatAlias) && !this.formats[f].accept("XYZ"))
					formatName = (this.formats[f].getName() + "@" + this.formats[f].getProviderClassName());
			}
			this.addDocumentFormatAlias(formatAlias, formatName, this.formatAliases.isEmpty(), false);
		}
		
		void addDocumentFormatAlias(String formatAlias, String formatName, boolean asDefault, boolean init) {
			DocumentFormatAliasLine line = new DocumentFormatAliasLine(formatAlias, this.formats, formatName, asDefault);
			this.formatAliases.put(formatAlias, line);
			
			if (init) return;
			
			this.dirty = true;
			this.layoutDocumentFormatAliases();
		}
		
		void removeDocumentFormatAlias() {
			if (this.selectedFormatAlias == null)
				return;
			if (this.formatAliases.size() == 1) {
				JOptionPane.showMessageDialog(this, ("The last " + (this.inputFormats ? "input" : "output") + " format alias cannot be removed."), "Cannot Remove Format Alias", JOptionPane.ERROR_MESSAGE);
				return;
			}
			this.formatAliases.remove(this.selectedFormatAlias);
			this.dirty = true;
			this.selectedFormatAlias = null;
			this.layoutDocumentFormatAliases();
			this.selectDocumentFormatAlias(null);
		}
		
		void selectDocumentFormatAlias(String formatAlias) {
			System.out.println("selected: " + formatAlias);
			this.selectedFormatAlias = formatAlias;
			for (Iterator lit = this.formatAliases.values().iterator(); lit.hasNext();) {
				DocumentFormatAliasLine line = ((DocumentFormatAliasLine) lit.next());
				if (line.formatAlias.equals(this.selectedFormatAlias)) {
					line.isDefault.setBorder(BorderFactory.createLineBorder(Color.BLUE));
					line.formatAliasDisplay.setBorder(BorderFactory.createLineBorder(Color.BLUE));
					line.formatSelector.setBorder(BorderFactory.createLineBorder(Color.BLUE));
				}
				else {
					line.isDefault.setBorder(BorderFactory.createLineBorder(this.getBackground()));
					line.formatAliasDisplay.setBorder(BorderFactory.createLineBorder(this.getBackground()));
					line.formatSelector.setBorder(BorderFactory.createLineBorder(this.getBackground()));
				}
			}
			this.linePanel.validate();
			this.linePanel.repaint();
		}
		
		private class DocumentFormatAliasLine {
			private JRadioButton isDefault;
			private String formatAlias;
			private JLabel formatAliasDisplay;
			private JComboBox formatSelector;
			private Properties formatLabelToFormatNames = new Properties();
			DocumentFormatAliasLine(final String formatAlias, DocumentFormat[] formats, String selectedFormatName, boolean isDefault) {
				this.formatAlias = formatAlias;
				this.formatAliasDisplay = new JLabel(formatAlias, JLabel.CENTER);
				this.formatAliasDisplay.setOpaque(true);
				this.formatAliasDisplay.setBackground(Color.WHITE);
				this.formatAliasDisplay.setBorder(BorderFactory.createLineBorder(getBackground()));
				this.formatAliasDisplay.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						selectDocumentFormatAlias(formatAlias);
					}
				});
				this.formatSelector = new JComboBox();
				String selectedFormatLabel = null;
				for (int f = 0; f < formats.length; f++) {
					String formatName = formats[f].getName();
					if (formatName == null)
						continue;
					formatName = (formatName + "@" + formats[f].getProviderClassName());
					String formatLabel = formats[f].getDescription();
					if (formatLabel == null)
						continue;
					if (formatName.equals(selectedFormatName))
						selectedFormatLabel = formatLabel;
					this.formatSelector.addItem(formatLabel);
					this.formatLabelToFormatNames.setProperty(formatLabel, formatName);
				}
				this.formatSelector.setBorder(BorderFactory.createLineBorder(getBackground()));
				if (selectedFormatLabel != null)
					this.formatSelector.setSelectedItem(selectedFormatLabel);
				this.formatSelector.setEditable(false);
				this.formatSelector.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						selectDocumentFormatAlias(formatAlias);
					}
				});
				this.formatSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						dirty = true;
					}
				});
				this.isDefault = new JRadioButton("");
				this.isDefault.setHorizontalAlignment(JRadioButton.CENTER);
				this.isDefault.setOpaque(true);
				this.isDefault.setBackground(Color.WHITE);
				this.isDefault.setBorder(BorderFactory.createLineBorder(getBackground()));
				this.isDefault.setBorderPainted(true);
				defaultButtonGroup.add(this.isDefault);
				this.isDefault.setSelected(isDefault);
				this.isDefault.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						selectDocumentFormatAlias(formatAlias);
					}
				});
				if (isDefault)
					defaultFormatAlias = formatAlias;
				this.isDefault.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						dirty = true;
						if (DocumentFormatAliasLine.this.isDefault.isSelected())
							defaultFormatAlias = formatAlias;
					}
				});
			}
			String getDocumentFormatName() {
				return this.formatLabelToFormatNames.getProperty((String) this.formatSelector.getSelectedItem());
			}
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		boolean isDataValid() {
			return (this.formatAliases.size() != 0);
		}
		
		String getDataError() {
			if (this.isDataValid())
				return null;
			return ("Please select at least one " + (this.inputFormats ? "input" : "output") + " format alias.");
		}
		
		void addSettings(Settings set) {
			for (Iterator lit = this.formatAliases.values().iterator(); lit.hasNext();) {
				DocumentFormatAliasLine line = ((DocumentFormatAliasLine) lit.next());
				set.setSetting(line.formatAlias, line.getDocumentFormatName());
			}
		}
		
		String getDefaultFormatAlias() {
			return this.defaultFormatAlias;
		}
	}
}