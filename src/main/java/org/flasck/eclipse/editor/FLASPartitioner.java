package org.flasck.eclipse.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;

public class FLASPartitioner implements IDocumentPartitioner {
	private IDocument document;

	@Override
	public void connect(IDocument document) {
		System.out.println("Connecting to " + document);
		this.document = document;
	}

	@Override
	public void disconnect() {
		System.out.println("Disconnecting from " + document);
		this.document = null;
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		System.out.println("aboutToChange " + event);
	}

	@Override
	public boolean documentChanged(DocumentEvent event) {
		System.out.println("changed " + event);
		return true;
	}

	@Override
	public String[] getLegalContentTypes() {
		System.out.println("getLegalContentTypes called");
		return null;
	}

	@Override
	public String getContentType(int offset) {
		System.out.println("getContentType called at " + offset);
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ITypedRegion[] computePartitioning(int offset, int length) {
		System.out.println("compute partitioning called on " + document + " " + offset + " " + length);
		List<ITypedRegion> rs = new ArrayList<ITypedRegion>();
		if (document != null) {
			FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(document);
			try {
				int max = offset + length;
				IRegion r1;
				while (offset < max && (r1 = finder.find(offset, "implements", true, false, true, false)) != null) {
					System.out.println(r1);
					rs.add(new TypedRegion(r1.getOffset(), 10, "keyword"));
					offset = r1.getOffset()+1;
				}
			} catch (Exception ex) { 
				ex.printStackTrace();
			}
		}	
		ITypedRegion[] ret = new ITypedRegion[rs.size()];
		rs.toArray(ret);
		return ret;
	}

	@Override
	public ITypedRegion getPartition(int offset) {
		System.out.println("getPartition(" + offset + ") called");
		return null;
	}

}
