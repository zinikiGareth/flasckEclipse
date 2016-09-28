package org.flasck.eclipse.builder;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.flasck.flas.Compiler;

public class FLASBuilder extends IncrementalProjectBuilder {
	
	public FLASBuilder() {
		System.out.println("At least we got created");
	}
	
	@Override
	protected void startupOnInitialize() {
		super.startupOnInitialize();
		System.out.println("soi called");
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		Set<File> inputs = null;
		switch (kind) {
		case FULL_BUILD: {
			System.out.println("full build");
			inputs = new HashSet<File>();
			for (IResource r : getProject().members(IResource.FILE)) {
				if (r.getName().endsWith(".fl"))
					inputs.add(r.getLocation().toFile().getParentFile());
			}
			break;
		}
		case AUTO_BUILD:
		case INCREMENTAL_BUILD: {
			System.out.println("incremental build");
			IResourceDelta delta = getDelta(getProject());
			FLASDeltaVisitor analyzer = new FLASDeltaVisitor();
			delta.accept(analyzer);
			inputs = analyzer.inputs;
			break;
		}
		case CLEAN_BUILD:
		default:
			System.out.println("How to handle build kind " + kind);
			break;
		}
		
		if (inputs != null) {
			Compiler compiler = getConfiguredCompiler();
			for (File dir : inputs)
				build(compiler, dir);
		}
		
		getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		
		// projects we depend on
		return null;
	}

	protected Compiler getConfiguredCompiler() {
		Compiler ret = new Compiler();
		ret.writeFlimTo(getProject().getFolder("flim").getLocation().toFile());
		ret.writeJSTo(getProject().getFolder("jsout").getLocation().toFile());
		return ret;
	}

	private void build(Compiler compiler, File dir) {
		compiler.compile(dir);
	}

}
