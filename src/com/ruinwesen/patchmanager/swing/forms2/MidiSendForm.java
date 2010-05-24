/**
 * Copyright (c) 2009, Christian Schneider
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the names of the authors nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.ruinwesen.patchmanager.swing.forms2;

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JProgressBar;
import javax.swing.ListModel;

import name.cs.csutils.CSAction;
import name.cs.csutils.CSUtils;
import name.cs.csutils.Platform;
import name.cs.csutils.Platform.OS;
import name.cs.csutils.concurrent.SimpleSwingWorker;

import com.ruinwesen.midisend.MidiDevice;
import com.ruinwesen.midisend.MidiSend;
import com.ruinwesen.midisend.MidiSendException;
import com.ruinwesen.midisend.RWMidiSend;
import com.ruinwesen.patch.DefaultPatch;
import com.ruinwesen.patch.Patch;
import com.ruinwesen.patch.PatchDataException;
import com.ruinwesen.patch.utils.HexFileValidator;
import com.ruinwesen.patchmanager.swing.SwingPatchManagerUtils;
import com.ruinwesen.patchmanager.swing.form.AbstractFormElementValidator;
import com.ruinwesen.patchmanager.swing.form.ComboBoxFormElement;
import com.ruinwesen.patchmanager.swing.form.CustomFormElement;
import com.ruinwesen.patchmanager.swing.form.FileFormElement;
import com.ruinwesen.patchmanager.swing.form.Form;
import com.ruinwesen.patchmanager.swing.form.FormContainer;
import com.ruinwesen.patchmanager.swing.form.FormElement;

public class MidiSendForm extends Form {

	private MidiSend midisend;
	FileFormElement feSourceFile;
	ComboBoxFormElement feMidiInput;
	ComboBoxFormElement feMidiOutput;
	CSAction acRefresh;
	JProgressBar progressBar;
	private boolean sourcefileSelectable;
	private String preferredInputDeviceName;
	private String preferredOutputDeviceName;
	private boolean firstRefresh = true;
	private FormContainer container;

	public MidiSendForm() {
		this(new RWMidiSend());
	}

	public MidiSendForm(MidiSend midisend) {
		super("midi-send");
		if (midisend == null) {
			throw new IllegalArgumentException("midisend==null");
		}
		this.midisend = midisend;

		feSourceFile = new FileFormElement();
		feSourceFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
		feSourceFile.setValidator(new SourceFileValidator("File must be a Intel Hex File or a patch containing an Intel Hex file"));
		feSourceFile.setLabel("File");
		feSourceFile.setFileFilterList(Arrays.asList(
				SwingPatchManagerUtils.HEX_FILE_FILTER,
				SwingPatchManagerUtils.RWP_FILE_FILTER
		));

		feMidiInput = new ComboBoxFormElement();
		feMidiInput.setLabel("MIDI-In");
		feMidiInput.setValidator(new DeviceValidator("No device selected"));
		feMidiOutput = new ComboBoxFormElement();
		feMidiOutput.setLabel("MIDI-Out");
		feMidiOutput.setValidator(new DeviceValidator("No device selected"));

		add(feSourceFile);
		add(feMidiInput);
		add(feMidiOutput);

		progressBar = new JProgressBar();

		acRefresh = new CSAction("Refresh")
		.useInvokationTarget(this,"refreshDeviceList")
		.useInvokeLater();
		JButton btnRefresh = new JButton(acRefresh);
		btnRefresh.setAlignmentX(Component.RIGHT_ALIGNMENT);

		add(new CustomFormElement(btnRefresh));

		CustomFormElement pbelem = new CustomFormElement(progressBar);
		pbelem.setLabel("Status");
		add(pbelem);

		refreshDeviceList();
	}

	public void setFormContainer(FormContainer container) {
		this.container = container;
	}

	private class DeviceRefresher extends SimpleSwingWorker {

		private static final long serialVersionUID = 7564497263941075449L;
		private List<MidiDevice> inputList = Collections.emptyList();
		private List<MidiDevice> outputList = Collections.emptyList();

		@Override
		protected void setup() {
			getProgressBar().setValue(getProgressBar().getMinimum());
			getProgressBar().setIndeterminate(true);
			acRefresh.setEnabled(false);
		}

		@Override
		protected synchronized void process() {
			try {
				inputList = midisend.getInputDeviceList();
			} catch (MidiSendException ex) {
				ex.printStackTrace();
			}

			if (Platform.isFlavor(OS.MacOSFlavor)) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			try {
				outputList = midisend.getOutputDeviceList();
			} catch (MidiSendException ex) {
				ex.printStackTrace();
			}
		}

		@Override
		protected void cleanup() {
			getProgressBar().setIndeterminate(false);
            setInputControlsEnabled(true);
			acRefresh.setEnabled(true);
			refreshDeviceList(feMidiInput, inputList);
			refreshDeviceList(feMidiOutput, outputList);
			if (firstRefresh) {
				firstRefresh = false;
				if (preferredInputDeviceName != null) {
					selectInputDeviceByName(preferredInputDeviceName);
				}
				if (preferredOutputDeviceName != null) {
					selectOutputDeviceByName(preferredOutputDeviceName);
				}
			}
			if (container != null)
				container.setValidationHintsEnabled(true);
		}

	}

	public void selectInputDeviceByName(String name) {
		this.preferredInputDeviceName = name;
		selectDeviceByName(feMidiInput.getField(), name);
	}

	public void selectOutputDeviceByName(String name) {
		this.preferredOutputDeviceName = name;
		selectDeviceByName(feMidiOutput.getField(), name);
	}

	private void selectDeviceByName(JComboBox cb, String name) {
		ComboBoxModel model = cb.getModel();
		for (int i=model.getSize()-1;i>=0;i--) {
			MidiDevice dev = (MidiDevice) model.getElementAt(i);
			if (dev.getName().equals(name)) {
				cb.setSelectedIndex(i);
				return;
			}
		}
	}

	public void setSourceFile(File file) {
		feSourceFile.setFile(file);
	}

	public void setSourceFileSelectable(boolean value) {
		this.sourcefileSelectable = value;
		feSourceFile.setEnabled(value);
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public byte[] getHexFileBytes() throws IOException, PatchDataException {
		File file = feSourceFile.getFile();
		if (file == null) {
			throw new FileNotFoundException("file not found: "+file);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		if (file.getName().toLowerCase().endsWith(".hex")) {
			CSUtils.copy(file, out);
		} else {
			// patch file
			InputStream is = DefaultPatch.getMidiFileInputStream(new DefaultPatch(file));
			if (is == null) {
				throw new IOException("patch does not contain a Intel Hex file");
			}
			CSUtils.copy(is, out);
		}

		byte[] data = out.toByteArray();
		if (data.length == 0) {
			throw new IOException("no data");
		}
		return data;
	}

	public MidiDevice getInputDevice() {
		return (MidiDevice) feMidiInput.getValue();
	}

	public MidiDevice getOutputDevice() {
		return (MidiDevice) feMidiOutput.getValue();
	}

	public void setInputControlsEnabled(boolean value) {
		feSourceFile.setEnabled(value);
		feMidiInput.setEnabled(value);
		feMidiOutput.setEnabled(value);
		acRefresh.setEnabled(value);
	}

	public void refreshDeviceList() {
		new Thread(new DeviceRefresher()).start();
	}

	private void refreshDeviceList(ComboBoxFormElement fe,
			List<MidiDevice> list) {
		Object previousSelection = fe.getField().getSelectedItem();
		ComboBoxModel model = fe.getField().getModel();
		if (!listModelEquals(model, list)) {
			model = new DefaultComboBoxModel(list.toArray());
			fe.getField().setModel(model);
			fe.getField().setSelectedItem(previousSelection);
		}
		if (model.getSize()>0 && fe.getField().getSelectedItem() == null) {
			fe.getField().setSelectedItem(model.getElementAt(0));
		}
	}

	private boolean listModelEquals(ListModel model, Collection<?> collection) {
		int msize = model.getSize();
		if (msize != collection.size()) {
			return false;
		}

		Iterator<?> iter = collection.iterator();
		int i=0;
		for (;i<msize && iter.hasNext();i++) {
			if (!CSUtils.equals(model.getElementAt(i), iter.next())) {
				return false;
			}
		}
		return i == msize;
	}

	public MidiSend getMidiSend() {
		return midisend;
	}


	private static class DeviceValidator extends AbstractFormElementValidator {
		public DeviceValidator(String errorMessage) {
			super(errorMessage);
		}

		public boolean isValid(FormElement elem) {
			return elem.getValue() != null;
		}
	}

	private static class SourceFileValidator extends AbstractFormElementValidator {

		public SourceFileValidator(String errorMessage) {
			super(errorMessage);
		}

		private File checkedFile = null;
		private boolean checkResult = false;

		private boolean checkHexFile(File file) {
			if (CSUtils.equals(checkedFile, file)) {
				return checkResult;
			}
			if (file == null || !file.isFile()) {
				checkedFile = file;
				checkResult = false;
				return  checkResult;
			}
			boolean valid = false;
			if (file.getName().toLowerCase().endsWith(".hex")) {
				try {
					InputStream is = new FileInputStream(file);
					try {
						HexFileValidator validator = new HexFileValidator(false);
						validator.update(is);
						valid = validator.isValid();
					} finally {
						is.close();
					}
				} catch (IOException ex) {
					valid = false;
				}
			} else {
				// patch file
				Patch patch = new DefaultPatch(file);
				try {
					InputStream is = DefaultPatch.getMidiFileInputStream(patch);
					valid = is != null;
				} catch (Exception ex) {
					valid = false;
				}
			}
			checkResult = valid;
			checkedFile = file;
			return checkResult;
		}

		public boolean isValid(FormElement elem) {
			File file = ((FileFormElement)elem).getFile();
			return checkHexFile(file);
		}
	}

}
