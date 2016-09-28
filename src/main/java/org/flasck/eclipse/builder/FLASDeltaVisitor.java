package org.flasck.eclipse.builder;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class FLASDeltaVisitor implements IResourceDeltaVisitor {
	final Set<IFolder> inputs = new HashSet<IFolder>();

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		if (resource.getType() == IResource.PROJECT)
			return true;
		if (resource.getType() == IResource.FOLDER) {
			// check if it is a "source" folder ... and only return "true" if so
			return true;
		}
		
		if (resource.getName().endsWith(".fl"))
			inputs.add((IFolder) resource.getParent());
		
		return false;
	}

}
