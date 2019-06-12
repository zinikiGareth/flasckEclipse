package org.flasck.eclipse.editor;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;
import org.flasck.flas.compiler.FLASCompiler;

public class FLASPartitioner implements IDocumentPartitioner {
	private IDocument document;
	private ITypedRegion[] partitions;

	@Override
	public void connect(IDocument document) {
		this.document = document;
	}

	@Override
	public void disconnect() {
		this.document = null;
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
//		System.out.println("aboutToChange " + event.getOffset());
	}

	@Override
	public boolean documentChanged(DocumentEvent event) {
//		System.out.println("changed " + event.getOffset() + " " + event.getLength());
		try {
			String s = document.get(event.getOffset(), event.getLength());
			for (int i=0;i<s.length();i++) {
				if (Character.isWhitespace(s.charAt(i))) {
					computePartitioning(0, document.getLength());
					return true;
				}
			}
		} catch (BadLocationException ex) {
			
		}
		return false;
	}

	@Override
	public String[] getLegalContentTypes() {
//		System.out.println("getLegalContentTypes called");
		return null;
	}

	@Override
	public String getContentType(int offset) {
//		System.out.println("getContentType called at " + offset);
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ITypedRegion[] computePartitioning(int offset, int length) {
//		System.out.println("compute partitioning called on " + document + " " + offset + " " + length);
		PartitionAccumulator acc = new PartitionAccumulator(document);
		if (document != null) {
			FLASCompiler compiler = new FLASCompiler(null);
			// It is ludicrous how hard it seems to be to get the file path from the document in Eclipse
			// In the meantime I'm just "making up" a package ID
//			StoryRet tree = compiler.parse("com.foo", document.get());
//			if (tree.er.hasErrors()) {
//				try {
//					tree.er.showTo(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true), 4);
//				} catch (IOException e2) {
//					e2.printStackTrace();
//				}
//			}
//			acc.processScope(tree.scope);
		}	
		partitions = acc.toArray();
		
		return partitions;
	}

	@Override
	public ITypedRegion getPartition(int offset) {
//		System.out.println("getPartition(" + offset + ") called");
		ITypedRegion ret = null;
		if (partitions == null) // this really shouldn't happen
			return new TypedRegion(0, document.getLength(), "flas-default");
		for (ITypedRegion r : partitions) {
			if (r.getOffset() <= offset && r.getOffset()+r.getLength() > offset)
				ret = r;
			else if (r.getOffset() >= offset)
				return ret;
		}
		return partitions[0];
	}

}
