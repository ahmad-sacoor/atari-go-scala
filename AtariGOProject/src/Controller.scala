import javafx.fxml.FXML
import javafx.scene.control._
import javafx.scene.layout.Pane
import javafx.scene.input.MouseEvent
import javafx.scene.shape.{Circle, Line}
import javafx.scene.paint.Color
import Types._
import Stone._
import Project._

class Controller {

  @FXML var boardPane: Pane = _
  @FXML var statusLabel1: Label = _
  @FXML var statusLabel2: Label = _
  @FXML var restartButton: Button = _
  @FXML var undoButton: Button = _
  @FXML var startButton: Button = _
  @FXML var boardSizeSpinner: Spinner[Integer] = _
  @FXML var captureGoalField: TextField = _
  @FXML var timeLimitField: TextField = _

  private var boardSize = 5
  private var captureGoal = 5
  private var timeLimit = 2000
  private var cellSize = 60.0
  private val offset = cellSize

  private var board: Board = _
  private var coords: List[Coord2D] = _
  private var rng = SeedManager.getNextRandom()
  private var history: List[GameState] = Nil
  private var playerCaptured = 0
  private var computerCaptured = 0
  private var gameOver = false
  private var playerTurnStartTime: Long = 0

  @FXML
  def initialize(): Unit = {
    boardSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 9, 5))
    captureGoalField.setText("5")
    timeLimitField.setText("15")
    boardPane.setOnMouseClicked(handleClick)
  }

  @FXML
  def onStartGame(): Unit = {
    boardSize = boardSizeSpinner.getValue
    captureGoal = captureGoalField.getText.toIntOption.getOrElse(5)
    timeLimit = timeLimitField.getText.toIntOption.getOrElse(15) * 1000

    board = List.fill(boardSize, boardSize)(Stone.Empty)
    coords = Project.generateAllCoords(boardSize)
    rng = SeedManager.getNextRandom()
    history = Nil
    playerCaptured = 0
    computerCaptured = 0
    gameOver = false
    drawGrid()
    renderStones()
    statusLabel1.setText("Score - You: 0 / CPU: 0")
    statusLabel2.setText("Game started!")
    playerTurnStartTime = System.currentTimeMillis()
  }

  def drawGrid(): Unit = {
    boardPane.getChildren.clear()
    val fullSize = offset * 2 + cellSize * (boardSize - 1)
    boardPane.setPrefWidth(fullSize)
    boardPane.setPrefHeight(fullSize)

    for (i <- 0 until boardSize) {
      val hLine = new Line(offset, offset + i * cellSize, offset + (boardSize - 1) * cellSize, offset + i * cellSize)
      val vLine = new Line(offset + i * cellSize, offset, offset + i * cellSize, offset + (boardSize - 1) * cellSize)
      hLine.setStroke(Color.BLACK)
      vLine.setStroke(Color.BLACK)
      boardPane.getChildren.addAll(hLine, vLine)
    }
  }

  def renderStones(): Unit = {
    boardPane.getChildren.removeIf(_.isInstanceOf[Circle])
    for {
      i <- board.indices
      j <- board(i).indices
      stone = board(i)(j) if stone != Stone.Empty
    } {
      val cx = offset + j * cellSize
      val cy = offset + i * cellSize
      val circle = new Circle(cx, cy, cellSize * 0.3)
      circle.setFill(stone match {
        case Stone.Black => Color.BLACK
        case Stone.White => Color.WHITE
        case _ => Color.TRANSPARENT
      })
      boardPane.getChildren.add(circle)
    }
  }

  def isValidClick(coord: Coord2D): Boolean =
    coords.contains(coord) && !wouldCaptureSelf(board, Stone.Black, coord)

  def applyPlayerMove(coord: Coord2D): Boolean = {
    Project.play(board, Stone.Black, coord, coords) match {
      case (Some(newBoard), newCoords) =>
        history = GameState(board, coords, rng, playerCaptured, computerCaptured) :: history
        val (afterPlayerCapture, capturedByPlayer) = Project.captureGroupStones(newBoard, Stone.Black)
        playerCaptured += capturedByPlayer
        val updatedCoordsAfterPlayer = Project.updateOpenCoordsAfterCapture(newBoard, afterPlayerCapture, newCoords)

        board = afterPlayerCapture
        coords = updatedCoordsAfterPlayer
        renderStones()

        if (playerCaptured >= captureGoal) {
          statusLabel1.setText(s"Score - You: $playerCaptured / CPU: $computerCaptured")
          statusLabel2.setText("You won! Click Start Game to play again")
          gameOver = true
          return false
        }

        true

      case _ =>
        statusLabel2.setText("Invalid move.")
        false
    }
  }

  def handleClick(event: MouseEvent): Unit = {
    if (gameOver || board == null) return

    val now = System.currentTimeMillis()
    if (now - playerTurnStartTime > timeLimit) {
      playComputerTurn()
      statusLabel2.setText("Time's up! CPU is playing...")
      return
    }

    val x = event.getX
    val y = event.getY
    val col = ((x - offset + cellSize / 2) / cellSize).toInt
    val row = ((y - offset + cellSize / 2) / cellSize).toInt
    val coord = (row, col)

    if (!isValidClick(coord)) {
      statusLabel2.setText("Invalid,that move would result in immediate capture. Try again. ")
      return
    }

    if (applyPlayerMove(coord)) {
      playComputerTurn()
    }

    playerTurnStartTime = System.currentTimeMillis()
  }

  def playComputerTurn(): Unit = {
    val (cpuBoard, newRng, updatedCoords) =
      Project.playRandomly(board, rng, Stone.White, coords, Project.randomMove)
    val (afterCpuCapture, capturedByCpu) = Project.captureGroupStones(cpuBoard, Stone.White)
    computerCaptured += capturedByCpu
    board = afterCpuCapture
    coords = Project.updateOpenCoordsAfterCapture(cpuBoard, afterCpuCapture, updatedCoords)
    rng = newRng
    renderStones()

    if (computerCaptured >= captureGoal) {
      statusLabel2.setText("Computer won! Click Start Game to play again")
      gameOver = true
    } else {
      statusLabel1.setText(s"Score - You: $playerCaptured / CPU: $computerCaptured")
      statusLabel2.setText("")
    }

    playerTurnStartTime = System.currentTimeMillis()
  }

  @FXML
  def onUndo(): Unit = {
    history match {
      case last :: rest =>
        board = last.board
        coords = last.openCoords
        rng = last.rng
        playerCaptured = last.playerCaptured
        computerCaptured = last.computerCaptured
        history = rest
        gameOver = false
        renderStones()
        statusLabel1.setText(s"Score - You: $playerCaptured / CPU: $computerCaptured")
        statusLabel2.setText("Undo completed.")
        playerTurnStartTime = System.currentTimeMillis()
      case Nil =>
        statusLabel2.setText("Nothing to undo.")
    }
  }

  @FXML
  def onRestart(): Unit = onStartGame()
}
