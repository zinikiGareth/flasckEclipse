package org.flasck.eclipse.project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.flasck.eclipse.Activator;
import org.zinutils.xml.XML;

@SuppressWarnings("restriction")
public class NewWizard extends BasicNewResourceWizard {
	private IProject newProject;
	private WizardNewProjectCreationPage mainPage;
	
	public NewWizard() {
		IDialogSettings wb = IDEWorkbenchPlugin.getDefault().getDialogSettings();
		IDialogSettings section = wb.getSection("BasicNewProjectResourceWizard");
		if (section == null) {
			section = wb.addNewSection("BasicNewProjectResourceWizard");
		}
		setDialogSettings(section);
	}
	
	@Override
	public void addPages() {
		super.addPages();
		WizardNewProjectCreationPage page = new WizardNewProjectCreationPage("FLASPage") {
			@Override
			public void createControl(Composite ctl) {
				super.createControl(ctl);
				setControl(ctl);
			}
		};
		page.setTitle("New FLAS Project");
		page.setDescription("Create a new project for use with the Flasck environment");
		addPage(page);
		mainPage = page;
	}
	
	@Override
	public boolean performFinish() {
		createNewProject();
		selectAndReveal(newProject);
		try {
			IProjectDescription pd = newProject.getDescription();

			String[] newNatures = new String[] { Activator.NATURE_ID };
			pd.setNatureIds(newNatures);
//			IWorkspace workspace = ResourcesPlugin.getWorkspace();
//			IStatus stat = workspace.validateNatureSet(newNatures);
			newProject.setDescription(pd, null);
			IFolder s = newProject.getFolder("src");
			s.create(false, true, null);
			IFolder sm = s.getFolder("main");
			sm.create(false, true, null);
			IFolder smf = sm.getFolder("flas");
			smf.create(false, true, null);
			IFolder flim = newProject.getFolder("flim");
			flim.create(false, true, null);
			IFolder jso = newProject.getFolder("jsout");
			jso.create(false, true, null);
			IFolder jvo = newProject.getFolder("jvmout");
			jvo.create(false, true, null);
			
			// create an outline 'settings.xml' file
			IFile sx = newProject.getFile("settings.xml");
			XML xml = XML.create("1.0", "Settings");
			xml.addElement("Source").setAttribute("dir", "src/main/flas");
			xml.addElement("JavaScript").setAttribute("dir", "jsout");
			xml.addElement("Flim").setAttribute("dir", "flim");
			xml.addElement("JVM").setAttribute("dir", "jvmout");
			xml.addElement("Reference").setAttribute("dir", "/Users/gareth/Ziniki/Over/FLAS2/src/main/resources/flim");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			xml.write(baos, false);
			ByteArrayInputStream is = new ByteArrayInputStream(baos.toByteArray());
			sx.create(is, false, null);
		} catch (CoreException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return true;
	}

	private IProject createNewProject() {
		if (newProject != null)
			return newProject;
		
		IProject proj = mainPage.getProjectHandle();
		
		URI location = null;
		if (!mainPage.useDefaults()) {
			location = mainPage.getLocationURI();
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription description = workspace.newProjectDescription(proj.getName());
		description.setLocationURI(location);
		
		// create the new project operation
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				CreateProjectOperation op = new CreateProjectOperation(
						description, ResourceMessages.NewProject_windowTitle);
				try {
					// see bug
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=219901
					// directly execute the operation so that the undo state is
					// not preserved. Making this undoable resulted in too many
					// accidental file deletions.
					op.execute(monitor,
							WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
				} catch (ExecutionException e) {
					throw new InvocationTargetException(e);
				}
			}
		};

		// run the new project creation operation
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return null;
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof ExecutionException
					&& t.getCause() instanceof CoreException) {
				CoreException cause = (CoreException) t.getCause();
				StatusAdapter status;
				if (cause.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
					status = new StatusAdapter(
							StatusUtil.newStatus(
									IStatus.WARNING,
									NLS.bind(
											ResourceMessages.NewProject_caseVariantExistsError,
											proj.getName()), cause));
				} else {
					status = new StatusAdapter(StatusUtil.newStatus(cause
							.getStatus().getSeverity(),
							ResourceMessages.NewProject_errorMessage, cause));
				}
//				status.setProperty(StatusAdapter.TITLE_PROPERTY,
//						ResourceMessages.NewProject_errorMessage);
				StatusManager.getManager().handle(status, StatusManager.BLOCK);
			} else {
				StatusAdapter status = new StatusAdapter(new Status(
						IStatus.WARNING, IDEWorkbenchPlugin.IDE_WORKBENCH, 0,
						NLS.bind(ResourceMessages.NewProject_internalError,
								t.getMessage()), t));
//				status.setProperty(StatusAdapter.TITLE_PROPERTY,
//						ResourceMessages.NewProject_errorMessage);
				StatusManager.getManager().handle(status,
						StatusManager.LOG | StatusManager.BLOCK);
			}
			return null;
		}
		newProject = proj;
		
		return newProject;
	}
	
	
}
