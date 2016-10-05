package org.flasck.eclipse.editor;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;
import org.flasck.flas.Compiler;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.stories.StoryRet;

public class FLASPartitioner implements IDocumentPartitioner {
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

	private IDocument document;
	private ITypedRegion[] partitions;

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
		Set<ITypedRegion> rs = new TreeSet<ITypedRegion>(new TRComparator());
		rs.add(new TypedRegion(offset, length, "default"));
		if (document != null) {
			// What should really happen here is we should call "Compiler.parse(<document>)" and get back the parse tree, with offsets
			// We should then look at the parse tree and figure everything out
			
			Compiler compiler = new Compiler();
			try {
				StoryRet tree = compiler.parse("com.serializedstories.foo", document.get());
				if (tree.er.hasErrors()) {
					try {
						tree.er.showTo(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true), 4);
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}
				partitionOnTree(rs, tree.top);
			} catch (ErrorResultException ex) {
				try {
					ex.errors.showTo(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true), 4);
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}	
		partitions = new ITypedRegion[rs.size()];
		rs.toArray(partitions);
		return partitions;
	}

	private void partitionOnTree(Set<ITypedRegion> rs, ScopeEntry tree) {
		System.out.println("tree = " + tree);
		if (tree == null)
			return; // we can't do a lot here; we should probably have had an exception
		Object o = tree.getValue();
		if (o == null)
			return;
		
		if (o instanceof PackageDefn) {
			processInner(rs, ((PackageDefn)o).innerScope());
		} else if (o instanceof CardDefinition) {
			CardDefinition card = (CardDefinition) o;
			identifier(rs, card.kw, "keyword");
			for (ContractImplements x : card.contracts) {
				identifier(rs, x.kw, "keyword");
			}
			for (HandlerImplements x : card.handlers) {
				identifier(rs, x.kw, "keyword");
			}
			processInner(rs, card.innerScope());
		} else if (o instanceof EventHandlerDefinition) {
			EventHandlerDefinition ehd = (EventHandlerDefinition) o;
			for (EventCaseDefn x : ehd.cases) {
				identifier(rs, x.kw, "keyword");
			}
		} else
			System.out.println("Yeah, whatever: " + o.getClass());
	}

	protected void processInner(Set<ITypedRegion> rs, Scope scope) {
		for (Entry<String, ScopeEntry> x : scope) {
			partitionOnTree(rs, x.getValue());
		}
	}

	private void identifier(Set<ITypedRegion> rs, InputPosition location, String style) {
		try {
			IRegion r = document.getLineInformation(location.lineNo-1);
			String dt = document.get(r.getOffset(), r.getLength());
			int s=0;
			while (s<dt.length() && Character.isWhitespace(dt.charAt(s)))
				s++;
			s += location.off;
			int e = s;
			while (e<dt.length() && (Character.isLetterOrDigit(dt.charAt(e)) || dt.charAt(e) == '.'))
				e++;
			System.out.println("keyword defn at " + (r.getOffset()+s) + " " + (e-s));
			rs.add(new TypedRegion(r.getOffset() + s, e-s, style));
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public ITypedRegion getPartition(int offset) {
//		System.out.println("getPartition(" + offset + ") called");
		ITypedRegion ret = null;
		for (ITypedRegion r : partitions) {
			if (r.getOffset() <= offset && r.getOffset()+r.getLength() > offset)
				ret = r;
			else if (r.getOffset() >= offset)
				return ret;
		}
		return partitions[0];
	}

}
