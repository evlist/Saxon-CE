package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.RoleLocator;
import client.net.sf.saxon.ce.expr.TypeChecker;
import client.net.sf.saxon.ce.expr.instruct.CallTemplate;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.ScheduleExecution;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.value.SequenceType;

/**
 * Extension element for Saxon client edition: ixsl:schedule-action. The content of the element
 * is currently restricted to a single xsl:call-template instruction
 */
public class IXSLScheduleAction extends StyleElement {

    Expression wait;
    XSLCallTemplate instruction;

    public boolean isInstruction() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        String waitAtt=null;

        AttributeCollection atts = getAttributeList();

        for (int a=0; a<atts.getLength(); a++) {
            int nc = atts.getNameCode(a);
            String f = getNamePool().getClarkName(nc);
            if (f.equals("wait")) {
                waitAtt = atts.getValue(a);
            } else {
                checkUnknownAttribute(nc);
            }
        }

        if (waitAtt!=null) {
            wait = makeExpression(waitAtt);
        }
    }

    public void validate(Declaration decl) throws XPathException {

        ExpressionVisitor visitor = makeExpressionVisitor();
        if (wait != null) {
            wait = typeCheck(wait);
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "ixsl:schedule-action/wait", 0);
                wait = TypeChecker.staticTypeCheck(wait, SequenceType.SINGLE_INTEGER, false, role, visitor);
            } catch (XPathException err) {
                compileError(err);
            }
        }

        boolean found = false;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            } else if (child instanceof XSLFallback) {
                // do nothing;
            } else if (child instanceof XSLCallTemplate) {
                if (found) {
                    compileError("ixsl:schedule-action must contain a single xsl:call-template instruction");
                }
                found = true;
                instruction = (XSLCallTemplate)child;
            }
        }
        if (!found) {
            compileError("ixsl:schedule-action must contain a single xsl:call-template instruction");
        }
    }


    @Override
    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        CallTemplate call = (CallTemplate)instruction.compile(exec, decl);
        call.setUseTailRecursion(true);
        return new ScheduleExecution(call, wait);
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.