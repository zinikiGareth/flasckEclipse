package org.flasck.eclipse.nature;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.flasck.eclipse.Activator;

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

		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		boolean found = false;

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(Activator.BUILDER_ID)) {
				found = true;
				break;
			}
		}
		
		if (!found) {
			ICommand c = desc.newCommand();
			c.setBuilderName(Activator.BUILDER_ID);
			ICommand[] newCommands = new ICommand[commands.length+1];
			System.arraycopy(commands, 0, newCommands, 0, commands.length);
			newCommands[commands.length] = c;
			desc.setBuildSpec(newCommands);
			project.setDescription(desc, null);
		}
		
		new Job("FLAS Build") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					System.out.println("Trying to start a full build");
					project.build(IncrementalProjectBuilder.FULL_BUILD,	Activator.BUILDER_ID, null, monitor);
					System.out.println("Build done");
				}
				catch (CoreException e) {
					System.out.println("Error trying to run scheduled build job");
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	@Override
	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub
		
	}

}
