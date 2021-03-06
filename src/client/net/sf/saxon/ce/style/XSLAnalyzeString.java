package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.instruct.AnalyzeString;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.type.ItemType;

/**
* An xsl:analyze-string elements in the stylesheet. New at XSLT 2.0<BR>
*/

public class XSLAnalyzeString extends StyleElement {

    private Expression select;
    private Expression regex;
    private Expression flags;
    private StyleElement matching;
    private StyleElement nonMatching;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain an xsl:fallback
    * instruction
    */

    public boolean mayContainFallback() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
    }

     public void prepareAttributes() throws XPathException {
		String selectAtt = null;
		String regexAtt = null;
		String flagsAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.REGEX)) {
        		regexAtt = atts.getValue(a);
			} else if (f.equals(StandardNames.SELECT)) {
        		selectAtt = atts.getValue(a);
			} else if (f.equals(StandardNames.FLAGS)) {
        		flagsAtt = atts.getValue(a); // not trimmed, see bugzilla 4315
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt==null) {
            reportAbsence("select");
            selectAtt = ".";    // for error recovery
        }
        select = makeExpression(selectAtt);

        if (regexAtt==null) {
            reportAbsence("regex");
            regexAtt = "xxx";      // for error recovery
        }
        regex = makeAttributeValueTemplate(regexAtt);

        if (flagsAtt==null) {
            flagsAtt = "";
        }
        flags = makeAttributeValueTemplate(flagsAtt);

    }


    public void validate(Declaration decl) throws XPathException {
        //checkWithinTemplate();

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof XSLFallback) {
                // no-op
            } else if (curr instanceof XSLMatchingSubstring) {
                boolean b = curr.getLocalPart().equals("matching-substring");
                if (b) {
                    if (matching!=null) {
                        compileError("xsl:matching-substring element must only appear once", "XTSE0010");
                    }
                    matching = (StyleElement)curr;
                } else {
                    if (nonMatching!=null) {
                        compileError("xsl:non-matching-substring element must only appear once", "XTSE0010");
                    }
                    nonMatching = (StyleElement)curr;
                }
            } else {
                compileError("Only xsl:matching-substring and xsl:non-matching-substring are allowed here", "XTSE0010");
            }
        }

        if (matching==null && nonMatching==null) {
            compileError("At least one xsl:matching-substring or xsl:non-matching-substring element must be present",
                    "XTSE1130");
        }

        select = typeCheck(select);
        regex = typeCheck(regex);
        flags = typeCheck(flags);

    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        Expression matchingBlock = null;
        if (matching != null) {
            matchingBlock = matching.compileSequenceConstructor(exec, decl, matching.iterateAxis(Axis.CHILD));
        }

        Expression nonMatchingBlock = null;
        if (nonMatching != null) {
            nonMatchingBlock = nonMatching.compileSequenceConstructor(exec, decl, nonMatching.iterateAxis(Axis.CHILD));
        }

        try {
            ExpressionVisitor visitor = makeExpressionVisitor();
            return new AnalyzeString(select,
                                     regex,
                                     flags,
                                     (matchingBlock==null ? null : matchingBlock.simplify(visitor)),
                                     (nonMatchingBlock==null ? null : nonMatchingBlock.simplify(visitor)));
        } catch (XPathException e) {
            compileError(e);
            return null;
        }
    }



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
