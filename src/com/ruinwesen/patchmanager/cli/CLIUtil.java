package com.ruinwesen.patchmanager.cli;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.ruinwesen.patch.metadata.*;
import com.ruinwesen.patch.metadata.PatchMetadataIDInfo.DeviceId;
import com.ruinwesen.patch.metadata.PatchMetadataIDInfo.EnvironmentId;
import com.ruinwesen.patchmanager.client.*;
import com.ruinwesen.patchmanager.client.protocol.*;

import name.cs.csutils.*;

import com.ruinwesen.patchmanager.swing.tasks.DeletePatchTask;


public class CLIUtil {

	static void uploadPatch(Auth auth, String title, String tags, String documentationFile, 
			String deviceId, String environmentId, 
			String comment, String filename) throws Exception {
		PatchMetadata meta = new DefaultPatchMetadata();
		meta.setTitle(title);
		meta.setAuthor(auth.getUsername());
		System.out.println("tags: " + tags);
		meta.setTags(Tagset.parseTags(tags));
		
		meta.setComment(comment);
		meta.setDeviceId(new DeviceId(deviceId));
		meta.setEnvironmentId(new EnvironmentId(environmentId));

		File tmpPatchFile = File.createTempFile("patch-upload", ".tmp");
		tmpPatchFile.deleteOnExit();
		System.out.println("uploading " + tmpPatchFile);
		
		try {
			File mididataFile = new File(filename);
			String documentationText = "";
			if (documentationFile != null) {
				try {
					InputStream is = new FileInputStream(documentationFile);
					if (is != null) {
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						CSUtils.copy(is, os);
						documentationText = new String(os.toByteArray());  
					}
				} catch (IOException ex) {
						ex.printStackTrace();
				}
			}

			InputStream is = null;
			RequestStorePatch.buildPatchFile(null, mididataFile, meta, documentationText, tmpPatchFile);
			is = new BufferedInputStream(new FileInputStream(tmpPatchFile));
			
			try {
				Request request = new RequestStorePatch(auth, meta, is);
				Response response = patchmanager.getPatchManagerClient().execute(request);

				if (!response.isOkStatus()) {
					throw new Exception("error: " + response.getMessage());
				} else {
					System.out.println("response " + response);
				}
				patchmanager.syncRepository(auth, null);
			} catch (Exception e) {
				System.out.println("exception " + e);
				e.printStackTrace();
			} finally {
				is.close();
			}

		} finally {
			tmpPatchFile.delete();
		}
	}

	public static void listPatches(Auth auth) {
		Response response = executeRequest(new RequestGetPatchSourceList("01/01/1970", auth));
		if (response != null) {
			if (response.isOkStatus()) {
				for (PatchSource patch : response.getPatchSourceList()) {
					System.out.println(patch.getPatchId() + "  "  
							+ patch.getMetadata().getLastModifiedDateString() + "  "
							+ patch.getMetadata().getTitle() + "\t" 
							// + " \"" + (patch.getMetadata().getTags()) + "\" "
							+ (patch.isDeleteFlagSet() ? "[deleted]" : ""));
				}
			}
		}
	}

	public static void approvePatch(Auth auth, String patchId) {
		Response response = executeRequest(new RequestApprovePatch(auth, patchId));
		if (response != null) {
			if (response.isOkStatus()) {
				System.out.println("approved patch " + patchId);
			} else {
				System.out.println("error while approving patch " + patchId);
			}
		}
	}

	public static void deletePatch(Auth auth, String patchId) {
		Response response = executeRequest(new RequestDeletePatch(auth, patchId));
		if (response != null) {
			if (response.isOkStatus()) {
				System.out.println("deleted patch " + patchId);
			} else {
				System.out.println("error while deleting patch " + patchId);
			}
		}
	}

	public static Response executeRequest(Request request) {
		Response response;
		try {
			response = patchmanager.getPatchManagerClient().execute(request);
			return response;
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	static PatchManager patchmanager = null;
	
	public static void usage() {
		System.out.println("Usage: cliutil [-password password] [-username username] [-since date] [-tags \"tag1 tag2\"] [-doc docfile]");
		System.out.println("[-device device] [-comment \"comment\"] [-title \"title\"] COMMAND");
		System.out.println("COMMANDS:");
		System.out.println("list: list all patches");
		System.out.println("delete patchId: delete patch");
		System.out.println("approve patchId: approve patch");
		System.out.println("upload: upload file");
	}

	public static void main(String[] args) {
		String username = null;
		String password = null;
		String since = "01/01/1970";
		String action = "server-info";
		String tags = "";
		String documentationFile = null;
		String comment = null;
		String environmentId = "mididuino-17";
		String deviceId = "minicommand";
		String title = null;

		if (args.length == 0) {
			usage();
			return;
		}
		
		int optind = 0;
		for (optind = 0; optind < args.length; optind++) {
			if (args[optind].equals("-username")) {
				username = args[++optind];
			} else if (args[optind].equals("-password")) {
				password = args[++optind];
			} else if (args[optind].equals("-since")) {
				since = args[++optind];
			} else if (args[optind].equals("-tags")) {
				tags = args[++optind];
			} else if (args[optind].equals("-doc")) {
				documentationFile = args[++optind];
			} else if (args[optind].equals("-environment")) {
				environmentId = args[++optind];
			} else if (args[optind].equals("-device")) {
				deviceId = args[++optind];
			} else if (args[optind].equals("-comment")) {
				comment = args[++optind];
			} else if (args[optind].equals("-title")) {
				title = args[++optind];
			} else {
				action = args[optind];
				break;
			}
		}

		//		System.out.println("action " + action);
		patchmanager = new PatchManager(null);
		Auth auth = new Auth(username, password);

		if (action.equals("list")) {
			listPatches(auth);
		} else if (action.equals("approve")) {
			String patchId = args[++optind];
			approvePatch(auth, patchId);
		} else if (action.equals("delete")) {
			String patchId = args[++optind];
			deletePatch(auth, patchId);
		} else if (action.equals("upload")) {
			String filename = args[++optind];
			try {
				uploadPatch(auth, title, tags, documentationFile, deviceId, environmentId, comment, filename);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
