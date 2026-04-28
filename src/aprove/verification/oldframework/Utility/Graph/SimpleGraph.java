package aprove.verification.oldframework.Utility.Graph;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

import org.json.*;

import aprove.*;
import aprove.prooftree.Export.Utility.*;
import aprove.verification.oldframework.Utility.*;
import aprove.verification.oldframework.Utility.GenericStructures.*;
import aprove.verification.oldframework.Utility.JSON.*;
import immutables.*;

/**
 * This class represents a directed graph.
 *
 * @author Carsten Pelikan, Peter Schneider-Kamp
 * @version $Id$
 */
public class SimpleGraph<N, E> implements java.io.Serializable, PrettyStringable, Exportable, JSONExport {

    /**
     * Integer constant representing the toDOT() method.
     */
    protected static final int DOT = 0;

    /**
     * Integer constant representing the toDOTDOT() method.
     */
    protected static final int DOTDOT1 = 4;

    /**
     * Integer constant representing the
     * toDOTDOT(boolean,float,float,boolean) method.
     */
    protected static final int DOTDOT2 = 5;

    /**
     * Integer constant representing the toSaveDOTwithEdges() method.
     */
    protected static final int EDGES = 2;

    /**
     * Integer constant representing the toInteractiveDOTwithEdges()
     * method.
     */
    protected static final int INTERACTIVE = 3;

    /**
     * Integer constant representing the toSaveDOT() method.
     */
    protected static final int SAVE = 1;

    private static final long serialVersionUID = -1932909372118750525L;

