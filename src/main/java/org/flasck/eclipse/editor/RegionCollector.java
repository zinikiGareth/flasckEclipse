package org.flasck.eclipse.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;
import org.flasck.flas.blockForm.InputPosition;

public class RegionCollector implements IRegionCollector {
	public class TRComparator implements Comparator<ITypedRegion> {
		@Override
		public int compare(ITypedRegion o1, ITypedRegion o2) {
			if (o1.getOffset() < o2.getOffset())
				return -1;
			else if (o1.getOffset() > o2.getOffset())
				return 1;
			if (o1.getLength() > o2.getLength())
				return -1;
			else if (o1.getLength() < o2.getLength())
				return 1;
			return 0;
		}
	}

	private final IDocument document;
	private final Set<ITypedRegion> rs = new TreeSet<ITypedRegion>(new TRComparator());

	public RegionCollector(IDocument document) {
		this.document = document;
	}

	public void add(InputPosition location, String style) {
		if (location == null)
			return;
		try {
			if (!location.hasEnd()) {
				System.out.println("Token " + location + " does not have end point");
				return;
			}
			IRegion r = document.getLineInformation(location.lineNo-1);
			String dt = document.get(r.getOffset(), r.getLength());
			int s=0;
			while (s<dt.length() && Character.isWhitespace(dt.charAt(s)))
				s++;
			int e = s + location.pastEnd();
			s += location.off;
//			System.out.println("identifier at " + (r.getOffset()+s) + " " + (e-s) + " has style " + style);
			if (r.getOffset()+s == 531)
				try {
					throw new RuntimeException(location.lineNo +":"+location.off + " - style = " + style + " length = " + (location.pastEnd() - location.off));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			rs.add(new TypedRegion(r.getOffset() + s, e-s, style));
			int off = 0;
			List<ITypedRegion> defaults = new ArrayList<>();
			for (ITypedRegion r1 : rs) {
				if (r1.getOffset() > off) {
					defaults.add(new TypedRegion(off, r1.getOffset()-off, "flas-default"));
				}
				off = r1.getOffset()+r1.getLength();
			}
			if (off < document.getLength())
				defaults.add(new TypedRegion(off, document.getLength()-off, "flas-default"));
//			rs.addAll(defaults);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	public void process() {
		/*
		System.out.println(">>>>>>> Document of length " + document.getLength());
		int prev = 0;
		for (ITypedRegion tr : rs) {
			if (tr.getOffset() < prev)
				System.out.print("****** ");
			System.out.print(prev + " " + tr);
			try {
				System.out.print(document.get(tr.getOffset(), tr.getLength()));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			System.out.println();
			prev = tr.getOffset() + tr.getLength();
		}
		System.out.println("<<<<<<<");
		*/
	}

	public ITypedRegion[] toArray() {
		return rs.toArray(new ITypedRegion[rs.size()]);
	}

}
