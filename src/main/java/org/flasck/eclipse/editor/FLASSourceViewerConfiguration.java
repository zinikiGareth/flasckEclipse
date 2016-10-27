package org.flasck.eclipse.editor;

import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

public class FLASSourceViewerConfiguration extends SourceViewerConfiguration {

	public static final String[] types = { "field", "keyword", "literal", "methodname", "symbol", "typename", "var" };
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler ret = new PresentationReconciler();
		FLASPresenter presenter = new FLASPresenter();
		for (String s : types) {
			ret.setDamager(presenter, s);
			ret.setRepairer(presenter, s);
		}
		return ret;
	}
}
