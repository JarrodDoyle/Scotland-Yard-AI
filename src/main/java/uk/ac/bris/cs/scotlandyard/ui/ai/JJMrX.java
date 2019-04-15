package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
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
				Graph<Integer,Transport> graph = view.getGraph();
				return (double) graph.getEdgesFrom(graph.getNode(((TicketMove) move).destination())).size();
			}
			return 0.1;
		}
	}
}