    /**
     * The set of edges of this graph. Changes in the graph are reflected in
     * this set. However, it is (up to now) not possible to change this set like
     * it is possible for the node-set.
     */
    final Set<Edge<E, N>> edges = new AbstractSet<Edge<E, N>>() {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean contains(final Object edge) {
            if (edge instanceof Edge) {
                final Edge<E, N> e = (Edge) edge;
                return SimpleGraph.this.contains(e.startNode, e.endNode);
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            for (final Map<Node<N>, E> outMap : SimpleGraph.this.out.values()) {
                if (!outMap.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Iterator<Edge<E, N>> iterator() {

            final Iterator<Map.Entry<Node<N>, Map<Node<N>, E>>> outMapIterator = SimpleGraph.this.out
                .entrySet().iterator();
            final int requiredModCount = SimpleGraph.this.modCount;

            return new Iterator<Edge<E, N>>() {

                Iterator<Map.Entry<Node<N>, E>> edgeIterator = null;

                Edge<E, N> nextEdge = null;

                boolean nextInvalid = true;

                Node<N> src = null;

                @Override
                public boolean hasNext() {
                    this.computeNext();
                    return this.nextEdge != null;
                }

                @Override
                public Edge<E, N> next() {
                    this.computeNext();
                    if (this.nextEdge == null) {
                        throw new NoSuchElementException();
                    } else {
                        this.nextInvalid = true;
                        return this.nextEdge;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                private void computeNext() {
                    if (SimpleGraph.this.modCount != requiredModCount) {
                        throw new ConcurrentModificationException();
                    }
                    while (this.nextInvalid) {
                        if (this.edgeIterator == null) {
                            if (outMapIterator.hasNext()) {
                                final Map.Entry<Node<N>, Map<Node<N>, E>> nextOut = outMapIterator
                                    .next();
                                this.edgeIterator = nextOut.getValue().entrySet()
                                    .iterator();
                                this.src = nextOut.getKey();
                            } else {
                                this.nextEdge = null;
                                this.nextInvalid = false;
                            }
                        } else {
                            if (this.edgeIterator.hasNext()) {
                                final Map.Entry<Node<N>, E> next = this.edgeIterator
                                    .next();
                                this.nextEdge = new Edge<E, N>(this.src, next.getKey(),
                                    next.getValue());
                                this.nextInvalid = false;
                            } else {
                                this.edgeIterator = null;
                            }
                        }
                    }
                }

            };
        }

        @Override
        public int size() {
            int size = 0;
            for (final Map<Node<N>, E> outMap : SimpleGraph.this.out.values()) {
                size += outMap.size();
            }
            return size;
        }

    };

    /**
     * The set of nodes of this graph. This set may directly be modified and
     * changes are reflected in the corresponding graph.
     */
    final Set<Node<N>> nodes =

        new SerializableSet<Node<N>>() {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean add(final Node<N> node) {
            return SimpleGraph.this.addNode(node);
        }

        @Override
        public boolean addAll(final Collection<? extends Node<N>> nodes) {
            boolean changed = false;
            for (final Node<N> node : nodes) {
                if (SimpleGraph.this.addNode(node)) {
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public void clear() {

            SimpleGraph.this.clearGraph();

        }

        @Override
        public boolean contains(final Object node) {
            return SimpleGraph.this.out.containsKey(node);
        }

        @Override
        public boolean containsAll(final Collection<?> arg0) {
            for (final Object o : arg0) {
                if (!SimpleGraph.this.out.containsKey(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(final Object o) {
            return SimpleGraph.this.out.keySet().equals(o);
        }

        @Override
        public int hashCode() {
            return SimpleGraph.this.out.keySet().hashCode();
        }

        @Override
        public boolean isEmpty() {
            return SimpleGraph.this.out.isEmpty();
        }

        @Override
        public Iterator<Node<N>> iterator() {

            return new Iterator<Node<N>>() {

                private final Iterator<Map.Entry<Node<N>, Map<Node<N>, E>>> iter = SimpleGraph.this.out
                    .entrySet().iterator();

                private Map.Entry<Node<N>, Map<Node<N>, E>> lastReturned = null;

                @Override
                public boolean hasNext() {
                    return this.iter.hasNext();
                }

                @Override
                public Node<N> next() {
                    this.lastReturned = this.iter.next();
                    return this.lastReturned.getKey();
                }

                @Override
                public void remove() {
                    if (this.lastReturned == null) {
                        throw new IllegalStateException();
                    } else {
                        this.iter.remove();
                        SimpleGraph.this.removeNode(this.lastReturned.getKey(),
                            this.lastReturned.getValue());
                        this.lastReturned = null;
                    }

                }

            };

        }

        @Override
        public boolean remove(final Object node) {
            return SimpleGraph.this.removeNode((Node<N>) node);
        }

        @Override
        public boolean removeAll(final Collection<?> arg0) {
            boolean changed = false;
            for (final Node<N> node : SimpleGraph.this.nodes) {
                if (SimpleGraph.this.removeNode(node)) {
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean retainAll(final Collection<?> set) {

            boolean changed = false;
            final Iterator<Node<N>> it = this.iterator();

            while (it.hasNext()) {
                if (!set.contains(it.next())) {
                    it.remove();
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public int size() {
            return SimpleGraph.this.out.size();
        }

        @Override
        public Object[] toArray() {
            final int n = this.size();
            final Object[] a = new Object[n];
            int i = 0;
            for (final Node<N> node : this) {
                a[i] = node;
                i++;
            }
            return a;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            final int n = this.size();
            int m = a.length;
            if (m < n) {
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass()
                    .getComponentType(), n);
                m = n;
            }
            int i = 0;
            final Object[] b = a;
            for (final Node<N> node : this) {
                b[i] = node;
                i++;
            }
            if (m != n) {
                b[i] = null;
            }
            return a;
        }

        @Override
        public String toString() {
            return SimpleGraph.this.out.keySet().toString();
        }

    };

    /**
     * The set of equivalence Classes of this graph. To nodes s and s' are
     * equivalent iff whenever s -> / <- t then s' -> / <- t' where t equiv t'
     */
    private volatile ImmutableSet<Set<Node<N>>> equivClasses = null;

    /**
     * A mapping from nodes to ingoing edges. These are mirrored values of out.
     * (Used for fast lookup of what incoming edges a node has)
     */
    private HashMap<Node<N>, Map<Node<N>, E>> in;

    private int modCount = 0;

    /**
     * A mapping from nodes to outgoing edges. These are the real values;
     *
     * Usually we iterate over this map and not over the dual in-map. Hence this
     * is a linked hash map whereas in is only a HashMap.
     */
    private LinkedHashMap<Node<N>, Map<Node<N>, E>> out;

    /**
     * Create an empty graph.
     */
    public SimpleGraph() {

        this(new LinkedHashSet<Node<N>>());

    }

    /**
     * Creates a graph from the given set of nodes. That graph will not contain
     * any edges yet.
     *
     * @param nodes
     *            The set of nodes for the new graph.
     */
    public SimpleGraph(final Set<Node<N>> nodes) {
        this(nodes, new LinkedHashSet<Edge<E, N>>());
    }

    /**
     * Creates a graph from the given set of nodes and edges.
     *
     * @param nodes
     *            The set of nodes for the new graph.
     * @param edges
     *            The set of edges the new graph should contain.
     */
    public SimpleGraph(final Set<Node<N>> nodes, final Set<Edge<E, N>> edges) {
        final int n = nodes.size();

        this.in = new HashMap<Node<N>, Map<Node<N>, E>>(n);
        this.out = new LinkedHashMap<Node<N>, Map<Node<N>, E>>(n);
        for (final Node<N> node : nodes) {
            this.addNode(node);
        }
        for (final Edge<E, N> edge : edges) {
            this.addEdge(edge.startNode, edge.endNode, edge.object);
        }
        this.modCount = 1;
    }

    /**
     * Creates a (sub-)graph consisting only of the given nodes and the
     * connections between them according to a given graph.
     */
    public SimpleGraph(final Set<Node<N>> nodes, final SimpleGraph<N, E> graph) {

        this(nodes);
        for (final Node<N> src : nodes) {
            final Map<Node<N>, E> edges = graph.out.get(src);
            // check whether src node is in nodes
            if (edges != null) {
                for (final Map.Entry<Node<N>, E> edge : edges.entrySet()) {
                    final Node<N> dest = edge.getKey();
                    // if dest node is also in nodes, then add the edge
                    if (nodes.contains(dest)) {
                        this.addEdge(src, dest, edge.getValue());
                    }
                }
            }
        }
    }

    /**
     * creates a copy of the other graph without creating new nodes
     *
     * @param other
     */
    SimpleGraph(final SimpleGraph<N, E> other) {
        this.in = new HashMap<Node<N>, Map<Node<N>, E>>(other.in);
        this.out = new LinkedHashMap<Node<N>, Map<Node<N>, E>>(other.out);
        for (final Map.Entry<Node<N>, Map<Node<N>, E>> entry : this.in.entrySet()) {
            entry.setValue(new LinkedHashMap<Node<N>, E>(entry.getValue()));
        }
        for (final Map.Entry<Node<N>, Map<Node<N>, E>> entry : this.out.entrySet()) {
            entry.setValue(new LinkedHashMap<Node<N>, E>(entry.getValue()));
        }
        this.modCount = 1;
        this.equivClasses = null;
    }

    /**
     * Adds this edge to the graph. In case any of the nodes is not already part
     * of the graph it will be added.
     *
     * @param edge
     *            The edge to add.
     * @return true, if a new edge was added
     */
    public boolean addEdge(final Edge<E, N> edge) {
        return this.addEdge(edge.startNode, edge.endNode, edge.object);
    }

    /**
     * Adds an (unlabeled) edge from a given start node to a given end node to
     * this graph.
     *
     * @param start
     *            the <code>Node</code> where the edge starts
     * @param end
     *            the <code>Node</code> where the edge ends
     * @return true, if a new edge was added
     */
    public boolean addEdge(final Node<N> start, final Node<N> end) {

        return this.addEdge(start, end, null);

    }

    /**
     * Adds an edge from a given start node to a given end node to this graph,
     * which is labeled by an <code>Object</code>.
     *
     * @param start
     *            the <code>Node</code> where the edge starts
     * @param end
     *            the <code>Node</code> where the edge ends
     * @param label
     *            an <code>Object</code> associated with this
     *            <code>Edge</code>
     * @return true, if a new edge was added
     */
    public boolean addEdge(final Node<N> start, final Node<N> end, final E label) {

        this.addNode(start);
        this.addNode(end);

        final Map<Node<N>, E> out = this.out.get(start);
        if (out.containsKey(end)) {
            return false;
        } else {
            out.put(end, label);
            this.in.get(end).put(start, label);
            this.modified();
            return true;
        }
    }

    /**
     * Adds a node to the graph.
     *
     * @param node
     *            The Node to add.
     * @return true iff the node was added (was a new node)
     */
    public boolean addNode(final Node<N> node) {
        if (node == null || this.out.containsKey(node)) {
            return false;
        } else {
            this.modified();
            this.out.put(node, new LinkedHashMap<Node<N>, E>());
            this.in.put(node, new LinkedHashMap<Node<N>, E>());
            return true;
        }

    }

    /**
     * deletes all nodes in the graph
     */
    public void clearGraph() {
        this.out.clear();
        this.in.clear();
    }

    /**
     * Checks whether the graph contains a given node.
     *
     * @param node
     *            Node to check for.
     * @return True if the graph contains such a node.
     */
    public boolean contains(final Node<N> node) {

        return this.out.containsKey(node);

    }

    /**
     * Checks whether the graph contains an edge from start to end
     */
    public boolean contains(final Node<N> start, final Node<N> end) {

        final Map<Node<N>, E> out = this.out.get(start);
        if (out == null) {
            return false;
        } else {
            return out.containsKey(end);
        }
    }

    /**
     * Determine all nodes reachable from a given set of start nodes.
     *
     * @param start
     *            the start nodes, from which all reachable nodes should be
     *            determined
     * @return all nodes reachable from a node within <code>start</code>. The
     *         returned set may be modified.
     */
    public Set<Node<N>> determineReachableNodes(final Collection<Node<N>> start) {

        final Set<Node<N>> result = new LinkedHashSet<Node<N>>(start);
        final Stack<Node<N>> todo = new Stack<Node<N>>();
        for (final Node<N> node : start) {
            todo.push(node);
        }
        while (!todo.isEmpty()) {
            final Node<N> node = todo.pop();
            for (final Node<N> succ : this.out.get(node).keySet()) {
                if (result.add(succ)) {
                    todo.push(succ);
                }
            }

        }
        return result;

    }

    /**
     * Determine all nodes reachable from a given set of start nodes.
     *
     * @param start
     *            the start nodes, from which all reachable nodes should be
     *            determined
     * @param filter
     *            a filter which edges may be selected
     * @return all nodes reachable from a node within <code>start</code>. The
     *         returned set may be modified.
     */
    public Set<Node<N>> determineReachableNodes(final Collection<Node<N>> start,
        final EdgeFilter<E, N> filter) {

        final Set<Node<N>> result = new LinkedHashSet<Node<N>>(start);
        final Stack<Node<N>> todo = new Stack<Node<N>>();
        for (final Node<N> node : start) {
            todo.push(node);
        }
        while (!todo.isEmpty()) {
            final Node<N> node = todo.pop();
            for (final Map.Entry<Node<N>, E> succ : this.out.get(node).entrySet()) {
                final Node<N> target = succ.getKey();
                if (filter.selectEdge(node, target, succ.getValue())) {
                    if (result.add(target)) {
                        todo.push(target);
                    }
                }
            }

        }
        return result;

    }

    /**
     * Checks if thew given graph is equal to this graph. This is not a check
     * for isomorphism.
     *
     * @param o
     *            Graph to check equality on.
     * @return True if the graphs are equal.
     */
    @Override
    public boolean equals(final Object o) {
        if (o != null && o instanceof SimpleGraph) {
            final SimpleGraph g = (SimpleGraph) o;
            return this.out.equals(g.out);
        } else {
            return false;
        }
    }

    @Override
    public String export(Export_Util eu) {
        return this.export("Graph:", false, eu);
    }

    /**
     * @param title The title for the graph export.
     * @param renderGraph Should this graph be rendered in HTML output?
     * @param eu The export utility.
     * @return A String representation of this graph formatted according to the export utility.
     */
    public String export(String title, boolean renderGraph, Export_Util eu) {
        final StringBuilder sb = new StringBuilder();
        sb.append(eu.export(title));
        sb.append(eu.linebreak());
        if (renderGraph && eu instanceof HTML_Util) {
            try {
                final File tempDOT = File.createTempFile("aprove", ".dot");
                tempDOT.deleteOnExit();
                final File tempSVG = File.createTempFile("aprove", ".svg");
                tempSVG.deleteOnExit();
                try (final PrintWriter pw = new PrintWriter(tempDOT)) {
                    pw.write(this.toDOT());
                }
                final Runtime run = Runtime.getRuntime();
                final Process pro =
                    run.exec("dot -Tsvg -o " + tempSVG.getAbsolutePath() + " " + tempDOT.getAbsolutePath());
                pro.waitFor();
                tempDOT.delete();
                final StringBuilder buffer = new StringBuilder();
                try (final Reader re = new BufferedReader(new FileReader(tempSVG), 4096)) {
                    while (true) {
                        final int ch = re.read();
                        if (ch == -1) {
                            break;
                        }
                        buffer.append((char) ch);
                    }
                }
                tempSVG.delete();
                final int index = buffer.indexOf("<svg ");
                final String svg = buffer.substring(index);
                sb.append(eu.linebreak());
                sb.append(eu.export(svg));
                sb.append(eu.linebreak());
            } catch (final IOException e) {
                e.printStackTrace();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        } else if (eu instanceof HTML_Util) {
            sb.append(eu.linebreak());
            sb.append("<textarea cols=\"80\" rows=\"25\">");
            sb.append(eu.export(this.toDOT()));
            sb.append("</textarea>");
            sb.append(eu.linebreak());
        } else {
            sb.append(eu.linebreak());
            sb.append(eu.export(JSONExportUtil.toJSONString(this)));
            sb.append(eu.linebreak());
        }
        return sb.toString();
    }

    /**
     * <b>Attention: This algorithm assumes that the graph is undirected (for
     * (x,y) there also exists (y, x))!</b> A clique is a maximal set of at
     * least three nodes where for every two contained nodes x, y there is an
     * edge (x, y).
     * @param k the minimal size of the returned cliques
     * @return cliques of this graph
     * @see http://www.dharwadker.org/clique/
     */
    public Collection<Collection<Node<N>>> getCliques(final int minSize) {
        final Collection<Collection<Node<N>>> maximalCliques =
            new LinkedHashSet<Collection<Node<N>>>();
        for (final Node<N> node : this.nodes) {
            final Collection<Node<N>> clique = new LinkedHashSet<Node<N>>();
            clique.add(node);
            Collection<Node<N>> largerClique = this.enlargeClique(clique);
            for (int r = 1; r < minSize; r++) {
                for (int i = 0; i < r; i++) {
                    largerClique = this.cliqueHelperTwo(largerClique);
                }
            }
            maximalCliques.add(largerClique);
        }
        final Collection<Collection<Node<N>>> result =
            new LinkedHashSet<Collection<Node<N>>>();
        final List<Pair<Collection<Node<N>>, Collection<Node<N>>>> pairs =
            Collection_Util.getPairs(maximalCliques);
        // also add (x, x)
        for (final Collection<Node<N>> clique : maximalCliques) {
            pairs.add(new Pair<Collection<Node<N>>, Collection<Node<N>>>(clique, clique));
        }
        for (final Pair<Collection<Node<N>>, Collection<Node<N>>> pair : pairs) {
            final Collection<Node<N>> intersected =
                new LinkedHashSet<Node<N>>(pair.x);
            intersected.retainAll(pair.y);
            Collection<Node<N>> largerClique = this.enlargeClique(intersected);
            for (int r = 1; r < minSize; r++) {
                for (int i = 0; i < r; i++) {
                    largerClique = this.cliqueHelperTwo(largerClique);
                }
            }
            result.add(largerClique);
        }
        return result;
    }

    /**
     * <b>Attention: This algorithm assumes that the graph is undirected (for
     * (x,y) there also exists (y, x))!</b> A clique is a maximal set of at
     * least three nodes where for every two contained nodes x, y there is an
     * edge (x, y).
     * @param k the minimal size of the returned cliques
     * @param removeRedundancy if true, edges are removed (only from the output)
     * once they are found to be in a clique
     * @return cliques of this graph
     * @see http://www.dharwadker.org/clique/
     */
    public Collection<Collection<Node<N>>> getCliques(final int minSize,
        final boolean removeRedundancy) {
        final Collection<Collection<Node<N>>> cliques = new LinkedHashSet<Collection<Node<N>>>();

        if (removeRedundancy) {
            // remove edges that are part of a clique before computing the next clique
            final SimpleGraph<N, E> graphCopy = new SimpleGraph<N, E>(this);
            boolean foundClique;
            do {
                foundClique = false;
                for (final Collection<Node<N>> clique : graphCopy.getCliques(minSize)) {
                    if (clique.size() < minSize) {
                        continue;
                    }
                    foundClique = true;
                    cliques.add(clique);
                    for (final Pair<Node<N>, Node<N>> pair : Collection_Util.getPairs(clique)) {
                        graphCopy.removeEdge(pair.x, pair.y);
                        graphCopy.removeEdge(pair.y, pair.x);
                    }
                    break;
                }
            } while (foundClique);
        } else {
            for (final Collection<Node<N>> clique : this.getCliques(minSize)) {
                if (clique.size() < minSize) {
                    continue;
                }
                cliques.add(clique);
            }
        }

        return cliques;
    }

    /**
     * Creates a copy of this graph (without creating new nodes, i.e. labels of
     * nodes remain shared!)
     */
    public SimpleGraph<N, E> getCopy() {
        return new SimpleGraph<N, E>(this);
    }

    /**
     * Return the edge between two given nodes.
     *
     * @return The edge between two given nodes, if it exists, null otherwise.
     */
    public Edge<E, N> getEdge(final Node<N> start, final Node<N> end) {
        final Map<Node<N>, E> out = this.out.get(start);

        if (out != null && out.containsKey(end)) {
            return new Edge<E, N>(start, end, out.get(end));
        } else {
            return null;
        }
    }

    /**
     * Returns object which is stored in edge which is specified by start and
     * end Node. *
     *
     * @param start
     *            start node of the specified edge
     * @param end
     *            end node of the specified edge
     * @return object which labels the specified edge. A null-value can occur if
     *         the edge is not present, or if the label was null.
     */
    public E getEdgeObject(final Node<N> start, final Node<N> end) {

        final Map<Node<N>, E> outMap = this.out.get(start);
        if (outMap != null) {
            return outMap.get(end);
        } else {
            return null;
        }
    }

    /**
     * Returns the set of edges of this graph. The returned set cannot be
     * modified. However, changes in the graph will be reflected in this set.
     *
     * @return The set of edges of this graph.
     */
    public Set<Edge<E, N>> getEdges() {
        return this.edges;
    }

    /**
     * Returns all equivalence classes. Two nodes n and m are equivalent iff for
     * all nodes k we have n->k iff m->k and k->n iff k->m IMPORTANT: do not
     * change the graph after calling this method. Otherwise, the equivalence
     * classes will be incorrect.
     *
     * NOTE (noschinski): This method exists probably for historical reasons. It
     * seems, that getSCCs computes the same thing and is a lot more widely
     * used. So consider using that method instead.
     */
    public ImmutableSet<Set<Node<N>>> getEquivalenceClasses() {
        if (this.equivClasses == null) {
            synchronized (this) {
                if (this.equivClasses == null) {
                    this.equivClasses = this.getPartitions(this.getNodes());
                }
            }
        }
        return this.equivClasses;
    }

    /**
     * Get all nodes which are parents of this node, e.g., there are incoming
     * edges to this node from the parents. Do not modify the returned set!
     *
     * @param node
     *            The <code>Node</code> for which to return its parents.
     * @return A set of all parents of the given node.
     */
    public Set<Node<N>> getIn(final Node<N> node) {

        return this.in.get(node).keySet();

    }

    /**
     * Gets all edges ending in a specified node
     *
     * @param node
     *            the <code>Node</code>, of which all entering edges should
     *            be determined, this node must be present in the graph.
     * @return all edges ending in <code>node</code>
     */
    public Set<Edge<E, N>> getInEdges(final Node<N> dest) {
        if (Globals.useAssertions) {
            assert (this.out.containsKey(dest));
        }
        final Map<Node<N>, E> theInMap = this.in.get(dest);

        return new AbstractSet<Edge<E, N>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean contains(final Object e) {
                final Edge<N, E> edge = (Edge) e;
                if (edge.endNode.equals(dest)) {
                    return theInMap.containsKey(edge.startNode);
                } else {
                    return false;
                }
            }

            @Override
            public boolean isEmpty() {
                return theInMap.isEmpty();
            }

            @Override
            public Iterator<Edge<E, N>> iterator() {

                final Iterator<Map.Entry<Node<N>, E>> inMapIter = theInMap
                    .entrySet().iterator();

                return new Iterator<Edge<E, N>>() {

                    @Override
                    public boolean hasNext() {
                        return inMapIter.hasNext();
                    }

                    @Override
                    public Edge<E, N> next() {
                        final Map.Entry<Node<N>, E> src = inMapIter.next();
                        return new Edge<E, N>(src.getKey(), dest, src
                            .getValue());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };

            }

            @Override
            public int size() {
                return theInMap.size();
            }

        };

    }

    /**
     * Returns the set of nodes of this graph. Note that all changes in this set
     * will be reflected in this graph and vice versa.
     *
     * @return The set of nodes of this graph.
     */
    public Set<Node<N>> getNodes() {
        return this.nodes;
    }

    /**
     * Get all nodes which are children of this node, e.g., which can be reached
     * from this node by one edge. Do not modify the returned set!
     *
     * @param node
     *            The <code>Node</code> for which to return its children.
     * @return A set of all children of the given node.
     */
    public Set<Node<N>> getOut(final Node<N> node) {

        return this.out.get(node).keySet();

    }

    /**
     * Gets all edges starting at a specified node
     *
     * @param node
     *            the <code>Node</code>, from which all leaving edges should
     *            be determined, this node must be present in the graph.
     *
     * @return all edges leaving <code>node</code> as keys and the values are
     *         the labels
     */
    public Set<Edge<E, N>> getOutEdges(final Node<N> src) {

        if (Globals.useAssertions) {
            assert (this.out.containsKey(src));
        }

        final Map<Node<N>, E> theOutMap = this.out.get(src);

        return new AbstractSet<Edge<E, N>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean contains(final Object e) {
                final Edge<N, E> edge = (Edge) e;
                if (edge.startNode.equals(src)) {
                    return theOutMap.containsKey(edge.endNode);
                } else {
                    return false;
                }
            }

            @Override
            public boolean isEmpty() {
                return theOutMap.isEmpty();
            }

            @Override
            public Iterator<Edge<E, N>> iterator() {

                final Iterator<Map.Entry<Node<N>, E>> outMapIter = theOutMap
                    .entrySet().iterator();

                return new Iterator<Edge<E, N>>() {

                    @Override
                    public boolean hasNext() {
                        return outMapIter.hasNext();
                    }

                    @Override
                    public Edge<E, N> next() {
                        final Map.Entry<Node<N>, E> dest = outMapIter.next();
                        return new Edge<E, N>(src, dest.getKey(), dest
                            .getValue());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };

            }

            @Override
            public int size() {
                return theOutMap.size();
            }

        };
    }

    /**
     * Returns a shortest path between <code>start</code> and <code>end</code>
     * if there exists one.
     * @param start Start node of the path.
     * @param end End node of the path.
     * @return a list of nodes making up a path in this graph, with
     * <code>start</code> as first and <code>end</code> as last element. If no
     * path exists, the <code>null</code> pointer is returned.
     */
    public LinkedList<Node<N>> getPath(final Node<N> start, final Node<N> end) {
        return getPath(start,end,null);
    }
    
    /**
     * Returns a shortest path between <code>start</code> and <code>end</code>
     * if there exists one.
     * @param start Start node of the path.
     * @param end End node of the path.
     * @param filter filters edges to step over, null means no filtering
     * @return a list of nodes making up a path in this graph, with
     * <code>start</code> as first and <code>end</code> as last element. If no
     * path exists, the <code>null</code> pointer is returned.
     */
    public LinkedList<Node<N>> getPath(final Node<N> start, final Node<N> end, EdgeFilter<E,N> filter) {
        if (Globals.useAssertions) {
            assert (start != null && end != null);
        }

        final Map<Node<N>, Node<N>> reachedOver = new HashMap<Node<N>, Node<N>>();
        reachedOver.put(start, null);

        final Stack<Node<N>> todo = new Stack<Node<N>>();
        todo.push(start);

        while (!todo.isEmpty()) {
            final Node<N> node = todo.pop();
            if (node.equals(end)) {
                final LinkedList<Node<N>> res = new LinkedList<Node<N>>();
                res.add(node);
                Node<N> pred = reachedOver.get(node);
                while (pred != null) {
                    res.addFirst(pred);
                    pred = reachedOver.get(pred);
                }
                return res;
            }
            Map<Node<N>,E> outMap = this.out.get(node);
            for (final Map.Entry<Node<N>,E>  outMapEntry : this.out.get(node).entrySet()) {
            	Node<N> succ = outMapEntry.getKey();
            	if(filter == null || filter.selectEdge(node, succ, outMapEntry.getValue()) )
                if (!reachedOver.containsKey(succ)) {
                    reachedOver.put(succ, node);
                    todo.push(succ);
                }
            }
        }

        return null;
    }
    
    
    /**
     * Returns all paths leading from the <code>start</code> node to the <code>end</code> node
     * without taking any loop iteration of potential loops in this path
     * @param start Start node of the path
     * @param end End node of the path
     * @return A set of all the paths that lead from <code>start</code> to <code>end</code> without
     * taking a loop iteration
     */
    public Set<List<Node<N>>> getAllPaths(final Node<N> start, final Node<N> end) {
        if (Globals.useAssertions) {
            assert start != null;
            assert end != null;
            assert getNodes().contains(start);
            assert getNodes().contains(end);
        }
        return getAllPaths(start, end, null);
    }
    
    /**
     * Returns all paths leading from the <code>start</code> node to the <code>end</code> node
     * without taking any loop iteration of potential loops in this path
     * @param start Start node of the path
     * @param end End node of the path
     * @param edgeFilter Must return true on edges that should be considered. Can be null (= selecting all edges)
     * @return A set of all the paths that lead from <code>start</code> to <code>end</code> without
     * taking a loop iteration or traversing an edge not selected by <code>edgeFilter</code>
     */
    public Set<List<Node<N>>> getAllPaths(final Node<N> start, final Node<N> end, EdgeFilter<E, N> edgeFilter) {
        if (Globals.useAssertions) {
            assert start != null;
            assert end != null;
            assert getNodes().contains(start);
            assert getNodes().contains(end);
        }
        final Set<List<Node<N>>> paths = new LinkedHashSet<>();
        
        final Stack<List<Node<N>>> todo = new Stack<>();
        todo.add(Collections.singletonList(start));
        
        while (!todo.empty()) {
            List<Node<N>> path = todo.pop();
            if (Globals.useAssertions) {
                assert path.size() > 0;
            }
            Node<N> lastNodeInPath = path.get(path.size() - 1);
            for (Node<N> succ : this.out.get(lastNodeInPath).keySet()) {
            	if(edgeFilter != null && !edgeFilter.selectEdge(lastNodeInPath, succ, this.out.get(lastNodeInPath).get(succ))) {
            		//filter says we should ignore the edge from lastNodeInPath to succ
            		continue;
            	}
            	
                if (path.contains(succ)) {
                    // We have found a loop in this path -> stop on this path
                    continue;
                }
                
                List<Node<N>> extendedPath = new LinkedList<>(path);
                extendedPath.add(succ);
                if (succ == end) {
                    paths.add(extendedPath);
                } else {
                    todo.add(extendedPath);
                }
            }
        }
        
        return paths;
    }

    /**
     * Checks whether an <code>Object</code> implements the
     * <code>PrettyStringable</code> interface. If so, its method is invoked
     * and the resulting String is returned, otherwise, the
     * <code>toString</code>-result of the object is returned.
     * @param obj the <code>Object</code> to check for
     * <code>PrettyStringable</code>
     * @return the result of <code>prettyToString</code> or
     * <code>toString</code>, depending if the object implements the
     * <code>PrettyStringable</code> interface or not
     */
    public String getPrettyString(final Object obj) {
        if (obj != null) {
            return obj instanceof PrettyStringable ? ((PrettyStringable)obj).prettyToString() : obj.toString();
        } else {
            return "NULL";
        }
    }

    /**
     * Compute a list of all ranks of this graph. Note that the graph must be
     * acyclic.
     *
     * @return A list of list of nodes.
     */
    public List<Set<Node<N>>> getRanks() {
        if (Globals.useAssertions) {
            assert (this.getSCCs().isEmpty());
        }
        Set<Node<N>> current = new LinkedHashSet<Node<N>>();
        final List<Set<Node<N>>> results = new ArrayList<Set<Node<N>>>();
        Integer rank = 0;
        final Map<Node<N>, Integer> ranks = new LinkedHashMap<Node<N>, Integer>();
        // look for rank 0
        for (final Node<N> node : this.nodes) {
            if (this.getOut(node).isEmpty()) {
                ranks.put(node, rank);
                current.add(node);
            }
        }
        results.add(current);

        final Set<Node<N>> toDelete = new LinkedHashSet<Node<N>>();

        // do other ranks
        while (!current.isEmpty()) {
            final Set<Node<N>> newcurrent = new LinkedHashSet<Node<N>>();
            rank++;
            for (final Node<N> node : current) {
                if (!toDelete.contains(node)) {
                    for (final Node<N> pred : this.getIn(node)) {
                        final Integer oldRank = ranks.put(pred, rank);
                        if (oldRank == null) {
                            newcurrent.add(pred);
                        } else {
                            if (oldRank.compareTo(rank) != 0) {
                                newcurrent.add(pred);
                                // this toDelete stuff is needed, because
                                // otherwise,
                                // we would modify the set "current" which would
                                // result
                                // in a concurrent modification
                                if (oldRank.intValue() == rank.intValue() - 1) {
                                    toDelete.add(pred);
                                } else {
                                    results.get(oldRank.intValue())
                                    .remove(node);
                                }
                            }
                        }
                    }
                }
            }
            current.removeAll(toDelete);
            toDelete.clear();

            results.add(newcurrent);
            current = newcurrent;
        }
        results.remove(results.size() - 1);
        return results;
    }

    /**
     * Determines all maximal strongly connected components of this graph, i.e.
     * all the subgraphs for which each node is reachable from every other node.
     *
     * @return The set of all maximal strongly connected components.
     */
    public LinkedHashSet<Cycle<N>> getSCCs() {

        return this.getSCCs(true, null);

    }

    /**
     * like getSCCs(), but one may choose whether only Real sccs should be
     * computed or singleton nodes should also be mentioned
     *
     * @param onlyReal
     * @return
     */
    public LinkedHashSet<Cycle<N>> getSCCs(final boolean onlyReal) {
        return this.getSCCs(onlyReal, null);
    }

    /**
     * computes the set of sccs in the following order: if a scc is returned
     * before another, then from the first scc there is no path to the later scc
     *
     * @param filter
     *            a possible filter on which edges may be used
     * @param onlyReal
     *            if set to true, the usual sccs are computed. If set to false,
     *            then also the singleton nodes which do not form a real scc are
     *            returned as "Scc".
     * @return
     */
    public LinkedHashSet<Cycle<N>> getSCCs(final boolean onlyReal,
        final EdgeFilter<E, N> filter) {
        int n = this.nodes.size();
        final HashSet<Node<N>> visitedNodes = new HashSet<Node<N>>(n);
        final List<Node<N>> sortedNodes = new ArrayList<Node<N>>(n);
        if (filter == null) {
            for (final Node<N> node : this.nodes) {
                this.visitFirst(node, visitedNodes, sortedNodes);
            }
        } else {
            for (final Node<N> node : this.nodes) {
                this.visitFirst(node, visitedNodes, sortedNodes, filter);
            }
        }

        // okay, now traverse in reverse order to compute Sccs
        visitedNodes.clear();
        final LinkedHashSet<Cycle<N>> allSccs = new LinkedHashSet<Cycle<N>>();
        while (n != 0) {
            n--;
            final Node<N> node = sortedNodes.get(n);
            if (visitedNodes.contains(node)) {
                // we already visited this node
                continue;
            }
            final Cycle<N> currentScc = new Cycle<N>();
            if (filter == null) {
                for (final Node<N> outNode : this.getOut(node)) {
                    this.visitSecond(outNode, currentScc, visitedNodes);
                }
            } else {
                for (final Map.Entry<Node<N>, E> outEdge : this.out.get(node)
                    .entrySet()) {
                    final Node<N> target = outEdge.getKey();
                    if (filter.selectEdge(node, target, outEdge.getValue())) {
                        this.visitSecond(target, currentScc, visitedNodes, filter);
                    }
                }
            }
            if (visitedNodes.add(node)) {
                // the node is not on a real SCC!
                if (!onlyReal) {
                    currentScc.add(node);
                    allSccs.add(currentScc);
                }
            } else {
                allSccs.add(currentScc);
            }

        }
        return allSccs;
    }

    /**
     * like getSCCs(), but only edges w.r.t. to the edgeFilter are considered.
     *
     * @param edgeFilter
     * @return
     */
    public LinkedHashSet<Cycle<N>> getSCCs(final EdgeFilter<E, N> edgeFilter) {
        return this.getSCCs(true, edgeFilter);
    }
    
    public Set<Set<Node<N>>> getWCCs() {
        return this.getWCCs(null);
    }

    /**
     * Determines all weakly connected components of this graph.
     * A weakly connected component is a set of nodes that are connected
     * when edge directions are ignored.
     *
     * @return A set containing sets of nodes, where each inner set is one
     * weakly connected component.
     */
    public Set<Set<Node<N>>> getWCCs(final EdgeFilter<E, N> filter) {
        Set<Set<Node<N>>> wccs = new LinkedHashSet<>();
        Set<Node<N>> visited = new HashSet<>();

        for (Node<N> node : this.getNodes()) {
            if (!visited.contains(node)) {
                // Found the start of a new component
                Set<Node<N>> currentComponent = new LinkedHashSet<>();
                Queue<Node<N>> queue = new LinkedList<>(); // using bfs

                queue.add(node);
                visited.add(node);

                while (!queue.isEmpty()) {
                    Node<N> currentNode = queue.poll();
                    currentComponent.add(currentNode);
                    // need to check both directions
                    for (Node<N> neighbor : this.getOut(currentNode)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                    for (Node<N> neighbor : this.getIn(currentNode)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
                wccs.add(currentComponent);
            }
        }
        return wccs;
    }
    
    
    /**
     * Creates the condensation graph of this graph, where each node represents a
     * Strongly Connected Component (SCC). The content of each new node is an
     * immutable set of the original node contents (type N).
     *
     * @param includeSingletons If true, single nodes not part of a cycle are
     * treated as their own SCC. If false, they are ignored.
     * @return A new, labelless SimpleGraph where nodes contain immutable sets of the original node contents.
     */
    public SimpleGraph<ImmutableLinkedHashSet<N>, Void> getCondensationGraph(boolean includeSingletons) {
        // 1. Find all Strongly Connected Components (as sets of Node<N>).
        final var sccs = this.getSCCs(!includeSingletons);

        // 2. Create a reverse map to quickly find the SCC (as a Set<N>) that a Node<N> belongs to.
        final Map<Node<N>, ImmutableLinkedHashSet<N>> nodeToSccMap = new HashMap<>();
        for (final Set<Node<N>> sccAsNodes : sccs) {
            // For each SCC, extract the contents (N) into a new set.
            final LinkedHashSet<N> sccContents = sccAsNodes.stream()
                    .map(Node::getObject)
                    .collect(Collectors.toCollection(LinkedHashSet::new)); // Use LinkedHashSet to preserve order

            // Create a single, safe, immutable representation for this SCC's contents.
            final ImmutableLinkedHashSet<N> immutableScc = ImmutableCreator.create(sccContents);

            // Map all original nodes in this SCC to this new immutable representation.
            for (final Node<N> originalNode : sccAsNodes) {
                nodeToSccMap.put(originalNode, immutableScc);
            }
        }

        // 3. Create the new Node objects for the condensation graph.
        final var sccToNewNodeMap = nodeToSccMap.values().stream()
                .distinct()
                .collect(Collectors.toMap(
                        scc -> scc,           // Key: the ImmutableLinkedHashSet<N>
                        scc -> new Node<>(scc)  // Value: the new Node object wrapping the set
                ));

        // 4. Instantiate the new graph and add its nodes.
        final var condensationGraph = new SimpleGraph<ImmutableLinkedHashSet<N>, Void>();
        for (final var newNode : sccToNewNodeMap.values()) {
            condensationGraph.addNode(newNode);
        }

        // 5. Iterate through all original edges to create the new, labelless edges.
        for (final var originalEdge : this.getEdges()) {
            var fromScc = nodeToSccMap.get(originalEdge.getStartNode());
            var toScc = nodeToSccMap.get(originalEdge.getEndNode());

            // An edge exists in the condensation graph if it connects two different SCCs.
            if (fromScc != null && toScc != null && !fromScc.equals(toScc)) {
                var newFromNode = sccToNewNodeMap.get(fromScc);
                var newToNode = sccToNewNodeMap.get(toScc);
                condensationGraph.addEdge(newFromNode, newToNode);
            }
        }
        return condensationGraph;
    }

    /**
     * Creates a (sub-)graph consisting only of the given nodes and the
     * connections between them according to this graph.
     */
    public SimpleGraph<N, E> getSubGraph(final Set<Node<N>> nodes) {
        return new SimpleGraph<N, E>(nodes, this);
    }

    @Override
    public int hashCode() {

        return this.out.hashCode();

    }

    /**
     * Checks whether the graph contains a path from start to end.
     * <P>
     * Note: There is always a path (of length 0) from a node to itself, even if
     * there is no such edge.
     *
     * @param start
     *            Start node of the path.
     * @param end
     *            End node of the path.
     * @return <code>true</code> if the graph contains such a path,
     *         <code>false</code> otherwise
     */
    public boolean hasPath(Node<N> start, Node<N> end) {
        if (Globals.useAssertions) {
            assert (start != null && end != null);
        }
        Set<Node<N>> done = new HashSet<Node<N>>();
        done.add(start);
        Stack<Node<N>> todo = new Stack<Node<N>>();
        todo.push(start);
        while (!todo.isEmpty()) {
            Node<N> node = todo.pop();
            if (node.equals(end)) {
                return true;
            }
            for (Node<N> succ : this.out.get(node).keySet()) {
                if (done.add(succ)) {
                    todo.push(succ);
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the graph contains a path from start to end.
     * <P>
     * Note: There is always a path (of length 0) from a node to itself, even if
     * there is no such edge.
     * @param start Start node of the path.
     * @param end End node of the path.
     * @param allowEmptyPaths if true, an empty path is allowed.
     * @param filter a filter deciding which edges may be selected (may be null,
     * means "every edge is allowed")
     * @return <code>true</code> if the graph contains such a path,
     * <code>false</code> otherwise
     */
    public boolean hasPath(final Node<N> start,
        final Node<N> end,
        final boolean allowEmptyPaths,
        final EdgeFilter<E, N> filter) {

        if (Globals.useAssertions) {
            assert (start != null && end != null);
        }

        final Set<Node<N>> done = new HashSet<Node<N>>();
        final Stack<Node<N>> todo = new Stack<Node<N>>();

        if (allowEmptyPaths) {
            todo.push(start);
            done.add(start);
        } else {
            for (final Entry<Node<N>, E> outEdgesToTarget : this.out.get(start).entrySet()) {
                final Node<N> target = outEdgesToTarget.getKey();
				if (filter == null || filter.selectEdge(start, target, outEdgesToTarget.getValue())) {
					done.add(target);
					todo.push(target);
				}
            }
        }

        while (!todo.isEmpty()) {
            final Node<N> node = todo.pop();
            if (node.equals(end)) {
                return true;
            }
            for (final Entry<Node<N>, E> outEdgesToTarget : this.out.get(node).entrySet()) {
                final Node<N> target = outEdgesToTarget.getKey();
                if (filter == null
                    || filter.selectEdge(node, target,
                        outEdgesToTarget.getValue())) {
                    if (done.add(target)) {
                        todo.push(target);
                    }
                }
            }
        }

        return false;

    }

    /**
     * merges a non-empty set of nodes into one node. All edges involving any
     * node of replaceUs will also be present in the resulting collapsed node.
     * This collapsed node is returned by this method. If nodes are labeled (by
     * non-null values) then a labelMerger must be provided, which collapses all
     * labels into one. The corresponding edgeMerger is for collapsing edges.
     *
     * @param replaceUs
     * @param labelMerger
     * @param edgeMerger
     * @return the node in which the replaceUs-set has been merged. Element of
     *         replaceUs.
     */
    public Node<N> merge(final Set<Node<N>> replaceUs,
        final BinaryOperation<N> labelMerger,
        final BinaryOperation<E> edgeMerger) {
        if (Globals.useAssertions) {
            assert (this.nodes.containsAll(replaceUs) && replaceUs.size() > 0);
        }
        if (replaceUs.size() == 1) {
            return replaceUs.iterator().next();
        }

        Iterator<Node<N>> it = replaceUs.iterator();
        final Node<N> mergeInto = it.next();

        N nodeLabel = mergeInto.object;

        // iterate over all outgoing edges from replaceUs \ {mergeInto} to any
        // node
        // and combine node-labels;
        while (it.hasNext()) {
            final Node<N> replaceMe = it.next();
            // merge labels
            if (nodeLabel == null) {
                nodeLabel = replaceMe.object;
            } else {
                nodeLabel = labelMerger.combine(nodeLabel, replaceMe.object);
            }

            for (final Map.Entry<Node<N>, E> outEdge : this.out.get(replaceMe)
                .entrySet()) {
                Node<N> dest = outEdge.getKey();
                if (replaceUs.contains(dest)) {
                    dest = mergeInto;
                }
                this.mergeEdge(mergeInto, dest, outEdge.getValue(), edgeMerger);
            }
        }

        // iterate over all incoming edges to replaceUs \ {mergeInto}
        // here we only consider edges not starting in replaceUs \ {mergeInto}
        // (these were already added in the previous loop)
        it = replaceUs.iterator();
        it.next(); // skip mergeInto node
        while (it.hasNext()) {
            final Node<N> replaceMe = it.next();
            for (final Map.Entry<Node<N>, E> inEdge : this.in.get(replaceMe)
                .entrySet()) {
                final Node<N> src = inEdge.getKey();
                if (!replaceUs.contains(src) || src.equals(mergeInto)) {
                    this.mergeEdge(src, mergeInto, inEdge.getValue(),
                        edgeMerger);
                }
            }
        }

        // and finally remove the nodes in replaceUs \ {mergeInto}
        it = replaceUs.iterator();
        it.next(); // skip mergeInto node
        while (it.hasNext()) {
            final Node<N> replaceMe = it.next();
            this.removeNode(replaceMe);
        }

        // and update the label
        this.setNodeObject(mergeInto, nodeLabel);

        return mergeInto;
    }

    /**
     * Inserts an edge from start to end. If the edge is already present and has
     * a non-null label, then the new label and the old will be merged using the
     * merger.
     *
     * @param start
     * @param end
     * @param label
     * @param merger
     */
    public void mergeEdge(final Node<N> start, final Node<N> end, E label,
        final BinaryOperation<E> merger) {
        this.addNode(start);
        this.addNode(end);
        final Map<Node<N>, E> outMap = this.out.get(start);
        if (!outMap.containsKey(end)) {
            // this edge is new
            this.modified();
        }
        final E old = outMap.put(end, label);
        if (old != null) {
            label = merger.combine(old, label);
            outMap.put(end, label);
        }
        this.in.get(end).put(start, label);
    }

    @Override
    public String prettyToString() {

        String out = "[Graph...\n [Nodes...\n";

        final Iterator<Node<N>> nodeIter = this.nodes.iterator();
        while (nodeIter.hasNext()) {
            final Node<N> node = nodeIter.next();
            out += "  " + node.getNodeNumber();
            if (node.getObject() != null) {
                out += "(" + this.getPrettyString(node.getObject()) + ")";
            }
            out += "\n";
        }

        out += " ...Nodes]\n [Edges...\n";

        final Iterator pathIter = this.edges.iterator();
        while (pathIter.hasNext()) {

            final Edge edge = (Edge) pathIter.next();
            out += "  [" + edge.getStartNode().getNodeNumber() + "]";
            out += " -" + this.getPrettyString(edge.getObject()) + "-> ";
            out += "[" + edge.getEndNode().getNodeNumber() + "]\n";

        }

        out += " ...Edges]\n...Graph]";
        return out;

    }

    /**
     * Removes an edge from this graph.
     *
     * @param edge the edge to remove.
     */
    public void removeEdge(final Edge<E, N> edge) {
        this.modified();
        this.out.get(edge.getStartNode()).remove(edge.getEndNode());
        this.in.get(edge.getEndNode()).remove(edge.getStartNode());
    }

    /**
     * Removes an edge from this graph which starts in start and ends in end
     *
     * @param start
     *            the starting point of the edge
     * @param end
     *            the ending point of the edge
     * @return true if the edge was removed
     */
    public void removeEdge(final Node<N> start, final Node<N> end) {
        this.modified();
        this.out.get(start).remove(end);
        this.in.get(end).remove(start);

    }

    /**
     * Removes an edge from this graph which starts in start and ends in end
     *
     * @param start
     *            the starting point of the edge
     * @param end
     *            the ending point of the edge
     * @return the old label. A null-value can mean the edge was not present or
     *         the former label was null.
     */
    public E removeEdgeAndReturnLabel(final Node<N> start, final Node<N> end) {
        this.modified();
        final E label = this.out.get(start).remove(end);
        this.in.get(end).remove(start);
        return label;
    }

    /**
     * Removes a node from this graph. All edges to and from this node are
     * deleted as well.
     *
     * @param node
     *            The node to remove.
     * @return true, iff the node was removed
     */
    public boolean removeNode(final Node<N> node) {
        final Map<Node<N>, E> outMap = this.out.remove(node);
        if (outMap == null) {
            return false;
        } else {
            this.removeNode(node, outMap);
            return true;
        }
    }

    /**
     * inserts an edge from start to end with given label and returns the old
     * label (if it exists). Both start and end nodes are inserted if not
     * already present in the graph.
     *
     * @param start
     * @param end
     * @param label
     * @return
     */
    public E replaceEdge(final Node<N> start, final Node<N> end, final E label) {
        this.addNode(start);
        this.addNode(end);
        final Map<Node<N>, E> outMap = this.out.get(start);
        if (!outMap.containsKey(end)) {
            // we will add a new edge
            this.modified();
        }
        final E old = this.out.get(start).put(end, label);
        this.in.get(end).put(start, label);
        return old;
    }

    /**
     * changes the label of a node
     *
     * @param node
     * @param label
     */
    public void setNodeObject(final Node<N> node, final N label) {
        if (Globals.useAssertions) {
            assert (this.nodes.contains(node));
        }
        node.object = label;
    }

    /**
     * Returns a String containing a DOT representation of this
     * graph with node numbers.
     * @return A String containing a DOT representation of this
     *         graph.
     */
    public String toDOT() {
        return this.toDOT(true);
    }

    /**
     * Returns a String containing a DOT representation of this
     * graph.
     * @param showNrs Indicates whether or not node numbers should
     *                be shown in the node labels.
     * @return A String containing a DOT representation of this
     *         graph.
     */
    public String toDOT(final boolean showNrs) {

        final StringBuffer t = new StringBuffer(
            "digraph dp_graph {\nnode [outthreshold=100, inthreshold=100, shape=box];");

        final Iterator<Node<N>> i = this.nodes.iterator();
        while (i.hasNext()) {
            final Node<N> from = i.next();
            Set<Node<N>> out = this.getOut(from);
            if (out == null) {
                out = new HashSet<Node<N>>();
            }
            t.append(from.getNodeNumber() + " [");
            if (from.object != null) {
                t.append("label=\""
                    + (showNrs ? from.getNodeNumber() + ": " : "")
                    + this.getDOTNodeLabelText(SimpleGraph.DOT, from) + "\", ");
            }
            t.append(this.getDOTFormatForNodeLabels(SimpleGraph.DOT, from));
            t.append("];");
            final Iterator<Node<N>> j = out.iterator();
            if (!j.hasNext()) {
                continue;
            }
            t.append(from.getNodeNumber() + " -> {");
            while (j.hasNext()) {
                final Node<N> to = j.next();
                t.append(to.getNodeNumber() + " ");
            }
            t.append("};\n");
        }
        return t.toString() + "}\n";
    }

    public String toDOTDOT() {
        final StringBuffer t = new StringBuffer("");
        t
        .append("digraph dp_graph {\nnode [outthreshold=100, inthreshold=100];");
        final Iterator<Node<N>> i = this.nodes.iterator();
        while (i.hasNext()) {
            final Node<N> from = i.next();
            Set<Node<N>> out = this.getOut(from);
            if (out == null) {
                out = new HashSet<Node<N>>();
            }
            t.append(from.getNodeNumber() + " [");
            if (from.object != null) {
                t.append("label=\"" + from.getNodeNumber() + "\", ");
            }
            t.append(this.getDOTFormatForNodeLabels(SimpleGraph.DOTDOT1, from));
            t.append("];");
            final Iterator j = out.iterator();
            if (!j.hasNext()) {
                continue;
            }
            t.append(from.getNodeNumber() + " -> {");
            while (j.hasNext()) {
                final Node to = (Node) j.next();
                t.append(to.getNodeNumber() + " ");
            }
            t.append("};\n");
        }
        return t.toString() + "}\n";
    }

    public String toDOTDOT(final boolean swap, final float width, final float height,
        final boolean usePDF) {
        final StringBuffer t = new StringBuffer("");
        final LinkedHashSet<Double<N>> doubles = new LinkedHashSet<Double<N>>();
        if (swap) {
            t.append("digraph dp_graph {\nrankdir=LR\nsize=\"" + width + ","
                + height + "\"\nnode [outthreshold=100, inthreshold=100];");
        } else {
            t.append("digraph dp_graph {\nsize=\"" + width + "," + height
                + "\"\nnode [outthreshold=100, inthreshold=100];");
        }
        final Iterator<Node<N>> i = this.nodes.iterator();
        while (i.hasNext()) {
            final Node<N> from = i.next();
            Set<Node<N>> out = this.getOut(from);
            if (out == null) {
                out = new HashSet<Node<N>>();
            }
            t.append(from.getNodeNumber() + " [");
            if (from.object != null) {
                t.append("label=\"" + from.getNodeNumber() + "\", ");
            }
            t.append(this.getDOTFormatForNodeLabels(SimpleGraph.DOTDOT2, from));
            t.append("];");
            final Iterator<Node<N>> j = out.iterator();
            if (!j.hasNext()) {
                continue;
            }
            t.append(from.getNodeNumber() + " -> {");
            while (j.hasNext()) {
                final Node<N> to = j.next();
                final Set<Node<N>> out2 = this.getOut(to);
                if (out2.contains(from) && !from.equals(to) && !usePDF) {
                    doubles.add(new Double<N>(from, to));
                } else {
                    t.append(to.getNodeNumber() + " ");
                }
            }
            t.append("};\n");
        }
        if (!doubles.isEmpty()) {
            t.append("\nedge [dir=both]\n");
            final Iterator it = doubles.iterator();
            while (it.hasNext()) {
                final Double tmp = (Double) it.next();
                t.append(tmp.toString());
            }
        }
        return t.toString() + "\n}\n";
    }

    /**
     * This method is used to save a graph in nice dot format without node
     * numbers. Additional, it labels the edges by using the toString() method
     * of the objects stored in the edges.
     * @return A String containing a DOT representation of this
     *         graph.
     */
    public String toInteractiveDOTwithEdges() {
        return this.toInteractiveDOTwithEdges(false);
    }

    /**
     * This method is used to save a graph in nice dot format.
     * Additional, it labels the edges by using the toString() method
     * of the objects stored in the edges.
     * @param showNumbers Indicates whether or not node numbers
     *                    should be shown in the node labels.
     * @return A String containing a DOT representation of this
     *         graph.
     */
    public String toInteractiveDOTwithEdges(final boolean showNumbers) {
        final StringBuffer t = new StringBuffer("");
//        LinkedHashSet<Double<N>> doubles = new LinkedHashSet<Double<N>>();
        t.append("digraph dp_graph {\nnode [outthreshold=100, inthreshold=100];\n");
        int maxNodeNr = 0;
        for (final Node<N> from : this.nodes) {
            final int nr = from.getNodeNumber();
            if (nr > maxNodeNr) {
                maxNodeNr = nr;
            }
            t.append(nr + " [");
            if (from.object != null) {
                t.append("label=\"" + (showNumbers ? nr + ": " : "") + this.getDOTNodeLabelText(SimpleGraph.INTERACTIVE, from)
                    + "\", ");
            }
            t.append(this.getDOTFormatForNodeLabels(SimpleGraph.INTERACTIVE, from));
            t.append("];\n");
        }

        for (final Edge<E, N> edge : this.getEdges()) {
            maxNodeNr++;
            t.append(maxNodeNr
                + " [label=\""
                + this.getDOTEdgeLabelText(edge)
                + "\", " + this.getDOTFormatForEdgeLabels(edge) + "];\n");
            final String edgeFormat = this.getDOTFormatForEdges(edge);
            t.append(edge.getStartNode().getNodeNumber() + " -> " + maxNodeNr
                + " [arrowhead = none " + (edgeFormat == "" ? "" : ", " + edgeFormat) + "];\n");
            t.append(maxNodeNr + " -> " + edge.getEndNode().getNodeNumber()
                + (edgeFormat == "" ? "" : "[" + edgeFormat + "]") + ";\n\n");
        }

        return t.toString() + "}\n";

    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("type", "Graph");
        JSONObject jsonNodes = new JSONObject();
        jsonNodes.put("type", "Nodes");
        for (Node<?> from : this.getNodes()) {
            jsonNodes.put("" + from.getNodeNumber(), JSONExportUtil.toJSON(from.getObject()));
        }
        res.put("nodes", jsonNodes);
        res.put("edges", JSONExportUtil.toJSON(this.getEdges()));
        return res;
    }

    public String toLaTeX() {
        final StringBuffer t = new StringBuffer();
        t.append("\n\\begin{longtable}{lrcl}\n");
        final TreeSet<Node<N>> tmp = new TreeSet<Node<N>>(this.nodes);
        final Iterator<Node<N>> i = tmp.iterator();
        while (i.hasNext()) {
            final Node<N> from = i.next();
            if (from.object != null) {
                final LaTeX_Able l = (LaTeX_Able) from.object;
                t.append(from.getNodeNumber() + ": & " + l.toLaTeX());
                if (i.hasNext()) {
                    t.append("\\\\\n");
                }
            }
        }
        t.append("\n\\end{longtable}\n");
        return t.toString();
    }

    /**
     * This method is used to save a graph in nice dot format without node
     * numbers.
     * @return A String containing a DOT representation of this
     *         graph.
     */
    public String toSaveDOT() {

        final StringBuffer t = new StringBuffer("");
        final LinkedHashSet<Double<N>> doubles = new LinkedHashSet<Double<N>>();
        t
        .append("digraph dp_graph {\nnode [outthreshold=100, inthreshold=100];\n");
        final Iterator<Node<N>> i = this.nodes.iterator();
        while (i.hasNext()) {
            final Node<N> from = i.next();
            Set<Node<N>> out = this.getOut(from);
            if (out == null) {
                out = new HashSet<Node<N>>();
            }
            t.append(from.getNodeNumber() + " [");
            if (from.object != null) {
                t.append("label=\"" + this.getDOTNodeLabelText(SimpleGraph.SAVE, from) + "\", ");
            }
            t.append(this.getDOTFormatForNodeLabels(SimpleGraph.SAVE, from));
            t.append("];\n");
            final Iterator<Node<N>> j = out.iterator();
            if (!j.hasNext()) {
                continue;
            }
            t.append(from.getNodeNumber() + " -> {");
            while (j.hasNext()) {
                final Node<N> to = j.next();
                final Set<Node<N>> out2 = this.getOut(to);
                if (out2.contains(from) && !from.equals(to)) {
                    doubles.add(new Double<N>(from, to));
                } else {
                    t.append(to.getNodeNumber() + " ");
                }
            }
            t.append("};\n");
        }
        if (!doubles.isEmpty()) {
            t.append("\nedge [dir=both]\n");
            final Iterator it = doubles.iterator();
            while (it.hasNext()) {
                final Double tmp = (Double) it.next();
                t.append(tmp.toString());
            }
        }
        return t.toString() + "\n}\n";

    }

    /**
     * This method is used to save a graph in nice dot format without node
     * numbers. Additional, it labels the edges by using the toString() method
     * of the objects stored in the edges.
     * @return A String containing a DOT representation of this
     *         graph.
     */
    public String toSaveDOTwithEdges() {

        final StringBuffer t = new StringBuffer("");
        final LinkedHashSet<Double<N>> doubles = new LinkedHashSet<Double<N>>();
        t.append("digraph dp_graph {\nnode [outthreshold=100, inthreshold=100];\n");
        final Iterator<Node<N>> i = this.nodes.iterator();
        while (i.hasNext()) {
            final Node<N> from = i.next();
            Set<Node<N>> out = this.getOut(from);
            if (out == null) {
                out = new HashSet<Node<N>>();
            }
            t.append(from.getNodeNumber() + " [");
            if (from.object != null) {
                t.append("label=\"" + this.getDOTNodeLabelText(SimpleGraph.EDGES, from)
                    + "\", ");
            }
            t.append(this.getDOTFormatForNodeLabels(SimpleGraph.EDGES, from));
            t.append("];\n");
            final Iterator<Node<N>> j = out.iterator();
            if (!j.hasNext()) {
                continue;
            }

            while (j.hasNext()) {
                final Node<N> to = j.next();
                final Edge<E, N> edge = this.getEdge(from, to);
                t.append(from.getNodeNumber() + " -> {");
                t.append(to.getNodeNumber() + "} ");
                if (edge.object == null) {
                    t.append(" [label = \"null\"]");
                } else {
                    t.append(" [label = \""
                        + this.getDOTEdgeLabelText(edge) + "\"]");
                }
                t.append(";\n");
            }
        }

        if (!doubles.isEmpty()) {
            t.append("\nedge [dir=both]\n");
            final Iterator it = doubles.iterator();
            while (it.hasNext()) {
                final Double tmp = (Double) it.next();
                t.append(tmp.toString());
            }
        }
        return t.toString() + "}\n";

    }

    /**
     * Overwrites the <code>toString</code> method of <code>Object</code>
     *
     * @return a <code>String</code> representing this object
     */
    @Override
    public String toString() {

        return this.nodes.toString() + "\n" + this.edges.toString();

    }

    /**
     * Returns the label text of an edge in the DOT representation
     * created with the method toInteractiveDOTwithEdges() of this
     * graph dependend on the edge.
     * @param edge The edge to be labeled with the text.
     * @return The specified edge's label text in the DOT
     *         representation.
     */
    protected String getDOTEdgeLabelText(final Edge<E, N> edge) {
        return this.getPrettyString(edge.object);
    }

    /**
     * Returns a formatting String for the DOT representation
     * created with the method toInteractiveDOTwithEdges() of
     * an edge label in this graph dependend on the edge.
     * @param edge The edge labeled with this format.
     * @return A formatting String for the DOT representation of
     *         an edge label in this graph.
     */
    protected String getDOTFormatForEdgeLabels(final Edge<E, N> edge) {
        return "fontsize=16, style = filled, fillcolor = yellow";
    }

    /**
     * Returns a formatting String for the DOT representation
     * created with the method toInteractiveDOTwithEdges() of
     * an edge in this graph dependend on the edge.
     * @param edge The edge with this format.
     * @return A formatting String for the DOT representation of
     *         an edge in this graph.
     */
    protected String getDOTFormatForEdges(final Edge<E, N> edge) {
        return "";
    }

    /**
     * Returns a formatting String for the DOT representation of
     * a node label in this graph dependend on the method called for
     * the representation, the node and a parameter object.
     * @param method An integer constant describing the calling
     *               method for DOT representation. Can be
     *               DOT, SAVE, EDGES, INTERACTIVE, DOTDOT1 or
     *               DOTDOT2.
     * @param node The node labeled with this format.
     * @return A formatting String for the DOT representation of
     *         a node label in this graph.
     */
    protected String getDOTFormatForNodeLabels(final int method, final Node<N> node) {
        switch (method) {
        case DOT: case DOTDOT2:
            final Set<Node<N>> out = this.getOut(node);
            if (out != null && out.contains(node)) {
                return "fontsize=16, style=dashed, color=red";
            }
            return "fontsize=16";
        case SAVE: case EDGES: case DOTDOT1:
            return "fontsize=16";
        case INTERACTIVE:
            return "fontsize=10";
        }
        return "";
    }

    /**
     * Returns the label text of a node in the DOT representation of
     * this graph dependend on the calling method for the DOT
     * representation and the node.
     * @param method An integer constant describing the calling
     *               method for DOT representation. Can be
     *               DOT, SAVE, EDGES or INTERACTIVE.
     * @param node The node to be labeled with the text.
     * @return The specified node's label text in the DOT
     *         representation.
     */
    protected String getDOTNodeLabelText(final int method, final Node<N> node) {
        if (node.object instanceof DOTStringAble) {
            return ((DOTStringAble) node.object).toDOTString();
        }
        switch (method) {
        case DOT: case SAVE:
            return node.object.toString();
        case EDGES: case INTERACTIVE:
            return this.getPrettyString(node.object);
        }
        return "";
    }

    /**
     * removes a node (which must be present), given its corresponding outMap
     * (which must be non-null). This method should be overwritten by
     * subclasses, if they have to delete additional info. The reason for
     * splitting removeNode into two parts is to be able to use the node-set
     * iterators remove method. Otherwise that method would cause an concurrent
     * modification exception!
     *
     * @param node
     *            the node to remove, must exist in graph
     * @param outMap
     *            the out map of this node, non-null
     */
    void removeNode(final Node<N> node, final Map<Node<N>, E> outMap) {
        this.modified();
        for (final Node<N> dest : outMap.keySet()) {
            this.in.get(dest).remove(node);
        }

        final Map<Node<N>, E> inMap = this.in.remove(node);
        for (final Node<N> src : inMap.keySet()) {
            if (!node.equals(src)) {
                this.out.get(src).remove(node);
            }
        }
    }

    /**
     * @param clique a clique
     * @param outsideNode a node which is not in the clique
     * @return true iff outsideNode is connected to all nodes in the clique
     */
    private boolean adjoinable(final Collection<Node<N>> clique,
        final Node<N> outsideNode) {
        for (final Node<N> inClique : clique) {
            if (!this.contains(inClique, outsideNode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Given a simple graph G with n vertices and a maximal clique Q of G, if
     * there is no vertex v outside Q such that there is exactly one vertex w in
     * Q that is not a neighbor of v, output Q. Else, find a vertex v outside Q
     * such that there is exactly one vertex w in Q that is not a neighbor of v.
     * Define Qv,w by adjoining v to Q and removing w from Q. Perform
     * cliqueHelperOne on Qv,w and output the resulting clique.
     * @return
     */
    private Collection<Node<N>> cliqueHelperTwo(final Collection<Node<N>> clique) {
        Node<N> v = null;
        Node<N> w = null;
        OUTER: for (final Node<N> vCand : this.nodes) {
            if (clique.contains(vCand)) {
                continue;
            }
            boolean foundNotConnected = false;
            for (final Node<N> wCand : clique) {
                if (!this.contains(wCand, vCand)) {
                    if (foundNotConnected) {
                        continue OUTER;
                    }
                    foundNotConnected = true;
                    w = wCand;
                }
            }
            if (foundNotConnected) {
                v = vCand;
                break;
            }
        }

        if (v == null) {
            return clique;
        }
        final Collection<Node<N>> modified = new LinkedHashSet<Node<N>>(clique);
        modified.add(v);
        modified.remove(w);
        return this.enlargeClique(modified);
    }



    /**
     * Enlarge this clique by adding adjoinable nodes as often as possible.
     * @param clique a clique
     * @return the enlarged clique
     */
    private Collection<Node<N>> enlargeClique(final Collection<Node<N>> clique) {
        int maxNum = Integer.MIN_VALUE;
        Collection<Node<N>> maxAdded = null;
        for (final Node<N> node : clique) {
            for (final Node<N> outsideNode : this.getOut(node)) {
                if (clique.contains(outsideNode)) {
                    continue;
                }
                if (!this.adjoinable(clique, outsideNode)) {
                    continue;
                }

                final Collection<Node<N>> added =
                    new LinkedHashSet<Node<N>>(clique);
                added.add(outsideNode);
                final int num = this.getNumOfAdjoinableNodes(added);
                if (num > maxNum) {
                    maxAdded = added;
                    maxNum = num;
                }
            }
        }
        if (maxAdded == null) {
            return clique;
        }
        return this.enlargeClique(maxAdded);
    }

    /**
     * @param clique a clique
     * @return the number of nodes that are adjoinable
     */
    private int getNumOfAdjoinableNodes(final Collection<Node<N>> clique) {
        int result = 0;
        for (final Node<N> outsideNode : this.nodes) {
            if (clique.contains(outsideNode)) {
                continue;
            }
            if (this.adjoinable(clique, outsideNode)) {
                result++;
            }
        }
        return result;
    }

    /**
     * Returns the partitions of a given set of nodes.
     */
    private ImmutableSet<Set<Node<N>>> getPartitions(final Set<Node<N>> v) {
        Set<Set<Node<N>>> p = new LinkedHashSet<Set<Node<N>>>();
        final Set<Set<Node<N>>> p_final = new LinkedHashSet<Set<Node<N>>>();
        p.add(v);
        for (final Node<N> n : v) {
            if (p.isEmpty()) {
                break;
            }
            final Set<Set<Node<N>>> p_prime = new LinkedHashSet<Set<Node<N>>>();
            for (final Set<Node<N>> v_i : p) {
                if (v_i.size() == 1) {
                    p_final.add(v_i);
                } else {
                    this.split(n, v_i, p_prime, p_final);
                }
            }
            p = p_prime;
        }
        p_final.addAll(p);
        return ImmutableCreator.create(p_final);
    }

    private void modified() {
        this.modCount++;
        this.equivClasses = null;
    }

    /**
     * Splits a partition.
     */
    private final void split(final Node<N> n, final Set<Node<N>> v, final Set<Set<Node<N>>> toDo,
        final Set<Set<Node<N>>> finished) {
        final Set<Node<N>> outs = this.getOut(n);
        final Set<Node<N>> ins = this.getIn(n);
        final ArrayList<Set<Node<N>>> sets = new ArrayList<Set<Node<N>>>(4);
        for (int i = 0; i < 4; i++) {
            sets.add(i, new LinkedHashSet<Node<N>>());
        }
        for (final Node<N> node : v) {
            final boolean out = outs.contains(node);
            final boolean in = ins.contains(node);
            if (out) {
                if (in) {
                    sets.get(0).add(node);
                } else {
                    sets.get(1).add(node);
                }
            } else {
                if (in) {
                    sets.get(2).add(node);
                } else {
                    sets.get(3).add(node);
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            final Set<Node<N>> set = sets.get(i);
            final int size = set.size();
            if (size == 1) {
                finished.add(set);
            } else if (size > 1) {
                toDo.add(set);
            }
        }
    }

    private void visitFirst(final Node<N> node, final Set<Node<N>> visited,
        final List<Node<N>> collect) {
        if (visited.add(node)) {
            for (final Node<N> inNode : this.getIn(node)) {
                this.visitFirst(inNode, visited, collect);
            }
            collect.add(node);
        }
    }

    private void visitFirst(final Node<N> node, final Set<Node<N>> visited,
        final List<Node<N>> collect, final EdgeFilter<E, N> filter) {
        if (visited.add(node)) {
            for (final Map.Entry<Node<N>, E> inEdge : this.in.get(node).entrySet()) {
                final Node<N> source = inEdge.getKey();
                if (filter.selectEdge(source, node, inEdge.getValue())) {
                    this.visitFirst(source, visited, collect, filter);
                }
            }
            collect.add(node);
        }
    }

    private void visitSecond(final Node<N> node, final Set<Node<N>> currentScc,
        final Set<Node<N>> visited) {
        if (visited.add(node)) {
            currentScc.add(node);
            for (final Node<N> outNode : this.getOut(node)) {
                this.visitSecond(outNode, currentScc, visited);
            }
        }
    }

    private void visitSecond(final Node<N> node, final Set<Node<N>> currentScc,
        final Set<Node<N>> visited, final EdgeFilter<E, N> filter) {
        if (visited.add(node)) {
            currentScc.add(node);
            for (final Map.Entry<Node<N>, E> outEdge : this.out.get(node).entrySet()) {
                final Node<N> target = outEdge.getKey();
                if (filter.selectEdge(node, target, outEdge.getValue())) {
                    this.visitSecond(target, currentScc, visited, filter);
                }
            }
        }
    }

    protected static class Double<N> {

        Node<N> from;

        Node<N> to;

        public Double(final Node<N> f, final Node<N> t) {
            this.from = f;
            this.to = t;
        }

        @Override
        public boolean equals(final Object o) {
            final Double d = (Double) o;
            return ((this.from.equals(d.from) && this.to.equals(d.to)) || (this.from
                .equals(d.to) && this.to.equals(d.from)));
        }

        public Node<N> getFrom() {
            return this.from;
        }

        public Node<N> getTo() {
            return this.to;
        }

        @Override
        public int hashCode() {
            return this.from.hashCode() + this.to.hashCode();
        }

        @Override
        public String toString() {
            return this.from.getNodeNumber() + "->" + this.to.getNodeNumber()
                + "  ";
        }
    }

    /**
     * provides a basic implementation of unchangeable sets which just mirror
     * the state of the graph. Moreover, hashes are cached and respect the
     * modification of the graph.
     */
    private abstract class AbstractSet<U> implements SerializableSet<U> {

        private int hashCode;

        private int lastHashCalculation;

        public AbstractSet() {
            this.hashCode = 0;
            this.lastHashCalculation = SimpleGraph.this.modCount - 1;
        }

        @Override
        public boolean add(final U arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(final Collection<? extends U> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(final Collection<?> arg) {
            for (final Object edge : arg) {
                if (!this.contains(edge)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null || !(o instanceof Set)) {
                return false;
            }
            final Set other = (Set) o;
            if (this.size() == other.size()) {
                return this.containsAll(other);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            while (this.lastHashCalculation != SimpleGraph.this.modCount) {
                this.lastHashCalculation = SimpleGraph.this.modCount;
                int hash = 0;
                for (final U elem : this) {
                    hash += elem.hashCode();
                }
                this.hashCode = hash;
            }
            return this.hashCode;
        }

        @Override
        public boolean remove(final Object arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(final Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(final Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            final int n = this.size();
            final Object[] a = new Object[n];
            int i = 0;
            for (final U u : this) {
                a[i] = u;
                i++;
            }
            return a;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            final int n = this.size();
            int m = a.length;
            if (m < n) {
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass()
                    .getComponentType(), n);
                m = n;
            }
            int i = 0;
            final Object[] b = a;
            for (final U u : this) {
                b[i] = u;
                i++;
            }
            if (m != n) {
                b[i] = null;
            }
            return a;
        }

        @Override
        public String toString() {
            final StringBuffer buf = new StringBuffer();
            boolean first = true;
            for (final U elem : this) {
                if (first) {
                    buf.append("{");
                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(elem);
            }
            if (first) {
                buf.append("empty set");
            } else {
                buf.append("}");
            }
            return buf.toString();
        }

    }

    private interface SerializableSet<U> extends Set<U>, Serializable {
    }

}
