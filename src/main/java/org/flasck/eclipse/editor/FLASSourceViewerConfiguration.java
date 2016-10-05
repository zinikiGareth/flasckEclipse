package org.flasck.eclipse.editor;

import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

public class FLASSourceViewerConfiguration extends SourceViewerConfiguration {

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		System.out.println("Asked for reconciler");
		PresentationReconciler ret = new PresentationReconciler();
		ret.setDamager(new FLASPresenter(), "keyword");
		ret.setRepairer(new FLASPresenter(), "keyword");
		return ret;
	}
}
