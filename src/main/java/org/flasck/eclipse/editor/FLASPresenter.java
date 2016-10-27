package org.flasck.eclipse.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

public class FLASPresenter implements IPresentationDamager, IPresentationRepairer {
	class Presentation {
		private final Color color;
		private final Font font;
		private final Color bgcolor;
		
		public Presentation(ColorRegistry colors, FontRegistry fonts, String key) {
			color = colors.getColorDescriptor("org.flasck.eclipse.flas.preferences.colors." + key).createColor(display);
			bgcolor = colors.getColorDescriptor("org.flasck.eclipse.flas.preferences.bgcolor." + key).createColor(display);
			FontData[] fd = fonts.getFontData("org.flasck.eclipse.flas.preferences.fonts." + key);
			if (fd != null && fd.length > 0)
				font = new Font(display, fd[0]);
			else
				font = null;
//			bgcolor = colors.getColorDescriptor("org.eclipse.jdt.ui.ColoredLabels.writeaccess_highlight").createColor(display);
		}
	}
	
	private final Display display;
	private final Map<String, Presentation> presentations = new HashMap<String, Presentation>();
//	private IDocument document;

	public FLASPresenter() {
		IWorkbench wb = PlatformUI.getWorkbench();
		display = Display.getDefault();
		IThemeManager mgr = wb.getThemeManager();
		ITheme theme = mgr.getCurrentTheme();
		ColorRegistry colors = theme.getColorRegistry();
		FontRegistry fonts = theme.getFontRegistry();
//		presenter(colors, fonts, "flas-default");
		for (String s : FLASSourceViewerConfiguration.types)
			presenter(colors, fonts, s);
//		for (String s : colors.getKeySet())
//			System.out.println("Have color symbolic name " + s);
//		for (String s : fonts.getKeySet())
//			System.out.println("Have font symbolic name " + s);
	}

	private void presenter(ColorRegistry colors, FontRegistry fonts, String string) {
		presentations.put(string, new Presentation(colors, fonts, string));
	}

	@Override
	public void setDocument(IDocument document) {
//		System.out.println("presenter.setDocument called with " + document);
//		this.document = document;
	}

	@Override
	public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent event, boolean documentPartitioningChanged) {
//		System.out.println("getDamageRegion called on " + document);
		return new Region(0, event.getDocument().getLength());
	}

	@Override
	public void createPresentation(TextPresentation presentation, ITypedRegion damage) {
		String dt = damage.getType();
//		System.out.println("createPresentation called on " + dt);
		Presentation p = presentations.get(dt);
		if (p == null) {
			System.out.println("There is no presentation for damage " + dt);
			return;
		}
		StyleRange r = new StyleRange();
		r.start = damage.getOffset();
		r.length = damage.getLength();
		r.foreground = p.color;
		r.background = p.bgcolor; 
		if (p.font != null)
			r.font = p.font;
		presentation.addStyleRange(r);
	}

}
