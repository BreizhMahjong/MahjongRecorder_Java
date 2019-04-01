/*
 * This file is part of Breizh Mahjong Recorder.
 *
 * Breizh Mahjong Recorder is free software: you can redistribute it AND/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Breizh Mahjong Recorder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Breizh Mahjong Recorder. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.bmj.bmjc.dataaccess.impl.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.bmj.bmjc.data.game.Player;
import fr.bmj.bmjc.data.game.Tournament;
import fr.bmj.bmjc.data.game.rcr.RCRGame;
import fr.bmj.bmjc.data.game.rcr.RCRScore;
import fr.bmj.bmjc.data.stat.rcr.RCRDataPackageAnalyze;
import fr.bmj.bmjc.data.stat.rcr.RCRDataPackageTrend;
import fr.bmj.bmjc.data.stat.rcr.RCRTotalScore;
import fr.bmj.bmjc.dataaccess.UpdateResult;
import fr.bmj.bmjc.dataaccess.rcr.DataAccessRCR;
import fr.bmj.bmjc.enums.EnumPeriodMode;
import fr.bmj.bmjc.enums.EnumRankingMode;
import fr.bmj.bmjc.enums.EnumScoreMode;
import fr.bmj.bmjc.enums.EnumSortingMode;

public class DataAccessDataBaseRCR extends DataAccessDataBaseCommon implements DataAccessRCR {

	private static final int NUMBER_TOP = 30;
	private static final int MINIMUM_GAME_MONTH = 2;
	private static final int MINIMUM_GAME_TRIMESTER = 5;
	private static final int MINIMUM_GAME_YEAR = 20;

	private boolean useMinimumGame;

	public DataAccessDataBaseRCR(final Connection dataBaseConnection) {
		super(dataBaseConnection);
	}

	private boolean isConnected() {
		try {
			return dataBaseConnection != null && !dataBaseConnection.isClosed();
		} catch (final SQLException e) {
			return false;
		}
	}

	@Override
	public List<Player> getRCRPlayers() {
		final List<Player> playerList = new ArrayList<Player>();
		if (isConnected()) {
			try {
				final Statement statement = dataBaseConnection.createStatement();
				final ResultSet result = statement
					.executeQuery("SELECT DISTINCT player.id, player.name, player.display_name FROM player, rcr_game_score WHERE player.id=rcr_game_score.player_id ORDER BY player.id");
				while (result.next()) {
					playerList.add(new Player(result.getInt(1), result.getString(2), result.getString(3), false));
				}
				result.close();
				statement.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return playerList;
	}

	@Override
	public UpdateResult addRCRTournament(final String tournamentName) {
		if (!isConnected()) {
			return new UpdateResult(false, "Pas de connxion à la base de données");
		}
		if (tournamentName == null) {
			return new UpdateResult(false, "Le nom ne peut pas être vide");
		}
		int newId;
		boolean added;
		try {
			final String query = "SELECT id FROM rcr_tournament ORDER BY id";
			final Statement statement = dataBaseConnection.createStatement();
			final ResultSet result = statement.executeQuery(query);
			newId = 1;
			while (result.next()) {
				if (newId == result.getInt(1)) {
					newId++;
				} else {
					break;
				}
			}
			result.close();
			statement.close();
		} catch (final SQLException e) {
			e.printStackTrace();
			return new UpdateResult(false, "Erreur de connexion de données");
		}

		try {
			final String query = "INSERT INTO rcr_tournament(id, name) VALUES(?, ?)";
			final PreparedStatement statement = dataBaseConnection.prepareStatement(query);
			statement.setInt(1, newId);
			statement.setString(2, tournamentName);
			added = statement.executeUpdate() == 1;
			statement.close();
		} catch (final SQLException e) {
			e.printStackTrace();
			return new UpdateResult(false, "Erreur de connexion de données");
		}

		if (added) {
			return new UpdateResult(true, "OK");
		} else {
			return new UpdateResult(false, "Le nom est déjà utilisé");
		}
	}

	@Override
	public UpdateResult modifyRCRTournament(final int tournamentId, final String tournamentName) {
		if (!isConnected()) {
			return new UpdateResult(false, "Pas de connxion à la base de données");
		}
		if (tournamentName == null) {
			return new UpdateResult(false, "Le nom ne peut pas être vide");
		}

		boolean modified;
		try {
			final String query = "UPDATE rcr_tournament SET name=? WHERE id=?";
			final PreparedStatement statement = dataBaseConnection.prepareStatement(query);
			statement.setString(1, tournamentName);
			statement.setInt(2, tournamentId);
			modified = statement.executeUpdate() == 1;
			statement.close();
		} catch (final SQLException e) {
			e.printStackTrace();
			return new UpdateResult(false, "Erreur de connexion de données");
		}

		if (modified) {
			return new UpdateResult(true, "OK");
		} else {
			return new UpdateResult(false, "Le nom est déjà utilisé");
		}
	}

	@Override
	public List<Tournament> getRCRTournaments() {
		final List<Tournament> tournamentList = new ArrayList<Tournament>();
		if (isConnected()) {
			try {
				final Statement statement = dataBaseConnection.createStatement();
				final ResultSet result = statement.executeQuery("SELECT id, name FROM rcr_tournament ORDER BY id DESC");
				while (result.next()) {
					tournamentList.add(new Tournament(result.getInt(1), result.getString(2)));
				}
				result.close();
				statement.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return tournamentList;
	}

	@Override
	public UpdateResult deleteRCRTournament(final int tournamentId) {
		if (!isConnected()) {
			return new UpdateResult(false, "Pas de connxion à la base de données");
		}

		boolean modified;
		try {
			final String query = "DELETE FROM rcr_tournament WHERE id=?";
			final PreparedStatement statement = dataBaseConnection.prepareStatement(query);
			statement.setInt(1, tournamentId);
			modified = statement.executeUpdate() == 1;
			statement.close();
		} catch (final SQLException e) {
			e.printStackTrace();
			return new UpdateResult(false, "Erreur de connexion de base de données");
		}

		if (modified) {
			return new UpdateResult(true, "OK");
		} else {
			return new UpdateResult(false, "Le tournoi n'a pas été supprimé");
		}
	}

	@Override
	public UpdateResult addRCRGame(final RCRGame game) {
		if (!isConnected()) {
			return new UpdateResult(false, "Pas de connxion à la base de données");
		}
		if (game == null) {
			return new UpdateResult(false, "L'information du jeu ne peut pas être vide");
		}

		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, game.getYear());
		calendar.set(Calendar.MONTH, game.getMonth());
		calendar.set(Calendar.DAY_OF_MONTH, game.getDay());
		final Date date = new Date(calendar.getTimeInMillis());
		int newId;
		try {
			final String query = "SELECT id FROM rcr_game_id WHERE date=? ORDER BY id";
			final PreparedStatement statement = dataBaseConnection.prepareStatement(query);
			statement.setDate(1, date);
			final ResultSet result = statement.executeQuery();
			newId = ((game.getYear() % 100 * 100 + game.getMonth() + 1) * 100 + game.getDay()) * 100 + 1;
			while (result.next()) {
				if (newId == result.getInt(1)) {
					newId++;
				} else {
					break;
				}
			}
			result.close();
			statement.close();
		} catch (final SQLException e) {
			e.printStackTrace();
			return new UpdateResult(false, "Erreur de connexion de base de données");
		}

		try {
			final String idTableQuery = "INSERT INTO rcr_game_id(id, rcr_tournament_id, date, nb_players, nb_rounds) VALUES(?, ?, ?, ?, ?)";
			final PreparedStatement idTableStatement = dataBaseConnection.prepareStatement(idTableQuery);
			idTableStatement.setInt(1, newId);
			idTableStatement.setInt(2, game.getTournamentId());
			idTableStatement.setDate(3, date);
			idTableStatement.setInt(4, game.getNbPlayers());
			idTableStatement.setInt(5, game.getNbRounds());
			idTableStatement.executeUpdate();
			idTableStatement.close();

			final String scoreTableQuery = "INSERT INTO rcr_game_score(rcr_game_id, player_id, ranking, game_score, uma_score, final_score) values(?, ?, ?, ?, ?, ?)";
			final PreparedStatement scoreTableStatement = dataBaseConnection.prepareStatement(scoreTableQuery);
			for (int playerIndex = 0; playerIndex < game.getScores().size(); playerIndex++) {
				final RCRScore score = game.getScores().get(playerIndex);
				scoreTableStatement.setInt(1, newId);
				scoreTableStatement.setInt(2, score.getPlayerId());
				scoreTableStatement.setInt(3, score.getPlace());
				scoreTableStatement.setInt(4, score.getGameScore());
				scoreTableStatement.setInt(5, score.getUmaScore());
				scoreTableStatement.setInt(6, score.getFinalScore());
				scoreTableStatement.execute();
				scoreTableStatement.clearParameters();
			}
			scoreTableStatement.close();
		} catch (final SQLException e) {
			e.printStackTrace();
			return new UpdateResult(false, "Erreur de connexion de base de données");
		}
		return new UpdateResult(true, "Scores enregistrés. ID " + Integer.toString(newId) + ".");
	}

	@Override
	public List<Integer> getRCRYears(final Tournament tournament) {
		final List<Integer> yearList = new ArrayList<Integer>();
		if (isConnected()) {
			try {
				final PreparedStatement statement = dataBaseConnection.prepareStatement("SELECT DISTINCT YEAR(date) FROM rcr_game_id WHERE rcr_tournament_id=? ORDER BY YEAR(date) DESC");
				statement.setInt(1, tournament.getId());
				final ResultSet result = statement.executeQuery();
				while (result.next()) {
					yearList.add(result.getInt(1));
				}
				result.close();
				statement.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return yearList;
	}

	@Override
	public List<Integer> getRCRGameDays(final Tournament tournament, final int year, final int month) {
		final List<Integer> dayList = new ArrayList<Integer>();
		if (isConnected()) {
			try {
				final PreparedStatement statement = dataBaseConnection
					.prepareStatement("SELECT DISTINCT day(date) FROM rcr_game_id WHERE rcr_tournament_id=? AND year(date)=? AND month(date)=? ORDER BY day(date)");
				statement.setInt(1, tournament.getId());
				statement.setInt(2, year);
				statement.setInt(3, month + 1);

				final ResultSet result = statement.executeQuery();
				while (result.next()) {
					dayList.add(result.getInt(1));
				}
				result.close();
				statement.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return dayList;
	}

	@Override
	public List<Integer> getRCRGameIds(final Tournament tournament, final int year, final int month, final int day) {
		final List<Integer> idList = new ArrayList<Integer>();
		if (isConnected()) {
			try {
				final Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.YEAR, year);
				calendar.set(Calendar.MONTH, month);
				calendar.set(Calendar.DAY_OF_MONTH, day);
				final PreparedStatement statement = dataBaseConnection.prepareStatement("SELECT id FROM rcr_game_id WHERE rcr_tournament_id=? AND date=? ORDER BY id");
				statement.setInt(1, tournament.getId());
				statement.setDate(2, new Date(calendar.getTimeInMillis()));

				final ResultSet result = statement.executeQuery();
				while (result.next()) {
					idList.add(result.getInt(1));
				}
				result.close();
				statement.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return idList;
	}

	@Override
	public RCRGame getRCRGame(final int id) {
		if (isConnected()) {
			try {
				final Calendar calendar = Calendar.getInstance();
				int nbPlayers = 0;
				int nbRounds = 0;
				int tournamentId = 0;
				PreparedStatement statement = dataBaseConnection.prepareStatement("SELECT rcr_tournament_id, date, nb_players, nb_rounds FROM rcr_game_id WHERE id=?");
				statement.setInt(1, id);
				ResultSet result = statement.executeQuery();
				if (result.next()) {
					tournamentId = result.getInt(1);
					calendar.setTimeInMillis(result.getDate(2).getTime());
					nbPlayers = result.getInt(3);
					nbRounds = result.getInt(4);
				}
				result.close();
				statement.close();

				if (nbPlayers > 0) {
					final List<RCRScore> scoreList = new ArrayList<RCRScore>(nbPlayers);
					statement = dataBaseConnection.prepareStatement(
						"SELECT player.id, player.name, player.display_name, rcr_game_score.ranking, rcr_game_score.game_score, rcr_game_score.uma_score, rcr_game_score.final_score FROM player, rcr_game_score WHERE player.id=rcr_game_score.player_id AND rcr_game_score.rcr_game_id=? ORDER BY rcr_game_score.ranking");
					statement.setInt(1, id);
					result = statement.executeQuery();
					while (result.next()) {
						scoreList.add(new RCRScore(result.getInt(1), result.getString(2), result.getString(3), result.getInt(4), result.getInt(5), result.getInt(6), result.getInt(7)));
					}
					result.close();
					statement.close();

					return new RCRGame(id, tournamentId, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), nbRounds, nbPlayers, scoreList);
				} else {
					return null;
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public UpdateResult deleteRCRGame(final int id) {
		if (!isConnected()) {
			return new UpdateResult(false, "Pas de connxion à la base de données");
		}

		boolean modified;
		try {
			final String query = "DELETE FROM rcr_game_id WHERE id=?";
			final PreparedStatement statement = dataBaseConnection.prepareStatement(query);
			statement.setInt(1, id);
			modified = statement.executeUpdate() == 1;
			statement.close();
		} catch (final SQLException e) {
			e.printStackTrace();
			return new UpdateResult(false, "Erreur de connexion de données");
		}

		if (modified) {
			return new UpdateResult(true, "OK");
		} else {
			return new UpdateResult(false, "Le tournoi n'a pas été supprimé");
		}
	}

	@Override
	public void setRCRUseMinimumGame(final boolean useMinimumGame) {
		this.useMinimumGame = useMinimumGame;
	}

	@Override
	public RCRDataPackageAnalyze getRCRDataPackageAnalyze(final Tournament tournament, final int playerId, final EnumScoreMode scoreMode, final EnumPeriodMode periodMode, final int year,
		final int trimester, final int month) {
		final Calendar calendarFrom = Calendar.getInstance();
		final Calendar calendarTo = Calendar.getInstance();
		switch (periodMode) {
			case ALL:
				break;
			case YEAR:
				calendarFrom.set(Calendar.YEAR, year);
				calendarFrom.set(Calendar.MONTH, 0);
				calendarFrom.set(Calendar.DAY_OF_MONTH, 1);
				calendarTo.set(Calendar.YEAR, year + 1);
				calendarTo.set(Calendar.MONTH, 0);
				calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				break;
			case TRIMESTER:
				calendarFrom.set(Calendar.YEAR, year);
				calendarFrom.set(Calendar.MONTH, trimester * 3);
				calendarFrom.set(Calendar.DAY_OF_MONTH, 1);
				if (trimester == 3) {
					calendarTo.set(Calendar.YEAR, year + 1);
					calendarTo.set(Calendar.MONTH, 0);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				} else {
					calendarTo.set(Calendar.YEAR, year);
					calendarTo.set(Calendar.MONTH, (trimester + 1) * 3);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				}
				break;
			case MONTH:
				calendarFrom.set(Calendar.YEAR, year);
				calendarFrom.set(Calendar.MONTH, month);
				calendarFrom.set(Calendar.DAY_OF_MONTH, 1);
				if (month == 11) {
					calendarTo.set(Calendar.YEAR, year + 1);
					calendarTo.set(Calendar.MONTH, 0);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				} else {
					calendarTo.set(Calendar.YEAR, year);
					calendarTo.set(Calendar.MONTH, month + 1);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				}
				break;
			default:
				break;
		}

		final RCRDataPackageAnalyze dataPackage = new RCRDataPackageAnalyze();
		try {
			String fieldString = null;
			switch (scoreMode) {
				case FINAL_SCORE:
					fieldString = "final_score";
					break;
				case NET_SCORE:
					fieldString = "game_score - 30000";
					break;
				case GAME_SCORE:
					fieldString = "game_score";
					break;
				default:
					fieldString = "";
					break;
			}

			PreparedStatement statement = null;
			if (periodMode == EnumPeriodMode.ALL) {
				statement = dataBaseConnection.prepareStatement("SELECT rcr_game_id.id, rcr_game_id.nb_players, rcr_game_score.ranking, rcr_game_score." + fieldString
					+ " FROM rcr_game_id, rcr_game_score WHERE rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_score.player_id=? AND rcr_game_id.rcr_tournament_id=? ORDER BY rcr_game_id.id ASC");
				statement.setInt(1, playerId);
				statement.setInt(2, tournament.getId());
			} else {
				statement = dataBaseConnection.prepareStatement("SELECT rcr_game_id.id, rcr_game_id.nb_players, rcr_game_score.ranking, rcr_game_score." + fieldString
					+ " FROM rcr_game_id, rcr_game_score WHERE rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_score.player_id=? AND rcr_game_id.rcr_tournament_id=? AND rcr_game_id.date>=? AND rcr_game_id.date<? ORDER BY rcr_game_id.id ASC");
				statement.setInt(1, playerId);
				statement.setInt(2, tournament.getId());
				statement.setDate(3, new Date(calendarFrom.getTimeInMillis()));
				statement.setDate(4, new Date(calendarTo.getTimeInMillis()));
			}
			final ResultSet result = statement.executeQuery();

			int numberOfGames = 0;
			final List<Integer> listGameID = new ArrayList<>();
			final List<Integer> listScore = new ArrayList<>();
			final List<Integer> listSum = new ArrayList<>();

			int numberOfFourPlayersGames = 0;
			int numberOfFivePlayersGames = 0;
			int totalScore = 0;
			int positiveGames = 0;
			int negativeGames = 0;
			final int placeFourPlayers[] = new int[4];
			final int placeFourPlayersPercent[] = new int[4];
			final int placeFivePlayers[] = new int[5];
			final int placeFivePlayersPercent[] = new int[5];
			int maxScore = Integer.MIN_VALUE;
			int minScore = Integer.MAX_VALUE;
			int maxTotal = Integer.MIN_VALUE;
			int minTotal = Integer.MAX_VALUE;

			while (result.next()) {
				final int gameID = result.getInt(1);
				final int nbPlayers = result.getInt(2);
				final int ranking = result.getInt(3);
				final int score = result.getInt(4);

				listGameID.add(numberOfGames, gameID);
				listScore.add(numberOfGames, score);

				if (score >= 0) {
					positiveGames++;
				} else {
					negativeGames++;
				}
				maxScore = Math.max(maxScore, score);
				minScore = Math.min(minScore, score);

				totalScore += score;
				listSum.add(numberOfGames, totalScore);
				maxTotal = Math.max(maxTotal, totalScore);
				minTotal = Math.min(minTotal, totalScore);

				if (nbPlayers == 4) {
					placeFourPlayers[ranking - 1]++;
					numberOfFourPlayersGames++;
				} else if (nbPlayers == 5) {
					placeFivePlayers[ranking - 1]++;
					numberOfFivePlayersGames++;
				}
				numberOfGames++;
			}
			final double averageScore = (double) totalScore / numberOfGames;
			double deviation = 0.0;
			for (int index = 0; index < numberOfGames; index++) {
				deviation += Math.pow(listScore.get(index) - averageScore, 2.0);
			}
			final long standardDeviation = numberOfGames <= 1 ? 0 : Math.round(Math.sqrt(deviation / numberOfGames));

			for (int index = 0; index < 4; index++) {
				placeFourPlayersPercent[index] = Math.round(placeFourPlayers[index] * 100f / numberOfFourPlayersGames);
			}
			for (int index = 0; index < 5; index++) {
				placeFivePlayersPercent[index] = Math.round(placeFivePlayers[index] * 100f / numberOfFivePlayersGames);
			}

			dataPackage.setLists(listGameID, listScore, listSum);
			dataPackage.setNumberOfGames(numberOfGames);

			dataPackage.setScoreMax(maxScore);
			dataPackage.setScoreMin(minScore);

			dataPackage.setPositiveGames(positiveGames);
			dataPackage.setPositiveGamesPercent(Math.round(positiveGames * 100f / numberOfGames));

			dataPackage.setNegativeGames(negativeGames);
			dataPackage.setNegativeGamesPercent(Math.round(negativeGames * 100f / numberOfGames));

			dataPackage.setScoreTotal(totalScore);
			dataPackage.setScoreMean((int) Math.round(averageScore));
			dataPackage.setScoreStandardDeviation((int) standardDeviation);

			dataPackage.setTotalMax(maxTotal);
			dataPackage.setTotalMin(minTotal);

			dataPackage.setNumberOfFourPlayerGames(numberOfFourPlayersGames);
			dataPackage.setFourPlayerGamePlaces(placeFourPlayers);
			dataPackage.setFourPlayerGamePlacePercent(placeFourPlayersPercent);

			dataPackage.setNumberOfFivePlayerGames(numberOfFivePlayersGames);
			dataPackage.setFivePlayerGamePlaces(placeFivePlayers);
			dataPackage.setFivePlayerGamePlacePercent(placeFivePlayersPercent);

			result.close();
			statement.close();
		} catch (final Exception e) {
			dataPackage.setNumberOfGames(0);
			e.printStackTrace();
		}
		return dataPackage;
	}

	private static final float MILLISECONDS_PER_YEAR = 31557600000.0f;

	private float getNumberOfYearOfAllGamePeriod(final Tournament tournament) {
		Date firstDate = null;
		Date lastDate = null;
		try {
			final PreparedStatement statement = dataBaseConnection.prepareStatement("SELECT MIN(date), MAX(date) FROM rcr_game_id WHERE rcr_tournament_id=?");
			statement.setInt(1, tournament.getId());
			final ResultSet result = statement.executeQuery();
			if (result.next()) {
				firstDate = result.getDate(1);
				lastDate = result.getDate(2);
			}
			result.close();
			statement.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if (firstDate != null && lastDate != null) {
			return (lastDate.getTime() - firstDate.getTime()) / MILLISECONDS_PER_YEAR;
		} else {
			return 0.0f;
		}
	}

	@Override
	public List<RCRTotalScore> getRCRDataPackageRanking(final Tournament tournament, final EnumRankingMode rankingMode, final EnumSortingMode sortingMode, final EnumPeriodMode periodMode,
		final int year, final int trimester, final int month) {
		final Calendar calendarFrom = Calendar.getInstance();
		final Calendar calendarTo = Calendar.getInstance();
		int minimumGames = 0;
		switch (periodMode) {
			case MONTH:
				calendarFrom.set(Calendar.YEAR, year);
				calendarFrom.set(Calendar.MONTH, month);
				calendarFrom.set(Calendar.DAY_OF_MONTH, 1);
				if (month == Calendar.DECEMBER) {
					calendarTo.set(Calendar.YEAR, year + 1);
					calendarTo.set(Calendar.MONTH, Calendar.JANUARY);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				} else {
					calendarTo.set(Calendar.YEAR, year);
					calendarTo.set(Calendar.MONTH, month + 1);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				}
				minimumGames = MINIMUM_GAME_MONTH;
				break;
			case TRIMESTER:
				calendarFrom.set(Calendar.YEAR, year);
				calendarFrom.set(Calendar.MONTH, trimester * 3);
				calendarFrom.set(Calendar.DAY_OF_MONTH, 1);
				if (trimester == 3) {
					calendarTo.set(Calendar.YEAR, year + 1);
					calendarTo.set(Calendar.MONTH, Calendar.JANUARY);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				} else {
					calendarTo.set(Calendar.YEAR, year);
					calendarTo.set(Calendar.MONTH, (trimester + 1) * 3);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				}
				minimumGames = MINIMUM_GAME_TRIMESTER;
				break;
			case YEAR:
				calendarFrom.set(Calendar.YEAR, year);
				calendarFrom.set(Calendar.MONTH, Calendar.JANUARY);
				calendarFrom.set(Calendar.DAY_OF_MONTH, 1);
				calendarTo.set(Calendar.YEAR, year + 1);
				calendarTo.set(Calendar.MONTH, Calendar.JANUARY);
				calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				minimumGames = MINIMUM_GAME_YEAR;
				break;
			case ALL:
				if (useMinimumGame) {
					minimumGames = Math.round(MINIMUM_GAME_YEAR * getNumberOfYearOfAllGamePeriod(tournament));
				}
				break;
			default:
				break;
		}

		final List<RCRTotalScore> rankingScores = new ArrayList<>();
		try {
			switch (rankingMode) {
				case TOTAL_SCORE: {
					final String querySelectPart = "SELECT player.name, player.display_name, SUM(rcr_game_score.final_score) AS total, COUNT(*) AS nb_games FROM player, rcr_game_id, rcr_game_score";
					final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?";
					final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
					final String queryGroupPart = " GROUP BY player.name, player.display_name";
					final String queryHavingPart = useMinimumGame ? " HAVING COUNT(*)>=" + Integer.toString(minimumGames) : "";
					final String queryOrderPart = sortingMode == EnumSortingMode.DESCENDING ? " ORDER BY total DESC" : " ORDER BY total ASC";
					PreparedStatement statement = null;
					if (periodMode == EnumPeriodMode.ALL) {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryGroupPart + queryHavingPart + queryOrderPart);
						statement.setInt(1, tournament.getId());
					} else {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryGroupPart + queryHavingPart + queryOrderPart);
						statement.setInt(1, tournament.getId());
						statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
						statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
					}

					final ResultSet result = statement.executeQuery();
					while (result.next()) {
						final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), 0, 0, 0);
						total.totalScore = result.getInt(3);
						total.numberOfGame = result.getInt(4);
						rankingScores.add(total);
					}
					result.close();
					statement.close();
				}
					break;
				case FINAL_SCORE: {
					final String querySelectPart = " SELECT player.name, player.display_name, YEAR(rcr_game_id.date), MONTH(rcr_game_id.date)-1, DAY(rcr_game_id.date), rcr_game_score.final_score, rcr_game_score.uma_score FROM player, rcr_game_id, rcr_game_score";
					final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?";
					final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
					final String queryOrderPart = sortingMode == EnumSortingMode.DESCENDING ? " ORDER BY rcr_game_score.final_score DESC" : " ORDER BY rcr_game_score.final_score ASC";
					final String queryFetchPart = " FETCH FIRST " + Integer.toString(NUMBER_TOP) + " ROWS ONLY";
					PreparedStatement statement = null;
					if (periodMode == EnumPeriodMode.ALL) {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryOrderPart + queryFetchPart);
						statement.setInt(1, tournament.getId());
					} else {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryOrderPart + queryFetchPart);
						statement.setInt(1, tournament.getId());
						statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
						statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
					}

					final ResultSet result = statement.executeQuery();
					while (result.next()) {
						final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), result.getInt(3), result.getInt(4), result.getInt(5));
						total.totalScore = result.getInt(6);
						total.umaScore = result.getInt(7);
						rankingScores.add(total);
					}
					result.close();
					statement.close();
				}
					break;
				case MEAN_FINAL_SCORE: {
					final String querySelectPart = "SELECT player.name, player.display_name, AVG(rcr_game_score.final_score) AS mean, STDDEV_POP(rcr_game_score.final_score) as stddev, COUNT(*) AS nb_games FROM player, rcr_game_id, rcr_game_score";
					final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?";
					final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
					final String queryGroupPart = " GROUP BY player.name, player.display_name";
					final String queryHavingPart = useMinimumGame ? " HAVING COUNT(*)>=" + Integer.toString(minimumGames) : "";
					final String queryOrderPart = sortingMode == EnumSortingMode.DESCENDING ? " ORDER BY mean DESC" : " ORDER BY mean ASC";
					PreparedStatement statement = null;
					if (periodMode == EnumPeriodMode.ALL) {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryGroupPart + queryHavingPart + queryOrderPart);
						statement.setInt(1, tournament.getId());
					} else {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryGroupPart + queryHavingPart + queryOrderPart);
						statement.setInt(1, tournament.getId());
						statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
						statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
					}

					final ResultSet result = statement.executeQuery();
					while (result.next()) {
						final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), 0, 0, 0);
						total.totalScore = result.getInt(3);
						total.umaScore = (int) Math.round(result.getDouble(4));
						total.numberOfGame = result.getInt(5);
						rankingScores.add(total);
					}
					result.close();
					statement.close();
				}
					break;
				case GAME_SCORE: {
					final String querySelectPart = "SELECT player.name, player.display_name, YEAR(rcr_game_id.date), MONTH(rcr_game_id.date)-1, DAY(rcr_game_id.date), rcr_game_score.game_score FROM player, rcr_game_id, rcr_game_score";
					final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?";
					final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
					final String queryOrderPart = sortingMode == EnumSortingMode.DESCENDING ? " ORDER BY rcr_game_score.game_score DESC" : " ORDER BY rcr_game_score.game_score ASC";
					final String queryFetchPart = " FETCH FIRST " + Integer.toString(NUMBER_TOP) + " ROWS ONLY";
					PreparedStatement statement = null;
					if (periodMode == EnumPeriodMode.ALL) {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryOrderPart + queryFetchPart);
						statement.setInt(1, tournament.getId());
					} else {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryOrderPart + queryFetchPart);
						statement.setInt(1, tournament.getId());
						statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
						statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
					}

					final ResultSet result = statement.executeQuery();
					while (result.next()) {
						final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), result.getInt(3), result.getInt(4), result.getInt(5));
						total.totalScore = result.getInt(6);
						rankingScores.add(total);
					}
					result.close();
					statement.close();
				}
					break;
				case MEAN_GAME_SCORE: {
					final String querySelectPart = " SELECT player.name, player.display_name, AVG(rcr_game_score.game_score) AS mean, STDDEV_POP(rcr_game_score.game_score) as stddev, COUNT(*) AS nb_games FROM player, rcr_game_id, rcr_game_score";
					final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?";
					final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
					final String queryGroupPart = " GROUP BY player.name, player.display_name";
					final String queryHavingPart = useMinimumGame ? " HAVING COUNT(*)>=" + Integer.toString(minimumGames) : "";
					final String queryOrderPart = sortingMode == EnumSortingMode.DESCENDING ? " ORDER BY mean DESC" : " ORDER BY mean ASC";
					PreparedStatement statement = null;
					if (periodMode == EnumPeriodMode.ALL) {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryGroupPart + queryHavingPart + queryOrderPart);
						statement.setInt(1, tournament.getId());
					} else {
						statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryGroupPart + queryHavingPart + queryOrderPart);
						statement.setInt(1, tournament.getId());
						statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
						statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
					}

					final ResultSet result = statement.executeQuery();
					while (result.next()) {
						final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), 0, 0, 0);
						total.totalScore = result.getInt(3);
						total.umaScore = (int) Math.round(result.getDouble(4));
						total.numberOfGame = result.getInt(5);
						rankingScores.add(total);
					}
					result.close();
					statement.close();
				}
					break;
				case WIN_RATE: {
					final Map<String, RCRTotalScore> mapNameScore = new HashMap<String, RCRTotalScore>();
					{
						final String querySelectPart = "SELECT player.name, player.display_name, COUNT(*) AS nb_games FROM player, rcr_game_id, rcr_game_score";
						final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?";
						final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
						final String queryGroupPart = " GROUP BY player.name, player.display_name";
						final String queryHavingPart = useMinimumGame ? " HAVING COUNT(*)>=" + Integer.toString(minimumGames) : "";
						PreparedStatement statement = null;
						if (periodMode == EnumPeriodMode.ALL) {
							statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryGroupPart + queryHavingPart);
							statement.setInt(1, tournament.getId());
						} else {
							statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryGroupPart + queryHavingPart);
							statement.setInt(1, tournament.getId());
							statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
							statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
						}

						final ResultSet result = statement.executeQuery();
						while (result.next()) {
							final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), 0, 0, 0);
							total.numberOfGame = result.getInt(3);
							mapNameScore.put(total.displayName, total);
						}
						result.close();
						statement.close();
					}
					{
						final String querySelectPart = "SELECT player.display_name, COUNT(*) AS nb_games FROM player, rcr_game_id, rcr_game_score";
						final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=? AND rcr_game_score.ranking=1";
						final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
						final String queryGroupPart = " GROUP BY player.display_name";
						PreparedStatement statement = null;
						if (periodMode == EnumPeriodMode.ALL) {
							statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryGroupPart);
							statement.setInt(1, tournament.getId());
						} else {
							statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryGroupPart);
							statement.setInt(1, tournament.getId());
							statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
							statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
						}

						final ResultSet result = statement.executeQuery();
						while (result.next()) {
							final String name = result.getString(1);
							final RCRTotalScore total = mapNameScore.get(name);
							if (total != null) {
								total.umaScore = result.getInt(2);
								total.totalScore = Math.round(total.umaScore * 1000.0f / total.numberOfGame);
							}
						}
						result.close();
						statement.close();
					}
					rankingScores.addAll(mapNameScore.values());
					if (sortingMode == EnumSortingMode.DESCENDING) {
						Collections.sort(rankingScores, (final RCRTotalScore o1, final RCRTotalScore o2) -> -Integer.compare(o1.totalScore, o2.totalScore));
					} else {
						Collections.sort(rankingScores, (final RCRTotalScore o1, final RCRTotalScore o2) -> Integer.compare(o1.totalScore, o2.totalScore));
					}
				}
					break;
				case POSITIVE_RATE: {
					final Map<String, RCRTotalScore> mapNameScore = new HashMap<String, RCRTotalScore>();
					{
						final String querySelectPart = "SELECT player.name, player.display_name, COUNT(*) AS nb_games FROM player, rcr_game_id, rcr_game_score";
						final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?";
						final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
						final String queryGroupPart = " GROUP BY player.name, player.display_name";
						final String queryHavingPart = useMinimumGame ? " HAVING COUNT(*)>=" + Integer.toString(minimumGames) : "";
						PreparedStatement statement = null;
						if (periodMode == EnumPeriodMode.ALL) {
							statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryGroupPart + queryHavingPart);
							statement.setInt(1, tournament.getId());
						} else {
							statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryGroupPart + queryHavingPart);
							statement.setInt(1, tournament.getId());
							statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
							statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
						}

						final ResultSet result = statement.executeQuery();
						while (result.next()) {
							final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), 0, 0, 0);
							total.numberOfGame = result.getInt(3);
							mapNameScore.put(total.displayName, total);
						}
						result.close();
						statement.close();
					}
					{
						final String querySelectPart = "SELECT player.display_name, COUNT(*) AS nb_games FROM player, rcr_game_id, rcr_game_score";
						final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=? AND rcr_game_score.final_score>0";
						final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
						final String queryGroupPart = " GROUP BY player.display_name";
						PreparedStatement statement = null;
						if (periodMode == EnumPeriodMode.ALL) {
							statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryGroupPart);
							statement.setInt(1, tournament.getId());
						} else {
							statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryGroupPart);
							statement.setInt(1, tournament.getId());
							statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
							statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
						}

						final ResultSet result = statement.executeQuery();
						while (result.next()) {
							final String name = result.getString(1);
							final RCRTotalScore total = mapNameScore.get(name);
							if (total != null) {
								total.umaScore = result.getInt(2);
								total.totalScore = Math.round(total.umaScore * 1000.0f / total.numberOfGame);
							}
						}
						result.close();
						statement.close();
					}
					rankingScores.addAll(mapNameScore.values());
					if (sortingMode == EnumSortingMode.DESCENDING) {
						Collections.sort(rankingScores, (final RCRTotalScore o1, final RCRTotalScore o2) -> -Integer.compare(o1.totalScore, o2.totalScore));
					} else {
						Collections.sort(rankingScores, (final RCRTotalScore o1, final RCRTotalScore o2) -> Integer.compare(o1.totalScore, o2.totalScore));
					}
				}
					break;
				case ANNUAL_SCORE: {
					final String querySelectPart = "SELECT name, display_name, y, SUM(final_score) AS total, COUNT(*) AS nb_games FROM";
					final String querySubSelectPart = " (SELECT player.name, player.display_name, YEAR(rcr_game_id.date) as y, rcr_game_score.final_score FROM player, rcr_game_id, rcr_game_score WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?) AS year_score";
					final String queryGroupPart = " GROUP BY name, display_name, y";
					final String queryHavingPart = " HAVING COUNT(*)>=" + Integer.toString(MINIMUM_GAME_YEAR);
					final String queryOrderPart = sortingMode == EnumSortingMode.DESCENDING ? " ORDER BY total DESC" : " ORDER BY total ASC";
					final String queryFetchPart = " FETCH FIRST " + Integer.toString(NUMBER_TOP) + " ROWS ONLY";
					final PreparedStatement statement = dataBaseConnection.prepareStatement(querySelectPart + querySubSelectPart + queryGroupPart + queryHavingPart + queryOrderPart + queryFetchPart);
					statement.setInt(1, tournament.getId());

					final ResultSet result = statement.executeQuery();
					while (result.next()) {
						final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), result.getInt(3), 0, 0);
						total.totalScore = result.getInt(4);
						total.numberOfGame = result.getInt(5);
						rankingScores.add(total);
					}
					result.close();
					statement.close();
				}
					break;
				case TRIMESTRIAL_SCORE: {
					final String querySelectPart = "SELECT name, display_name, y, t, SUM(final_score) AS total, COUNT(*) AS nb_games FROM";
					final String querySubSelectPart = " (SELECT player.name, player.display_name, YEAR(rcr_game_id.date) as Y, (MONTH(rcr_game_id.date)-1)/3 as t, rcr_game_score.final_score FROM player, rcr_game_id, rcr_game_score WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?) AS trimester_score";
					final String queryGroupPart = " GROUP BY name, display_name, y, t";
					final String queryHavingPart = " HAVING COUNT(*)>=" + Integer.toString(MINIMUM_GAME_TRIMESTER);
					final String queryOrderPart = sortingMode == EnumSortingMode.DESCENDING ? " ORDER BY total DESC" : " ORDER BY total ASC";
					final String queryFetchPart = " FETCH FIRST " + Integer.toString(NUMBER_TOP) + " ROWS ONLY";
					final PreparedStatement statement = dataBaseConnection.prepareStatement(querySelectPart + querySubSelectPart + queryGroupPart + queryHavingPart + queryOrderPart + queryFetchPart);
					statement.setInt(1, tournament.getId());

					final ResultSet result = statement.executeQuery();
					while (result.next()) {
						final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), result.getInt(3), result.getInt(4), 0);
						total.totalScore = result.getInt(5);
						total.numberOfGame = result.getInt(6);
						rankingScores.add(total);
					}
					result.close();
					statement.close();
				}
					break;
				case MENSUAL_SCORE: {
					final String querySelectPart = "SELECT name, display_name, y, m, SUM(final_score) AS total, COUNT(*) AS nb_games FROM";
					final String querySubSelectPart = " (SELECT player.name, player.display_name, YEAR(rcr_game_id.date) as y, MONTH(rcr_game_id.date)-1 as m, rcr_game_score.final_score FROM player, rcr_game_id, rcr_game_score WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?) AS month_score";
					final String queryGroupPart = " GROUP BY name, display_name, y, m";
					final String queryHavingPart = " HAVING COUNT(*)>=" + Integer.toString(MINIMUM_GAME_MONTH);
					final String queryOrderPart = sortingMode == EnumSortingMode.DESCENDING ? " ORDER BY total DESC" : " ORDER BY total ASC";
					final String queryFetchPart = " FETCH FIRST " + Integer.toString(NUMBER_TOP) + " ROWS ONLY";
					final PreparedStatement statement = dataBaseConnection.prepareStatement(querySelectPart + querySubSelectPart + queryGroupPart + queryHavingPart + queryOrderPart + queryFetchPart);
					statement.setInt(1, tournament.getId());

					final ResultSet result = statement.executeQuery();
					while (result.next()) {
						final RCRTotalScore total = new RCRTotalScore(result.getString(1), result.getString(2), result.getInt(3), result.getInt(4), 0);
						total.totalScore = result.getInt(5);
						total.numberOfGame = result.getInt(6);
						rankingScores.add(total);
					}
					result.close();
					statement.close();
				}
					break;
				default:
					break;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return rankingScores;
	}

	@Override
	public RCRDataPackageTrend getRCRDataPackageTrend(final Tournament tournament, final EnumPeriodMode periodMode, final int year, final int trimester, final int month) {
		final Calendar calendarFrom = Calendar.getInstance();
		final Calendar calendarTo = Calendar.getInstance();
		switch (periodMode) {
			case MONTH:
				calendarFrom.set(Calendar.YEAR, year);
				calendarFrom.set(Calendar.MONTH, month);
				calendarFrom.set(Calendar.DAY_OF_MONTH, 1);
				if (month == 11) {
					calendarTo.set(Calendar.YEAR, year + 1);
					calendarTo.set(Calendar.MONTH, 0);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				} else {
					calendarTo.set(Calendar.YEAR, year);
					calendarTo.set(Calendar.MONTH, month + 1);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				}
				break;
			case TRIMESTER:
				calendarFrom.set(Calendar.YEAR, year);
				calendarFrom.set(Calendar.MONTH, trimester * 3);
				calendarFrom.set(Calendar.DAY_OF_MONTH, 1);
				if (trimester == 3) {
					calendarTo.set(Calendar.YEAR, year + 1);
					calendarTo.set(Calendar.MONTH, 0);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				} else {
					calendarTo.set(Calendar.YEAR, year);
					calendarTo.set(Calendar.MONTH, (trimester + 1) * 3);
					calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				}
				break;
			case YEAR:
				calendarFrom.set(Calendar.YEAR, year);
				calendarFrom.set(Calendar.MONTH, 0);
				calendarFrom.set(Calendar.DAY_OF_MONTH, 1);
				calendarTo.set(Calendar.YEAR, year + 1);
				calendarTo.set(Calendar.MONTH, 0);
				calendarTo.set(Calendar.DAY_OF_MONTH, 1);
				break;
			case ALL:
				break;
			default:
				break;
		}

		try {
			final List<Integer> playerIDs = new ArrayList<Integer>();
			final List<String> playerNames = new ArrayList<String>();
			final List<String> displayNames = new ArrayList<String>();
			final Map<Integer, Integer> mapId2Index = new HashMap<Integer, Integer>();
			{
				final String querySelectPart = "SELECT DISTINCT player.id, player.name, player.display_name FROM player, rcr_game_id, rcr_game_score";
				final String queryWherePart = " WHERE player.id=rcr_game_score.player_id AND rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?";
				final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
				final String queryOrderPart = " ORDER BY player.id";
				PreparedStatement statement = null;
				if (periodMode == EnumPeriodMode.ALL) {
					statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryOrderPart);
					statement.setInt(1, tournament.getId());
				} else {
					statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryOrderPart);
					statement.setInt(1, tournament.getId());
					statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
					statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
				}
				final ResultSet result = statement.executeQuery();
				int index = 0;
				while (result.next()) {
					final int id = result.getInt(1);
					playerIDs.add(index, id);
					playerNames.add(index, result.getString(2));
					displayNames.add(index, result.getString(3));
					mapId2Index.put(id, index);
					index++;
				}
				result.close();
				statement.close();
			}
			final List<Long> dates = new ArrayList<Long>();
			final List<List<Integer>> data = new ArrayList<List<Integer>>(playerIDs.size());
			for (int index = 0; index < playerIDs.size(); index++) {
				final List<Integer> score = new ArrayList<Integer>();
				score.add(0, 0);
				data.add(index, score);
			}
			{
				final String querySelectPart = "SELECT rcr_game_id.date, rcr_game_score.player_id, SUM(rcr_game_score.final_score) FROM rcr_game_id, rcr_game_score";
				final String queryWherePart = " WHERE rcr_game_id.id=rcr_game_score.rcr_game_id AND rcr_game_id.rcr_tournament_id=?";
				final String queryPeriodPart = " AND rcr_game_id.date>=? AND rcr_game_id.date<?";
				final String queryGroupPart = " GROUP BY rcr_game_id.date, rcr_game_score.player_id";
				final String queryOrderPart = " ORDER BY rcr_game_id.date";
				PreparedStatement statement = null;
				if (periodMode == EnumPeriodMode.ALL) {
					statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryGroupPart + queryOrderPart);
					statement.setInt(1, tournament.getId());
				} else {
					statement = dataBaseConnection.prepareStatement(querySelectPart + queryWherePart + queryPeriodPart + queryGroupPart + queryOrderPart);
					statement.setInt(1, tournament.getId());
					statement.setDate(2, new Date(calendarFrom.getTimeInMillis()));
					statement.setDate(3, new Date(calendarTo.getTimeInMillis()));
				}

				int dateIndex = 0;
				long lastDate = 0;
				long date;
				dates.add(dateIndex, lastDate);
				final ResultSet result = statement.executeQuery();
				while (result.next()) {
					date = result.getDate(1).getTime();
					if (date != lastDate) {
						for (int playerIndex = 0; playerIndex < data.size(); playerIndex++) {
							final List<Integer> playerScore = data.get(playerIndex);
							playerScore.add(playerScore.get(dateIndex));
						}
						lastDate = date;
						dateIndex++;
						dates.add(dateIndex, lastDate);
					}
					final int playerIndex = mapId2Index.get(result.getInt(2));
					final List<Integer> playerScore = data.get(playerIndex);
					playerScore.set(dateIndex, playerScore.get(dateIndex) + result.getInt(3));
				}
			}
			return new RCRDataPackageTrend(dates, playerNames, displayNames, data);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
