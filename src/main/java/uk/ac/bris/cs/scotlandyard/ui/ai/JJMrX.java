package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.function.Consumer;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.TicketMove;
import uk.ac.bris.cs.scotlandyard.model.PassMove;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;
import uk.ac.bris.cs.scotlandyard.model.Transport;
import uk.ac.bris.cs.scotlandyard.model.Ticket;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.Graph;

@ManagedAI("JJ-MrX")
public class JJMrX implements PlayerFactory {
	private MyPlayer player;

	@Override
	public Player createPlayer(Colour colour) {
		// Only allow player creation for Mr X.
		if (colour.isDetective()) {
			throw new IllegalArgumentException("AI is for MrX only");
		}
		return player = new MyPlayer();
	}

	private static class MyPlayer implements Player{
		// List of tickets used since last reveal round.
		private List<Ticket> ticketsUsed = new ArrayList<>();

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {
			// Iterate over possible moves and select the one with the highest score
			Double maxScore = 0.0;
			ArrayList<Move> movesArray = new ArrayList<>(moves);
			Move bestMove = movesArray.get(0);
			for (Move m : movesArray) {
				if (moveScore(view, m) > maxScore) {
					bestMove = m;
				}
			}
			callback.accept(bestMove);
			ticketsUsed = updateTicketsUsed(view, bestMove, ticketsUsed);
		}

		public List<Ticket> updateTicketsUsed(ScotlandYardView view, Move move, List<Ticket> ticketsUsed) {
			// Return a copy of the passed tickets used list update according
			// to the move.
			List<Ticket> tickets = new ArrayList<>();
			tickets.addAll(ticketsUsed);
			if (move.getClass() == DoubleMove.class) {
				// If the second move of a double move is on a reveal round
				if (view.getRounds().get(view.getCurrentRound() + 1)) {
					tickets.clear();
				}
				// If the first move of a double move is on a reveal round
				else if (view.getRounds().get(view.getCurrentRound())) {
					tickets.clear();
					tickets.add(((DoubleMove) move).secondMove().ticket());
				}
				// If neither moves of a doule move are on a reveal round
				else {
					tickets.add(((DoubleMove) move).firstMove().ticket());
					tickets.add(((DoubleMove) move).secondMove().ticket());
				}
			}
			else if (move.getClass() == TicketMove.class) {
				// If the ticket move is on a reveal round
				if (view.getRounds().get(view.getCurrentRound())) {
					tickets.clear();
				}
				else {
					tickets.add(((TicketMove) move).ticket());
				}
			}
			return tickets;
		}

		public int updatePosition(ScotlandYardView view, Move move, int position) {
			// Returns an updated Mr X known position.
			// No need to make a copy.
			if (move.getClass() == DoubleMove.class) {
				// If the second move of a double move is on a reveal round
				if (view.getRounds().get(view.getCurrentRound() + 1)) {
					position = ((DoubleMove) move).secondMove().destination();
				}
				// If the first move of a double move is on a reveal round
				else if (view.getRounds().get(view.getCurrentRound())) {
					position = ((DoubleMove) move).firstMove().destination();
				}
				// If neither are true, position needs not be updated.
			}
			else if (move.getClass() == TicketMove.class) {
				// If the ticket move is on a reveal round
				if (view.getRounds().get(view.getCurrentRound())) {
					position = ((TicketMove) move).destination();
				}
				// If not true, position needs not be updated.
			}
			return position;
		}

		public Double moveScore(ScotlandYardView view, Move move) {
			// Returns a score value for the passed move
			double score = 0;
			if (move.getClass() == DoubleMove.class) {
				score += distanceValue(view, ((DoubleMove) move).secondMove());
				score += potentialPositionsValue(view, move);
			}
			else if (move.getClass() == TicketMove.class) {
				score += distanceValue(view, move);
				score *= potentialPositionsValue(view, move);
			}
			return score;
		}

		private Double distanceValue(ScotlandYardView view, Move move) {
			// Returns a distance value using Dijkstra's algorithm and
			// taking into account all detectives.

			// Calculate the distance values for each detective
			List<Integer> distances = new ArrayList<>();
			for (Colour colour : view.getPlayers()) {
				if (colour.isDetective()) {
					distances.add(dijkstra(view, ((TicketMove) move).destination(), view.getPlayerLocation(colour).get()));
				}
			}
			// Distance list is sorted so that it can be iterated over from
			// closest to furthest.
			Collections.sort(distances);

			double distanceScore = 0.0;
			int size = distances.size();
			// Iterate over sorted list. Closer detectives should be considered
			// more strongly than further detectives.
			for (int i = 0; i < size; i++) {
				distanceScore += ((double) distances.get(i)) * (1 - i / size);
			}
			return distanceScore;
		}

