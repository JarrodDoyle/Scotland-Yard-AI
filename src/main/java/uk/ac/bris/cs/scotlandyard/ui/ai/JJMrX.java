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
		if (colour.isDetective()) {
			throw new IllegalArgumentException("AI is for MrX only");
		}
		return player = new MyPlayer();
	}

	// TODO A sample player that selects a random move
	private static class MyPlayer implements Player{

		// private final Random random = new Random();
		private List<Ticket> ticketsUsed = new ArrayList<>();

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {
			// TODO do something interesting here; find the best move

			Double maxScore = 0.0;
			ArrayList<Move> movesArray = new ArrayList<>(moves);
			Move bestMove = movesArray.get(0);
			for (Move m : movesArray) {
				if (moveScore(view, m) > maxScore) {
					bestMove = m;
				}
			}
			callback.accept(bestMove);
			System.out.println("\nPositions: " + potentialPositionsValue(view, bestMove).toString());
			ticketsUsed = updateTicketsUsed(view, bestMove, ticketsUsed);
			System.out.println("CurrentRound: " + view.getCurrentRound());
			System.out.println(ticketsUsed.toString());
		}

		public List<Ticket> updateTicketsUsed(ScotlandYardView view, Move move, List<Ticket> ticketsUsed) {
			List<Ticket> tickets = new ArrayList<>();
			tickets.addAll(ticketsUsed);
			if (move.getClass() == DoubleMove.class) {
				if (view.getRounds().get(view.getCurrentRound() + 1)) {
					tickets.clear();
				}
				else if (view.getRounds().get(view.getCurrentRound())) {
					tickets.clear();
					tickets.add(((DoubleMove) move).secondMove().ticket());
				}
				else {
					tickets.add(((DoubleMove) move).firstMove().ticket());
					tickets.add(((DoubleMove) move).secondMove().ticket());
				}
			}
			else if (move.getClass() == TicketMove.class) {
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
			if (move.getClass() == DoubleMove.class) {
				if (view.getRounds().get(view.getCurrentRound() + 1)) {
					position = ((DoubleMove) move).secondMove().destination();
				}
				else if (view.getRounds().get(view.getCurrentRound())) {
					position = ((DoubleMove) move).firstMove().destination();
				}
			}
			else if (move.getClass() == TicketMove.class) {
				if (view.getRounds().get(view.getCurrentRound())) {
					position = ((TicketMove) move).destination();
				}
			}
			return position;
		}

		public Double moveScore(ScotlandYardView view, Move move) {
			/*
			Things that could be taken into account:
			- ~Distance from detectives (lesser effect for further ones?)~
			- How many moves can be made from the new location
			- ~How many potential places player can be based on previous location and tickets used~
			- Save secret/double moves for when Mr.X is in a dangerous position?
			*/
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
			List<Integer> distances = new ArrayList<>();
			for (Colour colour : view.getPlayers()) {
				if (colour.isDetective()) {
					distances.add(dijkstra(view, ((TicketMove) move).destination(), view.getPlayerLocation(colour).get()));
				}
			}
			Collections.sort(distances);

			double distanceScore = 0.0;
			int size = distances.size();
			for (int i = 0; i < size; i++) {
				distanceScore += ((double) distances.get(i)) * (1 - i / size);
			}
			return distanceScore;
		}

		private Double movesValue(ScotlandYardView view, Move move) {
			// Generate set of valid moves at new position
			// Multiply length of move set by 0.2(?)
			// Return
			return 0.0;
		}

		private Double potentialPositionsValue(ScotlandYardView view, Move move) {
			// Accurate position now being used.
			int location = view.getPlayerLocation(view.getCurrentPlayer()).get();
			location = updatePosition(view, move, location);
			if (location != 0) {
				Set<Integer> positions = new HashSet<>();
				Node<Integer> node = view.getGraph().getNode(location);
				possiblePositions(view, positions, node, updateTicketsUsed(view, move, ticketsUsed));
				return (double) positions.size();
			}
			return 1.0;
		}

		private static void possiblePositions(ScotlandYardView view, Set<Integer> positions, Node<Integer> next, List<Ticket> path) {
			if (path.isEmpty()) {
				positions.add(next.value());
			}
			else {
				for (Edge<Integer,Transport> edge : view.getGraph().getEdgesFrom(next)) {
					if (Ticket.fromTransport(edge.data()) == path.get(0) || path.get(0) == Ticket.SECRET) {
						possiblePositions(view, positions, edge.destination(), path.subList(1, path.size()));
					}
				}
			}
		}

		private Double dangerValue(ScotlandYardView view, Move move) {
			// Decide whether MrX is currently in a dangerous position
			// If not assign a low value, otherwise give a high one
			return 0.0;
		}

		// Tested, definitely works and I'm very surprised it worked first time
		public Integer dijkstra(ScotlandYardView view, int location, int targetID) {
			Graph<Integer,Transport> graph = view.getGraph();
			Node<Integer> target = graph.getNode(targetID);
			Set<Node<Integer>> unvisitedNodes = new HashSet<>(graph.getNodes());
			Map<Node<Integer>,Integer> distances = new HashMap<>();
			Map<Node<Integer>,Node<Integer>> previousNodes = new HashMap<>();
			for (Node<Integer> node : unvisitedNodes) {
				distances.put(node, -1);
				previousNodes.put(node, null);
			}
			distances.put(graph.getNode(location), 0);
			Node<Integer> currentNode;
			int minDistance;
			while (unvisitedNodes.size() > 0) {
				minDistance = -1;
				currentNode = null;
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

				if (currentNode.equals(target)) {
					break;
				}

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
