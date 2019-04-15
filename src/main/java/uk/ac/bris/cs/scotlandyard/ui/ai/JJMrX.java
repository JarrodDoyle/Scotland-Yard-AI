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
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;

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
			// // picks a random move
			// callback.accept(new ArrayList<>(moves).get(random.nextInt(moves.size())));

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
			return 0.1;
		}
	}
}