		private Double potentialPositionsValue(ScotlandYardView view, Move move) {
			// Return a score relating to how many potential positions Mr X
			// could be at based on his previously known position and the tickets
			// he has used since then.

			int location = view.getPlayerLocation(view.getCurrentPlayer()).get();
			location = updatePosition(view, move, location);
			// If Mr X has been previously revealed, calculate his possible
			// positions. Otherwise just return a default value.
			if (location != 0) {
				Set<Integer> positions = new HashSet<>();
				Node<Integer> node = view.getGraph().getNode(location);
				possiblePositions(view, positions, node, updateTicketsUsed(view, move, ticketsUsed));
				return (positions.size() > 4) ? 5.0 : (double) positions.size();
			}
			return 1.0;
		}

		private static void possiblePositions(ScotlandYardView view, Set<Integer> positions, Node<Integer> next, List<Ticket> path) {
			// Recursive function that returns a set of possible positions for Mr X.

			// If there are no more tickets that can be used, a potential position
			// has been reached so add it to the set. Because a set is used, no
			// duplicate positions will be counted.
			if (path.isEmpty()) {
				positions.add(next.value());
			}
			else {
				// Loop over the edges from the current position and if the next
				// ticket in the ticket path can be used then recursively call
				// this function with updated info.
				for (Edge<Integer,Transport> edge : view.getGraph().getEdgesFrom(next)) {
					if (Ticket.fromTransport(edge.data()) == path.get(0) || path.get(0) == Ticket.SECRET) {
						possiblePositions(view, positions, edge.destination(), path.subList(1, path.size()));
					}
				}
			}
		}

		public Integer dijkstra(ScotlandYardView view, int location, int targetID) {
			// Return the length of the shortest path between two points using
			// Dijkstra's algorithm.

			Graph<Integer,Transport> graph = view.getGraph();
			Node<Integer> target = graph.getNode(targetID);
			Set<Node<Integer>> unvisitedNodes = new HashSet<>(graph.getNodes());
			Map<Node<Integer>,Integer> distances = new HashMap<>();
			Map<Node<Integer>,Node<Integer>> previousNodes = new HashMap<>();
			// For each element in the set of unvisited nodes (all nodes at this time)
			// set the distance to that node to be -1 (uncalculated) and
			// set the previous node in the shortest path to the current node to be null
			for (Node<Integer> node : unvisitedNodes) {
				distances.put(node, -1);
				previousNodes.put(node, null);
			}
			// Update the distance map to for the starting position to be 0
			distances.put(graph.getNode(location), 0);
			Node<Integer> currentNode;
			int minDistance;
			// Carry on running the algorithm until all nodes have been visited
			while (unvisitedNodes.size() > 0) {
				minDistance = -1;
				currentNode = null;

				// Loop over the set of unvisited nodes and select the one with
				// the shortest distance to it. Remove this node from the set.
				for (Node<Integer> node : unvisitedNodes) {
					int  distance = distances.get(node);
					if (distance >= 0 && (minDistance < 0 || distance < minDistance)) {
						minDistance = distance;
						currentNode = node;
					}
				}
				if (minDistance < 0) {
					break;
				}
				unvisitedNodes.remove(currentNode);

				// If the node selected is the target, shortest path has been found
				// and the algorithm can break early.
				if (currentNode.equals(target)) {
					break;
				}

				// Otherwise if the target hasn't yet been found we must loop
				// over the edges from the current node that lead to an unvisited
				// node. For each of these edges, see if they allow a shorter path
				// to their destination, if they do then update the distance map.
				for (Edge<Integer,Transport> edge : graph.getEdgesFrom(currentNode)) {
					Node<Integer> destination = edge.destination();
					if (unvisitedNodes.contains(destination)) {
						int newDistance = distances.get(currentNode) + 1;
						if (newDistance < distances.get(destination) || distances.get(destination) < 0) {
							distances.put(destination, newDistance);
							previousNodes.put(destination, currentNode);
						}
					}
				}
			}
			return distances.get(target);
		}
	}
}
