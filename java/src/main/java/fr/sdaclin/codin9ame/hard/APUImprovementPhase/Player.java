package fr.sdaclin.codin9ame.hard.APUImprovementPhase;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The machines are gaining ground. Time to show them what we're really made of...
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        Configuration configuration;

        int width = in.nextInt(); // the number of cells on the X axis
        in.nextLine();
        int height = in.nextInt(); // the number of cells on the Y axis
        in.nextLine();

        configuration = new Configuration(width, height);
        for (int i = 0; i < height; i++) {
            configuration.addLine(in.nextLine()); // width characters, each either x number or x '.'
        }

        // Dump to be able to reproduce in tests
        configuration.dump();

        Solver solver = new Solver(configuration);
        solver.printResult();
    }

    /**
     * The most interesting part
     */
    static class Solver {
        private static final Predicate<Connection> isNotProcessed = (connection -> !connection.isProcessed());
        private final List<Node> nodes = new LinkedList<>();
        private final List<Connection> connections = new LinkedList<>();

        Solver(Configuration configuration) {
            initNodesAndConnections(configuration);
        }

        List<Connection> solve() {
            History history = new History();
            Context context = null;

            // Auto connect nodes where there are no assumption to be made
            autoSetSureThicknesses(nodes, history);

            boolean everythingIsProcessed = false;
            do {
                // Get or create current context with a node to process
                if (context == null) {
                    // Take one random connection that is not processed but that possibly concern a Partially connected node
                    Optional<Connection> optionalConnection = connections.stream().filter(isNotProcessed).findFirst();
                    if (optionalConnection.isPresent()) {
                        context = new Context(optionalConnection.get());
                    } else {
                        if (nodes.stream().filter(n -> n.state == Node.State.PARTIALLY_CONNECTED).count() > 0) {
                            if (history.history.size()==20){
                                printOutConnexions(history.history.stream().map(c->c.connection).collect(Collectors.toList()), true);
                            }
                            context = history.revert();
                            continue;
                        }
                        everythingIsProcessed = true;
                        continue;
                    }
                }



                // Make an assumption of its potential thickness between 0 to 2
                // Todo add in context to compute only once
                boolean oneConnectionHasBeenProcessed = false;
                int maxThicknessForThisConnectionNodes = context.getConnection().getA().getWeight() > context.getConnection().getB().getWeight() ? context.getConnection().getB().getWeight() : context.getConnection().getA().getWeight();
                for (int thickness = context.getConnectionThickness() - 1; thickness >= 0; thickness--) {
                    if (thickness > maxThicknessForThisConnectionNodes) {
                        continue;
                    }
                    try {
                        context.setConnectionThickness(thickness);
                        setWeight(history,context);
                    } catch (CrossingConnectionException cce) {
                        // Don't need to try other thickness, when this connexion cross another one, the only thickness possible is 0
                        thickness = 0;
                        context.setConnectionThickness(thickness);
                        setWeight(history,context);
                    }
                    processRelativeConnections(history, context.getConnection());
                    oneConnectionHasBeenProcessed = true;
                    break;
                }
                if (!oneConnectionHasBeenProcessed) {
                    context = history.revert();
                    continue;
                }

                // Save current assumption in history
                context = null;
                try {
                    autoSetSureThicknesses(nodes, history);
                } catch (CrossingConnectionException cce) {
                    context = history.revert();
                }
            } while (!everythingIsProcessed);

            return connections;
        }

        /**
         * Cycling each node, find the ones that have a weight that directly match the number of connections and set them
         * 1 nodes with only 1 connexion
         * nodes with 2 connexions and weight of 4 ( 2 connexions of 2)
         * <p>
         * When all this nodes are auto wired, some connection are bounded to a node that have a weight of 0 so this connection should be flagged as processed
         *
         * @param nodes
         * @param history
         */
        private void autoSetSureThicknesses(List<Node> nodes, History history) {
            final boolean[] oneConnectionHasBeenProcessed = {false};
            do {
                oneConnectionHasBeenProcessed[0] = false;
                try {
                    nodes.stream()//
                            .filter(node -> !(node.getState() == Node.State.FULLY_CONNECTED) && (node.getWeight() == 1 || node.getWeight() % 2 == 0)) // Matches 1,2,4,6,8
                            .forEach(node -> {
                                Set<Connection> currentNodeUnProcessedConnection = node.getConnections().stream().filter(isNotProcessed).collect(Collectors.toSet());
                                if (currentNodeUnProcessedConnection.size() == 0) {
                                    return;
                                }

                                try {
                                    if (currentNodeUnProcessedConnection.size() == 1) {
                                        Connection connection = currentNodeUnProcessedConnection.stream().findFirst().get();
                                        assert (node.getWeight() == 1 || node.getWeight() == 2);
                                        try {
                                            Context context = new Context(connection, node.getWeight());
                                            setWeight(history,context);
                                        } catch (CrossingConnectionException e) {
                                            // Todo something to propagate
                                            e.printStackTrace();
                                            throw e;
                                        }
                                        processRelativeConnections(history, connection);
                                        oneConnectionHasBeenProcessed[0] = true;
                                    } else if (currentNodeUnProcessedConnection.size() == node.getWeight() / 2) {
                                        assert node.getWeight() != 0;
                                        currentNodeUnProcessedConnection.forEach(connection -> {
                                            try {
                                                Context context = new Context(connection, 2);
                                                setWeight(history,context);
                                            } catch (CrossingConnectionException e) {
                                                // Todo something to propagate
                                                e.printStackTrace();
                                                throw e;
                                            }
                                            processRelativeConnections(history, connection);
                                        });
                                        oneConnectionHasBeenProcessed[0] = true;
                                    }
                                } catch (CrossingConnectionException e) {
                                    // Todo something to propagate
                                    e.printStackTrace();
                                }

                            });
                } catch (CrossingConnectionException e) {
                    // Todo something to propagate
                    e.printStackTrace();
                }
            } while (oneConnectionHasBeenProcessed[0]);

//            connections.stream()//
//                    .filter(connection -> (!connection.isProcessed() && (connection.getA().getState() == Node.State.FULLY_CONNECTED || connection.getB().getState() == Node.State.FULLY_CONNECTED)))//
//                    .forEach(connection1 -> connection1.setProcessed(true));
        }

        private void processRelativeConnections(History history, Connection connection) {
            if (connection.getA().getState() == Node.State.FULLY_CONNECTED) {
                connection.getA().getConnections().stream().filter(c -> !c.isProcessed()).forEach(c -> {
                    setWeight(history, new Context(c, 0));
                });
            }
            if (connection.getB().getState() == Node.State.FULLY_CONNECTED) {
                connection.getB().getConnections().stream().filter(c -> !c.isProcessed()).forEach(c -> {
                    setWeight(history, new Context(c, 0));
                });
            }
        }

        private void resetWeight(Connection connection) {
            connection.resetThickness();
        }

        private void setWeight(History history, Context context) throws CrossingConnectionException {
            if (context.getConnectionThickness() > 0 && connections.stream().filter(Connection::isProcessed).filter(c -> c.getThickness() > 0).filter(c -> intersects(context.getConnection(), c)).findFirst().isPresent()) {
                throw new CrossingConnectionException();
            }
            // Todo implement crossing connection exception
            context.getConnection().setThickness(context.getConnectionThickness());
            history.add(context);
        }

        private void initNodesAndConnections(Configuration configuration) {
            Map<Integer, Node> nodeByCol = new HashMap<>();
            for (int i = 0; i < configuration.getLines().size(); i++) {
                String line = configuration.getLines().get(i);
                String[] lineContent = line.split(".{0}");
                Node previousNodeInRow = null;
                for (int j = 0; j < lineContent.length; j++) {
                    String cellContent = lineContent[j];
                    if (cellContent.equals(".")) {
                        continue;
                    }
                    Node node = new Node(new Coordinate(j, i), Integer.parseInt(cellContent, 10));
                    if (previousNodeInRow != null) {
                        Connection connectionTo = node.connect(previousNodeInRow);
                        connections.add(connectionTo);
                    }
                    previousNodeInRow = node;
                    Node previousNodeInCol;
                    if ((previousNodeInCol = nodeByCol.get(j)) != null) {
                        Connection connectFrom = previousNodeInCol.connect(node);
                        connections.add(connectFrom);
                    }
                    nodeByCol.put(j, node);
                    nodes.add(node);
                }
            }
        }

        void printResult() {
            List<Connection> connections = solve();
            printOutConnexions(connections, false);
        }

        void printOutConnexions(List<Connection> connections, boolean forTesting) {
            connections.stream().filter(connection -> connection.getThickness() > 0).forEach(connection -> {
                if (forTesting) {
                    System.out.println("System.out.println(\"" + connection.getA().getCoordinate().getX() + " " + connection.getA().getCoordinate().getY() + " " + connection.getB().getCoordinate().getX() + " " + connection.getB().getCoordinate().getY() + " " + connection.getThickness() + "\");");
                } else {
                    System.out.println(connection.getA().getCoordinate().getX() + " " + connection.getA().getCoordinate().getY() + " " + connection.getB().getCoordinate().getX() + " " + connection.getB().getCoordinate().getY() + " " + connection.getThickness());
                }
            });
        }

        static Map<Line, Map<Line, Boolean>> intersectionCache = new HashMap<>();

        static boolean intersects(Line lineA, Line lineB) {
            if (lineA.getOrientation() == lineB.getOrientation()) {
                return false;
            }
            intersectionCache.computeIfAbsent(lineA, (line -> new HashMap<>()));
            return intersectionCache.get(lineA).computeIfAbsent(lineB, (line -> {
                int axeLineA, lBoundLineA, uBoundLineA;
                int axeLineB, lBoundLineB, uBoundLineB;
                if (lineA.getOrientation() == Line.Orientation.HORIZONTAL) {
                    axeLineA = lineA.getCoordinateOfA().getY();
                    lBoundLineA = lineA.getCoordinateOfA().getX() < lineA.getCoordinateOfB().getX() ? lineA.getCoordinateOfA().getX() : lineA.getCoordinateOfB().getX();
                    uBoundLineA = lineA.getCoordinateOfA().getX() > lineA.getCoordinateOfB().getX() ? lineA.getCoordinateOfA().getX() : lineA.getCoordinateOfB().getX();
                    assert lBoundLineA < uBoundLineA;
                    axeLineB = lineB.getCoordinateOfA().getX();
                    lBoundLineB = lineB.getCoordinateOfA().getY() < lineB.getCoordinateOfB().getY() ? lineB.getCoordinateOfA().getY() : lineB.getCoordinateOfB().getY();
                    uBoundLineB = lineB.getCoordinateOfA().getY() > lineB.getCoordinateOfB().getY() ? lineB.getCoordinateOfA().getY() : lineB.getCoordinateOfB().getY();
                    assert lBoundLineB < uBoundLineB;
                } else {
                    axeLineA = lineA.getCoordinateOfA().getX();
                    lBoundLineA = lineA.getCoordinateOfA().getY() < lineA.getCoordinateOfB().getY() ? lineA.getCoordinateOfA().getY() : lineA.getCoordinateOfB().getY();
                    uBoundLineA = lineA.getCoordinateOfA().getY() > lineA.getCoordinateOfB().getY() ? lineA.getCoordinateOfA().getY() : lineA.getCoordinateOfB().getY();
                    assert lBoundLineA < uBoundLineA;
                    axeLineB = lineB.getCoordinateOfA().getY();
                    lBoundLineB = lineB.getCoordinateOfA().getX() < lineB.getCoordinateOfB().getX() ? lineB.getCoordinateOfA().getX() : lineB.getCoordinateOfB().getX();
                    uBoundLineB = lineB.getCoordinateOfA().getX() > lineB.getCoordinateOfB().getX() ? lineB.getCoordinateOfA().getX() : lineB.getCoordinateOfB().getX();
                    assert lBoundLineB < uBoundLineB;
                }
                return (axeLineA != lBoundLineB && axeLineA != uBoundLineB)
                        && (axeLineB != lBoundLineA && axeLineB != uBoundLineA)
                        && axeLineA <uBoundLineB && axeLineA > lBoundLineB
                        && axeLineB <uBoundLineA && axeLineB > lBoundLineA;
            }));
        }


        public interface Line {
            enum Orientation {HORIZONTAL, VERTICAL}

            Coordinate getCoordinateOfA();

            Coordinate getCoordinateOfB();

            Orientation getOrientation();
        }

        static class Coordinate {
            private final int x;
            private final int y;

            Coordinate(int x, int y) {
                this.x = x;
                this.y = y;
            }

            @Override
            public String toString() {
                return "{" +
                        "" + x +
                        ", " + y +
                        '}';
            }

            public int getX() {
                return x;
            }

            public int getY() {
                return y;
            }
        }

        static class Connection implements Line {
            private final Node nodeA;
            private final Node nodeB;
            private final Orientation orientation;
            private int thickness = 0;
            private boolean processed = false;

            public Connection(Node nodeA, Node nodeB) {
                this.nodeA = nodeA;
                this.nodeB = nodeB;
                this.orientation = (nodeA.getCoordinate().getX() == nodeB.getCoordinate().getX()) ? Orientation.VERTICAL : Orientation.HORIZONTAL;
            }

            public Node getA() {
                return nodeA;
            }

            public Node getB() {
                return nodeB;
            }

            public int getThickness() {
                return thickness;
            }

            public void setThickness(int thickness) {
                assert thickness >= 0;
                if (thickness != 0) {
                    nodeA.changeWeight(-thickness);
                    nodeB.changeWeight(-thickness);
                }
                this.thickness = thickness;
                processed = true;
            }

            public void resetThickness() {
                if (this.thickness != 0) {
                    nodeA.changeWeight(this.thickness);
                    nodeB.changeWeight(this.thickness);
                }
                this.thickness = 0;
                processed = false;
            }

            @Override
            public Coordinate getCoordinateOfA() {
                return nodeA.coordinate;
            }

            @Override
            public Coordinate getCoordinateOfB() {
                return nodeB.coordinate;
            }

            @Override
            public Orientation getOrientation() {
                return orientation;
            }

            public boolean isProcessed() {
                return processed;
            }

            public void setProcessed(boolean processed) {
                this.processed = processed;
            }

            @Override
            public String toString() {
                return "Connection{" +
                        "nodeA=" + nodeA +
                        ", nodeB=" + nodeB +
                        '}';
            }
        }

        static class Node {
            public enum State {NOT_CONNECTED, PARTIALLY_CONNECTED, FULLY_CONNECTED}

            private final Coordinate coordinate;
            private final int initialWeight;
            private State state;
            private int weight;

            private Set<Connection> connections = new HashSet<>();

            public Node(Coordinate coordinate, int weight) {
                this.coordinate = coordinate;
                this.initialWeight = weight;
                this.weight = weight;
                this.state = State.NOT_CONNECTED;
            }

            public Connection connect(Node otherNode) {
                Connection connection = new Connection(this, otherNode);
                connections.add(connection);
                otherNode.connections.add(connection);
                return connection;
            }

            public void changeWeight(int toApply) {
                assert toApply != 0;
                this.weight += toApply;
                if (this.weight == initialWeight) {
                    state = State.NOT_CONNECTED;
                } else if (this.weight == 0) {
                    state = State.FULLY_CONNECTED;
                } else {
                    state = State.PARTIALLY_CONNECTED;
                }
            }

            public State getState() {
                return state;
            }

            public int getWeight() {
                return weight;
            }

            public Coordinate getCoordinate() {
                return coordinate;
            }

            public Set<Connection> getConnections() {
                return connections;
            }

            public Set<Connection> getUnprocessedConnections() {
                return connections.stream().filter((connection) -> !connection.isProcessed()).collect(Collectors.toSet());
            }

            @Override
            public String toString() {
                return "Node{" +
                        "coordinate=" + coordinate +
                        ", weight=" + weight + "/" + initialWeight +
                        ", " + state +
                        '}';
            }
        }

        /**
         * An assumption context
         */
        private class Context {
            private final boolean autoConnected;
            private Connection connection;
            private int connectionThickness;

            public Context(Connection connection) {
                this.autoConnected = false;
                this.connection = connection;
                this.connectionThickness = 3; // <-- start by being decremented
            }

            public Context(Connection connection, int connectionThickness) {
                this.autoConnected = true;
                this.connection = connection;
                this.connectionThickness = connectionThickness;
            }

            public Connection getConnection() {
                return connection;
            }

            public int getConnectionThickness() {
                return connectionThickness;
            }

            public void setConnectionThickness(int connectionWeight) {
                this.connectionThickness = connectionWeight;
            }

            public boolean isAutoConnected() {
                return autoConnected;
            }
        }

        private class CrossingConnectionException extends RuntimeException {
        }

        private class History {
            private LinkedList<Context> history = new LinkedList<>();

            public void add(Context context) {
                history.add(context);
            }

            public Context revert() {
                Context context;
                do {
                    context = history.removeLast();
                    resetWeight(context.getConnection());
                } while (context.isAutoConnected() || context.connectionThickness == 0);
                return context;
            }
        }
    }

    /**
     * A lightweight structure to be able to dump and restore level configuration
     */
    static class Configuration {
        private final int width;
        private final int height;
        private final List<String> lines = new ArrayList<>();

        Configuration(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public static Configuration forDump(String dump) {
            String[] lines = dump.split("[\\r\\n]+");
            String[] dimensions = lines[0].split(" ");
            Configuration configuration = new Configuration(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]));
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                configuration.addLine(line);
            }
            return configuration;
        }

        public void addLine(String line) {
            lines.add(line);
        }

        public void dump() {
            System.err.println(width + " " + height);
            lines.stream().forEach(System.err::println);

        }

        public List<String> getLines() {
            return lines;
        }
    }
}
