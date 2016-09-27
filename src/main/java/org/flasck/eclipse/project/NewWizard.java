package org.flasck.eclipse.project;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
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
				status.setProperty(StatusAdapter.TITLE_PROPERTY,
						ResourceMessages.NewProject_errorMessage);
				StatusManager.getManager().handle(status, StatusManager.BLOCK);
			} else {
				StatusAdapter status = new StatusAdapter(new Status(
						IStatus.WARNING, IDEWorkbenchPlugin.IDE_WORKBENCH, 0,
						NLS.bind(ResourceMessages.NewProject_internalError,
								t.getMessage()), t));
				status.setProperty(StatusAdapter.TITLE_PROPERTY,
						ResourceMessages.NewProject_errorMessage);
				StatusManager.getManager().handle(status,
						StatusManager.LOG | StatusManager.BLOCK);
			}
			return null;
		}
		newProject = proj;
		
		try {
			IProjectDescription pd = proj.getDescription();

			String[] newNatures = new String[] { "org.flasck.eclipse.flasNature" };
			description.setNatureIds(newNatures);
			IStatus stat = workspace.validateNatureSet(newNatures);
			System.out.println("nature stat = " + stat);
			proj.setDescription(pd, null);
			
			if (pd.getNatureIds() == null) {
				System.out.println("No natures");
			} else
				for (String s : pd.getNatureIds()) {
					System.out.println("Nature " + s);
				}
		} catch (CoreException ex) {
			ex.printStackTrace();
		}
		return newProject;
	}
	
	
}
