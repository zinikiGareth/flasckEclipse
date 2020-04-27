package org.flasck.eclipse.builder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.errors.FLASError;
import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class FLASBuilder extends IncrementalProjectBuilder {
	private String jsout;
	private String jvmOut;
	private String flim;
	private Set<String> refs = new TreeSet<String>();

	public FLASBuilder() {
	}
	
	@Override
	protected void startupOnInitialize() {
		super.startupOnInitialize();
		parseSettingsFile();
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
//		System.out.println("Build called for " + kind + " on " + getProject());
		Set<IFolder> inputs = null;
		String type = "unknown";
		switch (kind) {
		case FULL_BUILD: {
			type = "full";
			parseSettingsFile();
			inputs = new HashSet<IFolder>();
			IContainer from = getProject();
			collectInputs(inputs, from);
			break;
		}
		case AUTO_BUILD:
		case INCREMENTAL_BUILD: {
			type = "incremental";
			IResourceDelta delta = getDelta(getProject());
			FLASDeltaVisitor analyzer = new FLASDeltaVisitor();
			delta.accept(analyzer);
			inputs = analyzer.inputs;
			if (analyzer.reloadSettings)
				parseSettingsFile();
			break;
		}
		case CLEAN_BUILD:
		default:
			System.out.println("How to handle build kind " + kind + "?");
			break;
		}
		if (inputs != null) {
			System.out.println("starting " + type + " build for " + inputs);
			FLASCompiler compiler = getConfiguredCompiler();
			for (IFolder f : inputs)
				build(compiler, f);
		} else {
			System.out.println("Nothing to do for " + type + " build");
		}
		
		getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		
		// projects we depend on
		return null;
	}

	protected void collectInputs(Set<IFolder> inputs, IContainer from) throws CoreException {
		for (IResource r : from.members(IContainer.EXCLUDE_DERIVED)) {
			if (r instanceof IContainer)
				collectInputs(inputs, (IContainer) r);
			else if (r.getName().endsWith(".fl"))
				inputs.add((IFolder)r.getParent());
		}
	}

	private void parseSettingsFile() {
		// clean up what we had before so they can remove entries
		jsout = null;
		flim = null;
		refs.clear();
		
		// read the file
		XML sf = XML.fromFile(getProject().getFile("settings.xml").getLocation().toFile());
		for (XMLElement xe : sf.elementChildren()) {
			if (xe.hasTag("Source")) {
				// we are currently ignoring this and just going with "everything is source if it ends in .fl"
			} else if (xe.hasTag("JavaScript")) {
				jsout = xe.get("dir");
			} else if (xe.hasTag("JVM")) {
				jvmOut = xe.get("dir");
			} else if (xe.hasTag("Flim")) {
				flim = xe.get("dir");
			} else if (xe.hasTag("Reference")) {
				refs.add(xe.get("dir"));
			} else
				System.err.println("Cannot handle XE: " + xe.serialize(false));
		}
	}

	protected FLASCompiler getConfiguredCompiler() {
		FLASCompiler ret = new FLASCompiler(null, null);
		/*
		if (flim != null)
			ret.writeFlimTo(getProject().getFolder(flim).getLocation().toFile());
		if (jsout != null)
			ret.writeJSTo(getProject().getFolder(jsout).getLocation().toFile());
		if (jvmOut != null)
			ret.writeJVMTo(getProject().getFolder(jvmOut).getLocation().toFile());
		for (String s : refs) {
			ret.searchIn(new File(s));
		}
		*/
		return ret;
	}

	// TODO: this should be an interface, I think
	private void build(FLASCompiler compiler, IFolder f) {
		File dir = f.getLocation().toFile();
		try {
			try {
				f.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				System.out.println("Compiling in " + dir);
//				compiler.compile(dir);
				System.out.println("Compilation done");
			} catch (/*ErrorResult*/Exception ex) {
//				ex.printStackTrace(System.out);
				try {
					ErrorResult er = (ErrorResult) ((ErrorResultException)ex).errors;
					er.showTo(new PrintWriter(System.err), 4);
					for (int i=0;i<er.count();i++) {
						FLASError err = er.get(i);
						if (err == null)
							continue;
						IResource ef = err.loc != null ? f.getFile(err.loc.file) : null;
						if (ef == null)
							ef = f;
						
						// Create the marker in the problems window
						IMarker m = ef.createMarker(IMarker.PROBLEM);
						m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						m.setAttribute(IMarker.MESSAGE, err.msg);
						if (err.loc != null) {
							m.setAttribute(IMarker.LINE_NUMBER, err.loc.lineNo);
						
							// find out where it actually is ...
							if (ef instanceof IFile) {
								Document doc = new Document(new String(FileUtils.readAllStream(((IFile)ef).getContents())));
								int start = doc.getLineOffset(err.loc.lineNo-1);
								int len = doc.getLineLength(err.loc.lineNo-1);
								String offline = doc.get(start, len);
								int k = 0;
								while (k<len && Character.isWhitespace(offline.charAt(k)))
									k++;
								k += err.loc.off;
								// This shouldn't happen, but just in case, step over any whitespace at the official start point
								while (k<len && Character.isWhitespace(offline.charAt(k)))
									k++;
								if (k < len) {
									m.setAttribute(IMarker.CHAR_START, start+k);
									while (k < len && !Character.isWhitespace(offline.charAt(k)))
										k++;
									m.setAttribute(IMarker.CHAR_END, start+k);
								}
							}
						}
					}
				} catch (IOException e2) {
					e2.printStackTrace(System.out);
					// ok, then ...
				}
			}
		} catch (Exception ex) {
			System.err.println("Caught exception " + ex);
			ex.printStackTrace();
			try {
				IMarker m = f.createMarker(IMarker.PROBLEM);
				m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				m.setAttribute(IMarker.MESSAGE, "Internal Error: " + ex.toString());
			} catch (CoreException e2) {
				e2.printStackTrace();
			}
		}
	}

}
