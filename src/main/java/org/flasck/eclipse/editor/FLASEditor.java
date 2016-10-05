package org.flasck.eclipse.editor;

import org.eclipse.ui.editors.text.TextEditor;

public class FLASEditor extends TextEditor {

	public FLASEditor() {
		setSourceViewerConfiguration(new FLASSourceViewerConfiguration());
		setDocumentProvider(new FLASDocumentProvider());
	}
}
