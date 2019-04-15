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

		private final Random random = new Random();

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

		}

		public Double moveScore(ScotlandYardView view, Move move) {
			if (move.getClass() == DoubleMove.class) {
				return moveScore(view, ((DoubleMove) move).secondMove());
			}
			else if (move.getClass() == TicketMove.class) {
				List<Integer> distances = new ArrayList<Integer>();
				for (Colour colour : view.getPlayers()) {
					if (colour.isDetective()) {
						distances.add(dijkstra(view, ((TicketMove) move).destination(), view.getPlayerLocation(colour).get()));
					}
				}
				Collections.sort(distances);
				return (double) distances.get(0);
			}
			return 0.0;
		}

		public Integer dijkstra(ScotlandYardView view, int location, int targetID) {
			Graph<Integer,Transport> graph = view.getGraph();
			Node<Integer> target = graph.getNode(targetID);
			Set<Node<Integer>> unvisitedNodes = new HashSet<Node<Integer>>(graph.getNodes());
			Map<Node<Integer>,Integer> distances = new HashMap<Node<Integer>,Integer>();
			Map<Node<Integer>,Node<Integer>> previousNodes = new HashMap<Node<Integer>,Node<Integer>>();
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
