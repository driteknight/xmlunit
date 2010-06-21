/*
  This file is licensed to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package net.sf.xmlunit.diff;

import net.sf.xmlunit.util.Convert;
import javax.xml.transform.Source;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

/**
 * Difference engine based on DOM.
 */
public final class DOMDifferenceEngine implements DifferenceEngine {
    private final ComparisonListenerSupport listeners =
        new ComparisonListenerSupport();
    private ElementSelector elementSelector = ElementSelectors.Default;
    private DifferenceEvaluator diffEvaluator = DifferenceEvaluators.Default;

    public void addComparisonListener(ComparisonListener l) {
        if (l == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.addComparisonListener(l);
    }

    public void addMatchListener(ComparisonListener l) {
        if (l == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.addMatchListener(l);
    }

    public void addDifferenceListener(ComparisonListener l) {
        if (l == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.addDifferenceListener(l);
    }

    public void setElementSelector(ElementSelector s) {
        if (s == null) {
            throw new IllegalArgumentException("element selector must"
                                               + " not be null");
        }
        elementSelector = s;
    }

    public void setDifferenceEvaluator(DifferenceEvaluator e) {
        if (e == null) {
            throw new IllegalArgumentException("difference evaluator must"
                                               + " not be null");
        }
        diffEvaluator = e;
    }

    public void compare(Source control, Source test) {
        if (control == null) {
            throw new IllegalArgumentException("control must not be null");
        }
        if (test == null) {
            throw new IllegalArgumentException("test must not be null");
        }
        compareNodes(Convert.toNode(control), Convert.toNode(test));
    }

    /**
     * Recursively compares two XML nodes.
     *
     * <p>Performs comparisons common to all node types, the performs
     * the node type specific comparisons and finally recures into
     * the node's child lists.</p>
     *
     * <p>Stops as soon as any comparison returns
     * ComparisonResult.CRITICAL.</p>
     *
     * <p>package private to support tests.</p>
     */
    ComparisonResult compareNodes(Node control, Node test) {
        ComparisonResult lastResult =
            compare(new Comparison(ComparisonType.NODE_TYPE, control,
                                    null, control.getNodeType(),
                                    test, null, test.getNodeType()));
        if (lastResult == ComparisonResult.CRITICAL) {
            return lastResult;
        }
        lastResult =
            compare(new Comparison(ComparisonType.NAMESPACE_URI, control,
                                    null, control.getNamespaceURI(),
                                    test, null, test.getNamespaceURI()));
        if (lastResult == ComparisonResult.CRITICAL) {
            return lastResult;
        }
        lastResult =
            compare(new Comparison(ComparisonType.NAMESPACE_PREFIX, control,
                                    null, control.getPrefix(),
                                    test, null, test.getPrefix()));
        if (lastResult == ComparisonResult.CRITICAL) {
            return lastResult;
        }
        NodeList controlChildren = control.getChildNodes();
        NodeList testChildren = test.getChildNodes();
        lastResult =
            compare(new Comparison(ComparisonType.CHILD_NODELIST_LENGTH,
                                    control, null, controlChildren.getLength(),
                                    test, null, testChildren.getLength()));
        if (lastResult == ComparisonResult.CRITICAL) {
            return lastResult;
        }
        /* TODO node type specific stuff */
        return compareNodeLists(controlChildren, testChildren);
    }

    ComparisonResult compareNodeLists(NodeList control, NodeList test) {
        return ComparisonResult.EQUAL;
    }

    /**
     * Compares the detail values for object equality, lets the
     * difference evaluator evaluate the result, notifies all
     * listeners and returns the outcome.
     *
     * <p>package private to support tests.</p>
     */
    ComparisonResult compare(Comparison comp) {
        Object controlValue = comp.getControlNodeDetails().getValue();
        Object testValue = comp.getTestNodeDetails().getValue();
        boolean equal = controlValue == null
            ? testValue == null : controlValue.equals(testValue);
        ComparisonResult initial =
            equal ? ComparisonResult.EQUAL : ComparisonResult.DIFFERENT;
        ComparisonResult altered = diffEvaluator.evaluate(comp, initial);
        listeners.fireComparisonPerformed(comp, altered);
        return altered;
    }
}