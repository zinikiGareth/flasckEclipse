package org.flasck.eclipse.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class FLASNature implements IProjectNature {
	private IProject project;

	public FLASNature() {
		System.out.println("Flas Nature constructor called");
	}
	
	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject proj) {
		System.out.println("Setting project to " + proj);
		this.project = proj;
	}

	@Override
	public void configure() throws CoreException {
		System.out.println("Configure nature, which means add the builder, I think ...");
	}

	@Override
	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub
		
	}

}
