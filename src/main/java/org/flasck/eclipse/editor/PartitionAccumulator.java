package org.flasck.eclipse.editor;

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
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateFormat;
import org.flasck.flas.commonBase.template.TemplateList;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCardReference;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.TemplateToken;
import org.zinutils.reflection.Reflection;

public class PartitionAccumulator {
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

	public PartitionAccumulator(IDocument document) {
		this.document = document;
	}

	public void processScope(Scope scope) {
		if (scope == null)
			return;
		
		for (ScopeEntry x : scope) {
			if (x != null)
				processEntry(x.getValue());
		}
	}

	private void processEntry(Object o) {
		if (o == null)
			return;
		
		try {
			Reflection.call(this, "processObject", o);
		} catch (Exception ex) {
			System.out.println("Cannot process object of type " + o.getClass());
			if (!ex.toString().contains("There is no matching method"))
				ex.printStackTrace();
		}
	}

	private void processList(List<?> list) {
		if (list == null)
			return;
		for (Object o : list)
			processEntry(o);
	}

	public void processObject(CardDefinition card) {
		region(card.kw, "keyword");
		region(card.location(), "typename");
		processEntry(card.state);
		processList(card.templates);
		processList(card.contracts);
		processList(card.handlers);
		processScope(card.innerScope());
	}

	public void processObject(StateDefinition sd) {
		region(sd.location(), "keyword");
		processList(sd.fields);
	}
	
	public void processObject(StructField sf) {
		processEntry(sf.type);
		region(sf.location(), "field");
	}

	public void processObject(TypeReference tr) {
		region(tr.location(), "typename");
		if (tr.hasPolys())
			processList(tr.polys());
	}

	public void processObject(Template t) {
		region(t.kw, "keyword");
		region(t.location(), "field");
		processList(t.args);
		processEntry(t.content);
	}

	public void processObject(TemplateDiv td) {
		region(td.kw, "keyword");
		processList(td.attrs);
		processList(td.nested);
		processFormatsEvents(td);
	}

	public void processObject(TemplateList td) {
		region(td.kw, "keyword");
		region(td.listLoc, "field");
		region(td.iterLoc, "var");
		processList(td.formats);
		processFormats(td);
		processEntry(td.template);
	}

	public void processObject(TemplateCardReference cr) {
		region(cr.kw, "keyword");
		if (cr.explicitCard != null)
			region(cr.location, "typename");
		else
			region(cr.location, "field");
	}

	public void processObject(ContentExpr ce) {
		region(((Locatable)ce.expr).location(), "field");
		processFormatsEvents(ce);
	}
	
	public void processObject(EventHandler eh) {
		region(eh.actionPos, "keyword");
		region(eh.kw, "symbol");
		processEntry(eh.expr);
	}
	
	public void processObject(TemplateToken tt) {
		if (tt.type == TemplateToken.STRING) {
			region(tt.location, "literal");
		} else
			System.out.println("Handle template token " + tt.type);
		
//		region(cr.kw, "keyword");
//		if (cr.explicitCard != null)
//			region(cr.location, "typename");
//		else
//			region(cr.location, "field");
	}

	private void processFormatsEvents(TemplateFormatEvents td) {
		processList(td.handlers);
		processFormats(td);
	}

	private void processFormats(TemplateFormat td) {
		processList(td.formats);
	}

	public void processObject(ContractImplements ci) {
		region(ci.kw, "keyword");
		region(ci.location(), "typename");
		if (ci.varLocation != null)
			region(ci.varLocation, "field");
		processList(ci.methods);
	}
	public void processObject(HandlerImplements hi) {
		region(hi.kw, "keyword");
		region(hi.typeLocation, "typename");
		region(hi.location(), "typename");
		processList(hi.methods);
	}

	public void processObject(EventCaseDefn ecd) {
		region(ecd.kw, "keyword");
		region(ecd.location(), "methodname");
		processList(ecd.intro.args);
		processList(ecd.messages);
		processScope(ecd.innerScope());
	}

	public void processObject(MethodCaseDefn q) {
		region(q.location(), "methodname");
		processList(q.intro.args);
		processList(q.messages);
		processScope(q.innerScope());
	}

	public void processObject(VarPattern vp) {
		region(vp.location(), "var");
	}

	public void processObject(MethodMessage mm) {
		region(mm.kw, "symbol");
		if (mm.slot != null) {
			for (Locatable t : mm.slot) {
				region(t.location(), "field");
			}
		}
		processEntry(mm.expr);
	}
	
	public void processObject(ApplyExpr ae) {
		if (tryHerdingDots(ae)) {
			Object a2 = ae.args.get(1);
			System.out.println("a2 = " + a2.getClass());
			if (a2 instanceof UnresolvedVar)
				region(ae.location(), "field");
			else
				region(ae.location(), "typename");
			return;
		}
		processEntry(ae.fn);
		processList(ae.args);
	}

	private boolean tryHerdingDots(Object expr) {
		if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr)expr;
			if (ae.fn instanceof UnresolvedOperator) {
				UnresolvedOperator op = (UnresolvedOperator) ae.fn;
				return op.op.equals(".") && tryHerdingDots(ae.args.get(0));
			} else
				return false;
		} else if (expr instanceof UnresolvedVar) {
			return true;
		} else {
			System.out.println("Cannot herd " + expr.getClass());
			return false;
		}
	}

	public void processObject(StringLiteral sl) {
		region(sl.location, "literal");
	}

	public void processObject(NumericLiteral nl) {
		region(nl.location, "literal");
	}

	public void processObject(UnresolvedVar uv) {
		region(uv.location(), "var");
	}

	public void processObject(UnresolvedOperator op) {
		System.out.println("op = " + op.op);
		region(op.location(), "symbol");
	}

	private void region(InputPosition location, String style) {
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
			rs.add(new TypedRegion(r.getOffset() + s, e-s, style));
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ITypedRegion[] toArray() {
		return rs.toArray(new ITypedRegion[rs.size()]);
	}
}
