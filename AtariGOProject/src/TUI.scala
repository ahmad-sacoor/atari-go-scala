import Types._
import Stone._
import scala.annotation.tailrec
import Project._

object TUI {

  case class Config(
                     boardSize: Int,
                     captureGoal: Int,
                     maxMoveTimeMillis: Int
                   )


  def printBoard(board: Board): Unit = {
    def boardToString(board: Board): String = {
      def stoneToChar(stone: Stone): String = stone match {
        case Black => "B"
        case White => "W"
        case Empty => "."
      }

      def rowToString(row: List[Stone]): String =
        row.foldLeft("") {
          case ("", stone) => stoneToChar(stone)
          case (acc, stone) => acc + " " + stoneToChar(stone)
        }

      board.foldLeft("") {
        case ("", row) => rowToString(row)
        case (acc, row) => acc + "\n" + rowToString(row)
      } + "\n"
    }

    // Here's the fix: actually print the string
    println(boardToString(board))
  }


  @tailrec
  def mainMenuLoop(config: Config): Unit = {
    println("========= AtariGo =========")
    println(s"1. Start Game")
    println(s"2. Change Board Size (current: ${config.boardSize})")
    println(s"3. Set Capture Goal (current: ${config.captureGoal})")
    println(s"4. Set Time Limit per Turn (current: ${config.maxMoveTimeMillis / 1000} seconds)")
    println("5. Quit")
    println("============================")
    println("Choose an option:")

    scala.io.StdIn.readLine().trim match {
      case "1" =>
        val board = List.fill(config.boardSize, config.boardSize)(Empty)
        val coords = Project.generateAllCoords(config.boardSize)
        val rng = SeedManager.getNextRandom()
        runGame(board, coords, rng, Black, 0, 0, Nil, config)
        mainMenuLoop(config)

      case "2" =>
        println("Enter new board size:")
        val size = scala.io.StdIn.readLine().trim.toIntOption.getOrElse(config.boardSize)
        mainMenuLoop(config.copy(boardSize = math.max(2, size)))

      case "3" =>
        println("Enter new capture goal:")
        val goal = scala.io.StdIn.readLine().trim.toIntOption.getOrElse(config.captureGoal)
        mainMenuLoop(config.copy(captureGoal = math.max(1, goal)))

      case "4" =>
        println("Enter time limit per turn (in seconds):")
        val time = scala.io.StdIn.readLine().trim.toIntOption.getOrElse(config.maxMoveTimeMillis / 1000)
        mainMenuLoop(config.copy(maxMoveTimeMillis = math.max(1, time) * 1000))

      case "5" => println("Goodbye!")

      case _ =>
        println("Invalid option. Please try again.")
        mainMenuLoop(config)
    }
  }

  @tailrec
  def runGame(
               board: Board,
               openCoords: List[Coord2D],
               rng: MyRandom,
               currentPlayer: Stone,
               playerCaptured: Int,
               computerCaptured: Int,
               history: List[GameState],
               config: Config
             ): Unit = {
    printBoard(board)
    val startTime = System.currentTimeMillis()

    val (undoRequested, newBoard, newOpenCoords, newRng) =
      if (currentPlayer == Black) handleHumanTurn(board, openCoords, rng, config)
      else {
        val (nb, no, nr) = handleRandomTurn(board, openCoords, rng)
        (false, nb, no, nr)
      }

    val elapsed = System.currentTimeMillis() - startTime
    if (currentPlayer == Black && elapsed > config.maxMoveTimeMillis) {
      println(s"Time's up! You took ${elapsed / 1000} seconds. Move skipped.")
      runGame(board, openCoords, rng, White, playerCaptured, computerCaptured, history, config)
    }
    else if (undoRequested) {
      history match {
        case _ :: prev :: rest =>
          println("Undo completed.")
          runGame(prev.board, prev.openCoords, prev.rng, Black,
            prev.playerCaptured, prev.computerCaptured, rest, config)
        case last :: Nil =>
          println("Undo completed.")
          runGame(last.board, last.openCoords, last.rng, Black,
            last.playerCaptured, last.computerCaptured, Nil, config)
        case Nil =>
          println("Nothing to undo.")
          runGame(board, openCoords, rng, currentPlayer,
            playerCaptured, computerCaptured, history, config)
      }
    }
    else {
      val (capturedBoard, capturedCount) = captureGroupStones(newBoard, currentPlayer)
      val updatedOpenCoords = Project.updateOpenCoordsAfterCapture(board, capturedBoard, newOpenCoords)

      if (capturedCount > 0)
        println(s"$capturedCount stone(s) captured by $currentPlayer!")

      val (newPlayerCaptured, newComputerCaptured) =
        if (currentPlayer == Black) (playerCaptured + capturedCount, computerCaptured)
        else (playerCaptured, computerCaptured + capturedCount)

      checkWin(newPlayerCaptured, newComputerCaptured, config.captureGoal) match {
        case "player" =>
          printBoard(capturedBoard)
          println("You won!")
        case "computer" =>
          printBoard(capturedBoard)
          println("Computer won!")
        case _ =>
          val currentState = GameState(board, openCoords, rng, playerCaptured, computerCaptured)
          runGame(capturedBoard, updatedOpenCoords, newRng,
            if (currentPlayer == Black) White else Black,
            newPlayerCaptured, newComputerCaptured, currentState :: history, config)
      }
    }
  }

  private def handleHumanTurn(
                               board: Board,
                               openCoords: List[Coord2D],
                               rng: MyRandom,
                               config: Config
                             ): (Boolean, Board, List[Coord2D], MyRandom) = {
    println("Enter two digits (e.g., 02) or 'U' to undo:")
    println(s"Note: you only have ${config.maxMoveTimeMillis / 1000} seconds!")
    val input = scala.io.StdIn.readLine().trim.toUpperCase

    if (input == "U") (true, board, openCoords, rng)
    else if (input.length == 2 && input.forall(_.isDigit)) {
      val row = input.charAt(0).asDigit
      val col = input.charAt(1).asDigit
      val coord = (row, col)

      if (!openCoords.contains(coord)) {
        println("That position is already occupied. Try again.")
        handleHumanTurn(board, openCoords, rng, config)
      } else if (wouldCaptureSelf(board, Black, coord)) {
        println("Invalid,that move would result in immediate capture. Try again.")
        handleHumanTurn(board, openCoords, rng, config)
      } else {
        play(board, Black, coord, openCoords) match {
          case (Some(newBoard), newOpen) => (false, newBoard, newOpen, rng)
          case _ =>
            println("Unexpected error playing move.")
            handleHumanTurn(board, openCoords, rng, config)
        }
      }
    } else {
      println("Invalid input. Try again.")
      handleHumanTurn(board, openCoords, rng, config)
    }
  }

  private def handleRandomTurn(
                                board: Board,
                                openCoords: List[Coord2D],
                                rng: MyRandom
                              ): (Board, List[Coord2D], MyRandom) = {
    val (newBoard, newRng, newOpen) = Project.playRandomly(board, rng, White, openCoords, randomMove)
    (newBoard, newOpen, newRng)
  }



}

