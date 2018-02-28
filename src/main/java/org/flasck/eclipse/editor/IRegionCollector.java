package org.flasck.eclipse.editor;

import org.eclipse.jface.text.ITypedRegion;
import org.flasck.flas.blockForm.InputPosition;

public interface IRegionCollector {
	void add(InputPosition location, String style);

	void process();

	ITypedRegion[] toArray();
}
