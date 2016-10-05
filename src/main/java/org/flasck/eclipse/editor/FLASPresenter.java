package org.flasck.eclipse.editor;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class FLASPresenter implements IPresentationDamager, IPresentationRepairer {
	private IDocument document;
	private Color fgColor;

	public FLASPresenter() {
		fgColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
//		Color bgColor = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
	}

	@Override
	public void setDocument(IDocument document) {
		System.out.println("presenter.setDocument called with " + document);
		this.document = document;
	}

	@Override
	public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent event, boolean documentPartitioningChanged) {
		System.out.println("getDamageRegion called on " + document);
		return null;
	}

	@Override
	public void createPresentation(TextPresentation presentation, ITypedRegion damage) {
		System.out.println("createPresentation called on " + damage);
		StyleRange r = new StyleRange();
		r.start = damage.getOffset();
		r.length = damage.getLength();
		r.foreground = fgColor;
		r.fontStyle = SWT.BOLD; // for some reason this doesn't "take" ... I think the default font doesn't have bold/italic options
//		FontData[] fontList = Display.getCurrent().getFontList("Courier", true);
//		r.font = new Font(Display.getCurrent(), fontList[2]);
//		new StyleRange(damage.getOffset(), damage.getLength(), fgColor, bgColor, SWT.BOLD)
		presentation.addStyleRange(r);
	}

}
