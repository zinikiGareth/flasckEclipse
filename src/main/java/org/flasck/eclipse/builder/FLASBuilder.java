package org.flasck.eclipse.builder;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

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
		System.out.println("kind = " + kind + " args = " + args);
		
		// projects we depend on
		return null;
	}
}
