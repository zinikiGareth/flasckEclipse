package org.flasck.eclipse.builder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.flasck.flas.Compiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.errors.FLASError;
import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class FLASBuilder extends IncrementalProjectBuilder {
	private String jsout;
	private String droid;
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
		Set<IFolder> inputs = null;
		switch (kind) {
		case FULL_BUILD: {
			parseSettingsFile();
			inputs = new HashSet<IFolder>();
			for (IResource r : getProject().members(IResource.FILE)) {
				if (r.getName().endsWith(".fl"))
					inputs.add((IFolder)r.getParent());
			}
			break;
		}
		case AUTO_BUILD:
		case INCREMENTAL_BUILD: {
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
			System.out.println("How to handle build kind " + kind);
			break;
		}
		if (inputs != null) {
			Compiler compiler = getConfiguredCompiler();
			for (IFolder f : inputs)
				build(compiler, f);
		}
		
		getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		
		// projects we depend on
		return null;
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
			} else if (xe.hasTag("Android")) {
				droid = xe.get("dir");
			} else if (xe.hasTag("Flim")) {
				flim = xe.get("dir");
			} else if (xe.hasTag("Reference")) {
				refs.add(xe.get("dir"));
			} else
				System.err.println("Cannot handle XE: " + xe.serialize());
		}
	}

	protected Compiler getConfiguredCompiler() {
		Compiler ret = new Compiler();
		if (flim != null)
			ret.writeFlimTo(getProject().getFolder(flim).getLocation().toFile());
		if (jsout != null)
			ret.writeJSTo(getProject().getFolder(jsout).getLocation().toFile());
		if (droid != null)
			ret.writeDroidTo(getProject().getFolder(droid).getLocation().toFile());
		for (String s : refs) {
			ret.searchIn(new File(s));
		}
		return ret;
	}

	private void build(Compiler compiler, IFolder f) {
		File dir = f.getLocation().toFile();
		try {
			try {
				f.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				compiler.compile(dir);
			} catch (ErrorResultException ex) {
				try {
					ErrorResult er = ex.errors;
					er.showTo(new PrintWriter(System.err), 4);
					for (int i=0;i<er.count();i++) {
						FLASError err = er.get(i);
						IResource ef = f.getFile(err.loc.file);
						if (ef == null)
							ef = getProject();
						
						// Create the marker in the problems window
						IMarker m = ef.createMarker(IMarker.PROBLEM);
						m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						m.setAttribute(IMarker.MESSAGE, err.msg);
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
				} catch (IOException e2) {
					// ok, then ...
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
