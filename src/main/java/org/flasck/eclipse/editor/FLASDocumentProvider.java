package org.flasck.eclipse.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public class FLASDocumentProvider extends TextFileDocumentProvider  {
	@Override
	public void connect(Object element) throws CoreException {
		super.connect(element);
		IDocument doc = getDocument(element);
		if (doc != null) {
			FLASPartitioner p = new FLASPartitioner();
			doc.setDocumentPartitioner(p);
			p.connect(doc);
		}
	}
}
